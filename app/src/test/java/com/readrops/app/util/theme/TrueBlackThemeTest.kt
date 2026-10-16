package com.readrops.app.util.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class TrueBlackThemeTest {

    @Test
    fun trueBlackThemeUsesBlackForBackgroundAndMaterialSurfaces() {
        assertEquals(Color.Black, trueBlackDarkScheme.background)
        assertEquals(Color.Black, trueBlackDarkScheme.surface)
        assertEquals(Color.Black, trueBlackDarkScheme.surfaceContainerLowest)
        assertEquals(Color.Black, trueBlackDarkScheme.surfaceContainer)
        assertEquals(Color.Black, trueBlackDarkScheme.surfaceContainerHighest)
    }

    @Test
    fun trueBlackThemeKeepsSurfaceVariantVisibleForContrast() {
        assertEquals(Color(0xFF121212), trueBlackDarkScheme.surfaceVariant)
    }
}
