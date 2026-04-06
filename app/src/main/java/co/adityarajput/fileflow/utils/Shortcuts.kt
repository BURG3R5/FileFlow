package co.adityarajput.fileflow.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.ShortcutActivity
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.data.models.ALL_RULES
import co.adityarajput.fileflow.data.models.Group
import kotlinx.coroutines.flow.first
import kotlin.math.max

suspend fun Context.upsertShortcuts() {
    val groups = AppContainer(this).repository.groups().first().ifEmpty { listOf(ALL_RULES) }

    ShortcutManagerCompat.getDynamicShortcuts(this)
        .filter { it.id !in groups.map(Group::shortcutId) }
        .map { it.id }
        .let {
            ShortcutManagerCompat.disableShortcuts(
                this,
                it,
                getString(R.string.group_edited_or_deleted),
            )
            ShortcutManagerCompat.removeDynamicShortcuts(this, it)
        }

    groups.forEach {
        ShortcutManagerCompat.pushDynamicShortcut(
            this,
            ShortcutInfoCompat.Builder(this, it.shortcutId())
                .setRank(max(0, it.id))
                .setShortLabel("Execute ${it.name}")
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_shortcut))
                .setIntent(
                    Intent(this, ShortcutActivity::class.java).apply {
                        action = Constants.ACTION_EXECUTE_GROUP
                        putExtra(Constants.EXTRA_GROUP_ID, it.id)
                    },
                )
                .build(),
        )
    }
}
