@file:Suppress("DEPRECATION")

package com.readrops.app.util.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.readrops.app.R

private fun Uri.isWebUrl(): Boolean = scheme == "http" || scheme == "https"

fun Context.openUrl(url: String) {
    val uri = url.toUri()

    if (!uri.isWebUrl()) {
        toast(R.string.link_not_supported)
        return
    }

    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (exception: ActivityNotFoundException) {
        // no browser installed: tell the user instead of crashing
        toast(R.string.no_app_to_open_link)
    }
}

private fun Context.toast(@StringRes resId: Int) =
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

fun Context.openInCustomTab(url: String, theme: String?, color: Color) {
    val uri = url.toUri()

    if (!uri.isWebUrl()) {
        toast(R.string.link_not_supported)
        return
    }

    val colorScheme = when (theme) {
        "light" -> CustomTabsIntent.COLOR_SCHEME_LIGHT
        "dark" -> CustomTabsIntent.COLOR_SCHEME_DARK
        else -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
    }

    CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams
                .Builder()
                .setToolbarColor(color.toArgb())
                .build()
        )
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .setUrlBarHidingEnabled(true)
        .setColorScheme(colorScheme)
        .build()
        .also { customTabsIntent ->
            try {
                customTabsIntent.launchUrl(this, uri)
            } catch (exception: ActivityNotFoundException) {
                // no browser supporting custom tabs: fall back to a plain view intent
                openUrl(url)
            }
        }
}

// TODO arbitrary value, we might want to use windowClasses in the future
fun Context.isTabletUi(): Boolean {
    val configuration = resources.configuration
    return configuration.smallestScreenWidthDp >= 720
}

@Composable
@ReadOnlyComposable
fun isTabletUi(): Boolean = LocalContext.current.isTabletUi()


// non depreciated APIs are only available from API 23
@Suppress("DEPRECATION")
fun Context.isConnected(): Boolean {
    val connectivityManager = getSystemService<ConnectivityManager>()!!
    val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo

    return networkInfo?.isConnected == true
}
