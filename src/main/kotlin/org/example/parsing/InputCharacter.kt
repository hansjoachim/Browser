package org.example.parsing


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

    //FIXME: more precise
    fun isAlpha(): Boolean {
        return type == InputCharacterType.Character && Character.isAlphabetic(character.code)
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

    fun isWhitespace(): Boolean {
        return type == InputCharacterType.Character && Character.isWhitespace(character.code)
    }

    fun isUpperCase(): Boolean {
        return type == InputCharacterType.Character && Character.isUpperCase(character.code)
    }

    fun toLowerCase(): Char {
        return Character.toLowerCase(character)
    }

    override fun toString(): String {
        return "InputCharacter(type=$type, character=$character)"
    }
}