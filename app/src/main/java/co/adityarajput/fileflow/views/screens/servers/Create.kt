package co.adityarajput.fileflow.views.screens.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.viewmodel.compose.viewModel
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.viewmodels.Provider
import co.adityarajput.fileflow.viewmodels.servers.CreateServerViewModel
import co.adityarajput.fileflow.views.components.AppBar
import co.adityarajput.fileflow.views.components.ErrorText
import co.adityarajput.fileflow.views.textFieldColors
import kotlinx.coroutines.launch

@Composable
fun CreateServerScreen(
    goBack: () -> Unit,
    viewModel: CreateServerViewModel = viewModel(factory = Provider.Factory),
) {
    val coroutineScope = rememberCoroutineScope()

    val host = rememberTextFieldState()
    val port = rememberTextFieldState()
    val username = rememberTextFieldState()
    val password = rememberTextFieldState()
    val privateKey = rememberTextFieldState()

    Scaffold(
        topBar = { AppBar(stringResource(R.string.add_server), true, goBack) },
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues), Arrangement.SpaceBetween) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(dimensionResource(R.dimen.padding_small))
                    .padding(
                        dimensionResource(R.dimen.padding_large),
                        dimensionResource(R.dimen.padding_medium),
                    ),
                Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
            ) {
                OutlinedTextField(
                    host,
                    Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.host)) },
                    colors = textFieldColors,
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                OutlinedTextField(
                    port,
                    Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.port)) },
                    colors = textFieldColors,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    inputTransformation = InputTransformation {
                        if (!asCharSequence().isDigitsOnly())
                            revertAllChanges()
                    },
                )
                OutlinedTextField(
                    username,
                    Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.username)) },
                    colors = textFieldColors,
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                OutlinedSecureTextField(
                    password,
                    Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.password)) },
                    colors = textFieldColors,
                )
                OutlinedTextField(
                    privateKey,
                    Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.private_key)) },
                    colors = textFieldColors,
                )
                if (viewModel.error != null)
                    ErrorText(viewModel.error!!.message)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_large)),
                Arrangement.Center,
                Alignment.Bottom,
            ) {
                TextButton(
                    { goBack() },
                    Modifier
                        .fillMaxWidth(0.5f)
                        .padding(end = dimensionResource(R.dimen.padding_small)),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Normal,
                    )
                }
                TextButton(
                    {
                        coroutineScope.launch {
                            viewModel.submitForm(
                                host.text as String,
                                port.text as String,
                                username.text as String,
                                password.text as String,
                                privateKey.text as String,
                            )
                            if (viewModel.error == null) {
                                goBack()
                            }
                        }
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(start = dimensionResource(R.dimen.padding_small)),
                    listOf(host, port, username).all { it.text.isNotBlank() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Text(
                        stringResource(R.string.add),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
