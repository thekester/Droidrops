package com.readrops.app.util.extensions

import androidx.annotation.ColorInt
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.work.Data
import java.io.Serializable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun TextStyle.toDp(): Dp = fontSize.value.dp

val Data.serializables by lazy {
    mutableMapOf<String, Serializable>()
}

fun Data.putSerializable(key: String, parcelable: Serializable): Data {
    serializables[key] = parcelable
    return this
}

fun Data.getSerializable(key: String): Serializable? = serializables[key]

fun Data.clearSerializables() {
    serializables.clear()
}

/**
 * 3.0 is the WCAG AA floor for large text and for user interface components, so it is the
 * lowest bar any visible element should clear. The previous 1.75 let colours such as a mid
 * orange or a light green through, which rendered feed names around 2:1 against the
 * background. Colours below the bar fall back to the theme primary, which always reads.
 *
 * Small text formally asks for 4.5. The same colour is also used as a chip fill here, where
 * contrast against the page background is not the right criterion, so the floor is applied
 * uniformly rather than guessed per call site.
 */
fun Int.canDisplayOnBackground(@ColorInt background: Int, threshold: Float = 3f): Boolean =
    ColorUtils.calculateContrast(this, background) > threshold

/**
 * Returns whichever of white or black reads best on [background]. Comparing the two beats
 * testing one against a fixed threshold: a colour where white barely clears the bar often
 * gives black far more contrast, and the old test picked white anyway.
 */
fun bestForegroundOn(@ColorInt background: Int): Color =
    if (ColorUtils.calculateContrast(Color.White.toArgb(), background) >=
        ColorUtils.calculateContrast(Color.Black.toArgb(), background)
    ) {
        Color.White
    } else {
        Color.Black
    }
