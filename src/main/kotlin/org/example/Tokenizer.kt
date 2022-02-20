package org.example

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.Character.*
import kotlin.text.Charsets.UTF_8

class Tokenizer(page: String) {
    //TODO: stringstream instead of bytes to skip conversion back and forth?
    private val inputStream: InputStream = ByteArrayInputStream(page.toByteArray(UTF_8))
    private var currentToken: Token? = null
    private var tokens: MutableList<Token> = mutableListOf()

    private var state = TokenizationState.DataState

    fun tokenize(): List<Token> {
        tokenize(TokenizationState.DataState)

        if (hasNextCharacter()) {
            println("Not tokenized: " + String(inputStream.readAllBytes()))
        }

        return tokens
    }

    private fun tokenize(initialState: TokenizationState) {
        switchTo(initialState)

        do {
            when (state) {
                TokenizationState.DataState -> {

                    //FIXME: spec compliant order somehow
                    val hasNextCharacter = hasNextCharacter()

                    if (!hasNextCharacter) {
                        currentToken = Token.EndOfFileToken()
                        emitCurrentToken()
                        return
                    }
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '<') {
                        switchTo(TokenizationState.TagOpenState)
                    } else {
                        currentToken = Token.CharacterToken(consumedCharacter)
                        emitCurrentToken()
                    }
                }
                TokenizationState.TagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '!') {
                        switchTo(TokenizationState.MarkupDeclarationOpenState)
                    } else if (consumedCharacter == '/') {
                        switchTo(TokenizationState.EndTagOpenState)
                    } else if (isAlphabetic(consumedCharacter.code)) {
                        currentToken = Token.StartTagToken()

                        //Reconsume in the tag name state.
                        inputStream.reset()
                        switchTo(TokenizationState.TagNameState)
                    } else {
                        unhandledCase(TokenizationState.TagOpenState, consumedCharacter)
                    }
                }
                TokenizationState.MarkupDeclarationOpenState -> {
                    if (nextCharactersAre("--", inputStream)) {
                        consumeCharacters("--")
                        currentToken = Token.CommentToken()
                        switchTo(TokenizationState.CommentStartState)
                    } else if (nextCharactersAre("DOCTYPE", inputStream)) {
                        consumeCharacters("DOCTYPE")
                        switchTo(TokenizationState.DOCTYPEState)
                    } else {
                        unhandledCase(TokenizationState.MarkupDeclarationOpenState)
                    }
                }
                TokenizationState.DOCTYPEState -> {
                    val consumedCharacter = consumeCharacter()

                    if (isWhitespace(consumedCharacter)) {
                        switchTo(TokenizationState.BeforeDOCTYPENameState)
                    } else {
                        unhandledCase(TokenizationState.DOCTYPEState, consumedCharacter)
                    }
                }
                TokenizationState.BeforeDOCTYPENameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (isAlphabetic(consumedCharacter.code)) {
                        //FIXME names should be marked as missing (according to spec)

                        val initialName = consumedCharacter.toString()
                        currentToken = Token.DOCTYPEToken(initialName)

                        switchTo(TokenizationState.DOCTYPENameState)
                    }
                }
                TokenizationState.DOCTYPENameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '>') {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                    } else {
                        (currentToken as Token.DOCTYPEToken).name += consumedCharacter
                    }
                }
                TokenizationState.TagNameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (isWhitespace(consumedCharacter)) {
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter == '>') {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                    } else if (isUpperCase(consumedCharacter.code)) {
                        (currentToken as Token.TagToken).tagName += toLowerCase(consumedCharacter)
                    } else {
                        (currentToken as Token.TagToken).tagName += consumedCharacter
                    }
                }
                TokenizationState.BeforeAttributeNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (isWhitespace(consumedCharacter)) {
                        //do nothing
                    } else {
                        inputStream.reset()
                        //reconsume
                        (currentToken as Token.TagToken).attributes.add(Token.Attribute())
                        switchTo(TokenizationState.AttributeNameState)
                    }
                }
                TokenizationState.AttributeNameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '=') {
                        switchTo(TokenizationState.BeforeAttributeValueState)
                    } else {
                        //FIXME: horrible hack until I can point to current attribute
                        //Let's hope no one use more that one attribute per tag ;)
                        (currentToken as Token.TagToken).attributes[0].attributeName += consumedCharacter
                    }
                }
                TokenizationState.BeforeAttributeValueState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    //FIXME: lets hope no one use quotes :|

                    inputStream.reset()
                    //reconsume
                    switchTo(TokenizationState.AttributeValueUnquotedState)
                }
                TokenizationState.AttributeValueUnquotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '>') {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                    } else {
                        //FIXME: horrible hack until I can point to current attribute
                        //Let's hope no one use more that one attribute per tag ;)
                        (currentToken as Token.TagToken).attributes[0].value += consumedCharacter
                    }
                }
                TokenizationState.EndTagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (isAlphabetic(consumedCharacter.code)) {
                        currentToken = Token.EndTagToken("")

                        //Reset to reconsume
                        inputStream.reset()
                        switchTo(TokenizationState.TagNameState)
                    }
                }
                TokenizationState.CommentStartState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '-') {
                        switchTo(TokenizationState.CommentStartDashState)
                    } else {
                        inputStream.reset()
                        //reconsume
                        switchTo(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '-') {
                        switchTo(TokenizationState.CommentEndDashState)
                    } else {
                        (currentToken as Token.CommentToken).data += consumedCharacter
                    }
                }
                TokenizationState.CommentEndDashState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '-') {
                        switchTo(TokenizationState.CommentEndState)
                    } else {
                        unhandledCase(TokenizationState.CommentEndDashState, consumedCharacter)
                    }
                }
                TokenizationState.CommentEndState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '>') {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                    } else {
                        unhandledCase(TokenizationState.CommentEndState, consumedCharacter)
                    }
                }
                else -> {
                    println("Unhandled state: $initialState")
                }
            }
        } while (Token.EndOfFileToken() !in tokens)
    }

    private fun switchTo(state: TokenizationState) {
        this.state = state
    }

    private fun hasNextCharacter(): Boolean {
        return inputStream.available() > 0
    }

    private fun unhandledCase(state: TokenizationState, unhandledCharacter: Char = ' ') {
        println("Unhandled case in $state: $unhandledCharacter")
    }

    private fun emitCurrentToken() {
        tokens.add(currentToken!!)
        currentToken = null
    }

    private fun consumeCharacters(string: String) {
        for (c in string.toCharArray()) {
            consumeCharacter()
        }
    }

    private fun consumeCharacter(): Char {
        val consumeCharacter = inputStream.read()
        return Char(consumeCharacter)
    }

    private fun nextCharactersAre(needle: String, haystack: InputStream): Boolean {
        haystack.mark(needle.length)

        for (c in needle.toCharArray()) {
            if (haystack.available() <= 0) {
                return false
            }
            val peek = Char(haystack.read())
            if (peek != c) {
                haystack.reset()
                println("false!")
                return false
            }
        }
        haystack.reset()
        return true
    }
}