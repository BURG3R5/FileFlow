package co.adityarajput.fileflow.views.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.edit
import androidx.core.net.toUri
import co.adityarajput.fileflow.Constants.BRIGHTNESS
import co.adityarajput.fileflow.Constants.SETTINGS
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.utils.hasUnrestrictedBackgroundUsagePermission
import co.adityarajput.fileflow.viewmodels.AppearanceViewModel
import co.adityarajput.fileflow.views.Brightness
import co.adityarajput.fileflow.views.components.AppBar
import kotlinx.coroutines.launch

@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(
    goToAboutScreen: () -> Unit = {},
    goBack: () -> Unit = {},
    viewModel: AppearanceViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val handler = remember { Handler(Looper.getMainLooper()) }
    val sharedPreferences =
        remember { context.getSharedPreferences(SETTINGS, MODE_PRIVATE) }

    var isInvincible by remember {
        mutableStateOf(context.hasUnrestrictedBackgroundUsagePermission())
    }

    val watcher = object : Runnable {
        override fun run() {
            isInvincible = context.hasUnrestrictedBackgroundUsagePermission()
            handler.postDelayed(this, 1000)
        }
    }
    DisposableEffect(Unit) {
        handler.post(watcher)
        onDispose { handler.removeCallbacksAndMessages(null) }
    }

    Scaffold(
        topBar = { AppBar(stringResource(R.string.settings), true, goBack) },
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.padding_small)),
                Arrangement.Top,
                Alignment.CenterHorizontally,
            ) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.padding_small)),
                ) {
                    Text(
                        stringResource(R.string.settings_section_1),
                        Modifier.padding(
                            dimensionResource(R.dimen.padding_large),
                            dimensionResource(R.dimen.padding_medium),
                        ),
                        fontWeight = FontWeight.Medium,
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = dimensionResource(R.dimen.padding_large),
                                end = dimensionResource(R.dimen.padding_large),
                                bottom = dimensionResource(R.dimen.padding_medium),
                            ),
                        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                        Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.disable_battery_optimization),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stringResource(R.string.explain_disabling_battery_optimization),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            isInvincible,
                            {
                                if (it) {
                                    val intent = Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        "package:${context.packageName}".toUri(),
                                    )
                                    context.startActivity(intent)
                                } else {
                                    val intent =
                                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            },
                        )
                    }
                }
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.padding_small)),
                ) {
                    Text(
                        stringResource(R.string.settings_section_2),
                        Modifier.padding(
                            dimensionResource(R.dimen.padding_large),
                            dimensionResource(R.dimen.padding_medium),
                        ),
                        fontWeight = FontWeight.Medium,
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = dimensionResource(R.dimen.padding_large),
                                end = dimensionResource(R.dimen.padding_large),
                                bottom = dimensionResource(R.dimen.padding_medium),
                            ),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.app_theme),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        SingleChoiceSegmentedButtonRow {
                            Brightness.entries.forEachIndexed { i, b ->
                                SegmentedButton(
                                    i == viewModel.brightness.ordinal,
                                    {
                                        sharedPreferences.edit { putInt(BRIGHTNESS, i) }
                                        viewModel.brightness = Brightness.entries[i]
                                    },
                                    SegmentedButtonDefaults.itemShape(i, 3),
                                    label = {
                                        Icon(
                                            painterResource(b.icon),
                                            stringResource(b.description),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.padding_small)),
                ) {
                    val copySuccess = stringResource(R.string.copy_success)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_medium),
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_small),
                            )
                            .clickable {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipData.newPlainText(
                                            "logs",
                                            Logger.logs.joinToString("\n"),
                                        ).toClipEntry(),
                                    )
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                                        Toast
                                            .makeText(context, copySuccess, Toast.LENGTH_SHORT)
                                            .show()
                                }
                            },
                        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                    ) {
                        Icon(
                            painterResource(R.drawable.list_alt),
                            stringResource(R.string.alttext_logs),
                        )
                        Text(
                            stringResource(R.string.copy_logs),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_small),
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_medium),
                            )
                            .clickable { goToAboutScreen() },
                        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                    ) {
                        Icon(
                            painterResource(R.drawable.info),
                            stringResource(R.string.alttext_info),
                        )
                        Text(
                            stringResource(R.string.about_app),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
