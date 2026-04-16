package co.adityarajput.fileflow

import android.app.Application
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.utils.Crypto
import co.adityarajput.fileflow.utils.isDebugBuild
import co.adityarajput.fileflow.utils.scheduleWork
import co.adityarajput.fileflow.utils.upsertShortcuts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.Security

class FileFlowApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()

        container = AppContainer(this)

        // INFO: While debugging, populate database with demo data for screenshots
        if (isDebugBuild()) {
            container.seedDemoData()
        }

        Crypto.init(this)
        Security.removeProvider("BC")

        CoroutineScope(Dispatchers.IO).launch {
            scheduleWork()
            upsertShortcuts()
        }
    }
}
