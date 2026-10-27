package com.capsula.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.capsula.app.domain.SealCipher
import kotlinx.coroutines.delay

/**
 * Shows [text] "decoding" from seal glyphs into real letters, left to right.
 *
 * The number of revealed characters is kept in [rememberSaveable], so rotating the phone
 * mid-animation continues from the same place instead of restarting.
 */
@Composable
fun DecodingText(
    text: String,
    animate: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    var revealed by rememberSaveable(text) { mutableIntStateOf(if (animate) 0 else text.length) }

    LaunchedEffect(text) {
        // Long letters reveal several characters per frame so the effect always takes about 2.5 s.
        val step = (text.length / 150).coerceAtLeast(1)
        while (revealed < text.length) {
            delay(16)
            revealed = (revealed + step).coerceAtMost(text.length)
        }
    }

    val shown = remember(text, revealed) {
        if (revealed >= text.length) text else SealCipher.partiallyRevealed(text, revealed, salt = revealed)
    }
    Text(text = shown, style = style, color = color, modifier = modifier)
}
