package co.adityarajput.fileflow

import android.app.Application
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.services.Preferences
import co.adityarajput.fileflow.utils.Crypto
import co.adityarajput.fileflow.utils.scheduleWork
import co.adityarajput.fileflow.utils.upsertShortcuts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.acra.ACRA
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import java.security.Security

class FileFlowApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()

        if (ACRA.isACRASenderServiceProcess())
            return

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON

            mailSender {
                mailTo = Constants.CRASH_REPORT_EMAIL
                subject = "FileFlow Crash Report"
                additionalSharedPreferences = listOf(Constants.STATE, Constants.SETTINGS)
            }

            dialog {
                title = "App Crashed"
                text =
                    "FileFlow has encountered an unexpected error and crashed. Please report this incident to the developers using the following form."
                commentPrompt = "Your comments:"
                positiveButtonText = "Send email"
            }
        }

        Preferences.init(this)

        container = AppContainer(this)

        // INFO: While debugging, populate database with demo data for screenshots
        if (BuildConfig.DEBUG) {
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
