package org.example

import java.io.ByteArrayInputStream
import java.io.InputStream
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

                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('<')) {
                        switchTo(TokenizationState.TagOpenState)
                    } else if (consumedCharacter.isEndOfFile()) {
                        currentToken = EndOfFileToken()
                        emitCurrentToken()
                        return
                    } else {
                        currentToken = CharacterToken(consumedCharacter)
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

                    if (consumedCharacter.matches('!')) {
                        switchTo(TokenizationState.MarkupDeclarationOpenState)
                    } else if (consumedCharacter.matches('/')) {
                        switchTo(TokenizationState.EndTagOpenState)
                    } else if (consumedCharacter.isAlpha()) {
                        currentToken = StartTagToken()

                        reconsumeIn(TokenizationState.TagNameState)
                    } else {
                        unhandledCase(TokenizationState.TagOpenState, consumedCharacter)
                    }
                }
                TokenizationState.EndTagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAlpha()) {
                        currentToken = EndTagToken("")
                        reconsumeIn(TokenizationState.TagNameState)
                    }
                }
                TokenizationState.TagNameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter.matches('/')) {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter.matches('>')) {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                    } else if (consumedCharacter.isUpperCase()) {
                        (currentToken as TagToken).tagName += consumedCharacter.toLowerCase()
                    } else {
                        (currentToken as TagToken).tagName += consumedCharacter.character
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

                    if (consumedCharacter.isWhitespace()) {
                        //ignore
                    } else if (consumedCharacter.matches('/') || consumedCharacter.matches('>')) {
                        reconsumeIn(TokenizationState.AfterAttributeNameState)
                    } else {
                        (currentToken as TagToken).attributes.add(Attribute())
                        reconsumeIn(TokenizationState.AttributeNameState)
                    }
                }
                TokenizationState.AttributeNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()
                        || consumedCharacter.matches('/')
                        || consumedCharacter.matches('>')
                    ) {
                        reconsumeIn(TokenizationState.AfterAttributeNameState)
                    } else if (consumedCharacter.matches('=')) {
                        switchTo(TokenizationState.BeforeAttributeValueState)
                    } else {
                        val currentAttribute = (currentToken as TagToken).attributes.last()
                        currentAttribute.attributeName += consumedCharacter.character
                    }
                }
                TokenizationState.AfterAttributeNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        //ignore
                    } else if (consumedCharacter.matches('/')) {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter.matches('=')) {
                        switchTo(TokenizationState.BeforeAttributeValueState)
                    } else if (consumedCharacter.matches('>')) {
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

                    if (consumedCharacter.isWhitespace()) {
                        //Ignore
                    } else if (consumedCharacter.matches('"')) {
                        switchTo(TokenizationState.AttributeValueDoubleQuotedState)
                    } else if (consumedCharacter.matches('\'')) {
                        switchTo(TokenizationState.AttributeValueSingleQuotedState)
                    } else if (consumedCharacter.matches('>')) {
                        //This is a missing-attribute-value parse error.
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        reconsumeIn(TokenizationState.AttributeValueUnquotedState)
                    }
                }
                TokenizationState.AttributeValueDoubleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('"')) {
                        switchTo(TokenizationState.AfterAttributeValueQuotedState)
                        //TODO: more cases
                    } else {
                        val currentAttribute = (currentToken as TagToken).attributes.last()
                        currentAttribute.value += consumedCharacter.character
                    }
                }
                TokenizationState.AttributeValueSingleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('\'')) {
                        switchTo(TokenizationState.AfterAttributeValueQuotedState)
                        //TODO: more cases
                    } else {
                        val currentAtribute = (currentToken as TagToken).attributes.last()
                        currentAtribute.value += consumedCharacter.character
                    }
                }
                TokenizationState.AttributeValueUnquotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter.matches('>')) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        val currentAttribute = (currentToken as TagToken).attributes.last()
                        currentAttribute.value += consumedCharacter.character
                    }
                }
                TokenizationState.AfterAttributeValueQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter.matches('/')) {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter.matches('>')) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        unhandledCase(TokenizationState.AfterAttributeValueQuotedState, ' ')
                    }
                }
                TokenizationState.SelfClosingStartTagState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('>')) {
                        (currentToken as StartTagToken).selfClosing = true
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        unhandledCase(TokenizationState.SelfClosingStartTagState, consumedCharacter)
                    }
                }
                TokenizationState.BogusCommentState -> {
                    unhandledCase(TokenizationState.BogusCommentState, ' ')
                }
                TokenizationState.MarkupDeclarationOpenState -> {
                    if (nextCharactersAreCaseInsensitiveMatch("--", inputStream)) {
                        consumeCharacters("--")
                        currentToken = CommentToken()
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

                    if (consumedCharacter.matches('-')) {
                        switchTo(TokenizationState.CommentStartDashState)
                    } else if (consumedCharacter.matches('>')) {
                        //This is an abrupt-closing-of-empty-comment parse error.
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentStartDashState -> {
                    unhandledCase(TokenizationState.CommentStartDashState, ' ')
                }
                TokenizationState.CommentState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('<')) {
                        (currentToken as CommentToken).data += consumedCharacter.character
                        switchTo(TokenizationState.CommentLessThanSignState)
                    } else if (consumedCharacter.matches('-')) {
                        switchTo(TokenizationState.CommentEndDashState)
                    } else {
                        (currentToken as CommentToken).data += consumedCharacter.character
                    }
                }
                TokenizationState.CommentLessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()
                    if (consumedCharacter.matches('!')) {
                        (currentToken as CommentToken).data += consumedCharacter.character
                        switchTo(TokenizationState.CommentLessThanSignBangState)
                    } else if (consumedCharacter.matches('<')) {
                        (currentToken as CommentToken).data += consumedCharacter.character
                    } else {
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentLessThanSignBangState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('-')) {
                        switchTo(TokenizationState.CommentLessThanSignBangDashState)
                    } else {
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentLessThanSignBangDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('-')) {
                        switchTo(TokenizationState.CommentLessThanSignBangDashDashState)
                    } else {
                        reconsumeIn(TokenizationState.CommentEndDashState)
                    }
                }
                TokenizationState.CommentLessThanSignBangDashDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('>')) {
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

                    if (consumedCharacter.matches('-')) {
                        switchTo(TokenizationState.CommentEndState)
                        //TODO: deal with null character
                    } else {
                        //Explicity append, since the - has already been consumed
                        (currentToken as CommentToken).data += '-'
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentEndState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('>')) {
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

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeDOCTYPENameState)
                    } else {
                        unhandledCase(TokenizationState.DOCTYPEState, consumedCharacter)
                    }
                }
                TokenizationState.BeforeDOCTYPENameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAlpha()) {
                        //FIXME names should be marked as missing (according to spec)

                        val initialName = consumedCharacter.character.toString()
                        currentToken = DOCTYPEToken(initialName)

                        switchTo(TokenizationState.DOCTYPENameState)
                    }
                }
                TokenizationState.DOCTYPENameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('>')) {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                    } else {
                        (currentToken as DOCTYPEToken).name += consumedCharacter.character
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
        } while (EndOfFileToken() !in tokens)
    }

    private fun reconsumeIn(newState: TokenizationState) {
        inputStream.reset()
        switchTo(newState)
    }

    private fun switchTo(state: TokenizationState) {
        this.state = state
    }

    private fun unhandledCase(state: TokenizationState, unhandledCharacter: InputCharacter = InputCharacter()) {
        unhandledCase(state, unhandledCharacter.character)
    }

    private fun unhandledCase(state: TokenizationState, unhandledCharacter: Char) {
        // There's probably a more spec-compliant way to deal with unexpected cases.
        // For now though, emit whatever we were working on and insert an EndOfFile to break the endless loop and note where things went wrong
        if (currentToken != null) {
            emitCurrentToken()
        }
        tokens.add(EndOfFileToken())

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

    private fun hasNextCharacter(): Boolean {
        return inputStream.available() > 0
    }

    private fun consumeCharacter(): InputCharacter {

        if (hasNextCharacter()) {
            val consumedCharacter = inputStream.read()
            return InputCharacter(character = Char(consumedCharacter))
        }

        return InputCharacter(type = InputCharacterType.EndOfFile)
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