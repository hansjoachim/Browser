package org.example.parsing


enum class NamedCharacterReference(val referenceName: String, val character: Int, val glyph: Char) {
    NBSP_SEMICOLON("nbsp;", Tokenizer.NBSP_CODE, Tokenizer.NBSP),
    NBSP("nbsp", Tokenizer.NBSP_CODE, Tokenizer.NBSP)

    //TODO : the rest of the table, including multiple characters in second column
}