package co.adityarajput.fileflow.views.screens

import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.viewmodel.compose.viewModel
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.utils.*
import co.adityarajput.fileflow.viewmodels.*
import co.adityarajput.fileflow.views.components.*
import co.adityarajput.fileflow.views.textFieldColors
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

@Composable
fun UpsertRuleScreen(
    ruleString: String,
    goBack: () -> Unit,
    viewModel: UpsertRuleViewModel = viewModel(factory = Provider.createURVM(ruleString)),
) {
    val context = LocalContext.current
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
            AnimatedContent(
                viewModel.state.page,
                Modifier
                    .weight(1f)
                    .padding(dimensionResource(R.dimen.padding_small))
                    .padding(
                        dimensionResource(R.dimen.padding_large),
                        dimensionResource(R.dimen.padding_medium),
                    ),
                { fadeIn() togetherWith fadeOut() },
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
                ) {
                    when (it) {
                        FormPage.ACTION -> ActionPage(viewModel)
                        FormPage.SCHEDULE -> SchedulePage(viewModel)
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_large)),
                Arrangement.Center,
                Alignment.Bottom,
            ) {
                TextButton(
                    {
                        if (viewModel.state.page.isFirstPage()) {
                            goBack()
                        } else {
                            viewModel.updateForm(
                                context,
                                page = viewModel.state.page.previous(),
                            )
                        }
                    },
                    Modifier
                        .fillMaxWidth(0.5f)
                        .padding(end = dimensionResource(R.dimen.padding_small)),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        if (viewModel.state.page.isFirstPage()) stringResource(R.string.cancel)
                        else stringResource(R.string.back),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Normal,
                    )
                }
                TextButton(
                    {
                        if (viewModel.state.page.isFinalPage()) {
                            coroutineScope.launch {
                                viewModel.submitForm(context)
                                goBack()
                            }
                        } else {
                            viewModel.updateForm(
                                context,
                                page = viewModel.state.page.next(),
                            )
                        }
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(start = dimensionResource(R.dimen.padding_small)),
                    viewModel.state.error == null,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Text(
                        if (viewModel.state.page.isFinalPage()) {
                            if (viewModel.state.values.ruleId == 0) stringResource(R.string.add)
                            else stringResource(R.string.save)
                        } else stringResource(R.string.next),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        if (viewModel.folderPickerState != null) FolderPickerBottomSheet(viewModel)
    }
}

@Composable
private fun ColumnScope.ActionPage(viewModel: UpsertRuleViewModel) {
    val context = LocalContext.current
    val shouldUseCustomPicker = remember { context.isGranted(Permission.MANAGE_EXTERNAL_STORAGE) }

    var superlativeDropdownExpanded by remember { mutableStateOf(false) }

    val srcPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.requestPersistableFolderPermission(uri)
        viewModel.updateForm(context, viewModel.state.values.copy(src = uri.toString()))
    }
    val destPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.requestPersistableFolderPermission(uri)
        viewModel.updateForm(context, viewModel.state.values.copy(dest = uri.toString()))
    }

    Text(
        stringResource(R.string.action),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Normal,
    )
    Action.entries.forEach {
        Row(
            Modifier
                .fillMaxWidth()
                .selectable(it isSimilarTo viewModel.state.values.actionBase) {
                    viewModel.updateForm(
                        context,
                        viewModel.state.values.copy(actionBase = it),
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                it isSimilarTo viewModel.state.values.actionBase,
                null,
                Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
            )
            Text(
                stringResource(it.phrase),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
            )
        }
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
            .clickable {
                if (shouldUseCustomPicker) viewModel.folderPickerState = FolderPickerState.SRC
                else srcPicker.launch(null)
            },
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Normal,
    )
    Row(
        Modifier.toggleable(viewModel.state.values.scanSubdirectories) {
            viewModel.updateForm(context, viewModel.state.values.copy(scanSubdirectories = it))
        },
        Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        Alignment.CenterVertically,
    ) {
        Checkbox(viewModel.state.values.scanSubdirectories, null)
        Text(
            stringResource(R.string.scan_subdirectories),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Normal,
        )
    }
    OutlinedTextField(
        viewModel.state.values.srcFileNamePattern,
        {
            viewModel.updateForm(context, viewModel.state.values.copy(srcFileNamePattern = it))
        },
        Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.file_name_pattern)) },
        placeholder = { Text(stringResource(R.string.pattern_placeholder)) },
        supportingText = {
            if (viewModel.state.values.currentSrcFileNames.isNullOrEmpty())
                Text(stringResource(R.string.match_entire_filename))
            else
                Text(
                    stringResource(
                        R.string.pattern_should_match,
                        viewModel.state.values.currentSrcFileNames!!
                            .joinToString(stringResource(R.string.or), limit = 3),
                    ),
                )
        },
        colors = textFieldColors,
    )
    when (viewModel.state.values.actionBase) {
        is Action.MOVE -> {
            Row(
                Modifier.toggleable(!viewModel.state.values.keepOriginal) {
                    viewModel.updateForm(context, viewModel.state.values.copy(keepOriginal = !it))
                },
                Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                Alignment.CenterVertically,
            ) {
                Checkbox(!viewModel.state.values.keepOriginal, null)
                Text(
                    stringResource(R.string.delete_original),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                )
            }
            Box {
                Text(
                    buildAnnotatedString {
                        append(stringResource(R.string.choose_superlative))
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append(stringResource(viewModel.state.values.superlative.displayName))
                        }
                    },
                    Modifier.clickable { superlativeDropdownExpanded = true },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                )
                DropdownMenu(superlativeDropdownExpanded, { superlativeDropdownExpanded = false }) {
                    FileSuperlative.entries.forEach {
                        DropdownMenuItem(
                            { Text(stringResource(it.displayName)) },
                            {
                                viewModel.updateForm(
                                    context,
                                    viewModel.state.values.copy(superlative = it),
                                )
                                superlativeDropdownExpanded = false
                            },
                        )
                    }
                }
            }
            Icon(
                painterResource(R.drawable.arrow_down),
                stringResource(R.string.arrow_down),
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
                    .clickable {
                        if (shouldUseCustomPicker) viewModel.folderPickerState =
                            FolderPickerState.DEST
                        else destPicker.launch(null)
                    },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
            )
            if (viewModel.state.values.scanSubdirectories)
                Row(
                    Modifier.toggleable(viewModel.state.values.preserveStructure) {
                        viewModel.updateForm(
                            context,
                            viewModel.state.values.copy(preserveStructure = it),
                        )
                    },
                    Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                    Alignment.CenterVertically,
                ) {
                    Checkbox(viewModel.state.values.preserveStructure, null)
                    Text(
                        stringResource(R.string.preserve_structure),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Normal,
                    )
                }
            OutlinedTextField(
                viewModel.state.values.destFileNameTemplate,
                {
                    viewModel.updateForm(
                        context,
                        viewModel.state.values.copy(destFileNameTemplate = it),
                    )
                },
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.file_name_template)) },
                placeholder = { Text(stringResource(R.string.template_placeholder)) },
                supportingText = {
                    if (viewModel.state.values.predictedDestFileNames.isNullOrEmpty()) {
                        Text(
                            buildAnnotatedString {
                                append(stringResource(R.string.blank_template_supporting))
                                if (viewModel.state.values.destFileNameTemplate != "$0") {
                                    withLink(
                                        LinkAnnotation.Clickable(
                                            "keep_name",
                                            TextLinkStyles(
                                                SpanStyle(
                                                    MaterialTheme.colorScheme.primary,
                                                    textDecoration = TextDecoration.Underline,
                                                ),
                                            ),
                                        ) {
                                            viewModel.updateForm(
                                                context,
                                                viewModel.state.values.copy(destFileNameTemplate = "$0"),
                                            )
                                        },
                                    ) { append(stringResource(R.string.keep_name)) }
                                }
                            },
                        )
                    } else {
                        Text(
                            stringResource(
                                R.string.template_will_yield,
                                viewModel.state.values.predictedDestFileNames!!
                                    .joinToString(stringResource(R.string.or), limit = 3),
                            ),
                        )
                    }
                },
                colors = textFieldColors,
            )
            Row(
                Modifier.toggleable(viewModel.state.values.overwriteExisting) {
                    viewModel.updateForm(
                        context,
                        viewModel.state.values.copy(overwriteExisting = it),
                    )
                },
                Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                Alignment.CenterVertically,
            ) {
                Checkbox(viewModel.state.values.overwriteExisting, null)
                Text(
                    stringResource(R.string.overwrite_existing),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        is Action.DELETE_STALE -> {
            OutlinedTextField(
                viewModel.state.values.retentionDays.toString(),
                {
                    it.toIntOrNull()?.let { days ->
                        viewModel.updateForm(
                            context,
                            viewModel.state.values.copy(retentionDays = days),
                        )
                    }
                },
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.retention_days)) },
                suffix = { Text(stringResource(R.string.days)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
        }

        is Action.ZIP -> {
            Icon(
                painterResource(R.drawable.arrow_down),
                stringResource(R.string.arrow_down),
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
                    .clickable {
                        if (shouldUseCustomPicker) viewModel.folderPickerState =
                            FolderPickerState.DEST
                        else destPicker.launch(null)
                    },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
            )
            if (viewModel.state.values.scanSubdirectories)
                Row(
                    Modifier.toggleable(viewModel.state.values.preserveStructure) {
                        viewModel.updateForm(
                            context,
                            viewModel.state.values.copy(preserveStructure = it),
                        )
                    },
                    Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                    Alignment.CenterVertically,
                ) {
                    Checkbox(viewModel.state.values.preserveStructure, null)
                    Text(
                        stringResource(R.string.preserve_structure_zip),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Normal,
                    )
                }
            OutlinedTextField(
                viewModel.state.values.destFileNameTemplate,
                {
                    viewModel.updateForm(
                        context,
                        viewModel.state.values.copy(destFileNameTemplate = it),
                    )
                },
                Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.file_name_template)) },
                placeholder = { Text(stringResource(R.string.template_placeholder)) },
                supportingText = {
                    if (viewModel.state.values.predictedDestFileNames.isNullOrEmpty()) {
                        Text(stringResource(R.string.blank_template_supporting_zip))
                    } else {
                        Text(
                            stringResource(
                                R.string.template_will_yield,
                                viewModel.state.values.predictedDestFileNames!!.first(),
                            ),
                        )
                    }
                },
                colors = textFieldColors,
            )
            Row(
                Modifier.toggleable(viewModel.state.values.overwriteExisting) {
                    viewModel.updateForm(
                        context,
                        viewModel.state.values.copy(overwriteExisting = it),
                    )
                },
                Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
                Alignment.CenterVertically,
            ) {
                Checkbox(viewModel.state.values.overwriteExisting, null)
                Text(
                    stringResource(R.string.overwrite_existing_zip),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
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
    else if (viewModel.state.error == FormError.INVALID_TEMPLATE) ErrorText(R.string.invalid_template)
    else if (viewModel.state.error == FormError.MUST_END_IN_ZIP) ErrorText(R.string.must_end_in_zip)
    else if (viewModel.state.warning == FormWarning.NO_MATCHES_IN_SRC) WarningText(R.string.pattern_doesnt_match_src_files)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedulePage(viewModel: UpsertRuleViewModel) {
    val context = LocalContext.current

    Text(
        stringResource(R.string.schedule),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Normal,
    )
    val intervalInputEnabled = viewModel.state.values.interval != null
    val cronInputEnabled = viewModel.state.values.cronString != null
    // region never
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(!(intervalInputEnabled || cronInputEnabled)) {
                viewModel.updateForm(
                    context,
                    viewModel.state.values.copy(interval = null, cronString = null),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            !(intervalInputEnabled || cronInputEnabled),
            null,
            Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
        )
        Text(
            stringResource(R.string.never),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Normal,
        )
    }
    // endregion

    // region periodic
    var unit by remember { mutableStateOf(TimeUnit.MINUTES) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(intervalInputEnabled) {
                viewModel.updateForm(
                    context,
                    viewModel.state.values.copy(
                        interval = Constants.ONE_HOUR_IN_MILLIS,
                        cronString = null,
                    ),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            intervalInputEnabled,
            null,
            Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
        )
        Text(
            stringResource(R.string.periodic),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Normal,
        )
    }
    AnimatedVisibility(intervalInputEnabled) {
        Column(
            Modifier.fillMaxWidth(),
            Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
        ) {
            val displayValue =
                ((viewModel.state.values.interval
                    ?: Constants.ONE_HOUR_IN_MILLIS) / unit.inMillis).toInt()
            OutlinedTextField(
                displayValue.toString(),
                {
                    viewModel.updateForm(
                        context,
                        viewModel.state.values.copy(
                            interval = it.toIntOrNull()?.times(unit.inMillis)
                                ?: Constants.ONE_HOUR_IN_MILLIS,
                        ),
                    )
                },
                Modifier.fillMaxWidth(),
                intervalInputEnabled,
                label = { Text(stringResource(R.string.interval)) },
                placeholder = { Text(stringResource(R.string.interval_placeholder)) },
                trailingIcon = {
                    ExposedDropdownMenuBox(
                        dropdownExpanded,
                        { dropdownExpanded = !dropdownExpanded },
                    ) {
                        Row(
                            Modifier
                                .clickable(intervalInputEnabled) { dropdownExpanded = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                unit.text(displayValue),
                                Modifier.padding(start = dimensionResource(R.dimen.padding_medium)),
                            )
                            IconButton(
                                { dropdownExpanded = true },
                                enabled = intervalInputEnabled,
                            ) {
                                Icon(
                                    painterResource(R.drawable.arrow_drop_down),
                                    stringResource(R.string.arrow_down),
                                )
                            }
                        }
                        ExposedDropdownMenu(dropdownExpanded, { dropdownExpanded = false }) {
                            listOf(TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).forEach {
                                DropdownMenuItem(
                                    { Text(it.text(displayValue)) },
                                    {
                                        unit = it
                                        dropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
            Text(
                stringResource(R.string.interval_disclaimer),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Normal,
            )
            if (viewModel.state.error == FormError.INTERVAL_TOO_SHORT) ErrorText(R.string.interval_too_short)
            else if (viewModel.state.error == FormError.INTERVAL_TOO_LONG) ErrorText(R.string.interval_too_long)
        }
    }
    // endregion

    // region cron
    // region ExactAlarmPermission
    var hasExactAlarmPermission by remember { mutableStateOf(context.isGranted(Permission.UNRESTRICTED_BACKGROUND_USAGE)) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val watcher = object : Runnable {
        override fun run() {
            hasExactAlarmPermission = context.isGranted(Permission.UNRESTRICTED_BACKGROUND_USAGE)

            if (!hasExactAlarmPermission)
                handler.postDelayed(this, 500)
        }
    }
    DisposableEffect(Unit) {
        handler.post(watcher)
        onDispose { handler.removeCallbacksAndMessages(null) }
    }
    // endregion

    Row(
        Modifier
            .fillMaxWidth()
            .selectable(cronInputEnabled) {
                viewModel.updateForm(
                    context,
                    viewModel.state.values.copy(
                        interval = null,
                        cronString = "00 * * * *",
                    ),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            cronInputEnabled,
            null,
            Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small)),
        )
        Text(
            stringResource(R.string.cron_like),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Normal,
        )
    }
    AnimatedVisibility(cronInputEnabled) {
        Column(
            Modifier.fillMaxWidth(),
            Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium)),
        ) {
            OutlinedTextField(
                viewModel.state.values.cronString ?: "",
                {
                    viewModel.updateForm(
                        context,
                        viewModel.state.values.copy(cronString = it),
                    )
                },
                Modifier.fillMaxWidth(),
                cronInputEnabled,
                label = { Text(stringResource(R.string.cron_string)) },
                placeholder = { Text(stringResource(R.string.cron_placeholder)) },
                supportingText = {
                    viewModel.state.values.predictedExecutionTimes?.let {
                        Text(
                            stringResource(
                                R.string.rule_will_execute_at,
                                it.joinToString(", ", limit = 3) { dt ->
                                    '“' + dt.format(
                                        if (dt.isToday) DateTimeFormatter.ofLocalizedTime(
                                            FormatStyle.SHORT,
                                        )
                                        else DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT),
                                    ) + '”'
                                },
                            ),
                        )
                    }
                },
                colors = textFieldColors,
                singleLine = true,
            )
            Text(
                AnnotatedString.fromHtml(
                    stringResource(R.string.cron_advice),
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
            if (!hasExactAlarmPermission) {
                WarningText(R.string.exact_alarm_permission_description)
                Button(
                    { context.request(Permission.UNRESTRICTED_BACKGROUND_USAGE) },
                    Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                ) {
                    Text(
                        stringResource(R.string.disable_optimization),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
            if (viewModel.state.error == FormError.INVALID_CRON_STRING) ErrorText(R.string.invalid_cron_string)
            else if (viewModel.state.error == FormError.CRON_TOO_FREQUENT) ErrorText(R.string.cron_too_frequent)
        }
    }
    // endregion
}
