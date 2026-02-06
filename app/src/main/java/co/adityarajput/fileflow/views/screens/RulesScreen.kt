package co.adityarajput.fileflow.views.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.utils.getToggleString
import co.adityarajput.fileflow.viewmodels.DialogState
import co.adityarajput.fileflow.viewmodels.Provider
import co.adityarajput.fileflow.viewmodels.RulesViewModel
import co.adityarajput.fileflow.views.components.AppBar
import co.adityarajput.fileflow.views.components.ManageRuleDialog
import co.adityarajput.fileflow.views.components.Tile
import kotlinx.serialization.json.Json

@Composable
fun RulesScreen(
    goToUpsertRuleScreen: (String) -> Unit,
    goToExecutionsScreen: () -> Unit,
    goToSettingsScreen: () -> Unit,
    viewModel: RulesViewModel = viewModel(factory = Provider.Factory),
) {
    val state = viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            AppBar(stringResource(R.string.app_name), false) {
                IconButton(goToSettingsScreen) {
                    Icon(
                        painterResource(R.drawable.settings),
                        stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(goToExecutionsScreen) {
                    Icon(
                        painterResource(R.drawable.history),
                        stringResource(R.string.history),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                { goToUpsertRuleScreen("null") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(painterResource(R.drawable.add), stringResource(R.string.add_rule)) }
        },
    ) { paddingValues ->
        if (state.value.rules == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else if (state.value.rules!!.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.no_rules),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(paddingValues)
                    .padding(dimensionResource(R.dimen.padding_small))
                    .fillMaxSize(),
            ) {
                items(state.value.rules!!, { it.id }) {
                    Tile(
                        it.action.title,
                        stringResource(it.action.verb),
                        if (!it.enabled) stringResource(R.string.disabled)
                        else pluralStringResource(
                            R.plurals.execution,
                            it.executions,
                            it.executions,
                        ),
                        {
                            Text(
                                it.action.description,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        {
                            if (viewModel.selectedRule == it) viewModel.selectedRule = null
                            else viewModel.selectedRule = it
                        },
                        {
                            IconButton({ viewModel.dialogState = DialogState.EXECUTE }) {
                                Icon(
                                    painterResource(R.drawable.play_circle),
                                    stringResource(R.string.execute_rule),
                                )
                            }
                            IconButton({ viewModel.dialogState = DialogState.TOGGLE_RULE }) {
                                Icon(
                                    if (it.enabled) painterResource(R.drawable.archive)
                                    else painterResource(R.drawable.unarchive),
                                    stringResource(
                                        R.string.toggle_rule,
                                        it.enabled.getToggleString(),
                                    ),
                                )
                            }
                            IconButton({ goToUpsertRuleScreen(Json.encodeToString(it)) }) {
                                Icon(
                                    painterResource(R.drawable.edit),
                                    stringResource(R.string.edit_rule),
                                )
                            }
                            IconButton(
                                { viewModel.dialogState = DialogState.DELETE },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary,
                                ),
                            ) {
                                Icon(
                                    painterResource(R.drawable.delete),
                                    stringResource(R.string.delete),
                                )
                            }
                        },
                        viewModel.selectedRule == it,
                    )
                }
                item { Box(Modifier.height(100.dp)) {} }
            }
        }
        if (viewModel.selectedRule != null && viewModel.dialogState != null)
            ManageRuleDialog(viewModel)
    }
}
