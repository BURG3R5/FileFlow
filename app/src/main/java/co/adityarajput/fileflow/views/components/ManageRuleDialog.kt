package co.adityarajput.fileflow.views.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.utils.getToggleString
import co.adityarajput.fileflow.viewmodels.DialogState
import co.adityarajput.fileflow.viewmodels.RulesViewModel

@Composable
fun ManageRuleDialog(viewModel: RulesViewModel) {
    val context = LocalContext.current
    val hideDialog = { viewModel.dialogState = null }
    val dialogState = viewModel.dialogState!!
    val rule = viewModel.selectedRule!!

    AlertDialog(
        hideDialog,
        title = {
            Text(
                when (dialogState) {
                    DialogState.EXECUTE -> stringResource(R.string.execute_rule)

                    DialogState.TOGGLE_RULE -> stringResource(
                        R.string.toggle_rule,
                        rule.enabled.getToggleString(),
                    )

                    DialogState.DELETE -> stringResource(R.string.delete_rule)
                },
            )
        },
        text = {
            Text(
                when (dialogState) {
                    DialogState.EXECUTE -> stringResource(R.string.execute_confirmation)

                    DialogState.TOGGLE_RULE -> stringResource(
                        R.string.toggle_rule_confirmation,
                        rule.enabled.getToggleString(),
                    )

                    DialogState.DELETE -> stringResource(
                        R.string.delete_confirmation,
                        if (rule.enabled) stringResource(R.string.disable_suggestion) else "",
                    )
                },
            )
        },
        confirmButton = {
            Row {
                TextButton(
                    {
                        when (dialogState) {
                            DialogState.EXECUTE -> viewModel.executeRule(context)
                            DialogState.TOGGLE_RULE -> viewModel.toggleRule()
                            DialogState.DELETE -> viewModel.deleteRule()
                        }
                        hideDialog()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (dialogState == DialogState.DELETE) MaterialTheme.colorScheme.tertiary
                        else Color.Unspecified,
                    ),
                ) {
                    Text(
                        when (dialogState) {
                            DialogState.EXECUTE -> stringResource(R.string.execute)
                            DialogState.TOGGLE_RULE -> rule.enabled.getToggleString()
                            DialogState.DELETE -> stringResource(R.string.delete)
                        },
                    )
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
