package org.example.parsing

import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.text.Charsets.UTF_8

internal class Tokenizer(document: String) {
    //TODO: stringstream instead of bytes to skip conversion back and forth?
    private val inputStream: InputStream = ByteArrayInputStream(document.toByteArray(UTF_8))
    private var currentToken: Token? = null
    private var emittedToken: Token? = null

    private var state = TokenizationState.DataState

    fun nextToken(): Token {
        val emitted = tokenize()
        emittedToken = null
        return emitted
    }

    private fun tokenize(): Token {

        while (emittedToken == null) {
            when (state) {
                TokenizationState.DataState -> {

                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('<')) {
                        switchTo(TokenizationState.TagOpenState)
                    } else if (consumedCharacter.isEndOfFile()) {
                        emitEndOfFileToken()
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
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('<')) {
                        switchTo(TokenizationState.ScriptDataLessThanSignState)
                        //TODO: or null
                    } else if (consumedCharacter.isEndOfFile()) {
                        emitEndOfFileToken()
                    } else {
                        currentToken = CharacterToken(consumedCharacter)
                        emitCurrentToken()
                    }
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
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('/')) {
                        //FIXME: deal with temporary buffer
                        switchTo(TokenizationState.ScriptDataEndTagOpenState)
                    } else if (consumedCharacter.matches('!')) {
                        switchTo(TokenizationState.ScriptDataEscapeStartState)
                        //FIXME: can I even emit more than one thing at a time with the current setup?
                        currentToken = CharacterToken('<')
                        emitCurrentToken()
                        currentToken = CharacterToken('!')
                        emitCurrentToken()
                    } else {
                        currentToken = CharacterToken('<')
                        emitCurrentToken()
                        reconsumeIn(TokenizationState.ScriptDataState)
                    }
                }
                TokenizationState.ScriptDataEndTagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAlpha()) {
                        currentToken = EndTagToken()
                        reconsumeIn(TokenizationState.ScriptDataEndTagNameState)
                    } else {
                        //FIXME: can I even emit more than one thing at a time with the current setup?
                        currentToken = CharacterToken('<')
                        emitCurrentToken()
                        currentToken = CharacterToken('/')
                        emitCurrentToken()
                        reconsumeIn(TokenizationState.ScriptDataState)
                    }
                }
                TokenizationState.ScriptDataEndTagNameState -> {
                    val consumedCharacter = consumeCharacter()

                    //TODO: multiple other cases
                    if (consumedCharacter.matches('>')) {
                        //FIXME: If the current end tag token is an appropriate end tag token,
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isAlpha()) {
                        (currentToken as EndTagToken).tagName += consumedCharacter.character
                        //FIXME: Also append to the temporary buffer
                    } else {
                        unhandledCase(TokenizationState.ScriptDataEndTagNameState, ' ')
                    }
                }
                TokenizationState.ScriptDataEscapeStartState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('-')) {
                        switchTo(TokenizationState.ScriptDataEscapeStartDashState)
                        currentToken = CharacterToken('-')
                        emitCurrentToken()
                    } else {
                        reconsumeIn(TokenizationState.ScriptDataState)
                    }
                }
                TokenizationState.ScriptDataEscapeStartDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('-')) {
                        switchTo(TokenizationState.ScriptDataEscapedDashDashState)
                        currentToken = CharacterToken('-')
                        emitCurrentToken()
                    } else {
                        reconsumeIn(TokenizationState.ScriptDataState)
                    }
                }
                TokenizationState.ScriptDataEscapedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('-')) {
                        switchTo(TokenizationState.ScriptDataEscapedDashState)
                        currentToken = CharacterToken('-')
                        emitCurrentToken()
                    } else if (consumedCharacter.matches('<')) {
                        switchTo(TokenizationState.ScriptDataEscapedLessThanSignState)
                        //TODO: if null
                    } else if (consumedCharacter.isEndOfFile()) {
                        // This is an eof-in-script-html-comment-like-text parse error.
                        emitEndOfFileToken()
                    } else {
                        currentToken = CharacterToken(consumedCharacter)
                        emitCurrentToken()
                    }
                }
                TokenizationState.ScriptDataEscapedDashState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('-')) {
                        switchTo(TokenizationState.ScriptDataEscapedDashDashState)
                        currentToken = CharacterToken('-')
                        emitCurrentToken()
                    } else if (consumedCharacter.matches('<')) {
                        switchTo(TokenizationState.ScriptDataEscapedLessThanSignState)
                        //TODO: if null
                    } else if (consumedCharacter.isEndOfFile()) {
                        // This is an eof-in-script-html-comment-like-text parse error.
                        emitEndOfFileToken()
                    } else {
                        switchTo(TokenizationState.ScriptDataEscapedState)
                        currentToken = CharacterToken(consumedCharacter)
                        emitCurrentToken()
                    }
                }
                TokenizationState.ScriptDataEscapedDashDashState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches('-')) {
                        currentToken = CharacterToken('-')
                        emitCurrentToken()
                    } else if (consumedCharacter.matches('<')) {
                        switchTo(TokenizationState.ScriptDataEscapedLessThanSignState)
                    } else if (consumedCharacter.matches('>')) {
                        switchTo(TokenizationState.ScriptDataState)
                        currentToken = CharacterToken('>')
                        emitCurrentToken()
                        //TODO: if null
                    } else if (consumedCharacter.isEndOfFile()) {
                        // This is an eof-in-script-html-comment-like-text parse error.
                        emitEndOfFileToken()
                    } else {
                        switchTo(TokenizationState.ScriptDataEscapedState)
                        currentToken = CharacterToken(consumedCharacter)
                        emitCurrentToken()
                    }
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
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter.matches('/')) {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter.matches('>')) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        //This is an eof-in-tag parse error.
                        emitEndOfFileToken()
                    } else {
                        //This is a missing-whitespace-between-attributes parse error.
                        reconsumeIn(TokenizationState.BeforeAttributeNameState)
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
        }

        return emittedToken as Token
    }

    private fun reconsumeIn(newState: TokenizationState) {
        inputStream.reset()
        switchTo(newState)
    }

    internal fun switchTo(state: TokenizationState) {
        this.state = state
    }

    private fun unhandledCase(state: TokenizationState, unhandledCharacter: InputCharacter = InputCharacter()) {
        unhandledCase(state, unhandledCharacter.character)
    }

    private fun unhandledCase(state: TokenizationState, unhandledCharacter: Char) {
        // There's probably a more spec-compliant way to deal with unexpected cases.
        // For now though, dump whatever we were working on and emit an EndOfFile to break the endless loop and note where things went wrong
        println("Unhandled case in $state: $unhandledCharacter")
        println("Droppped token: $currentToken")
        println("Not tokenized: " + String(inputStream.readNBytes(100)) + "(...)")

        emitEndOfFileToken()
    }

    private fun emitCurrentToken() {
        val local = currentToken!!
        currentToken = null
        emittedToken = local
    }

    private fun emitEndOfFileToken() {
        emittedToken = EndOfFileToken()
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

    //TODO: tests
    private fun nextCharactersAreCaseInsensitiveMatch(needle: String, haystack: InputStream): Boolean {
        haystack.mark(needle.length)

        for (c in needle.toCharArray()) {
            if (haystack.available() <= 0) {
                return false
            }
            val peek = Char(haystack.read())
            if (!peek.toString().equals(c.toString(), ignoreCase = true)) {
                haystack.reset()
                return false
            }
        }
        haystack.reset()
        return true
    }

    internal fun reprocess(token: Token) {
        emittedToken = token
    }
}