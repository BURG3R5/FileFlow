package co.adityarajput.fileflow.views.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.utils.toShortHumanReadableTime
import co.adityarajput.fileflow.viewmodels.ExecutionsViewModel
import co.adityarajput.fileflow.viewmodels.Provider
import co.adityarajput.fileflow.views.components.AppBar
import co.adityarajput.fileflow.views.components.Tile

@Composable
fun ExecutionsScreen(
    goBack: () -> Unit,
    viewModel: ExecutionsViewModel = viewModel(factory = Provider.Factory),
) {
    val state = viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            AppBar(
                stringResource(R.string.history),
                true,
                goBack,
            )
        },
    ) { paddingValues ->
        if (state.value.executions == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else if (state.value.executions!!.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.no_executions),
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
                items(state.value.executions!!, { it.id }) {
                    Tile(
                        it.fileName,
                        stringResource(it.actionVerb),
                        it.timestamp.toShortHumanReadableTime(),
                    )
                }
            }
        }
    }
}
