package com.readrops.app.util

import android.content.Context
import android.content.res.Configuration
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

    private fun localizedContext(locale: Locale): Context {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }
}
