package co.adityarajput.fileflow.views.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Group
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.toShortHumanReadableTime

@Composable
fun Tile(
    title: String,
    leading: String? = null,
    trailing: String? = null,
    content: @Composable (() -> Unit) = { },
    onClick: () -> Unit = {},
    buttons: @Composable RowScope.() -> Unit = {},
    expanded: Boolean = false,
) {
    Card(
        onClick,
        Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_small))
            .animateContentSize(
                tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing,
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_large)),
            Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        ) {
            if (leading != null || trailing != null)
                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically,
                ) {
                    if (leading != null)
                        Text(
                            leading,
                            style = MaterialTheme.typography.bodySmall.copy(
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                11.sp,
                            ),
                        )
                    if (trailing != null)
                        Text(
                            trailing,
                            style = MaterialTheme.typography.bodySmall.copy(
                                MaterialTheme.colorScheme.onSurfaceVariant,
                                8.sp,
                            ),
                        )
                }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
            )
            content()
            if (expanded) Row(Modifier.fillMaxWidth(), Arrangement.End) { buttons() }
        }
    }
}

@Composable
fun Rule.Tile(
    expanded: Boolean = false,
    showExecutions: Boolean = true,
    onClick: () -> Unit = {},
    buttons: @Composable RowScope.() -> Unit = {},
) = Tile(
    name ?: action.srcFileNamePattern, stringResource(action.verb.forRules),
    if (!showExecutions) null
    else if (!enabled) stringResource(R.string.disabled)
    else pluralStringResource(R.plurals.execution, executions, executions),
    { Text(getDescription(), style = MaterialTheme.typography.bodySmall) },
    onClick, buttons, expanded,
)

@Composable
fun Execution.Tile() = Tile(
    fileName,
    stringResource(verb.forExecutions),
    stringResource(
        R.string.ago,
        (System.currentTimeMillis() - timestamp).toShortHumanReadableTime(),
    ),
)

@Composable
fun Group.Tile(
    expanded: Boolean = false,
    onClick: () -> Unit = {},
    buttons: @Composable RowScope.() -> Unit = {},
) = Tile(
    name, null, null,
    {
        Text(
            if (ruleIds.isEmpty())
                stringResource(R.string.all_rules)
            else
                pluralStringResource(R.plurals.rule, ruleIds.size, ruleIds.size),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Normal,
        )
    },
    onClick, buttons, expanded,
)
