package com.readrops.app.util

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import java.util.concurrent.CopyOnWriteArrayList

/** Reading actions reachable from a hardware keyboard, with the usual feed reader keys. */
enum class Shortcut {
    NEXT_ARTICLE,
    PREVIOUS_ARTICLE,
    TOGGLE_READ,
    TOGGLE_STAR,
    FULL_ARTICLE,
    OPEN_IN_BROWSER,
}

/**
 * J/K (or N/P) move between articles, M toggles read, S toggles favorite, F loads the full
 * article and O (or V) opens it in the browser. Keys combined with Ctrl, Alt, Meta or Shift
 * are left to the system and to text input.
 */
internal fun shortcutFor(keyCode: Int, hasModifiers: Boolean): Shortcut? {
    if (hasModifiers) return null

    return when (keyCode) {
        KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_N -> Shortcut.NEXT_ARTICLE
        KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_P -> Shortcut.PREVIOUS_ARTICLE
        KeyEvent.KEYCODE_M -> Shortcut.TOGGLE_READ
        KeyEvent.KEYCODE_S -> Shortcut.TOGGLE_STAR
        KeyEvent.KEYCODE_F -> Shortcut.FULL_ARTICLE
        KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_V -> Shortcut.OPEN_IN_BROWSER
        else -> null
    }
}

/**
 * Screens register what they can do; the activity hands over the key events nothing else
 * consumed, so typing in a text field never triggers a shortcut.
 */
object KeyboardShortcuts {

    private val handlers = CopyOnWriteArrayList<(Shortcut) -> Boolean>()

    fun register(handler: (Shortcut) -> Boolean): () -> Unit {
        handlers += handler
        return { handlers -= handler }
    }

    /** Returns true when a registered screen handled the key. */
    fun dispatch(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return false

        val hasModifiers = event.isCtrlPressed || event.isAltPressed || event.isMetaPressed ||
            event.isShiftPressed
        val shortcut = shortcutFor(event.keyCode, hasModifiers) ?: return false

        // the most recently shown screen first, e.g. the reader pane before the timeline
        return handlers.reversed().any { it(shortcut) }
    }
}

/** Registers [onShortcut] while this composable is shown and [enabled] is true. */
@Composable
fun KeyboardShortcutHandler(enabled: Boolean = true, onShortcut: (Shortcut) -> Boolean) {
    val currentHandler by rememberUpdatedState(onShortcut)

    DisposableEffect(enabled) {
        val unregister = if (enabled) KeyboardShortcuts.register { currentHandler(it) } else null
        onDispose { unregister?.invoke() }
    }
}
