package com.capsula.app.domain

/**
 * Turns a letter into "sealed" glyphs. It is not real encryption (the text never leaves the
 * device): it shows the *shape* of your words while hiding the content. Whitespace is kept,
 * so paragraphs and word lengths are still visible.
 *
 * The [salt] changes every second on the detail screen, so the seal visibly "shimmers".
 */
object SealCipher {

    private const val GLYPHS = "░▒▓◆◇◈●○◐◑◒◓※§¤∆∑Ω≈#@%&"

    fun glyphFor(char: Char, salt: Int): Char {
        val index = ((char.code * 31 + salt * 17) and Int.MAX_VALUE) % GLYPHS.length
        return GLYPHS[index]
    }

    fun scramble(text: String, salt: Int): String = buildString(text.length) {
        text.forEachIndexed { index, char ->
            append(if (char.isWhitespace()) char else glyphFor(char, salt + index))
        }
    }

    /**
     * Partially decoded text: the first [revealedCount] characters are real, the rest are glyphs.
     * Used by the opening animation.
     */
    fun partiallyRevealed(text: String, revealedCount: Int, salt: Int): String = buildString(text.length) {
        text.forEachIndexed { index, char ->
            append(
                when {
                    index < revealedCount || char.isWhitespace() -> char
                    else -> glyphFor(char, salt + index)
                },
            )
        }
    }
}
