package com.readrops.app.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.AdaptiveIconDrawable
import android.appwidget.AppWidgetManager
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.graphics.toArgb
import com.readrops.app.R
import com.readrops.app.util.accounterror.AccountError
import com.readrops.app.util.extensions.bestForegroundOn
import com.readrops.app.util.extensions.canDisplayOnBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import org.xmlpull.v1.XmlPullParser
import javax.net.ssl.SSLHandshakeException

@RunWith(AndroidJUnit4::class)
class IssueRegressionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun lowContrastFeedColorFallsBackAndForegroundChoosesReadableText() {
        val background = android.graphics.Color.parseColor("#F8FDFF")
        val orange = android.graphics.Color.parseColor("#FF9500")

        assertFalse(orange.canDisplayOnBackground(background))
        assertTrue(android.graphics.Color.BLACK.canDisplayOnBackground(background))
        assertEquals(android.graphics.Color.BLACK, bestForegroundOn(orange).toArgb())
    }

    @Test
    fun crashMessageUsesCorrectGitHubBrandingInEnglishAndFrench() {
        assertTrue(localizedContext(Locale.ENGLISH).getString(R.string.crash_message).contains("GitHub"))
        assertTrue(localizedContext(Locale.FRENCH).getString(R.string.crash_message).contains("GitHub"))
        assertFalse(localizedContext(Locale.ENGLISH).getString(R.string.crash_message).contains("Github"))
    }

    @Test
    fun tlsLoginFailureShowsActionableCertificateGuidance() {
        val englishContext = localizedContext(Locale.ENGLISH)
        val message = AccountError.Companion.DefaultAccountError(englishContext)
            .genericMessage(SSLHandshakeException("Trust anchor for certification path not found"))

        assertEquals(englishContext.getString(R.string.tls_error), message)
        assertTrue(message.contains("certificate", ignoreCase = true))
    }

    @Test
    fun localAccountUsesDedicatedPhoneIcon() {
        val icon = requireNotNull(context.getDrawable(R.drawable.ic_local_account))

        val density = context.resources.displayMetrics.density
        assertEquals(24f, icon.intrinsicWidth / density, 0.1f)
        assertEquals(24f, icon.intrinsicHeight / density, 0.1f)
    }

    @Test
    fun launcherAdaptiveIconProvidesMonochromeLayerForThemedIcons() {
        val icon = context.getDrawable(R.mipmap.ic_launcher)

        assertTrue("Launcher icon should be adaptive", icon is AdaptiveIconDrawable)
        assertTrue("Themed icons need a monochrome layer", (icon as AdaptiveIconDrawable).monochrome != null)
    }

    @Test
    fun latestArticlesWidgetIsPublishedToTheLauncherWidgetPicker() {
        val widget = AppWidgetManager.getInstance(context)
            .getInstalledProvidersForPackage(context.packageName, null)
            .single { it.provider.className.endsWith("LatestArticlesWidget") }

        assertTrue(widget.minWidth > 0)
        assertTrue(widget.minHeight > 0)
        assertTrue(widget.initialLayout != 0)
    }

    @Test
    fun backupRulesExcludeSessionDatabaseAndEncryptedCredentials() {
        val files = listOf(R.xml.backup_rules, R.xml.data_extraction_rules)
        for (resourceId in files) {
            val parser = context.resources.getXml(resourceId)
            val excludedPaths = mutableSetOf<String>()
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "exclude") {
                    if (parser.getAttributeValue(null, "domain") == "database") {
                        excludedPaths += parser.getAttributeValue(null, "path")
                    }
                    if (parser.getAttributeValue(null, "domain") == "sharedpref") {
                        excludedPaths += parser.getAttributeValue(null, "path")
                    }
                }
                parser.next()
            }
            parser.close()

            assertTrue("Room database must not be backed up", "readrops-db" in excludedPaths)
            assertTrue("Encrypted credentials must not be backed up", "account_credentials.xml" in excludedPaths)
        }
    }

    private fun localizedContext(locale: Locale): Context {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }
}
