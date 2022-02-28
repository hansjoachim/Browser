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
    TODO()
}