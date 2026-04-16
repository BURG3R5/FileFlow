package co.adityarajput.fileflow.views.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.services.SFTP
import co.adityarajput.fileflow.viewmodels.UpsertRuleViewModel
import net.schmizz.sshj.sftp.RemoteResourceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteFolderPickerBottomSheet(viewModel: UpsertRuleViewModel) {
    val context = LocalContext.current
    val hideSheet = { viewModel.remoteFolderPickerState = null }

    val server = when (viewModel.remoteFolderPickerState!!) {
        RemoteFolderPickerState.SRC -> viewModel.state.values.srcServer
        RemoteFolderPickerState.DEST -> viewModel.state.values.destServer
    }

    var currentDir by remember { mutableStateOf(("/")) }
    var items by remember(currentDir) {
        mutableStateOf<List<RemoteResourceInfo>?>(null)
    }

    LaunchedEffect(currentDir) {
        items = null
        try {
            items = SFTP.runOn(server!!) { ls(currentDir) }.orEmpty()
                .sortedBy { it.name.lowercase() }
                .sortedBy { it.isRegularFile }
        } catch (_: Exception) {
        }
    }

    ModalBottomSheet(
        hideSheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            currentDir,
            Modifier.padding(
                dimensionResource(R.dimen.padding_medium),
                dimensionResource(R.dimen.padding_small),
            ),
            style = MaterialTheme.typography.labelMedium,
            overflow = TextOverflow.StartEllipsis,
            maxLines = 1,
        )
        HorizontalDivider()
        LazyColumn(
            Modifier
                .weight(1f)
                .padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
        ) {
            if (currentDir != "/") {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { currentDir = currentDir.substringBeforeLast('/', "/") },
                        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            stringResource(R.string.parent_directory),
                        )
                        Text(stringResource(R.string.parent_directory))
                    }
                }
            }
            if (items == null) {
                item {
                    Box(
                        Modifier.fillMaxWidth(),
                        Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            } else if (items!!.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.empty_folder),
                        Modifier.fillMaxWidth(),
                        Color.Gray,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(items!!, { it.path }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(it.isDirectory) { currentDir = it.path },
                        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                    ) {
                        Icon(
                            painterResource(if (it.isDirectory) R.drawable.folder else R.drawable.file),
                            stringResource(if (it.isDirectory) R.string.folder else R.string.file),
                        )
                        Text(it.name)
                    }
                }
            }
        }
        Button(
            {
                viewModel.updateForm(
                    context,
                    viewModel.state.values.run {
                        if (viewModel.remoteFolderPickerState == RemoteFolderPickerState.SRC)
                            copy(src = currentDir)
                        else
                            copy(dest = currentDir)
                    },
                )
                hideSheet()
            },
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = dimensionResource(R.dimen.padding_small)),
            colors = ButtonDefaults.buttonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
        ) {
            Text(
                stringResource(R.string.use_this_folder),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

enum class RemoteFolderPickerState { SRC, DEST }
