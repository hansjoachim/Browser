package org.example.parsing

interface Token


class DOCTYPEToken(var name: String = "missing") : Token {
    var publicIdentifier: String = "missing"
    var systemIdentifier: String = "missing"
    var forceQuirks: String = "off"

    override fun toString(): String {
        return "DOCTYPEToken(name='$name')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DOCTYPEToken

        if (name != other.name) return false
        if (publicIdentifier != other.publicIdentifier) return false
        if (systemIdentifier != other.systemIdentifier) return false
        if (forceQuirks != other.forceQuirks) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + publicIdentifier.hashCode()
        result = 31 * result + systemIdentifier.hashCode()
        result = 31 * result + forceQuirks.hashCode()
        return result
    }
}

class Attribute(
    var attributeName: String = "",
    var value: String = ""
) {

    override fun toString(): String {
        return "Attribute(attributeName='$attributeName', value='$value')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Attribute

        if (attributeName != other.attributeName) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attributeName.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}

open class TagToken(
    var tagName: String,
    var attributes: MutableList<Attribute> = mutableListOf(),
    var selfClosing: Boolean = false
) : Token {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagToken

        if (tagName != other.tagName) return false
        if (selfClosing != other.selfClosing) return false
        if (attributes != other.attributes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tagName.hashCode()
        result = 31 * result + selfClosing.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }

}

class StartTagToken(
    tagName: String = "",
    attributes: MutableList<Attribute> = mutableListOf(),
    selfClosing: Boolean = false
) :
    TagToken(tagName, attributes, selfClosing) {
    override fun toString(): String {
        return "StartTagToken(tagName='$tagName', selfClosing=$selfClosing, attributes=$attributes)"
    }
}

class EndTagToken(tagName: String = "") : TagToken(tagName) {
    override fun toString(): String {
        return "EndTagToken(tagName='$tagName', selfClosing=$selfClosing, attributes=$attributes)"
    }
}

class CommentToken(var data: String = "") : Token {
    override fun toString(): String {
        return "CommentToken(data='$data')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommentToken

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }
}

class CharacterToken(val data: Char) : Token {
    constructor(inputCharacter: InputCharacter) :
            this(inputCharacter.character)

    override fun toString(): String {
        return "CharacterToken(data='$data')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharacterToken

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    fun isWhitespace(): Boolean {
        val whiteSpaceCharacters = listOf(
            Tokenizer.CHARACTER_TABULATION,
            Tokenizer.LINE_FEED,
            Tokenizer.FORM_FEED,
            Tokenizer.CARRIAGE_RETURN,
            Tokenizer.SPACE,
        )

        return whiteSpaceCharacters.contains(data)
    }

}

class EndOfFileToken : Token {
    override fun toString(): String {
        return "EndOfFileToken()"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
