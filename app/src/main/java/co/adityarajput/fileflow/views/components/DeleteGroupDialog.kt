package co.adityarajput.fileflow.views.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.viewmodels.GroupsViewModel

@Composable
fun DeleteGroupDialog(viewModel: GroupsViewModel) {
    val context = LocalContext.current
    val hideDialog = { viewModel.showDeleteDialog = false }

    AlertDialog(
        hideDialog,
        title = { Text(stringResource(R.string.delete_group)) },
        text = { Text(stringResource(R.string.delete_group_confirmation)) },
        confirmButton = {
            Row {
                TextButton(
                    {
                        viewModel.deleteGroup(context)
                        hideDialog()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        },
        dismissButton = {
            TextButton(hideDialog) {
                Text(stringResource(R.string.cancel), fontWeight = FontWeight.Normal)
            }
        },
    )
}
