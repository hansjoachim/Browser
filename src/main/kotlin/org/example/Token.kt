package org.example

open class Token {

    enum class TokenType {
        DOCTYPE,
        StartTag,
        EndTag,
        Comment,
        Character,
        EndOfFile
    }

    class DOCTYPEToken(var name: String) : Token() {
        val publicIdentifier: String = "missing"
        val systemIdentifier: String = "missing"
        val forceQuirks: String = "off"

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

    open class TagToken(var tagName: String) : Token() {
        val selfClosing: Boolean = false
        //val attributes: List = mutableListOf()
        // list of attributes, each of which has a name and a value.

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TagToken

            if (tagName != other.tagName) return false
            if (selfClosing != other.selfClosing) return false

            return true
        }

        override fun hashCode(): Int {
            var result = tagName.hashCode()
            result = 31 * result + selfClosing.hashCode()
            return result
        }

    }

    class StartTagToken(tagName: String) : TagToken(tagName) {

        override fun toString(): String {
            return "StartTagToken(tagName='$tagName', selfClosing=$selfClosing)"
        }
    }

    class EndTagToken(tagName: String) : TagToken(tagName) {

        override fun toString(): String {
            return "EndTagToken(tagName='$tagName', selfClosing=$selfClosing)"
        }
    }

    class CommentToken(var data: String) : Token() {
        override fun toString(): String {
            return "CommentToken(data='$data')"
        }
    }

    class CharacterToken(var data: Char) : Token() {
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

    }

    class EndOfFileToken : Token() {
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

}