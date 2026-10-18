package com.readrops.app.util

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardShortcutsTest {

    @Test
    fun readerKeysMapToTheirActions() {
        assertEquals(Shortcut.NEXT_ARTICLE, shortcutFor(KeyEvent.KEYCODE_J, hasModifiers = false))
        assertEquals(Shortcut.NEXT_ARTICLE, shortcutFor(KeyEvent.KEYCODE_N, hasModifiers = false))
        assertEquals(Shortcut.PREVIOUS_ARTICLE, shortcutFor(KeyEvent.KEYCODE_K, hasModifiers = false))
        assertEquals(Shortcut.PREVIOUS_ARTICLE, shortcutFor(KeyEvent.KEYCODE_P, hasModifiers = false))
        assertEquals(Shortcut.TOGGLE_READ, shortcutFor(KeyEvent.KEYCODE_M, hasModifiers = false))
        assertEquals(Shortcut.TOGGLE_STAR, shortcutFor(KeyEvent.KEYCODE_S, hasModifiers = false))
        assertEquals(Shortcut.FULL_ARTICLE, shortcutFor(KeyEvent.KEYCODE_F, hasModifiers = false))
        assertEquals(Shortcut.OPEN_IN_BROWSER, shortcutFor(KeyEvent.KEYCODE_O, hasModifiers = false))
        assertEquals(Shortcut.OPEN_IN_BROWSER, shortcutFor(KeyEvent.KEYCODE_V, hasModifiers = false))
    }

    @Test
    fun modifiedKeysAreLeftToTheSystem() {
        // Ctrl+S, Ctrl+V and the like keep their usual meaning
        assertNull(shortcutFor(KeyEvent.KEYCODE_S, hasModifiers = true))
        assertNull(shortcutFor(KeyEvent.KEYCODE_V, hasModifiers = true))
    }

    @Test
    fun otherKeysAreIgnored() {
        assertNull(shortcutFor(KeyEvent.KEYCODE_SPACE, hasModifiers = false))
        assertNull(shortcutFor(KeyEvent.KEYCODE_DPAD_DOWN, hasModifiers = false))
        assertNull(shortcutFor(KeyEvent.KEYCODE_A, hasModifiers = false))
    }
}
