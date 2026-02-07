package co.adityarajput.fileflow.services

import android.content.Context
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.utils.pathToFile
import kotlinx.coroutines.flow.first

class FlowExecutor(private val context: Context) {
    private val repository by lazy { AppContainer(context).repository }

    suspend fun run(rules: List<Rule>? = null) {
        val resolver = context.contentResolver

        for (rule in rules ?: repository.rules().first()) {
            Logger.d("FlowExecutor", "Executing $rule")

            if (!rule.enabled || rule.action !is Action.MOVE) continue

            val regex = Regex(rule.action.srcFileNamePattern)
            val destDir = context.pathToFile(rule.action.dest)

            if (destDir == null) {
                Logger.e("FlowExecutor", "${rule.action.dest} is invalid")
                continue
            }

            for (srcFile in context.pathToFile(rule.action.src)?.listFiles() ?: arrayOf()) {
                if (!srcFile.isFile || srcFile.name == null || !regex.matches(srcFile.name!!)) continue

                resolver.openInputStream(srcFile.uri).use { src ->
                    if (src == null) {
                        Logger.e("FlowExecutor", "Failed to open ${srcFile.name}")
                        continue
                    }

                    val destFileName = regex.replace(
                        srcFile.name!!,
                        rule.action.destFileNameTemplate,
                    )
                    var destFile = destDir.listFiles()
                        .filter { it.isFile }
                        .firstOrNull { it.name == destFileName }

                    if (destFile != null) {
                        if (!rule.action.overwriteExisting) {
                            Logger.e("FlowExecutor", "$destFileName already exists")
                            continue
                        }

                        resolver.openInputStream(destFile.uri).use { dest ->
                            if (dest == null) {
                                Logger.e("FlowExecutor", "Failed to open existing destination file")
                                continue
                            }

                            if (src.readBytes().contentEquals(dest.readBytes())) {
                                Logger.i(
                                    "FlowExecutor",
                                    "Source and destination files are identical",
                                )
                                continue
                            }
                        }

                        Logger.i("FlowExecutor", "Deleting existing $destFileName")
                        destFile.delete()
                    }

                    destFile = destDir.createFile(
                        srcFile.type ?: "application/octet-stream",
                        destFileName,
                    )

                    if (destFile == null) {
                        Logger.e("FlowExecutor", "Failed to create $destFileName")
                        continue
                    }

                    resolver.openOutputStream(destFile.uri).use { dest ->
                        if (dest == null) {
                            Logger.e("FlowExecutor", "Failed to open $destFileName")
                            continue
                        }

                        Logger.i("FlowExecutor", "Copying ${srcFile.name} to ${destFile.name}")
                        src.copyTo(dest)
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
