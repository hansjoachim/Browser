package org.example.parsing

import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.text.Charsets.UTF_8

internal class Tokenizer(document: String) {

    companion object {
        internal const val CHARACTER_TABULATION = 0x0009.toChar()
        internal const val LINE_FEED = 0x000A.toChar()
        internal const val FORM_FEED = 0x000C.toChar()
        internal const val SPACE = 0x0020.toChar()
        internal const val AMPERSAND = '&'
        internal const val LESS_THAN_SIGN = '<'
        internal const val NULL_CHARACTER = Char.MIN_VALUE
        internal const val REPLACEMENT_CHARACTER_CODE = 0xFFFD
        internal const val REPLACEMENT_CHARACTER = REPLACEMENT_CHARACTER_CODE.toChar()
        internal const val EXCLAMATION_MARK = '!'
        internal const val QUESTION_MARK = '?'
        internal const val SOLIDUS = '/'
        internal const val GREATER_THAN_SIGN = '>'
        internal const val HYPHEN_MINUS = '-'
        internal const val EQUALS_SIGN = '='
        internal const val QUOTATION_MARK = '"'
        internal const val APOSTROPHE = '\''
        internal const val SEMICOLON = ';'
        internal const val LATIN_SMALL_LETTER_X = 'x'
        internal const val LATIN_CAPITAL_LETTER_X = 'X'

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
    private var characterReferenceCode = 0

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
                        emitACharacterToken(REPLACEMENT_CHARACTER)
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
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        emitACharacterToken(REPLACEMENT_CHARACTER)
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
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        emitACharacterToken(REPLACEMENT_CHARACTER)
                    } else if (consumedCharacter.isEndOfFile()) {
                        emitEndOfFileToken()
                    } else {
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.PLAINTEXTState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        emitACharacterToken(REPLACEMENT_CHARACTER)
                    } else if (consumedCharacter.isEndOfFile()) {
                        emitEndOfFileToken()
                    } else {
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.TagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(EXCLAMATION_MARK)) {
                        switchTo(TokenizationState.MarkupDeclarationOpenState)
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        switchTo(TokenizationState.EndTagOpenState)
                    } else if (consumedCharacter.isAsciiAlpha()) {
                        currentToken = StartTagToken()

                        reconsumeIn(TokenizationState.TagNameState)
                    } else if (consumedCharacter.matches(QUESTION_MARK)) {
                        parseError("unexpected-question-mark-instead-of-tag-name", consumedCharacter)

                        currentToken = CommentToken()
                        reconsumeIn(TokenizationState.BogusCommentState)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-before-tag-name")
                        emitACharacterToken(LESS_THAN_SIGN)
                        emitEndOfFileToken()
                    } else {
                        parseError("invalid-first-character-of-tag-name", consumedCharacter)
                        emitACharacterToken(LESS_THAN_SIGN)
                        reconsumeIn(TokenizationState.DataState)
                    }
                }
                TokenizationState.EndTagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAsciiAlpha()) {
                        currentToken = EndTagToken()
                        reconsumeIn(TokenizationState.TagNameState)
                    } else if(consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("missing-end-tag-name", consumedCharacter)
                        switchTo(TokenizationState.DataState)
                    } else if(consumedCharacter.isEndOfFile()) {
                        parseError("eof-before-tag-name", consumedCharacter)
                        emitACharacterToken(LESS_THAN_SIGN)
                        emitACharacterToken(SOLIDUS)
                        emitEndOfFileToken()
                    } else {
                        parseError("invalid-first-character-of-tag-name", consumedCharacter)
                        currentToken = CommentToken()
                        reconsumeIn(TokenizationState.BogusCommentState)
                    }
                }
                TokenizationState.TagNameState -> {
                    //FIXME: all cases above are handled, move on further down from here
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

                    if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character parse error", consumedCharacter)
                        (currentToken as CommentToken).data += REPLACEMENT_CHARACTER
                    } else {
                        (currentToken as CommentToken).data += consumedCharacter.character
                    }
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
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeDOCTYPENameState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        reconsumeIn(TokenizationState.BeforeDOCTYPENameState)
                        //FIXME: eof cases
                    } else {
                        unhandledCase(TokenizationState.DOCTYPEState, consumedCharacter)
                    }
                }
                TokenizationState.BeforeDOCTYPENameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        //Ignore
                    } else if (consumedCharacter.isAsciiUpperAlpha()) {
                        currentToken = DOCTYPEToken()
                        val lowerCaseVersionOfTheCurrentInputCharacter = consumedCharacter.character + 0x0020
                        (currentToken as DOCTYPEToken).name = lowerCaseVersionOfTheCurrentInputCharacter.toString()

                        switchTo(TokenizationState.DOCTYPENameState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character parse error", consumedCharacter)
                        currentToken = DOCTYPEToken()
                        (currentToken as DOCTYPEToken).name = REPLACEMENT_CHARACTER.toString()

                        switchTo(TokenizationState.DOCTYPENameState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("missing-doctype-name", consumedCharacter)
                        currentToken = DOCTYPEToken()
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype", consumedCharacter)
                        currentToken = DOCTYPEToken()
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        currentToken = DOCTYPEToken()
                        (currentToken as DOCTYPEToken).name = consumedCharacter.character.toString()
                        switchTo(TokenizationState.DOCTYPENameState)
                    }
                }
                TokenizationState.DOCTYPENameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.AfterDOCTYPENameState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        emitCurrentToken()
                        switchTo(TokenizationState.DataState)
                        //FIXME: other cases
                    } else {
                        (currentToken as DOCTYPEToken).name += consumedCharacter.character
                    }
                }
                TokenizationState.AfterDOCTYPENameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        //Ignore
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        //Look for a match including the current charact so need to reset it so it can be parsed as the first match
                        //Might be a bit hacky :/
                        inputStream.reset()

                        if (nextCharactersAreCaseInsensitiveMatch("PUBLIC", inputStream)) {
                            consumeCharacters("PUBLIC")
                            switchTo(TokenizationState.AfterDOCTYPEPublicKeywordState)
                        } else if (nextCharactersAreCaseInsensitiveMatch("SYSTEM", inputStream)) {
                            consumeCharacters("SYSTEM")
                            switchTo(TokenizationState.AfterDOCTYPESystemKeywordState)
                        } else {
                            parseError("invalid-character-sequence-after-doctype-name", consumedCharacter)
                            (currentToken as DOCTYPEToken).forceQuirks = "on"
                            reconsumeIn(TokenizationState.BogusDOCTYPEState)
                        }
                    }
                }
                TokenizationState.AfterDOCTYPEPublicKeywordState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeDOCTYPEPublicIdentifierState)
                        //FIXME: other cases
                    } else {
                        unhandledCase(TokenizationState.AfterDOCTYPEPublicKeywordState, consumedCharacter)
                    }
                }
                TokenizationState.BeforeDOCTYPEPublicIdentifierState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        // Ignore
                    } else if (consumedCharacter.matches(QUOTATION_MARK)) {
                        (currentToken as DOCTYPEToken).publicIdentifier = ""
                        switchTo(TokenizationState.DOCTYPEPublicIdentifierDoubleQuotedState)
                    } else if (consumedCharacter.matches(APOSTROPHE)) {
                        (currentToken as DOCTYPEToken).publicIdentifier = ""
                        switchTo(TokenizationState.DOCTYPEPublicIdentifierSingleQuotedState)
                        //FIXME: more cases
                    } else {
                        unhandledCase(TokenizationState.BeforeDOCTYPEPublicIdentifierState, consumedCharacter)
                    }
                }
                TokenizationState.DOCTYPEPublicIdentifierDoubleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(QUOTATION_MARK)) {
                        switchTo(TokenizationState.AfterDOCTYPEPublicIdentifierState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character parse error", consumedCharacter)
                        (currentToken as DOCTYPEToken).publicIdentifier += REPLACEMENT_CHARACTER
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("abrupt-doctype-public-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        (currentToken as DOCTYPEToken).publicIdentifier += consumedCharacter.character
                    }
                }
                TokenizationState.DOCTYPEPublicIdentifierSingleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    unhandledCase(TokenizationState.DOCTYPEPublicIdentifierSingleQuotedState, consumedCharacter)
                }
                TokenizationState.AfterDOCTYPEPublicIdentifierState -> {
                    val consumedCharacter = consumeCharacter()
                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BetweenDOCTYPEPublicAndSystemIdentifiersState)
                        //FIXME: more cases
                    } else {
                        unhandledCase(TokenizationState.AfterDOCTYPEPublicIdentifierState, consumedCharacter)
                    }
                }
                TokenizationState.BetweenDOCTYPEPublicAndSystemIdentifiersState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        //Ignore
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.matches(QUOTATION_MARK)) {
                        (currentToken as DOCTYPEToken).systemIdentifier = ""
                        switchTo(TokenizationState.DOCTYPESystemIdentifierDoubleQuotedState)
                    } else if (consumedCharacter.matches(APOSTROPHE)) {
                        (currentToken as DOCTYPEToken).systemIdentifier = ""
                        switchTo(TokenizationState.DOCTYPESystemIdentifierSingleQuotedState)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        parseError("missing-quote-before-doctype-system-identifier", consumedCharacter)
                        reconsumeIn(TokenizationState.BogusDOCTYPEState)
                    }
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

                    if (consumedCharacter.matches(QUOTATION_MARK)) {
                        switchTo(TokenizationState.AfterDOCTYPESystemIdentifierState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        (currentToken as DOCTYPEToken).systemIdentifier += REPLACEMENT_CHARACTER
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("abrupt-doctype-system-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        (currentToken as DOCTYPEToken).systemIdentifier += consumedCharacter.character
                    }
                }
                TokenizationState.DOCTYPESystemIdentifierSingleQuotedState -> {
                    val consumedCharacter = consumeCharacter()
                    unhandledCase(TokenizationState.DOCTYPESystemIdentifierSingleQuotedState, consumedCharacter)
                }
                TokenizationState.AfterDOCTYPESystemIdentifierState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        //Ignore
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        parseError("unexpected-character-after-doctype-system-identifier", consumedCharacter)
                        reconsumeIn(TokenizationState.BogusDOCTYPEState)
                    }
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
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAlpha()) {
                        //FIXME: as part of attribute
                        emitAsACharacterToken(consumedCharacter)
                    } else if (consumedCharacter.matches(SEMICOLON)) {
                        parseError("unknown-named-character-reference", consumedCharacter)
                        reconsumeIn(returnState as TokenizationState)
                    } else {
                        reconsumeIn(returnState as TokenizationState)
                    }
                }
                TokenizationState.NumericCharacterReferenceState -> {
                    characterReferenceCode = 0

                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(LATIN_SMALL_LETTER_X)
                        || consumedCharacter.matches(LATIN_CAPITAL_LETTER_X)
                    ) {
                        temporaryBuffer += consumedCharacter.character
                        switchTo(TokenizationState.HexadecimalCharacterReferenceStartState)
                    } else {
                        reconsumeIn(TokenizationState.DecimalCharacterReferenceStartState)
                    }
                }
                TokenizationState.HexadecimalCharacterReferenceStartState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAsciiHexDigit()) {
                        reconsumeIn(TokenizationState.HexadecimalCharacterReferenceState)
                    } else {
                        parseError("absence-of-digits-in-numeric-character-reference", consumedCharacter)
                        flushCodePointsConsumedAsACharacterReference()
                        reconsumeIn(returnState as TokenizationState)
                    }
                }
                TokenizationState.DecimalCharacterReferenceStartState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAsciiDigit()) {
                        reconsumeIn(TokenizationState.DecimalCharacterReferenceState)
                    } else {
                        parseError("absence-of-digits-in-numeric-character-reference", consumedCharacter)
                        flushCodePointsConsumedAsACharacterReference()
                        reconsumeIn(returnState as TokenizationState)
                    }
                }
                TokenizationState.HexadecimalCharacterReferenceState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAsciiDigit()) {
                        characterReferenceCode *= 16

                        val numericVersionOfCurrentInputCharacter = consumedCharacter.character.code - 0x0030
                        characterReferenceCode += numericVersionOfCurrentInputCharacter
                    } else if (consumedCharacter.isAsciiUpperHexDigit()) {
                        characterReferenceCode *= 16

                        val numericVersionOfCurrentInputCharacter = consumedCharacter.character.code - 0x0037
                        characterReferenceCode += numericVersionOfCurrentInputCharacter
                    } else if (consumedCharacter.isAsciiLowerHexDigit()) {
                        characterReferenceCode *= 16

                        val numericVersionOfCurrentInputCharacter = consumedCharacter.character.code - 0x0057
                        characterReferenceCode += numericVersionOfCurrentInputCharacter
                    } else if (consumedCharacter.matches(SEMICOLON)) {
                        switchTo(TokenizationState.NumericCharacterReferenceEndState)
                    } else {
                        parseError("missing-semicolon-after-character-reference", consumedCharacter)
                        reconsumeIn(TokenizationState.NumericCharacterReferenceEndState)
                    }
                }
                TokenizationState.DecimalCharacterReferenceState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAsciiDigit()) {
                        characterReferenceCode *= 10

                        val numericVersionOfCurrentInputCharacter = consumedCharacter.character.code - 0x0030
                        characterReferenceCode += numericVersionOfCurrentInputCharacter
                    } else if (consumedCharacter.matches(SEMICOLON)) {
                        switchTo(TokenizationState.NumericCharacterReferenceEndState)
                    } else {
                        parseError("missing-semicolon-after-character-reference", consumedCharacter)

                        reconsumeIn(TokenizationState.NumericCharacterReferenceEndState)
                    }
                }
                TokenizationState.NumericCharacterReferenceEndState -> {
                    checkCharacterReferenceCode()
                    temporaryBuffer = ""
                    temporaryBuffer += Char(characterReferenceCode)
                    flushCodePointsConsumedAsACharacterReference()
                    switchTo(returnState as TokenizationState)
                }
            }
        }

        return emittedToken as Token
    }

    private fun checkCharacterReferenceCode() {
        if (characterReferenceCode == NULL_CHARACTER.code) {
            parseError(
                "null-character-reference",
                InputCharacter(type = InputCharacterType.Character, Char(characterReferenceCode))
            )
            characterReferenceCode = REPLACEMENT_CHARACTER_CODE
        } else if (characterReferenceCode >= 0x10FFFF) {
            parseError(
                "character-reference-outside-unicode-range",
                InputCharacter(type = InputCharacterType.Character, Char(characterReferenceCode))
            )
            characterReferenceCode = REPLACEMENT_CHARACTER_CODE
        } else {
            println("checked character reference code. The check is not complete, so don't know if we should have reacted to $characterReferenceCode")
        }
    }

    private fun matchInCharacterReferenceTable(): NamedCharacterReference? {
        var index = 0
        var filteredList = NamedCharacterReference.values().toMutableList()

        while (true) {
            val consumedCharacter = consumeCharacter()
            temporaryBuffer += consumedCharacter.character

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