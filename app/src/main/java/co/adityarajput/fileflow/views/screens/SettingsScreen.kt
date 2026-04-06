package co.adityarajput.fileflow.views.screens

import android.content.ClipData
import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import co.adityarajput.fileflow.Constants.BRIGHTNESS
import co.adityarajput.fileflow.Constants.SETTINGS
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.utils.Permission
import co.adityarajput.fileflow.utils.isGranted
import co.adityarajput.fileflow.utils.request
import co.adityarajput.fileflow.viewmodels.AppearanceViewModel
import co.adityarajput.fileflow.views.Brightness
import co.adityarajput.fileflow.views.components.AppBar
import kotlinx.coroutines.launch

private val permissions = listOf(
    Permission.UNRESTRICTED_BACKGROUND_USAGE,
    Permission.MANAGE_EXTERNAL_STORAGE,
)

@Composable
fun SettingsScreen(
    goToGroupsScreen: () -> Unit = {},
    goToLicensesScreen: () -> Unit = {},
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

    var hasPermissions by remember { mutableStateOf(context.isGranted(permissions)) }

    val watcher = object : Runnable {
        override fun run() {
            hasPermissions = context.isGranted(permissions)
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
                    Column(
                        Modifier.padding(
                            dimensionResource(R.dimen.padding_large),
                            dimensionResource(R.dimen.padding_medium),
                        ),
                        Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
                    ) {
                        Text(
                            stringResource(R.string.settings_section_1),
                            fontWeight = FontWeight.Medium,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
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
                                hasPermissions.getValue(Permission.UNRESTRICTED_BACKGROUND_USAGE),
                                { context.request(Permission.UNRESTRICTED_BACKGROUND_USAGE) },
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                            Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.all_files_access),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    stringResource(R.string.explain_all_files_access),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Switch(
                                hasPermissions.getValue(Permission.MANAGE_EXTERNAL_STORAGE),
                                { context.request(Permission.MANAGE_EXTERNAL_STORAGE) },
                            )
                        }
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
                            Modifier.padding(end = dimensionResource(R.dimen.padding_small)),
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
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { goToGroupsScreen() }
                            .padding(
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_medium),
                            ),
                        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                    ) {
                        Icon(
                            painterResource(R.drawable.group),
                            stringResource(R.string.groups),
                        )
                        Text(
                            stringResource(R.string.manage_groups),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.padding_small)),
                ) {
                    val copySuccess = stringResource(R.string.copy_success)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                dimensionResource(R.dimen.padding_large),
                                dimensionResource(R.dimen.padding_medium),
                            ),
                        Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
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
                                stringResource(R.string.logs),
                            )
                            Text(
                                stringResource(R.string.copy_logs),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { goToLicensesScreen() },
                            Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                        ) {
                            Icon(
                                painterResource(R.drawable.license),
                                stringResource(R.string.licenses),
                            )
                            Text(
                                stringResource(R.string.view_licenses),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { goToAboutScreen() },
                            Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                        ) {
                            Icon(
                                painterResource(R.drawable.info),
                                stringResource(R.string.info),
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
}
