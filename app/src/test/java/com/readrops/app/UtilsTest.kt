package com.readrops.app

import com.readrops.app.util.Utils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilsTest {

    @Test
    fun formatSelectionWithReferenceIncludesSelectedTextAndSource() {
        assertEquals(
            "Useful excerpt\n\nJane Doe, \"Research notes\", Science Weekly\n\nhttps://example.org/article",
            Utils.formatSelectionWithReference(
                selectedText = " Useful excerpt ",
                author = "Jane Doe",
                title = "Research notes",
                feedName = "Science Weekly",
                url = "https://example.org/article"
            )
        )
    }

    @Test
    fun formatSelectionWithReferenceOmitsMissingMetadata() {
        assertEquals(
            "A quote\n\n\"Untitled?\"",
            Utils.formatSelectionWithReference(
                selectedText = "A quote",
                author = null,
                title = "Untitled?",
                feedName = null,
                url = null
            )
        )
    }

    @Test
    fun truncateStringTest() {
        val result =
            Utils.truncateString("This is a very very long string with more than 30 characters", 30)

        assertEquals(33, result.length)
        assertTrue(result.contains("This is a very very long strin"))
        assertTrue(result.endsWith("..."))
    }

    @Test
    fun normalizeUrlTrimsWhitespaceAndRecognizesSchemesOnlyAtStart() {
        assertEquals("https://example.org/fever.php/", Utils.normalizeUrl("  example.org/fever.php  "))
        assertEquals("HTTP://example.org/fever.php/", Utils.normalizeUrl(" HTTP://example.org/fever.php "))
        assertEquals("https://example.org/path/http://legacy/", Utils.normalizeUrl("https://example.org/path/http://legacy"))
    }

    @Test
    fun normalizeUrlKeepsQueryAndFragmentOutsideThePath() {
        assertEquals(
            "https://example.org/fever.php/?token=abc&mode=read#api",
            Utils.normalizeUrl(" https://example.org/fever.php?token=abc&mode=read#api ")
        )
        assertEquals("https://example.org/?token=abc", Utils.normalizeUrl("example.org?token=abc"))
    }

    @Test
    fun normalizeUrlLeavesBlankInputBlank() {
        assertEquals("", Utils.normalizeUrl("   "))
    }
}
