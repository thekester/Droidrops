package com.readrops.app.util.diagnostics

/**
 * Keeps what ends up in the diagnostic log safe to paste into a public GitHub issue.
 *
 * Secrets are always removed, at the moment an entry is recorded, so they never reach the disk.
 * Server addresses are only masked on export, and only if the user asks: a self hosted
 * instance often has a personal domain, yet the port and the error are what diagnose a
 * network that blocks the connection, so both are kept.
 */
object DiagnosticRedaction {

    private const val REDACTED = "<redacted>"
    private const val SERVER = "<server>"
    private const val IP = "<ip>"

    private val urlCredentials = Regex("""(?i)\b(https?://)[^/\s@]+@""")
    private val sensitiveQueryParameter = Regex(
        """(?i)([?&](?:email|passwd|password|pass|login|user|username|api_?key|access_?token|""" +
                """token|auth|sid|secret|client_?secret|key|t)=)[^&\s#"']*"""
    )
    private val authorizationHeader =
        Regex("""(?i)\b(authorization|proxy-authorization|cookie|set-cookie)(\s*[:=]\s*)[^\r\n]*""")
    private val authorizationScheme = Regex("""(?i)\b(GoogleLogin\s+auth=|Bearer\s+|Basic\s+)[\w.~+/=-]+""")
    private val clientLoginToken = Regex("""(?im)^(\s*(?:SID|LSID|Auth)=)\S+""")
    private val email = Regex("""[\w.%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    private val urlHost = Regex("""(?i)\b(https?://)(\[[0-9a-f:.]+]|[^/\s:?#"'<>]+)""")
    private val okHttpHostAndIp = Regex("""\b[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*/\d{1,3}(?:\.\d{1,3}){3}\b""")
    private val quotedHost = Regex("""(?i)(\bhost(?:name)?\s+")([^"]+)(")""")
    private val ipv4 = Regex("""\b\d{1,3}(?:\.\d{1,3}){3}\b""")

    fun redactSecrets(text: String): String = text
        .replace(urlCredentials) { "${it.groupValues[1]}$REDACTED@" }
        .replace(sensitiveQueryParameter) { "${it.groupValues[1]}$REDACTED" }
        .replace(authorizationHeader) { "${it.groupValues[1]}${it.groupValues[2]}$REDACTED" }
        .replace(authorizationScheme) { "${it.groupValues[1]}$REDACTED" }
        .replace(clientLoginToken) { "${it.groupValues[1]}$REDACTED" }
        .replace(email, "<email>")

    /**
     * Replaces host names and IP addresses, keeping ports and paths. [knownHosts], typically the
     * account servers, are replaced wherever they appear, which catches the messages whose
     * format no pattern here anticipates.
     */
    fun maskServerAddresses(text: String, knownHosts: Set<String> = emptySet()): String {
        var masked = text
        knownHosts.filter { it.isNotBlank() }
            .sortedByDescending { it.length }
            .forEach { host -> masked = masked.replace(host, SERVER, ignoreCase = true) }

        return masked
            .replace(urlHost) { "${it.groupValues[1]}$SERVER" }
            .replace(okHttpHostAndIp, "$SERVER/$IP")
            .replace(quotedHost) { "${it.groupValues[1]}$SERVER${it.groupValues[3]}" }
            .replace(ipv4, IP)
    }
}
