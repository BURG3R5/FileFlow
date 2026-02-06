package co.adityarajput.fileflow.views.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import co.adityarajput.fileflow.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String,
    canNavigateBack: Boolean,
    leadingIconOnClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = when {
                        canNavigateBack -> MaterialTheme.typography.headlineMedium.fontSize
                        else -> MaterialTheme.typography.headlineLarge.fontSize
                    },
                ),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
        navigationIcon = {
            IconButton(leadingIconOnClick) {
                if (canNavigateBack) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        stringResource(R.string.alttext_back_button),
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.automation),
                        stringResource(R.string.alttext_app_logo),
                    )
                }
            }
        },
        actions = actions,
    )
}
