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
import androidx.compose.ui.unit.sp
import co.adityarajput.fileflow.R

@Composable
fun Tile(
    title: String,
    leading: String,
    trailing: String,
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
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically,
            ) {
                Text(
                    leading,
                    style = MaterialTheme.typography.bodySmall.copy(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        11.sp,
                    ),
                )
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
