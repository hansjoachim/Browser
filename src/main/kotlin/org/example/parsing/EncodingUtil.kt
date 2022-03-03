package org.example.parsing

enum class EncodingConfidence {
    tentative, certain, irrelevant
}

enum class Encoding(val encodingName: String, val labels: List<String>) {
    UTF_8(
        "UTF-8", listOf(
            "unicode-1-1-utf-8",
            "unicode11utf8",
            "unicode20utf8",
            "utf-8",
            "utf8",
            "x-unicode20utf8"
        )
    );

    //TODO: legacy encodings missing from enum
}


fun getEncoding(label: String): Encoding? {
    val trimmedLabel = label.trim()
    return Encoding.values()
        .firstOrNull { encoding ->
            encoding.labels
                .any { it.equals(trimmedLabel, ignoreCase = true) }
        }
}

fun extractCharacterEncodingFromMetaElement(s: String): Encoding? {
    var position = 0

    position = s.indexOf("charset", startIndex = position, ignoreCase = true)
    if (position < 0) {
        return null
    }
    val equalsValue = s.substring(startIndex = position + "charset".length)
        .trimStart()

    if (equalsValue[0] != '=') {
        //TODO loop again
        return null
    }

    //Skip past equals character and ignore whitespace before the value
    val value = equalsValue.substring(1).trimStart()

    val nextCharacter = value[0]

    if (nextCharacter == '"' && value.indexOf('"', startIndex = 1) > 0) {
        val matchingQuotation = value.indexOf('"', startIndex = 1)
        return getEncoding(value.substring(1, matchingQuotation))
    } else if (nextCharacter == '\'' && value.indexOf('\'', startIndex = 1) > 0) {
        val matchingQuotation = value.indexOf('\'', startIndex = 1)
        return getEncoding(value.substring(1, matchingQuotation))
    } else if (isAnUnmatchedCharacter(value, '"')
        || isAnUnmatchedCharacter(value, '\'')
        || value.isBlank()
    ) {
        return null
    } else {
        return getEncoding(value)
    }
}

private fun isAnUnmatchedCharacter(value: String, character: Char) =
    value[1] == character && value.indexOf(character, startIndex = 1) < 0