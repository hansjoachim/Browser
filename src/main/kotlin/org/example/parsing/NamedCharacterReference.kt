package org.example.parsing

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

@kotlinx.serialization.Serializable
data class NamedChararcterReference(val name: String, val codepoints: List<Int>, val characters: String) {
    /** Skips the first character which is always ampersand (&) when comparing */
    fun matchableName(): String {
        return name.substring(1)
    }
}

class NamedChararcterReferenceContainer {
    val namedCharacters: List<NamedChararcterReference>

    init {
        val rawJSONtable = NamedChararcterReferenceContainer::class.java.getResource("/entities.json").readText()
        namedCharacters = Json.decodeFromString(rawJSONtable)
    }
}
