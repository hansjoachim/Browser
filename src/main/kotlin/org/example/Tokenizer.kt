package org.example

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.Character.*
import kotlin.text.Charsets.UTF_8

class Tokenizer(document: String) {
    //TODO: stringstream instead of bytes to skip conversion back and forth?
    private val inputStream: InputStream = ByteArrayInputStream(document.toByteArray(UTF_8))
    private var currentToken: Token? = null
    private var tokens: MutableList<Token> = mutableListOf()

    private var state = TokenizationState.DataState

    fun tokenize(): List<Token> {
        tokenize(TokenizationState.DataState)
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
                TokenizationState.RCDATAState -> {
                    unhandledCase(TokenizationState.RCDATAState, ' ')
                }
                TokenizationState.RAWTEXTState -> {
                    unhandledCase(TokenizationState.RAWTEXTState, ' ')
                }
                TokenizationState.ScriptDataState -> {
                    unhandledCase(TokenizationState.ScriptDataState, ' ')
                }
                TokenizationState.PLAINTEXTState -> {
                    unhandledCase(TokenizationState.PLAINTEXTState, ' ')
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

                        reconsumeIn(TokenizationState.TagNameState)
                    } else {
                        unhandledCase(TokenizationState.TagOpenState, consumedCharacter)
                    }
                }
                TokenizationState.EndTagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (isAlphabetic(consumedCharacter.code)) {
                        currentToken = Token.EndTagToken("")
                        reconsumeIn(TokenizationState.TagNameState)
                    }
                }
                TokenizationState.TagNameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (isWhitespace(consumedCharacter)) {
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter == '/') {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter == '>') {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                    } else if (isUpperCase(consumedCharacter.code)) {
                        (currentToken as Token.TagToken).tagName += toLowerCase(consumedCharacter)
                    } else {
                        (currentToken as Token.TagToken).tagName += consumedCharacter
                    }
                }
                TokenizationState.RCDATALessThanSignState -> {
                    unhandledCase(TokenizationState.RCDATALessThanSignState, ' ')
                }
                TokenizationState.RCDATAEndTagOpenState -> {
                    unhandledCase(TokenizationState.RCDATAEndTagOpenState, ' ')
                }
                TokenizationState.RCDATAEndTagNameState -> {
                    unhandledCase(TokenizationState.RCDATAEndTagNameState, ' ')
                }
                TokenizationState.RAWTEXTLessThanSignState -> {
                    unhandledCase(TokenizationState.RAWTEXTLessThanSignState, ' ')
                }
                TokenizationState.RAWTEXTEndTagOpenState -> {
                    unhandledCase(TokenizationState.RAWTEXTEndTagOpenState, ' ')
                }
                TokenizationState.RAWTEXTEndTagNameState -> {
                    unhandledCase(TokenizationState.RAWTEXTEndTagNameState, ' ')
                }
                TokenizationState.ScriptDataLessThanSignState -> {
                    unhandledCase(TokenizationState.ScriptDataLessThanSignState, ' ')
                }
                TokenizationState.ScriptDataEndTagOpenState -> {
                    unhandledCase(TokenizationState.ScriptDataEndTagOpenState, ' ')
                }
                TokenizationState.ScriptDataEndTagNameState -> {
                    unhandledCase(TokenizationState.ScriptDataEndTagNameState, ' ')
                }
                TokenizationState.ScriptDataEscapeStartState -> {
                    unhandledCase(TokenizationState.ScriptDataEscapeStartState, ' ')
                }
                TokenizationState.ScriptDataEscapeStartDashState -> {
                    unhandledCase(TokenizationState.ScriptDataEscapeStartDashState, ' ')
                }
                TokenizationState.ScriptDataEscapedState -> {
                    unhandledCase(TokenizationState.ScriptDataEscapedState, ' ')
                }
                TokenizationState.ScriptDataEscapedDashState -> {
                    unhandledCase(TokenizationState.ScriptDataEscapedDashState, ' ')
                }
                TokenizationState.ScriptDataEscapedDashDashState -> {
                    unhandledCase(TokenizationState.ScriptDataEscapedDashDashState, ' ')
                }
                TokenizationState.ScriptDataEscapedLessThanSignState -> {
                    unhandledCase(TokenizationState.ScriptDataEscapedLessThanSignState, ' ')
                }
                TokenizationState.ScriptDataEscapedEndTagOpenState -> {
                    unhandledCase(TokenizationState.ScriptDataEscapedEndTagOpenState, ' ')
                }
                TokenizationState.ScriptDataEscapedEndTagNameState -> {
                    unhandledCase(TokenizationState.ScriptDataEscapedEndTagNameState, ' ')
                }
                TokenizationState.ScriptDataDoubleEscapeStartState -> {
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapeStartState, ' ')
                }
                TokenizationState.ScriptDataDoubleEscapedState -> {
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapedState, ' ')
                }
                TokenizationState.ScriptDataDoubleEscapedDashState -> {
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapedDashState, ' ')
                }
                TokenizationState.ScriptDataDoubleEscapedDashDashState -> {
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapedDashDashState, ' ')
                }
                TokenizationState.ScriptDataDoubleEscapedLessThanSignState -> {
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapedLessThanSignState, ' ')
                }
                TokenizationState.ScriptDataDoubleEscapeEndState -> {
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapeEndState, ' ')
                }
                TokenizationState.BeforeAttributeNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (isWhitespace(consumedCharacter)) {
                        //ignore
                    } else if (consumedCharacter == '/' || consumedCharacter == '>') {
                        val newState = TokenizationState.AfterAttributeNameState
                        reconsumeIn(newState)
                    } else {
                        (currentToken as Token.TagToken).attributes.add(Token.Attribute())
                        reconsumeIn(TokenizationState.AttributeNameState)
                    }
                }
                TokenizationState.AttributeNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (isWhitespace(consumedCharacter) || consumedCharacter == '/' || consumedCharacter == '>') {
                        reconsumeIn(TokenizationState.AfterAttributeNameState)
                    } else if (consumedCharacter == '=') {
                        switchTo(TokenizationState.BeforeAttributeValueState)
                    } else {
                        //FIXME: horrible hack until I can point to current attribute
                        //Let's hope no one use more that one attribute per tag ;)
                        (currentToken as Token.TagToken).attributes[0].attributeName += consumedCharacter
                    }
                }
                TokenizationState.AfterAttributeNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (isWhitespace((consumedCharacter))) {
                        //ignore
                    } else if (consumedCharacter == '/') {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter == '=') {
                        switchTo(TokenizationState.BeforeAttributeValueState)
                    } else if (consumedCharacter == '>') {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                        //TODO: EOF case
                    } else {
                        //FIXME: Start a new attribute in the current tag token. Set that attribute name and value to the empty string.
                        reconsumeIn(TokenizationState.AttributeNameState)
                    }
                }
                TokenizationState.BeforeAttributeValueState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (isWhitespace(consumedCharacter)) {
                        //Ignore
                    } else if (consumedCharacter == '"') {
                        switchTo(TokenizationState.AttributeValueDoubleQuotedState)
                    } else if (consumedCharacter == '\'') {
                        switchTo(TokenizationState.AttributeValueSingleQuotedState)
                    } else if (consumedCharacter == '>') {
                        //This is a missing-attribute-value parse error.
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        reconsumeIn(TokenizationState.AttributeValueUnquotedState)
                    }
                }
                TokenizationState.AttributeValueDoubleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '"') {
                        switchTo(TokenizationState.AfterAttributeValueQuotedState)
                        //TODO: more cases
                    } else {
                        //FIXME: horrible hack until I can point to current attribute
                        //Let's hope no one use more that one attribute per tag ;)
                        (currentToken as Token.TagToken).attributes[0].value += consumedCharacter
                    }
                }
                TokenizationState.AttributeValueSingleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '\'') {
                        switchTo(TokenizationState.AfterAttributeValueQuotedState)
                        //TODO: more cases
                    } else {
                        //FIXME: horrible hack until I can point to current attribute
                        //Let's hope no one use more that one attribute per tag ;)
                        (currentToken as Token.TagToken).attributes[0].value += consumedCharacter
                    }
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
                TokenizationState.AfterAttributeValueQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (isWhitespace(consumedCharacter)) {
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter == '/') {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter == '>') {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        unhandledCase(TokenizationState.AfterAttributeValueQuotedState, ' ')
                    }
                }
                TokenizationState.SelfClosingStartTagState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '>') {
                        (currentToken as Token.StartTagToken).selfClosing = true
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        unhandledCase(TokenizationState.SelfClosingStartTagState, ' ')
                    }
                }
                TokenizationState.BogusCommentState -> {
                    unhandledCase(TokenizationState.BogusCommentState, ' ')
                }
                TokenizationState.MarkupDeclarationOpenState -> {
                    if (nextCharactersAreCaseInsensitiveMatch("--", inputStream)) {
                        consumeCharacters("--")
                        currentToken = Token.CommentToken()
                        switchTo(TokenizationState.CommentStartState)
                    } else if (nextCharactersAreCaseInsensitiveMatch("DOCTYPE", inputStream)) {
                        consumeCharacters("DOCTYPE")
                        switchTo(TokenizationState.DOCTYPEState)
                    } else {
                        unhandledCase(TokenizationState.MarkupDeclarationOpenState)
                    }
                }
                TokenizationState.CommentStartState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    when (consumedCharacter) {
                        '-' -> {
                            switchTo(TokenizationState.CommentStartDashState)
                        }
                        '>' -> {
                            //This is an abrupt-closing-of-empty-comment parse error.
                            switchTo(TokenizationState.DataState)
                            emitCurrentToken()
                        }
                        else -> {
                            reconsumeIn(TokenizationState.CommentState)
                        }
                    }
                }
                TokenizationState.CommentStartDashState -> {
                    unhandledCase(TokenizationState.CommentStartDashState, ' ')
                }
                TokenizationState.CommentState -> {
                    val consumedCharacter = consumeCharacter()

                    when (consumedCharacter) {
                        '<' -> {
                            (currentToken as Token.CommentToken).data += consumedCharacter
                            switchTo(TokenizationState.CommentLessThanSignState)
                        }
                        '-' -> {
                            switchTo(TokenizationState.CommentEndDashState)
                        }
                        else -> {
                            (currentToken as Token.CommentToken).data += consumedCharacter
                        }
                    }
                }
                TokenizationState.CommentLessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()
                    if (consumedCharacter == '!') {
                        (currentToken as Token.CommentToken).data += consumedCharacter
                        switchTo(TokenizationState.CommentLessThanSignBangState)
                    } else if (consumedCharacter == '<') {
                        (currentToken as Token.CommentToken).data += consumedCharacter
                    } else {
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentLessThanSignBangState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '-') {
                        switchTo(TokenizationState.CommentLessThanSignBangDashState)
                    } else {
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentLessThanSignBangDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '-') {
                        switchTo(TokenizationState.CommentLessThanSignBangDashDashState)
                    } else {
                        reconsumeIn(TokenizationState.CommentEndDashState)
                    }
                }
                TokenizationState.CommentLessThanSignBangDashDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '>') {
                        //TODO: or EOF
                        reconsumeIn(TokenizationState.CommentEndState)
                    } else {
                        //This is a nested-comment parse error.
                        reconsumeIn(TokenizationState.CommentEndState)
                    }
                }
                TokenizationState.CommentEndDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter == '-') {
                        switchTo(TokenizationState.CommentEndState)
                        //TODO: deal with null character
                    } else {
                        //Explicity append, since the - has already been consumed
                        (currentToken as Token.CommentToken).data += '-'
                        reconsumeIn(TokenizationState.CommentState)
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
                TokenizationState.CommentEndBangState -> {
                    unhandledCase(TokenizationState.CommentEndBangState, ' ')
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
                TokenizationState.AfterDOCTYPENameState -> {
                    unhandledCase(TokenizationState.AfterDOCTYPENameState, ' ')
                }
                TokenizationState.AfterDOCTYPEPublicKeywordState -> {
                    unhandledCase(TokenizationState.AfterDOCTYPEPublicKeywordState, ' ')
                }
                TokenizationState.BeforeDOCTYPEPublicIdentifierState -> {
                    unhandledCase(TokenizationState.BeforeDOCTYPEPublicIdentifierState, ' ')
                }
                TokenizationState.DOCTYPEPublicIdentifierDoubleQuotedState -> {
                    unhandledCase(TokenizationState.DOCTYPEPublicIdentifierDoubleQuotedState, ' ')
                }
                TokenizationState.DOCTYPEPublicIdentifierSingleQuotedState -> {
                    unhandledCase(TokenizationState.DOCTYPEPublicIdentifierSingleQuotedState, ' ')
                }
                TokenizationState.AfterDOCTYPEPublicIdentifierState -> {
                    unhandledCase(TokenizationState.AfterDOCTYPEPublicIdentifierState, ' ')
                }
                TokenizationState.BetweenDOCTYPEPublicAndSystemIdentifiersState -> {
                    unhandledCase(TokenizationState.BetweenDOCTYPEPublicAndSystemIdentifiersState, ' ')
                }
                TokenizationState.AfterDOCTYPESystemKeywordState -> {
                    unhandledCase(TokenizationState.AfterDOCTYPESystemKeywordState, ' ')
                }
                TokenizationState.BeforeDOCTYPESystemIdentifierState -> {
                    unhandledCase(TokenizationState.BeforeDOCTYPESystemIdentifierState, ' ')
                }
                TokenizationState.DOCTYPESystemIdentifierDoubleQuotedState -> {
                    unhandledCase(TokenizationState.DOCTYPESystemIdentifierDoubleQuotedState, ' ')
                }
                TokenizationState.DOCTYPESystemIdentifierSingleQuotedState -> {
                    unhandledCase(TokenizationState.DOCTYPESystemIdentifierSingleQuotedState, ' ')
                }
                TokenizationState.AfterDOCTYPESystemIdentifierState -> {
                    unhandledCase(TokenizationState.AfterDOCTYPESystemIdentifierState, ' ')
                }
                TokenizationState.BogusDOCTYPEState -> {
                    unhandledCase(TokenizationState.BogusDOCTYPEState, ' ')
                }
                TokenizationState.CDATASectionState -> {
                    unhandledCase(TokenizationState.CDATASectionState, ' ')
                }
                TokenizationState.CDATASectionBracketState -> {
                    unhandledCase(TokenizationState.CDATASectionBracketState, ' ')
                }
                TokenizationState.CDATASectionEndState -> {
                    unhandledCase(TokenizationState.CDATASectionEndState, ' ')
                }
                TokenizationState.CharacterReferenceState -> {
                    unhandledCase(TokenizationState.CharacterReferenceState, ' ')
                }
                TokenizationState.NamedCharacterReferenceState -> {
                    unhandledCase(TokenizationState.NamedCharacterReferenceState, ' ')
                }
                TokenizationState.AmbiguousAmpersandState -> {
                    unhandledCase(TokenizationState.AmbiguousAmpersandState, ' ')
                }
                TokenizationState.NumericCharacterReferenceState -> {
                    unhandledCase(TokenizationState.NumericCharacterReferenceState, ' ')
                }
                TokenizationState.HexadecimalCharacterReferenceStartState -> {
                    unhandledCase(TokenizationState.HexadecimalCharacterReferenceStartState, ' ')
                }
                TokenizationState.DecimalCharacterReferenceStartState -> {
                    unhandledCase(TokenizationState.DecimalCharacterReferenceStartState, ' ')
                }
                TokenizationState.HexadecimalCharacterReferenceState -> {
                    unhandledCase(TokenizationState.HexadecimalCharacterReferenceState, ' ')
                }
                TokenizationState.DecimalCharacterReferenceState -> {
                    unhandledCase(TokenizationState.DecimalCharacterReferenceState, ' ')
                }
                TokenizationState.NumericCharacterReferenceEndState -> {
                    unhandledCase(TokenizationState.NumericCharacterReferenceEndState, ' ')
                }
            }
        } while (Token.EndOfFileToken() !in tokens)
    }

    private fun reconsumeIn(newState: TokenizationState) {
        inputStream.reset()
        switchTo(newState)
    }

    private fun switchTo(state: TokenizationState) {
        this.state = state
    }

    private fun hasNextCharacter(): Boolean {
        return inputStream.available() > 0
    }

    private fun unhandledCase(state: TokenizationState, unhandledCharacter: Char = ' ') {
        // There's probably a more spec-compliant way to deal with unexpected cases.
        // For now though, emit whatever we were working on and insert an EndOfFile to break the endless loop and note where things went wrong
        if (currentToken != null) {
            emitCurrentToken()
        }
        tokens.add(Token.EndOfFileToken())

        println("Unhandled case in $state: $unhandledCharacter")
        println("Not tokenized: " + String(inputStream.readNBytes(100)) + "(...)")
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

    private fun nextCharactersAreCaseInsensitiveMatch(needle: String, haystack: InputStream): Boolean {
        haystack.mark(needle.length)

        for (c in needle.toCharArray()) {
            if (haystack.available() <= 0) {
                return false
            }
            val peek = Char(haystack.read())
            if (!peek.toString().equals(c.toString(), ignoreCase = true)) {
                haystack.reset()
                println("false!")
                return false
            }
        }
        haystack.reset()
        return true
    }
}