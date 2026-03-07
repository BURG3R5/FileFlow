package co.adityarajput.fileflow.views.components

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import co.adityarajput.fileflow.viewmodels.UpsertRuleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerBottomSheet(viewModel: UpsertRuleViewModel) {
    val context = LocalContext.current
    val hideSheet = { viewModel.folderPickerState = null }
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val items = remember(currentDir) {
        currentDir.listFiles()?.sortedBy { it.name.lowercase() }?.sortedBy { it.isFile }
            ?: emptyList()
    }

    ModalBottomSheet(
        hideSheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            currentDir.path,
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
            Modifier.padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
        ) {
            if (currentDir.parentFile?.canRead() ?: false) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { currentDir = currentDir.parentFile },
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
            if (items.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.empty_folder),
                        Modifier.fillMaxWidth(),
                        Color.Gray,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(items, { it.path }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(it.isDirectory) { currentDir = it },
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
                        if (viewModel.folderPickerState == FolderPickerState.SRC) copy(src = currentDir.path)
                        else copy(dest = currentDir.path)
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

enum class FolderPickerState { SRC, DEST }
