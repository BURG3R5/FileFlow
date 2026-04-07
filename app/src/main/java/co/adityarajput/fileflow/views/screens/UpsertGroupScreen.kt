package co.adityarajput.fileflow.views.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.viewmodels.Provider
import co.adityarajput.fileflow.viewmodels.UpsertGroupViewModel
import co.adityarajput.fileflow.views.components.AppBar
import co.adityarajput.fileflow.views.components.Tile
import co.adityarajput.fileflow.views.textFieldColors
import kotlinx.coroutines.launch

@Composable
fun UpsertGroupScreen(
    groupString: String,
    goBack: () -> Unit,
    viewModel: UpsertGroupViewModel = viewModel(factory = Provider.createUGVM((groupString))),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rules = viewModel.rules.collectAsState().value

    Scaffold(
        topBar = {
            AppBar(
                stringResource(
                    if (viewModel.state.values.groupId == 0) R.string.add_group
                    else R.string.edit_group,
                ),
                true,
                goBack,
            )
        },
    ) { paddingValues ->
        Column(
            Modifier.padding(paddingValues),
            Arrangement.SpaceBetween,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(dimensionResource(R.dimen.padding_small))
                    .padding(
                        dimensionResource(R.dimen.padding_large),
                        dimensionResource(R.dimen.padding_medium),
                    ),
                Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
            ) {
                OutlinedTextField(
                    viewModel.state.values.name,
                    { viewModel.updateForm(viewModel.state.values.copy(name = it)) },
                    Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.group_name)) },
                    placeholder = { Text(stringResource(R.string.group_name_placeholder)) },
                    singleLine = true,
                    colors = textFieldColors,
                )
                Text(
                    stringResource(R.string.rules_to_include),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                )
                val allRules = viewModel.state.values.allRules
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(allRules) {
                            viewModel.updateForm(
                                viewModel.state.values.copy(allRules = !allRules),
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        allRules,
                        null,
                        Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
                    )
                    Text(
                        stringResource(R.string.all_rules),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Normal,
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(!allRules) {
                            viewModel.updateForm(
                                viewModel.state.values.copy(allRules = !allRules),
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        !allRules,
                        null,
                        Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
                    )
                    Text(
                        stringResource(R.string.some_rules),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Normal,
                    )
                }
                AnimatedVisibility(!allRules) {
                    if (rules == null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                        ) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    } else if (rules.isEmpty()) {
                        Text(
                            stringResource(R.string.no_rules_while_grouping),
                            Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        val selectedRuleIds = viewModel.state.values.ruleIds
                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensionResource(R.dimen.padding_small)),
                        ) {
                            items(rules, { it.id }) {
                                Tile(
                                    it.action.srcFileNamePattern,
                                    stringResource(it.action.verb.resource),
                                    null,
                                    {
                                        Text(
                                            it.getDescription(),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    },
                                    {
                                        viewModel.updateForm(
                                            viewModel.state.values.copy(
                                                ruleIds = if (it.id in selectedRuleIds) selectedRuleIds - it.id
                                                else selectedRuleIds + it.id,
                                            ),
                                        )
                                    },
                                    { Checkbox(it.id in selectedRuleIds, null) },
                                    true,
                                )
                            }
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_large)),
                Arrangement.Center,
                Alignment.Bottom,
            ) {
                TextButton(
                    { goBack() },
                    Modifier
                        .fillMaxWidth(0.5f)
                        .padding(end = dimensionResource(R.dimen.padding_small)),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Normal,
                    )
                }
                TextButton(
                    {
                        coroutineScope.launch {
                            viewModel.submitForm(context)
                            goBack()
                        }
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(start = dimensionResource(R.dimen.padding_small)),
                    viewModel.state.error == null,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Text(
                        if (viewModel.state.values.groupId == 0) stringResource(R.string.add)
                        else stringResource(R.string.save),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
