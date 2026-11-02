package com.readrops.app.feeds.newfeed

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.readrops.api.localfeed.FeedUrlCheckResult
import com.readrops.api.localfeed.FeedUrlCheckStatus
import com.readrops.api.localfeed.TlsProblem
import com.readrops.app.R
import com.readrops.app.util.theme.ShortSpacer
import com.readrops.app.util.theme.VeryShortSpacer
import com.readrops.app.util.theme.spacing

private enum class Severity { SUCCESS, HINT, PROBLEM }

private enum class StepState { OK, PARTIAL, FAILED, SKIPPED }

private enum class ServerOwner { SOMEONE_ELSE, ME, ORGANIZATION }

private enum class ServerSoftware(val label: String) { CADDY("Caddy"), NGINX("Nginx"), APACHE("Apache"), UNKNOWN("") }

/**
 * The outcome of a feed address check, written for people who do not know what DNS or TLS is:
 * a plain title, what it means, the next step as a button, and the raw facts folded away.
 */
@Composable
fun FeedUrlCheckCard(
    result: FeedUrlCheckResult,
    onUseFeed: (String) -> Unit,
    onAddFeed: () -> Unit,
    onEnterCredentials: () -> Unit,
    onSwitchToPlainHttp: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val severity = result.severity()
    val (container, content, icon) = when (severity) {
        Severity.SUCCESS -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Default.CheckCircle)
        Severity.HINT -> Triple(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface, Icons.Default.Info)
        Severity.PROBLEM -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Default.Warning)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(MaterialTheme.spacing.mediumSpacing),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(MaterialTheme.spacing.mediumSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(MaterialTheme.spacing.shortSpacing))
                Text(stringResource(result.titleRes()), style = MaterialTheme.typography.titleMedium)
            }
            ShortSpacer()
            Text(result.explanation(), style = MaterialTheme.typography.bodyMedium)

            if (result.status == FeedUrlCheckStatus.TLS_FAILED && !result.plainHttpAvailable) {
                VeryShortSpacer()
                Text(stringResource(R.string.feed_check_tls_not_password), style = MaterialTheme.typography.bodySmall)
            }

            ShortSpacer()
            CheckSteps(result)

            val suggestions = result.discoveredFeeds.ifEmpty { result.nearbyFeeds }
            if (suggestions.isNotEmpty()) {
                ShortSpacer()
                Text(
                    stringResource(if (result.discoveredFeeds.isNotEmpty()) R.string.feed_check_choose_feed else R.string.feed_check_nearby_feeds),
                    style = MaterialTheme.typography.labelLarge
                )
                VeryShortSpacer()
                suggestions.forEach { SuggestedFeed(it, onUseFeed) }
            }

            NextStep(result, onAddFeed, onEnterCredentials, onSwitchToPlainHttp, onRetry)

            if (result.status == FeedUrlCheckStatus.TLS_FAILED && result.tlsProblem != TlsProblem.PROTOCOL) {
                ShortSpacer()
                TlsHelp(result)
            }

            TechnicalDetails(result)
        }
    }
}

@Composable
private fun CheckSteps(result: FeedUrlCheckResult) {
    val steps = listOf(
        R.string.feed_check_step_address,
        R.string.feed_check_step_server,
        if (result.isHttps) R.string.feed_check_step_secure_connection else R.string.feed_check_step_connection,
        R.string.feed_check_step_response,
        R.string.feed_check_step_feed,
    ).zip(result.stepStates())

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(MaterialTheme.spacing.shortSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(MaterialTheme.spacing.shortSpacing)) {
            steps.forEach { (label, state) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    StepIcon(state)
                    Spacer(Modifier.width(MaterialTheme.spacing.shortSpacing))
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state == StepState.SKIPPED) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIcon(state: StepState) {
    val description = stringResource(
        when (state) {
            StepState.OK -> R.string.feed_check_step_ok
            StepState.PARTIAL -> R.string.feed_check_step_partial
            StepState.FAILED -> R.string.feed_check_step_failed
            StepState.SKIPPED -> R.string.feed_check_step_skipped
        }
    )
    when (state) {
        StepState.OK -> Icon(Icons.Default.CheckCircle, description, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        StepState.PARTIAL -> Icon(Icons.Default.Info, description, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
        StepState.FAILED -> Icon(Icons.Default.Warning, description, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        StepState.SKIPPED -> Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(14.dp).border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

@Composable
private fun SuggestedFeed(url: String, onUseFeed: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(MaterialTheme.spacing.shortSpacing),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = MaterialTheme.spacing.shortSpacing)) {
            Text(
                url.substringAfter("://"),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onUseFeed(url) }) { Text(stringResource(R.string.feed_check_use_this_feed)) }
        }
    }
}

@Composable
private fun NextStep(
    result: FeedUrlCheckResult,
    onAddFeed: () -> Unit,
    onEnterCredentials: () -> Unit,
    onSwitchToPlainHttp: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val buttonModifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.shortSpacing)

    when {
        result.status == FeedUrlCheckStatus.FEED_RESPONSE ->
            Button(onClick = onAddFeed, modifier = buttonModifier) { Text(stringResource(R.string.feed_check_add_this_feed)) }

        result.status == FeedUrlCheckStatus.AUTH_REQUIRED ->
            FilledTonalButton(onClick = onEnterCredentials, modifier = buttonModifier) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(MaterialTheme.spacing.shortSpacing))
                Text(stringResource(if (result.credentialsSent) R.string.feed_check_fix_credentials else R.string.feed_check_enter_credentials))
            }

        result.status == FeedUrlCheckStatus.TLS_FAILED && result.plainHttpAvailable -> {
            OutlinedButton(onClick = onSwitchToPlainHttp, modifier = buttonModifier) { Text(stringResource(R.string.feed_check_use_plain_http)) }
            Text(stringResource(R.string.feed_check_plain_http_warning), style = MaterialTheme.typography.bodySmall)
        }

        result.status == FeedUrlCheckStatus.TLS_FAILED && result.tlsProblem == TlsProblem.NOT_YET_VALID ->
            OutlinedButton(onClick = { context.openSettings(Settings.ACTION_DATE_SETTINGS) }, modifier = buttonModifier) {
                Text(stringResource(R.string.feed_check_open_date_settings))
            }

        result.status in RETRYABLE || (result.status == FeedUrlCheckStatus.HTTP_ERROR && (result.httpCode ?: 0) >= 500) ->
            OutlinedButton(onClick = onRetry, modifier = buttonModifier) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(MaterialTheme.spacing.shortSpacing))
                Text(stringResource(R.string.feed_check_retry))
            }
    }
}

@Composable
private fun TlsHelp(result: FeedUrlCheckResult) {
    val context = LocalContext.current
    var owner by rememberSaveable { mutableStateOf<ServerOwner?>(null) }
    var software by rememberSaveable { mutableStateOf<ServerSoftware?>(null) }
    val checkCommand = tlsCheckCommand(result)

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    ShortSpacer()
    Text(stringResource(R.string.feed_check_who_manages), style = MaterialTheme.typography.titleSmall)
    VeryShortSpacer()

    ServerOwner.entries
        .filter { it != ServerOwner.ORGANIZATION || result.tlsProblem in setOf(TlsProblem.UNTRUSTED, TlsProblem.OTHER) }
        .forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .selectable(selected = owner == option, role = Role.RadioButton, onClick = { owner = option })
            ) {
                RadioButton(selected = owner == option, onClick = null)
                Spacer(Modifier.width(MaterialTheme.spacing.shortSpacing))
                Text(
                    stringResource(
                        when (option) {
                            ServerOwner.SOMEONE_ELSE -> R.string.feed_check_owner_someone_else
                            ServerOwner.ME -> R.string.feed_check_owner_me
                            ServerOwner.ORGANIZATION -> R.string.feed_check_owner_organization
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.shortSpacing)
                )
            }
        }

    when (owner) {
        ServerOwner.SOMEONE_ELSE -> {
            Text(stringResource(R.string.feed_check_someone_else_help), style = MaterialTheme.typography.bodySmall)
            val message = stringResource(
                R.string.feed_check_admin_message,
                result.host,
                stringResource(result.tlsProblemLabel()),
                checkCommand
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.shortSpacing)) {
                FilledTonalButton(onClick = { context.copyText(message) }) {
                    Icon(painterResource(R.drawable.ic_copy), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(MaterialTheme.spacing.shortSpacing))
                    Text(stringResource(R.string.feed_check_copy_message))
                }
                OutlinedButton(onClick = { context.shareText(message) }) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(MaterialTheme.spacing.shortSpacing))
                    Text(stringResource(R.string.feed_check_share))
                }
            }
        }

        ServerOwner.ME -> {
            Text(stringResource(R.string.feed_check_which_software), style = MaterialTheme.typography.bodySmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.shortSpacing),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                ServerSoftware.entries.forEach { option ->
                    FilterChip(
                        selected = software == option,
                        onClick = { software = option },
                        label = { Text(option.label.ifEmpty { stringResource(R.string.feed_check_software_unknown) }) }
                    )
                }
            }
            software?.let { selected ->
                ShortSpacer()
                Text(stringResource(R.string.feed_check_step_check_title), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.feed_check_step_check_help), style = MaterialTheme.typography.bodySmall)
                CommandBlock(checkCommand)

                val fix = tlsFix(result, selected)
                if (fix != null) {
                    ShortSpacer()
                    Text(stringResource(R.string.feed_check_step_fix_title), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(fix.first, result.host), style = MaterialTheme.typography.bodySmall)
                    fix.second?.let { CommandBlock(it) }
                } else {
                    VeryShortSpacer()
                    Text(stringResource(R.string.feed_check_software_unknown_help), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        ServerOwner.ORGANIZATION -> {
            Text(stringResource(R.string.feed_check_organization_help), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = { context.openSettings(Settings.ACTION_SECURITY_SETTINGS) },
                modifier = Modifier.padding(top = MaterialTheme.spacing.shortSpacing)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(MaterialTheme.spacing.shortSpacing))
                Text(stringResource(R.string.feed_check_open_security_settings))
            }
        }

        null -> Unit
    }
}

@Composable
private fun CommandBlock(command: String) {
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = RoundedCornerShape(MaterialTheme.spacing.shortSpacing),
        modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.veryShortSpacing)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                command,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(MaterialTheme.spacing.shortSpacing)
            )
            IconButton(onClick = { context.copyText(command) }) {
                Icon(painterResource(R.drawable.ic_copy), contentDescription = stringResource(R.string.copy_log))
            }
        }
    }
}

@Composable
private fun TechnicalDetails(result: FeedUrlCheckResult) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.padding(top = MaterialTheme.spacing.shortSpacing)) {
        Text(stringResource(R.string.feed_check_technical_details))
        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
    }

    if (expanded) {
        val report = result.report()
        Text(report, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        VeryShortSpacer()
        Text(stringResource(R.string.feed_check_report_private), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { context.copyText(report) }) {
            Icon(painterResource(R.drawable.ic_copy), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(MaterialTheme.spacing.shortSpacing))
            Text(stringResource(R.string.feed_check_copy_report))
        }
    }
}

private val RETRYABLE = setOf(
    FeedUrlCheckStatus.TIMEOUT,
    FeedUrlCheckStatus.CONNECTION_REFUSED,
    FeedUrlCheckStatus.NETWORK_ERROR,
)

private fun FeedUrlCheckResult.severity(): Severity = when {
    status == FeedUrlCheckStatus.FEED_RESPONSE -> Severity.SUCCESS
    status == FeedUrlCheckStatus.WEBSITE_WITH_FEEDS || nearbyFeeds.isNotEmpty() -> Severity.HINT
    status == FeedUrlCheckStatus.AUTH_REQUIRED && !credentialsSent -> Severity.HINT
    else -> Severity.PROBLEM
}

/** Address, server, connection, response, feed: where the check stopped and why. */
private fun FeedUrlCheckResult.stepStates(): List<StepState> {
    val failedAt = when (status) {
        FeedUrlCheckStatus.INVALID_URL -> 0
        FeedUrlCheckStatus.DNS_FAILED -> 1
        FeedUrlCheckStatus.TLS_FAILED, FeedUrlCheckStatus.TIMEOUT,
        FeedUrlCheckStatus.CONNECTION_REFUSED, FeedUrlCheckStatus.NETWORK_ERROR -> 2
        FeedUrlCheckStatus.AUTH_REQUIRED, FeedUrlCheckStatus.FORBIDDEN, FeedUrlCheckStatus.NOT_FOUND,
        FeedUrlCheckStatus.METHOD_NOT_ALLOWED, FeedUrlCheckStatus.HTTP_ERROR -> 3
        FeedUrlCheckStatus.WEBSITE_NO_FEEDS, FeedUrlCheckStatus.UNRECOGNIZED_CONTENT,
        FeedUrlCheckStatus.WEBSITE_WITH_FEEDS -> 4
        FeedUrlCheckStatus.FEED_RESPONSE -> 5
    }
    val lastState = if (status == FeedUrlCheckStatus.WEBSITE_WITH_FEEDS) StepState.PARTIAL else StepState.FAILED

    return List(5) { index ->
        when {
            index < failedAt -> StepState.OK
            index == failedAt -> lastState
            else -> StepState.SKIPPED
        }
    }
}

@StringRes
private fun FeedUrlCheckResult.titleRes(): Int = when (status) {
    FeedUrlCheckStatus.INVALID_URL -> R.string.feed_check_title_invalid_url
    FeedUrlCheckStatus.DNS_FAILED -> R.string.feed_check_title_dns_failed
    FeedUrlCheckStatus.TLS_FAILED ->
        if (plainHttpAvailable) R.string.feed_check_title_plain_http else R.string.feed_check_title_tls_failed
    FeedUrlCheckStatus.TIMEOUT -> R.string.feed_check_title_timeout
    FeedUrlCheckStatus.CONNECTION_REFUSED -> R.string.feed_check_title_connection_refused
    FeedUrlCheckStatus.NETWORK_ERROR -> R.string.feed_check_title_network_error
    FeedUrlCheckStatus.AUTH_REQUIRED ->
        if (credentialsSent) R.string.feed_check_title_credentials_rejected else R.string.feed_check_title_auth_required
    FeedUrlCheckStatus.FORBIDDEN -> R.string.feed_check_title_forbidden
    FeedUrlCheckStatus.NOT_FOUND -> R.string.feed_check_title_not_found
    FeedUrlCheckStatus.METHOD_NOT_ALLOWED -> R.string.feed_check_title_method_not_allowed
    FeedUrlCheckStatus.HTTP_ERROR -> R.string.feed_check_title_http_error
    FeedUrlCheckStatus.FEED_RESPONSE -> R.string.feed_check_title_feed_found
    FeedUrlCheckStatus.WEBSITE_WITH_FEEDS -> R.string.feed_check_title_website_with_feeds
    FeedUrlCheckStatus.WEBSITE_NO_FEEDS -> R.string.feed_check_title_website_no_feeds
    FeedUrlCheckStatus.UNRECOGNIZED_CONTENT -> R.string.feed_check_title_unrecognized_content
}

@Composable
private fun FeedUrlCheckResult.explanation(): String = when (status) {
    FeedUrlCheckStatus.INVALID_URL -> stringResource(R.string.feed_check_invalid_url)
    FeedUrlCheckStatus.DNS_FAILED -> stringResource(R.string.feed_check_dns_failed)
    FeedUrlCheckStatus.TLS_FAILED -> stringResource(
        when {
            plainHttpAvailable -> R.string.feed_check_tls_plain_http
            else -> when (tlsProblem) {
                TlsProblem.EXPIRED -> R.string.feed_check_tls_expired
                TlsProblem.NOT_YET_VALID -> R.string.feed_check_tls_not_yet_valid
                TlsProblem.HOSTNAME_MISMATCH -> R.string.feed_check_tls_hostname
                TlsProblem.UNTRUSTED -> R.string.feed_check_tls_untrusted
                else -> R.string.feed_check_tls_failed
            }
        }
    )
    FeedUrlCheckStatus.TIMEOUT -> stringResource(R.string.feed_check_timeout)
    FeedUrlCheckStatus.CONNECTION_REFUSED -> stringResource(R.string.feed_check_connection_refused)
    FeedUrlCheckStatus.NETWORK_ERROR -> stringResource(R.string.feed_check_network_error)
    FeedUrlCheckStatus.AUTH_REQUIRED -> {
        val base = stringResource(if (credentialsSent) R.string.feed_check_credentials_rejected else R.string.feed_check_auth_required)
        val scheme = authScheme
        if (scheme != null && !scheme.equals("Basic", ignoreCase = true)) {
            base + " " + stringResource(R.string.feed_check_unsupported_auth, scheme)
        } else {
            base
        }
    }
    FeedUrlCheckStatus.FORBIDDEN -> stringResource(R.string.feed_check_forbidden)
    FeedUrlCheckStatus.NOT_FOUND -> stringResource(R.string.feed_check_not_found)
    FeedUrlCheckStatus.METHOD_NOT_ALLOWED -> stringResource(R.string.feed_check_method_not_allowed)
    FeedUrlCheckStatus.HTTP_ERROR -> stringResource(R.string.feed_check_http_error, httpCode ?: 0)
    FeedUrlCheckStatus.FEED_RESPONSE -> stringResource(R.string.feed_check_feed_found)
    FeedUrlCheckStatus.WEBSITE_WITH_FEEDS -> stringResource(R.string.feed_check_website_with_feeds)
    FeedUrlCheckStatus.WEBSITE_NO_FEEDS -> stringResource(R.string.feed_check_website_no_feeds)
    FeedUrlCheckStatus.UNRECOGNIZED_CONTENT -> stringResource(R.string.feed_check_unrecognized_content)
}

@StringRes
private fun FeedUrlCheckResult.tlsProblemLabel(): Int = when (tlsProblem) {
    TlsProblem.EXPIRED -> R.string.feed_check_tls_label_expired
    TlsProblem.NOT_YET_VALID -> R.string.feed_check_tls_label_not_yet_valid
    TlsProblem.HOSTNAME_MISMATCH -> R.string.feed_check_tls_label_hostname
    TlsProblem.UNTRUSTED -> R.string.feed_check_tls_label_untrusted
    else -> R.string.feed_check_tls_label_other
}

/** Read-only commands: they print the certificate, they change nothing on the server. */
private fun tlsCheckCommand(result: FeedUrlCheckResult): String {
    val target = "${result.host}:${result.port}"
    return "echo | openssl s_client -connect $target -servername ${result.host} -verify_hostname ${result.host} 2>/dev/null | grep -E \"^(subject|issuer)=|Verify return code\"\n" +
            "echo | openssl s_client -connect $target -servername ${result.host} 2>/dev/null | openssl x509 -noout -dates -ext subjectAltName"
}

/** An explanation, with the host as its argument, and the commands that apply it, if any. */
private fun tlsFix(result: FeedUrlCheckResult, software: ServerSoftware): Pair<Int, String?>? {
    if (software == ServerSoftware.UNKNOWN) return null
    val service = if (software == ServerSoftware.APACHE) "apache2" else software.name.lowercase()

    return when (result.tlsProblem) {
        TlsProblem.EXPIRED -> when (software) {
            ServerSoftware.CADDY -> R.string.feed_check_fix_renew_caddy to
                    "sudo systemctl reload caddy\nsudo journalctl -u caddy --since \"-1h\" | grep -iE \"certificate|acme|error\""
            else -> R.string.feed_check_fix_renew to "sudo certbot renew\nsudo systemctl reload $service\nsystemctl list-timers | grep certbot"
        }

        TlsProblem.NOT_YET_VALID -> R.string.feed_check_fix_clock to "timedatectl\nsudo timedatectl set-ntp true"

        TlsProblem.HOSTNAME_MISMATCH -> when (software) {
            ServerSoftware.CADDY -> R.string.feed_check_fix_name_caddy to
                    "${result.host} {\n    reverse_proxy localhost:8080\n}\n\nsudo systemctl reload caddy"
            else -> R.string.feed_check_fix_name_certbot to "sudo certbot --${software.name.lowercase()} -d ${result.host}"
        }

        TlsProblem.UNTRUSTED -> when (software) {
            ServerSoftware.CADDY -> R.string.feed_check_fix_chain_caddy to "sudo systemctl reload caddy"
            ServerSoftware.NGINX -> R.string.feed_check_fix_chain_file to
                    "ssl_certificate /etc/letsencrypt/live/${result.host}/fullchain.pem;\n\nsudo nginx -t && sudo systemctl reload nginx"
            else -> R.string.feed_check_fix_chain_file to
                    "SSLCertificateFile /etc/letsencrypt/live/${result.host}/fullchain.pem\n\nsudo apachectl configtest && sudo systemctl reload apache2"
        }

        else -> R.string.feed_check_fix_logs to when (software) {
            ServerSoftware.CADDY -> "sudo journalctl -u caddy --since \"-1h\" | grep -i tls"
            ServerSoftware.NGINX -> "sudo nginx -T | grep -E \"listen|ssl_\""
            else -> "sudo apachectl -S"
        }
    }
}

/** Facts only, without the address or credentials, so it can go into a public issue as is. */
private fun FeedUrlCheckResult.report(): String = buildString {
    appendLine("Droidrops feed check")
    appendLine("Result: $status")
    appendLine("Scheme: ${if (isHttps) "https" else "http"}, port $port")
    appendLine("DNS addresses: ${resolvedAddresses.size}")
    tlsProblem?.let { appendLine("TLS: $it") }
    if (status == FeedUrlCheckStatus.TLS_FAILED) appendLine("Plain HTTP on this port: ${if (plainHttpAvailable) "yes" else "no"}")
    httpCode?.let { appendLine("HTTP: $it") }
    if (contentType.isNotBlank()) appendLine("Content-Type: $contentType")
    allowedMethods?.let { appendLine("Allow: $it") }
    authScheme?.let { appendLine("Authentication: $it") }
    appendLine("Credentials sent: ${if (credentialsSent) "yes" else "no"}")
    if (discoveredFeeds.isNotEmpty()) appendLine("Feeds declared by the page: ${discoveredFeeds.size}")
    if (nearbyFeeds.isNotEmpty()) appendLine("Feeds found on the same site: ${nearbyFeeds.size}")
    detail?.let { appendLine("Error: $it") }
    append("Duration: $durationMs ms")
}

private fun Context.copyText(text: String) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText("Droidrops", text))
    Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
}

private fun Context.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        startActivity(Intent.createChooser(intent, getString(R.string.feed_check_share)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.no_app_to_open_link, Toast.LENGTH_SHORT).show()
    }
}

private fun Context.openSettings(action: String) {
    try {
        startActivity(Intent(action))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
