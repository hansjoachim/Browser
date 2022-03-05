package org.example.parsing

import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.text.Charsets.UTF_8

internal class Tokenizer(document: String) {

    companion object {
        internal const val AMPERSAND = '&'
        internal const val LESS_THAN_SIGN = '<'
        internal const val NULL_CHARACTER = Char.MIN_VALUE
        internal const val EXCLAMATION_MARK = '!'
        internal const val SOLIDUS = '/'
        internal const val GREATER_THAN_SIGN = '>'
        internal const val HYPHEN_MINUS = '-'
        internal const val EQUALS_SIGN = '='
        internal const val QUOTATION_MARK = '"'
        internal const val APOSTROPHE = '\''

        internal const val NBSP_CODE = 0x000A0
        internal const val NBSP = NBSP_CODE.toChar()
    }

    //TODO: stringstream instead of bytes to skip conversion back and forth?
    private val inputStream: InputStream = ByteArrayInputStream(document.toByteArray(UTF_8))
    private var currentToken: Token? = null
    private var emittedToken: Token? = null

    private val parseErrors: MutableList<ParseError> = mutableListOf()

    private var state = TokenizationState.DataState

    private var returnState: TokenizationState? = null

    private var temporaryBuffer = ""

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

                    if (consumedCharacter.matches(AMPERSAND)) {
                        returnState = TokenizationState.DataState
                        switchTo(TokenizationState.CharacterReferenceState)
                    } else if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        switchTo(TokenizationState.TagOpenState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        emitAsACharacterToken(consumedCharacter)
                    } else if (consumedCharacter.isEndOfFile()) {
                        emitEndOfFileToken()
                    } else {
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.RCDATAState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(AMPERSAND)) {
                        returnState = TokenizationState.RCDATAState
                        switchTo(TokenizationState.CharacterReferenceState)
                    } else if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        switchTo(TokenizationState.RCDATALessThanSignState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        //FIXME: all cases above are handled, move on further down from here
                        //TODO: represent replacement character as a const  emitAsACharacterToken(U+FFFD REPLACEMENT CHARACTER )
                        unhandledCase(TokenizationState.RCDATAState, consumedCharacter)
                    } else if (consumedCharacter.isEndOfFile()) {
                        emitEndOfFileToken()
                    } else {
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.RAWTEXTState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        switchTo(TokenizationState.RAWTEXTLessThanSignState)
                        //TODO: null character
                    } else if (consumedCharacter.isEndOfFile()) {
                        emitEndOfFileToken()
                    } else {
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.ScriptDataState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        switchTo(TokenizationState.ScriptDataLessThanSignState)
                        //TODO: or null
                    } else if (consumedCharacter.isEndOfFile()) {
                        emitEndOfFileToken()
                    } else {
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.PLAINTEXTState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.PLAINTEXTState, consumedCharacter)
                }
                TokenizationState.TagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(EXCLAMATION_MARK)) {
                        switchTo(TokenizationState.MarkupDeclarationOpenState)
                    } else if (consumedCharacter.matches(SOLIDUS)) {
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
                        currentToken = EndTagToken()
                        reconsumeIn(TokenizationState.TagNameState)
                    } else {
                        unhandledCase(TokenizationState.EndTagOpenState, consumedCharacter)
                    }
                }
                TokenizationState.TagNameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                    } else if (consumedCharacter.isUpperCase()) {
                        (currentToken as TagToken).tagName += consumedCharacter.toLowerCase()
                    } else {
                        (currentToken as TagToken).tagName += consumedCharacter.character
                    }
                }
                TokenizationState.RCDATALessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()
                    if (consumedCharacter.matches(SOLIDUS)) {
                        //TODO set the temporary buffer to the empty string
                        switchTo(TokenizationState.RCDATAEndTagOpenState)
                    } else {
                        emitACharacterToken(LESS_THAN_SIGN)
                        reconsumeIn(TokenizationState.RCDATAState)
                    }
                }
                TokenizationState.RCDATAEndTagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()
                    if (consumedCharacter.isAlpha()) {
                        currentToken = EndTagToken()
                        reconsumeIn(TokenizationState.RCDATAEndTagNameState)
                    } else {
                        //TODO Can I even emit more than one thing with the current setup?
                        emitACharacterToken(LESS_THAN_SIGN)
                        emitACharacterToken(SOLIDUS)
                        reconsumeIn(TokenizationState.RCDATAState)
                    }
                }
                TokenizationState.RCDATAEndTagNameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        //TODO: If the current end tag token is an appropriate end tag token,
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        //TODO: If the current end tag token is an appropriate end tag token,
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        //TODO: If the current end tag token is an appropriate end tag token,
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                        //FIXME: uppercase/lowercase
                    } else if (consumedCharacter.isAlpha()) {
                        (currentToken as TagToken).tagName += consumedCharacter.character
                        //TODO also append to the temporary buffer
                    } else {

                        //uses the temporary buffer
                        unhandledCase(TokenizationState.RCDATAEndTagNameState, consumedCharacter)
                    }
                }
                TokenizationState.RAWTEXTLessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(SOLIDUS)) {
                        //TODO Set the temporary buffer to the empty string.
                        switchTo(TokenizationState.RAWTEXTEndTagOpenState)
                    } else {
                        emitACharacterToken(LESS_THAN_SIGN)

                        reconsumeIn(TokenizationState.RAWTEXTState)
                    }
                }
                TokenizationState.RAWTEXTEndTagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAlpha()) {
                        currentToken = EndTagToken()
                        reconsumeIn(TokenizationState.RAWTEXTEndTagNameState)
                    } else {
                        //FIXME: can I even emit more than one thing with the current setup?
                        emitACharacterToken(LESS_THAN_SIGN)
                        emitACharacterToken(SOLIDUS)

                        reconsumeIn(TokenizationState.RAWTEXTState)
                    }
                }
                TokenizationState.RAWTEXTEndTagNameState -> {
                    val consumedCharacter = consumeCharacter()
                    if (consumedCharacter.isWhitespace()) {
                        //TODO If the current end tag token is an appropriate end tag token,
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        //TODO If the current end tag token is an appropriate end tag token,
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        //TODO If the current end tag token is an appropriate end tag token,
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                        //FIXME: uppercase/lowercase
                    } else if (consumedCharacter.isAlpha()) {
                        (currentToken as TagToken).tagName += consumedCharacter
                        //TODO also append to the temporary buffer
                    } else {
                        unhandledCase(TokenizationState.RAWTEXTEndTagNameState, consumedCharacter)
                    }
                }
                TokenizationState.ScriptDataLessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(SOLIDUS)) {
                        //FIXME: deal with temporary buffer
                        switchTo(TokenizationState.ScriptDataEndTagOpenState)
                    } else if (consumedCharacter.matches(EXCLAMATION_MARK)) {
                        switchTo(TokenizationState.ScriptDataEscapeStartState)
                        //FIXME: can I even emit more than one thing at a time with the current setup?
                        emitACharacterToken(LESS_THAN_SIGN)
                        emitACharacterToken(EXCLAMATION_MARK)
                    } else {
                        emitACharacterToken(LESS_THAN_SIGN)
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
                        emitACharacterToken(LESS_THAN_SIGN)
                        emitACharacterToken(SOLIDUS)
                        reconsumeIn(TokenizationState.ScriptDataState)
                    }
                }
                TokenizationState.ScriptDataEndTagNameState -> {
                    val consumedCharacter = consumeCharacter()

                    //TODO: multiple other cases
                    if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        //FIXME: If the current end tag token is an appropriate end tag token,
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isAlpha()) {
                        (currentToken as EndTagToken).tagName += consumedCharacter.character
                        //FIXME: Also append to the temporary buffer
                    } else {
                        unhandledCase(TokenizationState.ScriptDataEndTagNameState, consumedCharacter)
                    }
                }
                TokenizationState.ScriptDataEscapeStartState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.ScriptDataEscapeStartDashState)
                        emitACharacterToken(HYPHEN_MINUS)
                    } else {
                        reconsumeIn(TokenizationState.ScriptDataState)
                    }
                }
                TokenizationState.ScriptDataEscapeStartDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.ScriptDataEscapedDashDashState)
                        emitACharacterToken(HYPHEN_MINUS)
                    } else {
                        reconsumeIn(TokenizationState.ScriptDataState)
                    }
                }
                TokenizationState.ScriptDataEscapedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.ScriptDataEscapedDashState)
                        emitACharacterToken(HYPHEN_MINUS)
                    } else if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        switchTo(TokenizationState.ScriptDataEscapedLessThanSignState)
                        //TODO: if null
                    } else if (consumedCharacter.isEndOfFile()) {
                        // This is an eof-in-script-html-comment-like-text parse error.
                        emitEndOfFileToken()
                    } else {
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.ScriptDataEscapedDashState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.ScriptDataEscapedDashDashState)
                        emitACharacterToken(HYPHEN_MINUS)
                    } else if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        switchTo(TokenizationState.ScriptDataEscapedLessThanSignState)
                        //TODO: if null
                    } else if (consumedCharacter.isEndOfFile()) {
                        // This is an eof-in-script-html-comment-like-text parse error.
                        emitEndOfFileToken()
                    } else {
                        switchTo(TokenizationState.ScriptDataEscapedState)
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.ScriptDataEscapedDashDashState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        emitACharacterToken(HYPHEN_MINUS)
                    } else if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        switchTo(TokenizationState.ScriptDataEscapedLessThanSignState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.ScriptDataState)
                        emitACharacterToken(GREATER_THAN_SIGN)
                        //TODO: if null
                    } else if (consumedCharacter.isEndOfFile()) {
                        // This is an eof-in-script-html-comment-like-text parse error.
                        emitEndOfFileToken()
                    } else {
                        switchTo(TokenizationState.ScriptDataEscapedState)
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.ScriptDataEscapedLessThanSignState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.ScriptDataEscapedLessThanSignState, consumedCharacter)
                }
                TokenizationState.ScriptDataEscapedEndTagOpenState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.ScriptDataEscapedEndTagOpenState, consumedCharacter)
                }
                TokenizationState.ScriptDataEscapedEndTagNameState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.ScriptDataEscapedEndTagNameState, consumedCharacter)
                }
                TokenizationState.ScriptDataDoubleEscapeStartState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapeStartState, consumedCharacter)
                }
                TokenizationState.ScriptDataDoubleEscapedState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapedState, consumedCharacter)
                }
                TokenizationState.ScriptDataDoubleEscapedDashState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapedDashState, consumedCharacter)
                }
                TokenizationState.ScriptDataDoubleEscapedDashDashState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapedDashDashState, consumedCharacter)
                }
                TokenizationState.ScriptDataDoubleEscapedLessThanSignState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapedLessThanSignState, consumedCharacter)
                }
                TokenizationState.ScriptDataDoubleEscapeEndState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.ScriptDataDoubleEscapeEndState, consumedCharacter)
                }
                TokenizationState.BeforeAttributeNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        //ignore
                    } else if (consumedCharacter.matches(SOLIDUS) || consumedCharacter.matches(GREATER_THAN_SIGN)) {
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
                        || consumedCharacter.matches(SOLIDUS)
                        || consumedCharacter.matches(GREATER_THAN_SIGN)
                    ) {
                        reconsumeIn(TokenizationState.AfterAttributeNameState)
                    } else if (consumedCharacter.matches(EQUALS_SIGN)) {
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
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter.matches(EQUALS_SIGN)) {
                        switchTo(TokenizationState.BeforeAttributeValueState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
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
                    } else if (consumedCharacter.matches(QUOTATION_MARK)) {
                        switchTo(TokenizationState.AttributeValueDoubleQuotedState)
                    } else if (consumedCharacter.matches(APOSTROPHE)) {
                        switchTo(TokenizationState.AttributeValueSingleQuotedState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        //This is a missing-attribute-value parse error.
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        reconsumeIn(TokenizationState.AttributeValueUnquotedState)
                    }
                }
                TokenizationState.AttributeValueDoubleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(QUOTATION_MARK)) {
                        switchTo(TokenizationState.AfterAttributeValueQuotedState)
                        //TODO: more cases
                    } else {
                        val currentAttribute = (currentToken as TagToken).attributes.last()
                        currentAttribute.value += consumedCharacter.character
                    }
                }
                TokenizationState.AttributeValueSingleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(APOSTROPHE)) {
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
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
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
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-tag")
                        emitEndOfFileToken()
                    } else {
                        //This is a missing-whitespace-between-attributes parse error.
                        reconsumeIn(TokenizationState.BeforeAttributeNameState)
                    }
                }
                TokenizationState.SelfClosingStartTagState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        (currentToken as StartTagToken).selfClosing = true
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        unhandledCase(TokenizationState.SelfClosingStartTagState, consumedCharacter)
                    }
                }
                TokenizationState.BogusCommentState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.BogusCommentState, consumedCharacter)
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

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.CommentStartDashState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        //This is an abrupt-closing-of-empty-comment parse error.
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentStartDashState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.CommentStartDashState, consumedCharacter)
                }
                TokenizationState.CommentState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        (currentToken as CommentToken).data += consumedCharacter.character
                        switchTo(TokenizationState.CommentLessThanSignState)
                    } else if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.CommentEndDashState)
                    } else {
                        (currentToken as CommentToken).data += consumedCharacter.character
                    }
                }
                TokenizationState.CommentLessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()
                    if (consumedCharacter.matches(EXCLAMATION_MARK)) {
                        (currentToken as CommentToken).data += consumedCharacter.character
                        switchTo(TokenizationState.CommentLessThanSignBangState)
                    } else if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        (currentToken as CommentToken).data += consumedCharacter.character
                    } else {
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentLessThanSignBangState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.CommentLessThanSignBangDashState)
                    } else {
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentLessThanSignBangDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.CommentLessThanSignBangDashDashState)
                    } else {
                        reconsumeIn(TokenizationState.CommentEndDashState)
                    }
                }
                TokenizationState.CommentLessThanSignBangDashDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        //TODO: or EOF
                        reconsumeIn(TokenizationState.CommentEndState)
                    } else {
                        parseError("nested-comment")
                        reconsumeIn(TokenizationState.CommentEndState)
                    }
                }
                TokenizationState.CommentEndDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.CommentEndState)
                        //TODO: deal with null character
                    } else {
                        //Explicity append, since the - has already been consumed
                        (currentToken as CommentToken).data += HYPHEN_MINUS
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentEndState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                    } else {
                        unhandledCase(TokenizationState.CommentEndState, consumedCharacter)
                    }
                }
                TokenizationState.CommentEndBangState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.CommentEndBangState, consumedCharacter)
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
                    } else {
                        unhandledCase(TokenizationState.BeforeDOCTYPENameState, consumedCharacter)
                    }
                }
                TokenizationState.DOCTYPENameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                    } else {
                        (currentToken as DOCTYPEToken).name += consumedCharacter.character
                    }
                }
                TokenizationState.AfterDOCTYPENameState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.AfterDOCTYPENameState, consumedCharacter)
                }
                TokenizationState.AfterDOCTYPEPublicKeywordState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.AfterDOCTYPEPublicKeywordState, consumedCharacter)
                }
                TokenizationState.BeforeDOCTYPEPublicIdentifierState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.BeforeDOCTYPEPublicIdentifierState, consumedCharacter)
                }
                TokenizationState.DOCTYPEPublicIdentifierDoubleQuotedState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.DOCTYPEPublicIdentifierDoubleQuotedState, consumedCharacter)
                }
                TokenizationState.DOCTYPEPublicIdentifierSingleQuotedState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.DOCTYPEPublicIdentifierSingleQuotedState, consumedCharacter)
                }
                TokenizationState.AfterDOCTYPEPublicIdentifierState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.AfterDOCTYPEPublicIdentifierState, consumedCharacter)
                }
                TokenizationState.BetweenDOCTYPEPublicAndSystemIdentifiersState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.BetweenDOCTYPEPublicAndSystemIdentifiersState, consumedCharacter)
                }
                TokenizationState.AfterDOCTYPESystemKeywordState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.AfterDOCTYPESystemKeywordState, consumedCharacter)
                }
                TokenizationState.BeforeDOCTYPESystemIdentifierState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.BeforeDOCTYPESystemIdentifierState, consumedCharacter)
                }
                TokenizationState.DOCTYPESystemIdentifierDoubleQuotedState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.DOCTYPESystemIdentifierDoubleQuotedState, consumedCharacter)
                }
                TokenizationState.DOCTYPESystemIdentifierSingleQuotedState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.DOCTYPESystemIdentifierSingleQuotedState, consumedCharacter)
                }
                TokenizationState.AfterDOCTYPESystemIdentifierState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.AfterDOCTYPESystemIdentifierState, consumedCharacter)
                }
                TokenizationState.BogusDOCTYPEState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.BogusDOCTYPEState, consumedCharacter)
                }
                TokenizationState.CDATASectionState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.CDATASectionState, consumedCharacter)
                }
                TokenizationState.CDATASectionBracketState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.CDATASectionBracketState, consumedCharacter)
                }
                TokenizationState.CDATASectionEndState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.CDATASectionEndState, consumedCharacter)
                }
                TokenizationState.CharacterReferenceState -> {
                    temporaryBuffer = ""
                    temporaryBuffer += AMPERSAND

                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAlpha()) {
                        reconsumeIn(TokenizationState.NamedCharacterReferenceState)
                    } else if (consumedCharacter.matches('#')) {
                        temporaryBuffer += consumedCharacter.character
                        switchTo(TokenizationState.NumericCharacterReferenceState)
                    } else {
                        flushCodePointsConsumedAsACharacterReference()
                        reconsumeIn(returnState as TokenizationState)
                    }
                }
                TokenizationState.NamedCharacterReferenceState -> {
                    val possibleMatch = matchInCharacterReferenceTable()
                    if (possibleMatch != null) {
                        //FIXME: deal with attributes

                        temporaryBuffer = ""
                        temporaryBuffer += Char(possibleMatch.character)
                        flushCodePointsConsumedAsACharacterReference()
                        switchTo(returnState as TokenizationState)
                    } else {
                        flushCodePointsConsumedAsACharacterReference()
                        switchTo(TokenizationState.AmbiguousAmpersandState)
                    }
                }
                TokenizationState.AmbiguousAmpersandState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.AmbiguousAmpersandState, consumedCharacter)
                }
                TokenizationState.NumericCharacterReferenceState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.NumericCharacterReferenceState, consumedCharacter)
                }
                TokenizationState.HexadecimalCharacterReferenceStartState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.HexadecimalCharacterReferenceStartState, consumedCharacter)
                }
                TokenizationState.DecimalCharacterReferenceStartState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.DecimalCharacterReferenceStartState, consumedCharacter)
                }
                TokenizationState.HexadecimalCharacterReferenceState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.HexadecimalCharacterReferenceState, consumedCharacter)
                }
                TokenizationState.DecimalCharacterReferenceState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.DecimalCharacterReferenceState, consumedCharacter)
                }
                TokenizationState.NumericCharacterReferenceEndState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.NumericCharacterReferenceEndState, consumedCharacter)
                }
            }
        }

        return emittedToken as Token
    }

    private fun matchInCharacterReferenceTable(): NamedCharacterReference? {
        var index = 0
        var filteredList = NamedCharacterReference.values().toMutableList()

        while (true) {
            val consumedCharacter = consumeCharacter()
            temporaryBuffer += consumedCharacter

            filteredList = filteredList
                .filter { it.referenceName.length > index }
                .filter { it.referenceName[index] == consumedCharacter.character } as MutableList<NamedCharacterReference>

            if (filteredList.isEmpty()) {
                return null
            } else if (filteredList.size == 1) {
                val referenceNameLength = filteredList.first().referenceName.length
                val charactersRead = index + 1
                val hasMatchedCompleteName = referenceNameLength == charactersRead
                if (hasMatchedCompleteName) {
                    return filteredList.first()
                }
            }
            index++
        }

    }

    private fun flushCodePointsConsumedAsACharacterReference() {
        //FIXME: should deal with attributes, or else
        temporaryBuffer.toCharArray().map { emitACharacterToken(it) }
    }

    data class ParseError(val errorMessage: String, val inputCharacter: InputCharacter?)

    private fun parseError(errorMessage: String, inputCharacter: InputCharacter? = null) {
        parseErrors.add(ParseError(errorMessage, inputCharacter))
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
        println("Additionally, we found the following parse errors$parseErrors")

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

    private fun emitACharacterToken(character: Char) {
        currentToken = CharacterToken(character)
        emitCurrentToken()
    }

    private fun emitAsACharacterToken(consumedCharacter: InputCharacter) {
        currentToken = CharacterToken(consumedCharacter)
        emitCurrentToken()
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