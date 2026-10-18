package com.readrops.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.readrops.app.testutil.ReadropsTestRule
import com.readrops.app.util.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
class TimelineSourcePreferenceTest : KoinTest {
    @get:Rule
    val koinRule = ReadropsTestRule()

    @Test
    fun sourceVisibilityPreferenceDefaultsOnAndCanBeTurnedOff() = runBlocking {
        val preference = get<Preferences>().showTimelineSource

        assertTrue(preference.flow.first())
        preference.write(false)
        assertFalse(preference.flow.first())
        preference.write(true)
        assertTrue(preference.flow.first())
    }

    @Test
    fun dateAndReadTimeVisibilityPreferencesDefaultOnAndCanBeTurnedOff() = runBlocking {
        val preferences = get<Preferences>()

        assertTrue(preferences.showTimelineDate.flow.first())
        assertTrue(preferences.showTimelineReadTime.flow.first())

        preferences.showTimelineDate.write(false)
        preferences.showTimelineReadTime.write(false)
        assertFalse(preferences.showTimelineDate.flow.first())
        assertFalse(preferences.showTimelineReadTime.flow.first())

        preferences.showTimelineDate.write(true)
        preferences.showTimelineReadTime.write(true)
        assertTrue(preferences.showTimelineDate.flow.first())
        assertTrue(preferences.showTimelineReadTime.flow.first())
    }

    @Test
    fun articleTextSizePreferenceDefaultsToNormalAndPersistsSelection() = runBlocking {
        val preference = get<Preferences>().articleFontSize

        assertTrue(preference.flow.first() == "normal")
        preference.write("extra_large")
        assertTrue(preference.flow.first() == "extra_large")
        preference.write("normal")
        assertTrue(preference.flow.first() == "normal")
    }
}
