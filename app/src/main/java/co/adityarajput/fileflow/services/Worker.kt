package co.adityarajput.fileflow.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class Worker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        FlowExecutor(context).run()
        return Result.success()
    }
}
