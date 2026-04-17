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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.data.models.Rule
import kotlinx.serialization.json.Json

@Composable
fun ImproperRulesetDialog(
    rulesToBeMigrated: List<Rule>,
    goToUpsertRuleScreen: (String) -> Unit,
    hideDialog: () -> Unit,
) {
    AlertDialog(
        hideDialog,
        title = { Text(stringResource(R.string.improper_ruleset)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.explain_improper_rules),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    rulesToBeMigrated.forEach {
                        it.Tile(onClick = { goToUpsertRuleScreen(Json.encodeToString(it)) })
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
