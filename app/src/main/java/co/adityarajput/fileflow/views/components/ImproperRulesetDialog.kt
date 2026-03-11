package co.adityarajput.fileflow.views.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.Permission
import co.adityarajput.fileflow.utils.isGranted
import kotlinx.serialization.json.Json

@Composable
fun ImproperRulesetDialog(
    rulesToBeMigrated: List<Rule>,
    goToUpsertRuleScreen: (String) -> Unit,
    hideDialog: () -> Unit,
) {
    val context = LocalContext.current
    val hasAllFilesAccess = remember { context.isGranted(Permission.MANAGE_EXTERNAL_STORAGE) }

    AlertDialog(
        hideDialog,
        title = { Text(stringResource(R.string.improper_ruleset)) },
        text = {
            Column {
                Text(
                    stringResource(
                        if (hasAllFilesAccess) R.string.explain_saf_to_io_migration
                        else R.string.explain_missing_saf_access,
                    ) + " " + stringResource(R.string.please_reselect_folders),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    rulesToBeMigrated.forEach {
                        Tile(
                            it.action.srcFileNamePattern,
                            stringResource(it.action.verb.resource),
                            if (!it.enabled) stringResource(R.string.disabled)
                            else pluralStringResource(
                                R.plurals.execution,
                                it.executions,
                                it.executions,
                            ),
                            {
                                Text(
                                    it.action.getDescription(),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            { goToUpsertRuleScreen(Json.encodeToString(it)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(hideDialog) {
                Text(stringResource(R.string.dismiss), fontWeight = FontWeight.Normal)
            }
        },
    )
}
