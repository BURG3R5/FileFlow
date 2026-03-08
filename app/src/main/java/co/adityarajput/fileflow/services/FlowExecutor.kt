package co.adityarajput.fileflow.services

import android.content.Context
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.*
import kotlinx.coroutines.flow.first

class FlowExecutor(private val context: Context) {
    private val repository by lazy { AppContainer(context).repository }

    suspend fun run(rules: List<Rule>? = null) {
        for (rule in rules ?: repository.rules().first()) {
            Logger.d("FlowExecutor", "Executing $rule")

            if (!rule.enabled) continue

            val regex = Regex(rule.action.srcFileNamePattern)

            when (rule.action) {
                is Action.MOVE -> {
                    val destDir = File.fromPath(context, rule.action.dest)

                    if (destDir == null) {
                        Logger.e("FlowExecutor", "${rule.action.dest} is invalid")
                        continue
                    }

                    val srcFiles = File.fromPath(context, rule.action.src)?.listFiles()
                        ?.filter { it.isFile && it.name != null && regex.matches(it.name!!) }
                        ?.let {
                            if (rule.action.superlative != FileSuperlative.NONE)
                                listOf(it.maxByOrNull(rule.action.superlative.selector) ?: continue)
                            else
                                it
                        } ?: continue

                    for (srcFile in srcFiles) {
                        val destFileName = regex.replace(
                            srcFile.name!!,
                            rule.action.destFileNameTemplate,
                        )
                        var destFile = destDir.listFiles().firstOrNull {
                            it.isFile && it.name == destFileName
                        }

                        if (destFile != null) {
                            if (!rule.action.overwriteExisting) {
                                Logger.e("FlowExecutor", "${destFile.name} already exists")
                                continue
                            }

                            if (srcFile.isIdenticalTo(destFile, context)) {
                                Logger.i(
                                    "FlowExecutor",
                                    "Source and destination files are identical",
                                )
                                continue
                            }


                            Logger.i("FlowExecutor", "Deleting existing ${destFile.name}")
                            destFile.delete()
                        }

                        destFile = destDir.createFile(srcFile.type, destFileName)

                        if (destFile == null) {
                            Logger.e("FlowExecutor", "Failed to create $destFileName")
                            continue
                        }

                        val result = context.copyFile(srcFile, destFile)
                        if (!result) {
                            Logger.e(
                                "FlowExecutor",
                                "Failed to copy ${srcFile.name} to ${destFile.name}",
                            )
                            destFile.delete()
                            continue
                        }

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

                is Action.DELETE_STALE -> {
                    val srcFiles = File.fromPath(context, rule.action.src)?.listFiles()
                        ?.filter { it.isFile && it.name != null && regex.matches(it.name!!) }
                        ?.filter {
                            System.currentTimeMillis() - it.lastModified() >=
                                    // INFO: While debugging, treat days as seconds
                                    if (context.isDebugBuild()) rule.action.retentionDays * 1000L
                                    else rule.action.retentionTimeInMillis()
                        }
                        ?: continue

                    for (srcFile in srcFiles) {
                        Logger.i("FlowExecutor", "Deleting ${srcFile.name}")

                        val result = srcFile.delete()
                        if (!result) {
                            Logger.e("FlowExecutor", "Failed to delete ${srcFile.name}")
                            continue
                        }

                        repository.registerExecution(
                            rule,
                            Execution(srcFile.name!!, rule.action.verb),
                        )
                    }
                }
            }
        }
    }
}
