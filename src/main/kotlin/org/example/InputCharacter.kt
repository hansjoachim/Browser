package org.example


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

    fun isAlpha(): Boolean {
        return type == InputCharacterType.Character && Character.isAlphabetic(character.code)
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