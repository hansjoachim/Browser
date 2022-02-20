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
    }

    open class TagToken(var tagName: String) : Token() {
        val selfClosing: Boolean = false
        //val attributes: List = mutableListOf()
        // list of attributes, each of which has a name and a value.
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

    class CharacterToken(var data: String) : Token() {
        override fun toString(): String {
            return "CharacterToken(data='$data')"
        }
    }

    class EndOfFileToken : Token() {
        override fun toString(): String {
            return "EndOfFileToken()"
        }
    }

}