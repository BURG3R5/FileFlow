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

            val srcFile = context.pathToFile(rule.action.src)?.listFiles()
                ?.filter { it.isFile && it.name != null && regex.matches(it.name!!) }
                ?.maxByOrNull(rule.action.superlative.selector)
                ?: continue

            val destFileName = regex.replace(srcFile.name!!, rule.action.destFileNameTemplate)
            var destFile = destDir.listFiles().firstOrNull { it.isFile && it.name == destFileName }

            if (destFile != null) {
                if (!rule.action.overwriteExisting) {
                    Logger.e("FlowExecutor", "${destFile.name} already exists")
                    continue
                }

                resolver.openInputStream(srcFile.uri).use { src ->
                    resolver.openInputStream(destFile.uri).use { dest ->
                        if (src == null || dest == null) {
                            Logger.e("FlowExecutor", "Failed to open file(s)")
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
                }

                Logger.i("FlowExecutor", "Deleting existing ${destFile.name}")
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

            resolver.openInputStream(srcFile.uri).use { src ->
                resolver.openOutputStream(destFile.uri).use { dest ->
                    if (src == null || dest == null) {
                        Logger.e("FlowExecutor", "Failed to open file(s)")
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
