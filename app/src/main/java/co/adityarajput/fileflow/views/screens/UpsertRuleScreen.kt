package co.adityarajput.fileflow.views.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.viewmodel.compose.viewModel
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.utils.getGetDirectoryFromUri
import co.adityarajput.fileflow.utils.takePersistablePermission
import co.adityarajput.fileflow.viewmodels.FormError
import co.adityarajput.fileflow.viewmodels.Provider
import co.adityarajput.fileflow.viewmodels.UpsertRuleViewModel
import co.adityarajput.fileflow.views.components.AppBar
import co.adityarajput.fileflow.views.components.ErrorText
import co.adityarajput.fileflow.views.components.WarningText
import kotlinx.coroutines.launch

@Composable
fun UpsertRuleScreen(
    ruleString: String,
    goBack: () -> Unit,
    viewModel: UpsertRuleViewModel = viewModel(factory = Provider.createURVM(ruleString)),
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppBar(
                stringResource(
                    if (viewModel.state.values.ruleId == 0) R.string.add_rule
                    else R.string.edit_rule,
                ),
                true,
                goBack,
            )
        },
    ) { paddingValues ->
        Column(
            Modifier.padding(paddingValues),
            Arrangement.SpaceBetween,
        ) {
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
                ActionPage(viewModel)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_large)),
                Arrangement.Center,
                Alignment.Bottom,
            ) {
                TextButton(
                    goBack,
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
                            viewModel.submitForm()
                            goBack()
                        }
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(start = dimensionResource(R.dimen.padding_small)),
                    viewModel.state.error == null,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Text(
                        if (viewModel.state.values.ruleId == 0) stringResource(R.string.add)
                        else stringResource(R.string.save),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ActionPage(viewModel: UpsertRuleViewModel) {
    val context = LocalContext.current

    val filesInSrc = remember(viewModel.state.values.src) { viewModel.getFilesInSrc(context) }
    val showWarning = remember(viewModel.state.values) {
        if (viewModel.state.error != null || filesInSrc == null || filesInSrc.isEmpty()) false
        else filesInSrc.none { Regex(viewModel.state.values.srcFileNamePattern).matches(it) }
    }

    val srcPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            context.takePersistablePermission(uri)
            viewModel.updateForm(
                values = viewModel.state.values.copy(src = uri.toString()),
            )
        }
    val destPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            context.takePersistablePermission(uri)
            viewModel.updateForm(
                values = viewModel.state.values.copy(dest = uri.toString()),
            )
        }

    Text(
        buildAnnotatedString {
            append(stringResource(R.string.source))
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(
                    viewModel.state.values.src.getGetDirectoryFromUri()
                        .ifBlank { stringResource(R.string.select_folder) },
                )
            }
        },
        Modifier
            .fillMaxWidth()
            .clickable { srcPicker.launch(null) },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Normal,
    )
    OutlinedTextField(
        viewModel.state.values.srcFileNamePattern,
        {
            viewModel.updateForm(
                viewModel.state.values.copy(srcFileNamePattern = it),
            )
        },
        Modifier.fillMaxWidth(),
        label = {
            Text(stringResource(R.string.file_name_pattern))
        },
        placeholder = { Text(stringResource(R.string.pattern_placeholder)) },
        supportingText = {
            if (filesInSrc == null || filesInSrc.isEmpty())
                Text(stringResource(R.string.match_entire_filename))
            else
                Text(
                    stringResource(
                        R.string.pattern_should_match,
                        filesInSrc.joinToString(stringResource(R.string.or), limit = 3),
                    ),
                )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        singleLine = true,
    )
    Row(
        Modifier.toggleable(!viewModel.state.values.keepOriginal) {
            viewModel.updateForm(viewModel.state.values.copy(keepOriginal = !it))
        },
        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        Alignment.Top,
    ) {
        Checkbox(!viewModel.state.values.keepOriginal, null)
        Text(
            stringResource(R.string.delete_original),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Normal,
        )
    }
    Icon(
        painterResource(R.drawable.arrow_down),
        stringResource(R.string.alttext_arrow_down),
        Modifier.align(Alignment.CenterHorizontally),
    )
    Text(
        buildAnnotatedString {
            append(stringResource(R.string.destination))
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(
                    viewModel.state.values.dest.getGetDirectoryFromUri()
                        .ifBlank { stringResource(R.string.select_folder) },
                )
            }
        },
        Modifier
            .fillMaxWidth()
            .clickable { destPicker.launch(null) },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Normal,
    )
    OutlinedTextField(
        viewModel.state.values.destFileNameTemplate,
        {
            viewModel.updateForm(
                viewModel.state.values.copy(destFileNameTemplate = it),
            )
        },
        Modifier.fillMaxWidth(),
        label = {
            Text(stringResource(R.string.file_name_template))
        },
        placeholder = { Text(stringResource(R.string.template_placeholder)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        singleLine = true,
    )
    Row(
        Modifier.toggleable(viewModel.state.values.overwriteExisting) {
            viewModel.updateForm(viewModel.state.values.copy(overwriteExisting = it))
        },
        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        Alignment.Top,
    ) {
        Checkbox(viewModel.state.values.overwriteExisting, null)
        Text(
            stringResource(R.string.overwrite_existing),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Normal,
        )
    }
    Text(
        AnnotatedString.fromHtml(
            stringResource(R.string.pattern_advice),
            TextLinkStyles(
                SpanStyle(
                    MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
            ),
        ),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Normal,
    )
    if (viewModel.state.error == FormError.INVALID_REGEX) ErrorText(R.string.invalid_regex)
    if (showWarning) WarningText(R.string.pattern_doesnt_match_src_files)
}
