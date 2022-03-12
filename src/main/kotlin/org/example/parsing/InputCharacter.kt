package org.example.parsing

import org.example.parsing.Tokenizer.Companion.CHARACTER_TABULATION
import org.example.parsing.Tokenizer.Companion.FORM_FEED
import org.example.parsing.Tokenizer.Companion.LINE_FEED
import org.example.parsing.Tokenizer.Companion.SPACE


enum class InputCharacterType {
    Character,
    EndOfFile
}

class InputCharacter(
    private val type: InputCharacterType = InputCharacterType.Character,
    val character: Char = ' '
) {
    fun matches(reference: Char): Boolean {
        return type == InputCharacterType.Character && character == reference
    }

    fun isEndOfFile(): Boolean {
        return type == InputCharacterType.EndOfFile
    }

    fun isAsciiDigit(): Boolean {
        return type == InputCharacterType.Character && character.code >= 0x0030 && character.code <= 0x0039
    }

    fun isAsciiUpperHexDigit(): Boolean {
        return isAsciiDigit() || (type == InputCharacterType.Character && character.code >= 0x0041 && character.code <= 0x0046)
    }

    fun isAsciiLowerHexDigit(): Boolean {
        return isAsciiDigit() || (type == InputCharacterType.Character && character.code >= 0x0061 && character.code <= 0x0066)
    }

    fun isAsciiHexDigit(): Boolean {
        return isAsciiUpperHexDigit() || isAsciiLowerHexDigit()
    }

    fun isAsciiUpperAlpha(): Boolean {
        return type == InputCharacterType.Character && character.code >= 0x0041 && character.code <= 0x005A
    }

    fun isAsciiLowerAlpha(): Boolean {
        return type == InputCharacterType.Character && character.code >= 0x0061 && character.code <= 0x007A
    }

    fun isAsciiAlpha(): Boolean {
        return isAsciiUpperAlpha() || isAsciiLowerAlpha()
    }

    fun isAsciiAlphaNumeric(): Boolean {
        return isAsciiDigit() || isAsciiAlpha()
    }

    fun isWhitespace(): Boolean {
        val whiteSpaceCharacters = listOf(
            CHARACTER_TABULATION,
            LINE_FEED,
            FORM_FEED,
            SPACE,
        )
        return type == InputCharacterType.Character && whiteSpaceCharacters.contains(character)
    }

    override fun toString(): String {
        return "InputCharacter(type=$type, character=$character)"
    }
}