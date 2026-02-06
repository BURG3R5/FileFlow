package co.adityarajput.fileflow

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.services.Worker
import co.adityarajput.fileflow.utils.isDebugBuild
import java.util.concurrent.TimeUnit

class FileFlowApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()

        container = AppContainer(this)

        // INFO: While debugging, populate database with demo data for screenshots
        if (isDebugBuild()) {
            container.seedDemoData()
        }

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.WORKER_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<Worker>(
                // INFO: While debugging, use a shorter interval
                if (isDebugBuild()) 15 else 60,
                TimeUnit.MINUTES,
            ).build(),
        )
    }
}
