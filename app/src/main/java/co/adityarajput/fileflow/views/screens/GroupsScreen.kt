package co.adityarajput.fileflow.views.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.viewmodels.GroupsViewModel
import co.adityarajput.fileflow.viewmodels.Provider
import co.adityarajput.fileflow.views.components.AppBar
import co.adityarajput.fileflow.views.components.DeleteGroupDialog
import co.adityarajput.fileflow.views.components.Tile
import kotlinx.serialization.json.Json

@Composable
fun GroupsScreen(
    goToUpsertGroupScreen: (String) -> Unit,
    goBack: () -> Unit,
    viewModel: GroupsViewModel = viewModel(factory = Provider.Factory),
) {
    val context = LocalContext.current
    val groups = viewModel.state.collectAsState().value.groups

    Scaffold(
        topBar = {
            AppBar(
                stringResource(R.string.groups),
                true,
                goBack,
            )
        },
        floatingActionButton = {
            val toastText = stringResource(R.string.shortcuts_limited, Constants.MAX_SHORTCUTS)
            FloatingActionButton(
                {
                    if ((groups?.size ?: 0) < Constants.MAX_SHORTCUTS)
                        goToUpsertGroupScreen("null")
                    else
                        Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    painterResource(R.drawable.add),
                    stringResource(R.string.add_group),
                )
            }
        },
    ) { paddingValues ->
        if (groups == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else if (groups.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.padding_extra_large)),
                Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.no_groups),
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
                items(groups, { it.id }) {
                    Tile(
                        it.name,
                        content = {
                            Text(
                                if (it.ruleIds.isEmpty())
                                    stringResource(R.string.all_rules)
                                else
                                    pluralStringResource(
                                        R.plurals.rule,
                                        it.ruleIds.size,
                                        it.ruleIds.size,
                                    ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Normal,
                            )
                        },
                        onClick = {
                            if (viewModel.selectedGroup == it) viewModel.selectedGroup = null
                            else viewModel.selectedGroup = it
                        },
                        buttons = {
                            IconButton({ goToUpsertGroupScreen(Json.encodeToString(it)) }) {
                                Icon(
                                    painterResource(R.drawable.edit),
                                    stringResource(R.string.edit_group),
                                )
                            }
                            IconButton(
                                { viewModel.showDeleteDialog = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary,
                                ),
                            ) {
                                Icon(
                                    painterResource(R.drawable.delete),
                                    stringResource(R.string.delete_group),
                                )
                            }
                        },
                        expanded = viewModel.selectedGroup == it,
                    )
                }
                item { Box(Modifier.height(100.dp)) {} }
            }
        }
        if (viewModel.selectedGroup != null && viewModel.showDeleteDialog)
            DeleteGroupDialog(viewModel)
    }
}
