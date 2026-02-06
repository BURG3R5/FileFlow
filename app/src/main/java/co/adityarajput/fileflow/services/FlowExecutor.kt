package co.adityarajput.fileflow.services

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.Logger
import kotlinx.coroutines.flow.first

class FlowExecutor(private val context: Context) {
    private val repository by lazy { AppContainer(context).repository }

    suspend fun run(rules: List<Rule>? = null) {
        val resolver = context.contentResolver

        for (rule in rules ?: repository.rules().first()) {
            Logger.d("FlowExecutor", "Executing $rule")

            if (!rule.enabled || rule.action !is Action.MOVE) continue

            val regex = Regex(rule.action.srcFileNamePattern)
            val destDir = context.pathToFile(rule.action.dest) ?: continue

            for (srcFile in context.pathToFile(rule.action.src)?.listFiles() ?: arrayOf()) {
                if (!srcFile.isFile || srcFile.name == null || !regex.matches(srcFile.name!!)) continue

                val destFileName = regex.replace(
                    srcFile.name!!,
                    rule.action.destFileNameTemplate,
                )
                val destFiles = destDir.listFiles().filter { it.isFile }
                var destFile = destFiles.firstOrNull { it.name == destFileName }

                if (destFile != null) {
                    if (!rule.action.overwriteExisting) {
                        Logger.e("FlowExecutor", "$destFileName already exists")
                        continue
                    }

                    Logger.i("FlowExecutor", "Deleting existing $destFileName")
                    destFile.delete()
                }

                destFile = destDir.createFile(
                    srcFile.type ?: "application/octet-stream",
                    destFileName,
                ) ?: continue

                resolver.openInputStream(srcFile.uri).use { src ->
                    resolver.openOutputStream(destFile.uri).use { dest ->
                        if (src == null || dest == null) continue

                        src.copyTo(dest)
                        Logger.i("FlowExecutor", "Copied ${srcFile.name} to ${destFile.name}")
                        repository.registerExecution(
                            rule,
                            Execution(srcFile.name!!, rule.action.verb),
                        )
                        if (!rule.action.keepOriginal) {
                            Logger.i("FlowExecutor", "Deleting original ${srcFile.name}")
                            srcFile.delete()
                        }
                    }
                }
            }
        }
    }
}

fun Context.pathToFile(path: String): DocumentFile? = DocumentFile.fromTreeUri(this, path.toUri())
