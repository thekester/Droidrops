package com.readrops.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import java.security.KeyStore
import java.util.UUID

class EncryptedPreferencesRecoveryTest {

    @Test
    fun staleEncryptedPreferencesDoNotPreventStartupTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferencesName = "encrypted_preferences_recovery_test"
        val masterKeyAlias = "droidrops_test_${UUID.randomUUID()}"

        try {
            val oldPreferences = createEncryptedPreferences(context, preferencesName, masterKeyAlias)
            check(oldPreferences.edit().putString("credential", "unreadable-after-key-loss").commit())

            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
                deleteEntry(masterKeyAlias)
            }

            val recoveredPreferences = createEncryptedPreferences(context, preferencesName, masterKeyAlias)

            assertFalse(recoveredPreferences.contains("credential"))
        } finally {
            context.deleteSharedPreferences(preferencesName)
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
                deleteEntry(masterKeyAlias)
            }
        }
    }
}
