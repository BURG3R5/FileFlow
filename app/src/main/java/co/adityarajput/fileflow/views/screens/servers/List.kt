package co.adityarajput.fileflow.views.screens.servers

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.viewmodels.Provider
import co.adityarajput.fileflow.viewmodels.servers.ServersViewModel
import co.adityarajput.fileflow.views.components.AppBar
import co.adityarajput.fileflow.views.components.DeleteServerDialog
import co.adityarajput.fileflow.views.components.Tile

@Composable
fun ServersScreen(
    goToCreateServerScreen: () -> Unit,
    goBack: () -> Unit,
    viewModel: ServersViewModel = viewModel(factory = Provider.Factory),
) {
    val servers = viewModel.state.collectAsState().value.servers

    Scaffold(
        topBar = { AppBar(stringResource(R.string.servers), true, goBack) },
        floatingActionButton = {
            FloatingActionButton(
                goToCreateServerScreen,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(painterResource(R.drawable.add), stringResource(R.string.add_server)) }
        },
    ) { paddingValues ->
        if (servers == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else if (servers.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.padding_extra_large)),
                Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.no_servers),
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
                items(servers, { it.id }) {
                    Tile(
                        "${it.username}@${it.host}:${it.port}",
                        onClick = {
                            if (viewModel.selectedServer == it) viewModel.selectedServer = null
                            else viewModel.selectedServer = it
                        },
                        buttons = {
                            IconButton(
                                { viewModel.showDeleteDialog = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary,
                                ),
                            ) {
                                Icon(
                                    painterResource(R.drawable.delete),
                                    stringResource(R.string.delete_server),
                                )
                            }
                        },
                        expanded = viewModel.selectedServer == it,
                    )
                }
                item { Box(Modifier.height(100.dp)) {} }
            }
        }
        if (viewModel.selectedServer != null && viewModel.showDeleteDialog)
            DeleteServerDialog(viewModel)
    }
}
