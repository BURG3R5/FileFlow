package co.adityarajput.fileflow.views.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.viewmodels.servers.ServersViewModel

@Composable
fun DeleteServerDialog(viewModel: ServersViewModel) {
    val hideDialog = { viewModel.showDeleteDialog = false }

    AlertDialog(
        hideDialog,
        title = { Text(stringResource(R.string.delete_server)) },
        text = { Text(stringResource(R.string.delete_server_confirmation)) },
        confirmButton = {
            Row {
                TextButton(
                    {
                        viewModel.deleteServer()
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
