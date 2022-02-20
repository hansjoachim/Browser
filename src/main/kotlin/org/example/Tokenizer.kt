package org.example

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.lang.Character.isAlphabetic
import java.lang.Character.isWhitespace
import kotlin.text.Charsets.UTF_8

class Tokenizer(page: String) {
    //TODO: stringstream instead of bytes to skip conversion back and forth?
    private val inputStream: BufferedInputStream = BufferedInputStream(ByteArrayInputStream(page.toByteArray(UTF_8)))
    private var currentToken: Token? = null
    private var tokens: MutableList<Token> = mutableListOf()

    fun tokenize(): List<Token> {
        val initialState = TokenizationState.DataState
        tokenize(initialState)

        if (hasNextCharacter()) {
            println("Unparsed: " + String(inputStream.readAllBytes()))
        }

        return tokens
    }

    private fun tokenize(state: TokenizationState) {
        when (state) {
            TokenizationState.DataState -> {
                val hasNextCharacter = hasNextCharacter()

                if (!hasNextCharacter) {
                    currentToken = Token.EndOfFileToken()
                    emitCurrentToken()
                    return
                }
                val consumedCharacter = consumeCharacter()

                if (consumedCharacter == '<') {
                    tokenize(TokenizationState.TagOpenState)
                }
            }
            TokenizationState.TagOpenState -> {
                inputStream.mark(1)
                val consumedCharacter = consumeCharacter()

                if (consumedCharacter == '!') {
                    tokenize(TokenizationState.MarkupDeclarationOpenState)
                } else if (consumedCharacter == '/') {
                    tokenize(TokenizationState.EndTagOpenState)
                } else if (isAlphabetic(consumedCharacter.code)) {
                    //FIXME: parse tags
                    currentToken = Token.StartTagToken("")

                    //Reconsume in the tag name state.
                    inputStream.reset()
                    tokenize(TokenizationState.TagNameState)

                } else {
                    unhandledCase(TokenizationState.TagOpenState, consumedCharacter)
                }
            }
            TokenizationState.MarkupDeclarationOpenState -> {
                if (nextCharactersAre("DOCTYPE", inputStream)) {
                    consumeCharacters("DOCTYPE")
                    tokenize(TokenizationState.DOCTYPEState)
                }
            }
            TokenizationState.DOCTYPEState -> {
                val consumedCharacter = consumeCharacter()

                if (isWhitespace(consumedCharacter)) {
                    tokenize(TokenizationState.BeforeDOCTYPENameState)
                } else {
                    unhandledCase(TokenizationState.DOCTYPEState, consumedCharacter)
                }
            }
            TokenizationState.BeforeDOCTYPENameState -> {
                val consumedCharacter = consumeCharacter()

                if (isAlphabetic(consumedCharacter.code)) {
                    //FIXME names should be marked as missing

                    //FIXME: lowercase name
                    val initialName = consumedCharacter.toString()
                    currentToken = Token.DOCTYPEToken(initialName) //, "missing", "missing")

                    //Hm... loop automatically?
                    tokenize(TokenizationState.DOCTYPENameState)
                }
            }
            TokenizationState.DOCTYPENameState -> {
                val consumedCharacter = consumeCharacter()

                if (consumedCharacter == '>') {
                    emitCurrentToken()
                    tokenize(TokenizationState.DataState)
                }
                if (isAlphabetic(consumedCharacter.code)) {
                    //FIXME: lowercase name
                    (currentToken as Token.DOCTYPEToken).name += consumedCharacter

                    tokenize(TokenizationState.DOCTYPENameState)
                }
            }
            TokenizationState.TagNameState -> {
                val consumedCharacter = consumeCharacter()

                if (consumedCharacter == '>') {
                    emitCurrentToken()
                    tokenize(TokenizationState.DataState)
                } else if (isAlphabetic(consumedCharacter.code)) {
                    //FIXME: lowercase name
                    (currentToken as Token.TagToken).tagName += consumedCharacter
                    //Hm... loop automatically?
                    tokenize(TokenizationState.TagNameState)
                } else {
                    unhandledCase(TokenizationState.TagNameState, consumedCharacter)
                }
            }
            TokenizationState.EndTagOpenState -> {
                inputStream.mark(1)
                val consumedCharacter = consumeCharacter()

                if (isAlphabetic(consumedCharacter.code)) {
                    currentToken = Token.EndTagToken("")

                    //Reset to reconsume
                    inputStream.reset()
                    tokenize(TokenizationState.TagNameState)
                }
            }
            else -> {
                println("Unhandled state: $state")
            }
        }
    }

    private fun hasNextCharacter(): Boolean {
        return inputStream.available() > 0
    }

    private fun unhandledCase(state: TokenizationState, unhandledCharacter: Char) {
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

    private fun nextCharactersAre(needle: String, haystack: BufferedInputStream): Boolean {
        haystack.mark(needle.length)

        for (c in needle.toCharArray()) {
            //FIXME: and exists
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