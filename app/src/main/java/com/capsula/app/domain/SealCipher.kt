package com.capsula.app.domain

// / Класс для шифрования текста капсулы с использованием глифов.
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

    // Возвращает частично раскрытый текст капсулы, где первые revealedCount символов остаются видимыми, а остальные заменяются глифами.
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
