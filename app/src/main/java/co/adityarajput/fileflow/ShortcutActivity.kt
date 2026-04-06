package co.adityarajput.fileflow

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sendBroadcast(
            Intent(Constants.ACTION_EXECUTE_GROUP).apply {
                setPackage(packageName)
                putExtra(Constants.EXTRA_GROUP_ID, intent.getIntExtra(Constants.EXTRA_GROUP_ID, -1))
            },
        )
        finish()
    }
}
