package com.readrops.app.util.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.getSystemService
import com.readrops.api.utils.NetworkFailureListener
import com.readrops.app.BuildConfig
import okhttp3.HttpUrl

/**
 * The network the device is on, in the words a bug report needs. "Wi-Fi, internet not
 * validated, captive portal" explains a failure on hospital or hotel Wi-Fi at a glance.
 * No network name is read, it would need the location permission and identify a place.
 */
fun describeNetwork(context: Context): String = runCatching {
    val connectivityManager = context.getSystemService<ConnectivityManager>() ?: return "unknown"

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        @Suppress("DEPRECATION")
        return connectivityManager.activeNetworkInfo?.typeName ?: "offline"
    }

    val network = connectivityManager.activeNetwork ?: return "offline"
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "unknown"

    val transport = when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile data"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        else -> "other network"
    }

    buildList {
        add(transport)
        if (transport != "VPN" && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            add("through a VPN")
        }
        add(
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                "internet validated"
            } else {
                "internet not validated"
            }
        )
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)) {
            add("captive portal")
        }
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            add("metered")
        }
    }.joinToString(", ")
}.getOrDefault("unknown")

fun diagnosticEnvironment(context: Context) = DiagnosticEnvironment(
    app = "Droidrops ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.BUILD_TYPE}",
    android = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    device = "${Build.MANUFACTURER} ${Build.MODEL}",
    network = describeNetwork(context),
)

/**
 * Records failed requests with the network they failed on. The query string is left out: it
 * can carry tokens, and the host, port and path are what locate the failure.
 */
fun diagnosticNetworkListener(log: DiagnosticLog, context: Context) =
    NetworkFailureListener { request, exception ->
        val url = request.url
        val port = if (url.port != HttpUrl.defaultPort(url.scheme)) ":${url.port}" else ""

        log.warning(
            tag = NETWORK_TAG,
            message = "${request.method} ${url.scheme}://${url.host}$port${url.encodedPath} " +
                    "on ${describeNetwork(context)}",
            throwable = exception
        )
    }

const val NETWORK_TAG = "Network"
