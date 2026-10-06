package com.readrops.app.more.diagnostics

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.readrops.app.R
import com.readrops.app.util.components.AndroidScreen
import com.readrops.app.util.components.dialog.TwoChoicesDialog
import com.readrops.app.util.diagnostics.DiagnosticEntry
import com.readrops.app.util.diagnostics.DiagnosticLevel
import com.readrops.app.util.diagnostics.DiagnosticReport
import com.readrops.app.util.diagnostics.copyReport
import com.readrops.app.util.diagnostics.reportOnGitHub
import com.readrops.app.util.diagnostics.shareReport
import com.readrops.app.util.displayToast
import com.readrops.app.util.theme.MediumSpacer
import com.readrops.app.util.theme.ShortSpacer
import com.readrops.app.util.theme.VeryShortSpacer
import com.readrops.app.util.theme.spacing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class LevelFilter(@StringRes val label: Int, val levels: Set<DiagnosticLevel>) {
    ALL(R.string.filter_all, DiagnosticLevel.entries.toSet()),
    ERRORS(R.string.filter_errors, setOf(DiagnosticLevel.ERROR)),
    WARNINGS(R.string.filter_warnings, setOf(DiagnosticLevel.WARNING)),
}

class DiagnosticLogScreen : AndroidScreen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()
        val screenModel = koinScreenModel<DiagnosticLogScreenModel>()

        val entries by screenModel.entries.collectAsStateWithLifecycle()
        var hideServers by rememberSaveable { mutableStateOf(true) }
        var filter by rememberSaveable { mutableStateOf(LevelFilter.ALL) }
        var showClearDialog by rememberSaveable { mutableStateOf(false) }

        val visibleEntries = remember(entries, filter) {
            entries.filter { it.level in filter.levels }
        }

        // the report is built on demand, so it always matches what the switch says
        fun withReport(action: (DiagnosticReport) -> Unit) {
            coroutineScope.launch { action(screenModel.report(hideServers)) }
        }

        if (showClearDialog) {
            TwoChoicesDialog(
                title = stringResource(R.string.clear_log),
                text = stringResource(R.string.clear_log_question),
                icon = rememberVectorPainter(Icons.Default.Delete),
                confirmText = stringResource(R.string.clear),
                dismissText = stringResource(R.string.cancel),
                onDismiss = { showClearDialog = false },
                onConfirm = {
                    screenModel.clear()
                    showClearDialog = false
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.diagnostic_log)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { withReport { context.shareReport(it) } },
                            enabled = entries.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_log)
                            )
                        }

                        IconButton(
                            onClick = { showClearDialog = true },
                            enabled = entries.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.clear_log)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (entries.isNotEmpty()) {
                    ReportBar(
                        onCopy = {
                            withReport {
                                context.copyReport(it)
                                displayToast(context)
                            }
                        },
                        onReport = { withReport { context.reportOnGitHub(it) } }
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.shortSpacing),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.mediumSpacing,
                    end = MaterialTheme.spacing.mediumSpacing,
                    top = paddingValues.calculateTopPadding() + MaterialTheme.spacing.shortSpacing,
                    bottom = paddingValues.calculateBottomPadding() + MaterialTheme.spacing.mediumSpacing
                )
            ) {
                item(key = "privacy") {
                    PrivacyCard(
                        hideServers = hideServers,
                        onHideServersChange = { hideServers = it }
                    )
                }

                if (entries.isEmpty()) {
                    item(key = "empty") { EmptyLog() }
                } else {
                    item(key = "filters") {
                        LevelFilters(
                            entries = entries,
                            selected = filter,
                            onSelect = { filter = it }
                        )
                    }

                    items(visibleEntries, key = { it.id }) { entry ->
                        DiagnosticEntryItem(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyCard(
    hideServers: Boolean,
    onHideServersChange: (Boolean) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.mediumSpacing)) {
            Row {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.shortSpacing))

                Text(
                    text = stringResource(R.string.diagnostic_log_privacy),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            ShortSpacer()

            // the whole row toggles, a larger target than the switch alone
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = hideServers,
                        role = Role.Switch,
                        onValueChange = onHideServersChange
                    )
                    .padding(vertical = MaterialTheme.spacing.shortSpacing)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.hide_server_addresses),
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = stringResource(R.string.hide_server_addresses_summary),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.shortSpacing))

                Switch(checked = hideServers, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun LevelFilters(
    entries: List<DiagnosticEntry>,
    selected: LevelFilter,
    onSelect: (LevelFilter) -> Unit,
) {
    // scrolls rather than wraps, so long translations stay on one line on small phones
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.shortSpacing),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        LevelFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = {
                    Text(stringResource(filter.label, entries.count { it.level in filter.levels }))
                }
            )
        }
    }
}

@Composable
private fun DiagnosticEntryItem(entry: DiagnosticEntry) {
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    val presentation = levelPresentation(entry.level)
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.mediumSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = presentation.icon,
                    contentDescription = null,
                    tint = presentation.color,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.shortSpacing))

                // the level is written out, colour alone would not tell it
                Text(
                    text = "${presentation.label} · ${entry.tag}",
                    style = MaterialTheme.typography.labelLarge,
                    color = presentation.color,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = buildString {
                        append(formatter.format(Instant.ofEpochMilli(entry.timestamp)))
                        if (entry.repeatCount > 1) append(" · ×").append(entry.repeatCount)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ShortSpacer()

            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodyMedium
            )

            entry.summary?.let { summary ->
                VeryShortSpacer()

                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (entry.details != null && entry.details != entry.summary) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Text(
                        text = stringResource(if (expanded) R.string.hide_details else R.string.show_details)
                    )

                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // stack frames keep one line each, scrolled sideways rather than wrapped
                        Text(
                            text = entry.details,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            softWrap = false,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(MaterialTheme.spacing.shortSpacing)
                        )
                    }
                }
            }
        }
    }
}

private class LevelPresentation(val icon: Painter, val color: Color, val label: String)

@Composable
private fun levelPresentation(level: DiagnosticLevel): LevelPresentation = when (level) {
    DiagnosticLevel.ERROR -> LevelPresentation(
        icon = painterResource(R.drawable.ic_error),
        color = MaterialTheme.colorScheme.error,
        label = stringResource(R.string.level_error)
    )

    DiagnosticLevel.WARNING -> LevelPresentation(
        icon = painterResource(R.drawable.ic_warning),
        color = MaterialTheme.colorScheme.tertiary,
        label = stringResource(R.string.level_warning)
    )

    DiagnosticLevel.INFO -> LevelPresentation(
        icon = rememberVectorPainter(Icons.Outlined.Info),
        color = MaterialTheme.colorScheme.primary,
        label = stringResource(R.string.level_info)
    )
}

@Composable
private fun EmptyLog() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.veryLargeSpacing)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        MediumSpacer()

        Text(
            text = stringResource(R.string.diagnostic_log_empty_title),
            style = MaterialTheme.typography.titleMedium
        )

        ShortSpacer()

        Text(
            text = stringResource(R.string.diagnostic_log_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReportBar(
    onCopy: () -> Unit,
    onReport: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.shortSpacing),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = MaterialTheme.spacing.mediumSpacing,
                    vertical = MaterialTheme.spacing.shortSpacing
                )
        ) {
            OutlinedButton(onClick = onCopy) {
                Text(text = stringResource(R.string.copy_log))
            }

            Button(
                onClick = onReport,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_github),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.shortSpacing))

                Text(text = stringResource(R.string.report_on_github))
            }
        }
    }
}
