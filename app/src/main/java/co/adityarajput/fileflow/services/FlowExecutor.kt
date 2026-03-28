package co.adityarajput.fileflow.services

import android.content.Context
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.utils.File
import co.adityarajput.fileflow.utils.FileSuperlative
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.utils.isDebugBuild
import java.nio.file.FileAlreadyExistsException

class FlowExecutor(private val context: Context) {
    suspend fun run(ruleId: Int) {
        val repository = AppContainer(context).repository

        val rule = repository.rule(ruleId)
        if (rule == null || !rule.enabled) return

        Logger.d("FlowExecutor", "Executing $rule")

        when (rule.action) {
            is Action.MOVE -> {
                val destDir = File.fromPath(context, rule.action.dest)
                if (destDir == null) {
                    Logger.e("FlowExecutor", "${rule.action.dest} is invalid")
                    return
                }

                val srcFiles = File.fromPath(context, rule.action.src)
                    ?.listChildren(rule.action.scanSubdirectories)
                    ?.filter {
                        it.isFile
                                && it.name != null
                                && Regex(rule.action.srcFileNamePattern).matches(it.name!!)
                    }
                    ?.let {
                        if (rule.action.superlative == FileSuperlative.NONE) it else
                            listOf(it.maxByOrNull(rule.action.superlative.selector) ?: return)
                    } ?: return

                for (srcFile in srcFiles) {
                    val srcFileName = srcFile.name ?: continue
                    val destFileName = rule.action.getDestFileName(srcFile)

                    val relativePath = srcFile.parent!!.pathRelativeTo(rule.action.src)
                    val destSubDir =
                        if (!rule.action.preserveStructure || relativePath == null) destDir
                        else destDir.createDirectory(relativePath)
                    if (destSubDir == null) {
                        Logger.e(
                            "FlowExecutor",
                            "Failed to create subdirectory in ${destDir.path}",
                        )
                        continue
                    }

                    if (
                        destSubDir
                            .listChildren(false)
                            .firstOrNull { it.isFile && it.name == destFileName }
                            ?.isIdenticalTo(srcFile, context)
                        == true
                    ) {
                        Logger.i(
                            "FlowExecutor",
                            "Source and destination files are identical",
                        )
                        continue
                    }

                    try {
                        Logger.i(
                            "FlowExecutor",
                            "Moving $srcFileName to ${destSubDir.path}/$destFileName",
                        )
                        srcFile.moveTo(
                            destSubDir,
                            destFileName,
                            rule.action.keepOriginal,
                            rule.action.overwriteExisting,
                            context,
                        )
                    } catch (e: FileAlreadyExistsException) {
                        Logger.e("FlowExecutor", "$destFileName already exists", e)
                        continue
                    } catch (e: Exception) {
                        Logger.e("FlowExecutor", "Failed to move $srcFileName", e)
                        continue
                    }

                    repository.registerExecution(rule, Execution(srcFileName, rule.action.verb))
                }
            }

            is Action.DELETE_STALE -> {
                val srcFiles = File.fromPath(context, rule.action.src)
                    ?.listChildren(rule.action.scanSubdirectories)
                    ?.filter {
                        it.isFile
                                && it.name != null
                                && Regex(rule.action.srcFileNamePattern).matches(it.name!!)
                    }
                    ?.filter {
                        System.currentTimeMillis() - it.lastModified() >=
                                // INFO: While debugging, treat days as seconds
                                if (context.isDebugBuild()) rule.action.retentionDays * 1000L
                                else rule.action.retentionTimeInMillis()
                    }
                    ?: return

                for (srcFile in srcFiles) {
                    val srcFileName = srcFile.name ?: continue
                    Logger.i("FlowExecutor", "Deleting $srcFileName")

                    val result = srcFile.delete()
                    if (!result) {
                        Logger.e("FlowExecutor", "Failed to delete $srcFileName")
                        continue
                    }

                    repository.registerExecution(rule, Execution(srcFileName, rule.action.verb))
                }
            }
        }
    }
}
