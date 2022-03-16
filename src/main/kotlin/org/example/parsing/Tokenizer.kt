package org.example.parsing

import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.text.Charsets.UTF_8

internal class Tokenizer(document: String) {

    companion object {
        internal const val CHARACTER_TABULATION = 0x0009.toChar()
        internal const val LINE_FEED = 0x000A.toChar()
        internal const val FORM_FEED = 0x000C.toChar()
        internal const val CARRIAGE_RETURN = 0x000D.toChar()
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
        internal const val GRAVE_ACCENT = '`'
        internal const val QUOTATION_MARK = '"'
        internal const val APOSTROPHE = '\''
        internal const val SEMICOLON = ';'
        internal const val LATIN_SMALL_LETTER_X = 'x'
        internal const val LATIN_CAPITAL_LETTER_X = 'X'
        internal const val RIGHT_SQUARE_BRACKET = ']'
        internal const val NUMBER_SIGN = '#'
    }

    //TODO: stringstream instead of bytes to skip conversion back and forth?
    private val inputStream: InputStream = ByteArrayInputStream(document.toByteArray(UTF_8))
    private var currentToken: Token? = null
    private var currentAttribute: Attribute? = null
    private var emittedTokens = mutableListOf<Token>()
    private var lastEmittedStartTagToken: StartTagToken? = null

    private val parseErrors: MutableList<ParseError> = mutableListOf()

    private var state = TokenizationState.DataState

    private var returnState: TokenizationState? = null

    private var temporaryBuffer = ""
    private var characterReferenceCode = 0

    fun nextToken(): Token {

        while (emittedTokens.isEmpty()) {
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
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("missing-end-tag-name", consumedCharacter)
                        switchTo(TokenizationState.DataState)
                    } else if (consumedCharacter.isEndOfFile()) {
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
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        switchTo(TokenizationState.SelfClosingStartTagState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isAsciiUpperAlpha()) {
                        val lowercaseVersionOfTheCurrentInputToken = consumedCharacter.character + 0x0020
                        (currentToken as TagToken).tagName += lowercaseVersionOfTheCurrentInputToken
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        (currentToken as TagToken).tagName += REPLACEMENT_CHARACTER
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-tag", consumedCharacter)
                        emitEndOfFileToken()
                    } else {
                        (currentToken as TagToken).tagName += consumedCharacter.character
                    }
                }
                TokenizationState.RCDATALessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(SOLIDUS)) {
                        temporaryBuffer = ""
                        switchTo(TokenizationState.RCDATAEndTagOpenState)
                    } else {
                        emitACharacterToken(LESS_THAN_SIGN)
                        reconsumeIn(TokenizationState.RCDATAState)
                    }
                }
                TokenizationState.RCDATAEndTagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAsciiAlpha()) {
                        currentToken = EndTagToken()
                        reconsumeIn(TokenizationState.RCDATAEndTagNameState)
                    } else {
                        emitACharacterToken(LESS_THAN_SIGN)
                        emitACharacterToken(SOLIDUS)
                        reconsumeIn(TokenizationState.RCDATAState)
                    }
                }
                TokenizationState.RCDATAEndTagNameState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.BeforeAttributeNameState)
                        } else {
                            RCDATAEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.SelfClosingStartTagState)
                        } else {
                            RCDATAEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.DataState)
                            emitCurrentToken()
                        } else {
                            RCDATAEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.isAsciiUpperAlpha()) {
                        val lowercaseVersionOfTheCurrentInputCharacter = consumedCharacter.character + 0x0020
                        (currentToken as TagToken).tagName += lowercaseVersionOfTheCurrentInputCharacter

                        temporaryBuffer += consumedCharacter.character
                    } else if (consumedCharacter.isAsciiLowerAlpha()) {
                        (currentToken as TagToken).tagName += consumedCharacter.character

                        temporaryBuffer += consumedCharacter.character
                    } else {
                        RCDATAEndTagNameStateAnythingElse()
                    }
                }
                TokenizationState.RAWTEXTLessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(SOLIDUS)) {
                        temporaryBuffer = ""
                        switchTo(TokenizationState.RAWTEXTEndTagOpenState)
                    } else {
                        emitACharacterToken(LESS_THAN_SIGN)

                        reconsumeIn(TokenizationState.RAWTEXTState)
                    }
                }
                TokenizationState.RAWTEXTEndTagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAsciiAlpha()) {
                        currentToken = EndTagToken()
                        reconsumeIn(TokenizationState.RAWTEXTEndTagNameState)
                    } else {
                        emitACharacterToken(LESS_THAN_SIGN)
                        emitACharacterToken(SOLIDUS)

                        reconsumeIn(TokenizationState.RAWTEXTState)
                    }
                }
                TokenizationState.RAWTEXTEndTagNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.BeforeAttributeNameState)
                        } else {
                            RAWTEXTEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.SelfClosingStartTagState)
                        } else {
                            RAWTEXTEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.DataState)
                            emitCurrentToken()
                        } else {
                            RAWTEXTEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.isAsciiUpperAlpha()) {
                        val lowercaseVersionOfTheCurrentInputCharacter = consumedCharacter.character + 0x0020
                        (currentToken as TagToken).tagName += lowercaseVersionOfTheCurrentInputCharacter
                        temporaryBuffer += consumedCharacter.character
                    } else if (consumedCharacter.isAsciiLowerAlpha()) {
                        (currentToken as TagToken).tagName += consumedCharacter.character
                        temporaryBuffer += consumedCharacter.character
                    } else {
                        RAWTEXTEndTagNameStateAnythingElse()
                    }
                }
                TokenizationState.ScriptDataLessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(SOLIDUS)) {
                        temporaryBuffer = ""
                        switchTo(TokenizationState.ScriptDataEndTagOpenState)
                    } else if (consumedCharacter.matches(EXCLAMATION_MARK)) {
                        switchTo(TokenizationState.ScriptDataEscapeStartState)
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

                    if (consumedCharacter.isAsciiAlpha()) {
                        currentToken = EndTagToken()
                        reconsumeIn(TokenizationState.ScriptDataEndTagNameState)
                    } else {
                        emitACharacterToken(LESS_THAN_SIGN)
                        emitACharacterToken(SOLIDUS)
                        reconsumeIn(TokenizationState.ScriptDataState)
                    }
                }
                TokenizationState.ScriptDataEndTagNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.BeforeAttributeNameState)
                        } else {
                            ScriptDataEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.SelfClosingStartTagState)
                        } else {
                            ScriptDataEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.DataState)
                            emitCurrentToken()
                        } else {
                            ScriptDataEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.isAsciiUpperAlpha()) {
                        val lowercaseVersionOfTheCurrentInputCharacter = consumedCharacter.character + 0x0020
                        (currentToken as EndTagToken).tagName += lowercaseVersionOfTheCurrentInputCharacter
                        temporaryBuffer += consumedCharacter.character
                    } else if (consumedCharacter.isAsciiLowerAlpha()) {
                        (currentToken as EndTagToken).tagName += consumedCharacter.character
                        temporaryBuffer += consumedCharacter.character
                    } else {
                        ScriptDataEndTagNameStateAnythingElse()
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
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        emitACharacterToken(REPLACEMENT_CHARACTER)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-script-html-comment-like-text", consumedCharacter)
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
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        switchTo(TokenizationState.ScriptDataEscapedState)
                        emitACharacterToken(REPLACEMENT_CHARACTER)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-script-html-comment-like-text", consumedCharacter)
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
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        switchTo(TokenizationState.ScriptDataEscapedState)
                        emitACharacterToken(REPLACEMENT_CHARACTER)

                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-script-html-comment-like-text", consumedCharacter)
                        emitEndOfFileToken()
                    } else {
                        switchTo(TokenizationState.ScriptDataEscapedState)
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.ScriptDataEscapedLessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(SOLIDUS)) {
                        temporaryBuffer = ""
                        switchTo(TokenizationState.ScriptDataEscapedEndTagOpenState)
                    } else if (consumedCharacter.isAsciiAlpha()) {
                        temporaryBuffer = ""
                        emitACharacterToken(LESS_THAN_SIGN)
                        reconsumeIn(TokenizationState.ScriptDataDoubleEscapeStartState)
                    } else {
                        emitACharacterToken(LESS_THAN_SIGN)
                        reconsumeIn(TokenizationState.ScriptDataEscapedState)
                    }
                }
                TokenizationState.ScriptDataEscapedEndTagOpenState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAsciiAlpha()) {
                        currentToken = EndTagToken()
                        reconsumeIn(TokenizationState.ScriptDataEscapedEndTagNameState)
                    } else {
                        emitACharacterToken(LESS_THAN_SIGN)
                        emitACharacterToken(SOLIDUS)

                        reconsumeIn(TokenizationState.ScriptDataEscapedState)
                    }
                }
                TokenizationState.ScriptDataEscapedEndTagNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.BeforeAttributeNameState)
                        } else {
                            ScriptDataEscapedEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.matches(SOLIDUS)) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.SelfClosingStartTagState)
                        } else {
                            ScriptDataEscapedEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        if (isAnAppropriateEndTagToken(currentToken as EndTagToken)) {
                            switchTo(TokenizationState.DataState)
                            emitCurrentToken()
                        } else {
                            ScriptDataEscapedEndTagNameStateAnythingElse()
                        }
                    } else if (consumedCharacter.isAsciiUpperAlpha()) {
                        val lowercaseVersionOfTheCurrentInputCharacter = consumedCharacter.character + 0x0020
                        (currentToken as TagToken).tagName += lowercaseVersionOfTheCurrentInputCharacter

                        temporaryBuffer += consumedCharacter.character
                    } else if (consumedCharacter.isAsciiLowerAlpha()) {
                        (currentToken as TagToken).tagName += consumedCharacter.character

                        temporaryBuffer += consumedCharacter.character
                    } else {
                        ScriptDataEscapedEndTagNameStateAnythingElse()
                    }
                }
                TokenizationState.ScriptDataDoubleEscapeStartState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()
                        || consumedCharacter.matches(SOLIDUS)
                        || consumedCharacter.matches(GREATER_THAN_SIGN)
                    ) {
                        if (temporaryBuffer == "script") {
                            switchTo(TokenizationState.ScriptDataDoubleEscapedState)
                        } else {
                            switchTo(TokenizationState.ScriptDataEscapedState)
                        }
                        emitAsACharacterToken(consumedCharacter)
                    } else if (consumedCharacter.isAsciiUpperAlpha()) {
                        val lowercaseVersionOfTheCurrentInputCharacter = consumedCharacter.character + 0x0020
                        temporaryBuffer += lowercaseVersionOfTheCurrentInputCharacter
                        emitAsACharacterToken(consumedCharacter)
                    } else if (consumedCharacter.isAsciiLowerAlpha()) {
                        temporaryBuffer += consumedCharacter.character
                        emitAsACharacterToken(consumedCharacter)
                    } else {
                        reconsumeIn(TokenizationState.ScriptDataEscapedState)
                    }
                }
                TokenizationState.ScriptDataDoubleEscapedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.ScriptDataDoubleEscapedDashState)
                        emitACharacterToken(HYPHEN_MINUS)
                    } else if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        switchTo(TokenizationState.ScriptDataDoubleEscapedLessThanSignState)
                        emitACharacterToken(LESS_THAN_SIGN)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        emitACharacterToken(REPLACEMENT_CHARACTER)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-script-html-comment-like-text", consumedCharacter)
                        emitEndOfFileToken()
                    } else {
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.ScriptDataDoubleEscapedDashState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.ScriptDataDoubleEscapedDashDashState)
                        emitACharacterToken(HYPHEN_MINUS)
                    } else if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        switchTo(TokenizationState.ScriptDataDoubleEscapedLessThanSignState)
                        emitACharacterToken(LESS_THAN_SIGN)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        switchTo(TokenizationState.ScriptDataDoubleEscapedState)
                        emitACharacterToken(REPLACEMENT_CHARACTER)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-script-html-comment-like-text", consumedCharacter)
                        emitEndOfFileToken()
                    } else {
                        switchTo(TokenizationState.ScriptDataDoubleEscapedState)
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.ScriptDataDoubleEscapedDashDashState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        emitACharacterToken(HYPHEN_MINUS)
                    } else if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        switchTo(TokenizationState.ScriptDataDoubleEscapedLessThanSignState)
                        emitACharacterToken(LESS_THAN_SIGN)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.ScriptDataState)
                        emitACharacterToken(GREATER_THAN_SIGN)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        switchTo(TokenizationState.ScriptDataDoubleEscapedState)
                        emitACharacterToken(REPLACEMENT_CHARACTER)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-script-html-comment-like-text", consumedCharacter)
                        emitEndOfFileToken()
                    } else {
                        switchTo(TokenizationState.ScriptDataDoubleEscapedState)
                        emitAsACharacterToken(consumedCharacter)
                    }
                }
                TokenizationState.ScriptDataDoubleEscapedLessThanSignState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(SOLIDUS)) {
                        temporaryBuffer = ""
                        switchTo(TokenizationState.ScriptDataDoubleEscapeEndState)
                        emitACharacterToken(SOLIDUS)
                    } else {
                        reconsumeIn(TokenizationState.ScriptDataDoubleEscapedState)
                    }
                }
                TokenizationState.ScriptDataDoubleEscapeEndState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()
                        || consumedCharacter.matches(SOLIDUS)
                        || consumedCharacter.matches(GREATER_THAN_SIGN)
                    ) {
                        if (temporaryBuffer == "script") {
                            switchTo(TokenizationState.ScriptDataEscapedState)
                        } else {
                            switchTo(TokenizationState.ScriptDataDoubleEscapedState)
                        }
                        emitAsACharacterToken(consumedCharacter)
                    } else if (consumedCharacter.isAsciiUpperAlpha()) {
                        val lowercaseVersionOfTheCurrentInputCharacter = consumedCharacter.character + 0x0020
                        temporaryBuffer += lowercaseVersionOfTheCurrentInputCharacter
                        emitAsACharacterToken(consumedCharacter)
                    } else if (consumedCharacter.isAsciiLowerAlpha()) {
                        temporaryBuffer += consumedCharacter.character
                        emitAsACharacterToken(consumedCharacter)
                    } else {
                        reconsumeIn(TokenizationState.ScriptDataDoubleEscapedState)
                    }
                }
                TokenizationState.BeforeAttributeNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        //ignore
                    } else if (consumedCharacter.matches(SOLIDUS)
                        || consumedCharacter.matches(GREATER_THAN_SIGN)
                        || consumedCharacter.isEndOfFile()
                    ) {
                        reconsumeIn(TokenizationState.AfterAttributeNameState)
                    } else if (consumedCharacter.matches(EQUALS_SIGN)) {
                        parseError("unexpected-equals-sign-before-attribute-name", consumedCharacter)
                        startANewAttributeIn(
                            (currentToken as TagToken),
                            attributeName = consumedCharacter.character.toString()
                        )

                        switchTo(TokenizationState.AttributeNameState)
                    } else {
                        startANewAttributeIn((currentToken as TagToken))
                        reconsumeIn(TokenizationState.AttributeNameState)
                    }
                }
                TokenizationState.AttributeNameState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()
                        || consumedCharacter.matches(SOLIDUS)
                        || consumedCharacter.matches(GREATER_THAN_SIGN)
                        || consumedCharacter.isEndOfFile()
                    ) {
                        reconsumeIn(TokenizationState.AfterAttributeNameState)
                    } else if (consumedCharacter.matches(EQUALS_SIGN)) {
                        switchTo(TokenizationState.BeforeAttributeValueState)
                    } else if (consumedCharacter.isAsciiUpperAlpha()) {
                        val lowercaseVersionOfTheCurrentInputCharacter = consumedCharacter.character + 0x0020
                        (currentAttribute as Attribute).attributeName += lowercaseVersionOfTheCurrentInputCharacter
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        (currentAttribute as Attribute).attributeName += REPLACEMENT_CHARACTER
                    } else if (consumedCharacter.matches(QUOTATION_MARK)
                        || consumedCharacter.matches(APOSTROPHE)
                        || consumedCharacter.matches(LESS_THAN_SIGN)
                    ) {
                        parseError("unexpected-character-in-attribute-name", consumedCharacter)
                        AttributeNameStateAnythingElse(consumedCharacter)
                    } else {
                        AttributeNameStateAnythingElse(consumedCharacter)
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
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-tag", consumedCharacter)
                        emitEndOfFileToken()
                    } else {
                        startANewAttributeIn((currentToken as TagToken))
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
                        parseError("missing-attribute-value", consumedCharacter)
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
                    } else if (consumedCharacter.matches(AMPERSAND)) {
                        returnState = TokenizationState.AttributeValueDoubleQuotedState
                        switchTo(TokenizationState.CharacterReferenceState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        (currentAttribute as Attribute).value += REPLACEMENT_CHARACTER
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-tag")
                        emitEndOfFileToken()
                    } else {
                        (currentAttribute as Attribute).value += consumedCharacter.character
                    }
                }
                TokenizationState.AttributeValueSingleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(APOSTROPHE)) {
                        switchTo(TokenizationState.AfterAttributeValueQuotedState)
                    } else if (consumedCharacter.matches(AMPERSAND)) {
                        returnState = TokenizationState.AttributeValueSingleQuotedState
                        switchTo(TokenizationState.CharacterReferenceState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        (currentAttribute as Attribute).value += REPLACEMENT_CHARACTER
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-tag")
                        emitEndOfFileToken()
                    } else {
                        (currentAttribute as Attribute).value += consumedCharacter.character
                    }
                }
                TokenizationState.AttributeValueUnquotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeAttributeNameState)
                    } else if (consumedCharacter.matches(AMPERSAND)) {
                        returnState = TokenizationState.AttributeValueUnquotedState
                        switchTo(TokenizationState.CharacterReferenceState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        (currentAttribute as Attribute).value += REPLACEMENT_CHARACTER
                    } else if (
                        consumedCharacter.matches(QUOTATION_MARK)
                        || consumedCharacter.matches(APOSTROPHE)
                        || consumedCharacter.matches(LESS_THAN_SIGN)
                        || consumedCharacter.matches(EQUALS_SIGN)
                        || consumedCharacter.matches(GRAVE_ACCENT)
                    ) {
                        parseError("unexpected-character-in-unquoted-attribute-value", consumedCharacter)
                        AttributeValueUnquotedStateAnythingElse(consumedCharacter)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-tag")
                        emitEndOfFileToken()
                    } else {
                        AttributeValueUnquotedStateAnythingElse(consumedCharacter)
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
                        parseError("missing-whitespace-between-attributes", consumedCharacter)
                        reconsumeIn(TokenizationState.BeforeAttributeNameState)
                    }
                }
                TokenizationState.SelfClosingStartTagState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        (currentToken as StartTagToken).selfClosing = true
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-tag")
                        emitEndOfFileToken()
                    } else {
                        parseError("unexpected-solidus-in-tag", consumedCharacter)
                        reconsumeIn(TokenizationState.BeforeAttributeNameState)
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
                    if (nextFewCharactersMatch("--", inputStream)) {
                        consumeCharacters("--")
                        currentToken = CommentToken()
                        switchTo(TokenizationState.CommentStartState)
                    } else if (nextFewCharactersMatch("DOCTYPE", inputStream, ignoreCase = true)) {
                        consumeCharacters("DOCTYPE")
                        switchTo(TokenizationState.DOCTYPEState)
                    } else if (nextFewCharactersMatch("[CDATA[", inputStream)) {
                        consumeCharacters("[CDATA[")

                        if (isAnAdjustedCurrentNodeAndItIsNotAnElementInTheHTMLNamespace()) {
                            switchTo(TokenizationState.CDATASectionState)
                        } else {
                            parseError("cdata-in-html-content")
                            currentToken = CommentToken(data = "[CDATA[")
                            switchTo(TokenizationState.BogusCommentState)
                        }
                    } else {
                        parseError("incorrectly-opened-comment")
                        currentToken = CommentToken()
                        switchTo(TokenizationState.BogusCommentState)
                    }
                }
                TokenizationState.CommentStartState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.CommentStartDashState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("abrupt-closing-of-empty-comment", consumedCharacter)
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else {
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentStartDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.CommentEndState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("abrupt-closing-of-empty-comment", consumedCharacter)
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-comment")
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        (currentToken as CommentToken).data += HYPHEN_MINUS
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(LESS_THAN_SIGN)) {
                        (currentToken as CommentToken).data += consumedCharacter.character
                        switchTo(TokenizationState.CommentLessThanSignState)
                    } else if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.CommentEndDashState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character")
                        (currentToken as CommentToken).data += REPLACEMENT_CHARACTER
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-comment")
                        emitCurrentToken()
                        emitEndOfFileToken()
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

                    if (consumedCharacter.matches(GREATER_THAN_SIGN)
                        || consumedCharacter.isEndOfFile()
                    ) {
                        reconsumeIn(TokenizationState.CommentEndState)
                    } else {
                        parseError("nested-comment", consumedCharacter)
                        reconsumeIn(TokenizationState.CommentEndState)
                    }
                }
                TokenizationState.CommentEndDashState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        switchTo(TokenizationState.CommentEndState)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-comment")
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        //Explicity append, since the - has already been consumed
                        (currentToken as CommentToken).data += HYPHEN_MINUS
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentEndState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.matches(EXCLAMATION_MARK)) {
                        switchTo(TokenizationState.CommentEndBangState)
                    } else if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        (currentToken as CommentToken).data += HYPHEN_MINUS
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-comment")
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        //Explicity append, since the -- has already been consumed
                        (currentToken as CommentToken).data += HYPHEN_MINUS
                        (currentToken as CommentToken).data += HYPHEN_MINUS
                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.CommentEndBangState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(HYPHEN_MINUS)) {
                        (currentToken as CommentToken).data += HYPHEN_MINUS
                        (currentToken as CommentToken).data += HYPHEN_MINUS
                        (currentToken as CommentToken).data += EXCLAMATION_MARK

                        switchTo(TokenizationState.CommentEndDashState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("incorrectly-closed-comment", consumedCharacter)
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-comment")
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        (currentToken as CommentToken).data += HYPHEN_MINUS
                        (currentToken as CommentToken).data += HYPHEN_MINUS
                        (currentToken as CommentToken).data += EXCLAMATION_MARK

                        reconsumeIn(TokenizationState.CommentState)
                    }
                }
                TokenizationState.DOCTYPEState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeDOCTYPENameState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        reconsumeIn(TokenizationState.BeforeDOCTYPENameState)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype")

                        currentToken = DOCTYPEToken()
                        (currentToken as DOCTYPEToken).forceQuirks = "on"

                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        parseError("missing-whitespace-before-doctype-name", consumedCharacter)
                        reconsumeIn(TokenizationState.BeforeDOCTYPENameState)
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
                        parseError("unexpected-null-character", consumedCharacter)
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
                    } else if (consumedCharacter.isAsciiUpperAlpha()) {
                        val lowerCaseVersionOfTheCurrentInputCharacter = consumedCharacter.character + 0x0020
                        (currentToken as DOCTYPEToken).name += lowerCaseVersionOfTheCurrentInputCharacter

                        switchTo(TokenizationState.DOCTYPENameState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        (currentToken as DOCTYPEToken).name += REPLACEMENT_CHARACTER

                        switchTo(TokenizationState.DOCTYPENameState)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype")

                        (currentToken as DOCTYPEToken).forceQuirks = "on"

                        emitCurrentToken()
                        emitEndOfFileToken()
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
                        parseError("eof-in-doctype")
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        //Look for a match including the current character. Need to reset so it can be included as the first match
                        //Might be a bit hacky :/
                        inputStream.reset()

                        if (nextFewCharactersMatch("PUBLIC", inputStream, ignoreCase = true)) {
                            consumeCharacters("PUBLIC")
                            switchTo(TokenizationState.AfterDOCTYPEPublicKeywordState)
                        } else if (nextFewCharactersMatch("SYSTEM", inputStream, ignoreCase = true)) {
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
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeDOCTYPEPublicIdentifierState)
                    } else if (consumedCharacter.matches(QUOTATION_MARK)) {
                        parseError("missing-whitespace-after-doctype-public-keyword", consumedCharacter)
                        (currentToken as DOCTYPEToken).publicIdentifier = ""
                        switchTo(TokenizationState.DOCTYPEPublicIdentifierDoubleQuotedState)
                    } else if (consumedCharacter.matches(APOSTROPHE)) {
                        parseError("missing-whitespace-after-doctype-public-keyword", consumedCharacter)
                        (currentToken as DOCTYPEToken).publicIdentifier = ""
                        switchTo(TokenizationState.DOCTYPEPublicIdentifierSingleQuotedState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("missing-doctype-public-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype")
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        parseError("missing-quote-before-doctype-public-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        reconsumeIn(TokenizationState.BogusDOCTYPEState)
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
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("missing-doctype-public-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype")
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        parseError("missing-quote-before-doctype-public-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        reconsumeIn(TokenizationState.BogusDOCTYPEState)
                    }
                }
                TokenizationState.DOCTYPEPublicIdentifierDoubleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(QUOTATION_MARK)) {
                        switchTo(TokenizationState.AfterDOCTYPEPublicIdentifierState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        (currentToken as DOCTYPEToken).publicIdentifier += REPLACEMENT_CHARACTER
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("abrupt-doctype-public-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype")
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        (currentToken as DOCTYPEToken).publicIdentifier += consumedCharacter.character
                    }
                }
                TokenizationState.DOCTYPEPublicIdentifierSingleQuotedState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(APOSTROPHE)) {
                        switchTo(TokenizationState.AfterDOCTYPEPublicIdentifierState)
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character", consumedCharacter)
                        (currentToken as DOCTYPEToken).publicIdentifier += REPLACEMENT_CHARACTER
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("abrupt-doctype-public-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype")
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        (currentToken as DOCTYPEToken).publicIdentifier += consumedCharacter.character
                    }
                }
                TokenizationState.AfterDOCTYPEPublicIdentifierState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BetweenDOCTYPEPublicAndSystemIdentifiersState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.matches(QUOTATION_MARK)) {
                        parseError(
                            "missing-whitespace-between-doctype-public-and-system-identifiers",
                            consumedCharacter
                        )
                        (currentToken as DOCTYPEToken).systemIdentifier = ""
                        switchTo(TokenizationState.DOCTYPESystemIdentifierDoubleQuotedState)
                    } else if (consumedCharacter.matches(APOSTROPHE)) {
                        parseError(
                            "missing-whitespace-between-doctype-public-and-system-identifiers",
                            consumedCharacter
                        )
                        (currentToken as DOCTYPEToken).systemIdentifier = ""
                        switchTo(TokenizationState.DOCTYPESystemIdentifierSingleQuotedState)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype")
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        parseError("missing-quote-before-doctype-system-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        reconsumeIn(TokenizationState.BogusDOCTYPEState)
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
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        switchTo(TokenizationState.BeforeDOCTYPESystemIdentifierState)
                    } else if (consumedCharacter.matches(QUOTATION_MARK)) {
                        parseError(
                            "missing-whitespace-after-doctype-system-keyword parse error",
                            consumedCharacter
                        )
                        (currentToken as DOCTYPEToken).systemIdentifier = ""
                        switchTo(TokenizationState.DOCTYPESystemIdentifierDoubleQuotedState)
                    } else if (consumedCharacter.matches(APOSTROPHE)) {
                        parseError(
                            "missing-whitespace-after-doctype-system-keyword parse error",
                            consumedCharacter
                        )
                        (currentToken as DOCTYPEToken).systemIdentifier = ""
                        switchTo(TokenizationState.DOCTYPESystemIdentifierSingleQuotedState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("missing-doctype-system-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype")
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        parseError("missing-quote-before-doctype-system-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        reconsumeIn(TokenizationState.BogusDOCTYPEState)
                    }
                }
                TokenizationState.BeforeDOCTYPESystemIdentifierState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isWhitespace()) {
                        //Ignore
                    } else if (consumedCharacter.matches(QUOTATION_MARK)) {
                        (currentToken as DOCTYPEToken).systemIdentifier = ""
                        switchTo(TokenizationState.DOCTYPESystemIdentifierDoubleQuotedState)
                    } else if (consumedCharacter.matches(APOSTROPHE)) {
                        (currentToken as DOCTYPEToken).systemIdentifier = ""
                        switchTo(TokenizationState.DOCTYPESystemIdentifierSingleQuotedState)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        parseError("missing-doctype-system-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype")
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        parseError("missing-quote-before-doctype-system-identifier", consumedCharacter)
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        reconsumeIn(TokenizationState.BogusDOCTYPEState)
                    }
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

                    if (consumedCharacter.matches(APOSTROPHE)) {
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
                        parseError("eof-in-doctype")
                        (currentToken as DOCTYPEToken).forceQuirks = "on"
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        (currentToken as DOCTYPEToken).systemIdentifier += consumedCharacter.character
                    }
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

                    if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                        emitCurrentToken()
                    } else if (consumedCharacter.matches(NULL_CHARACTER)) {
                        parseError("unexpected-null-character")
                        //Ignore
                    } else if (consumedCharacter.isEndOfFile()) {
                        emitCurrentToken()
                        emitEndOfFileToken()
                    } else {
                        //Ignore
                    }
                }
                TokenizationState.CDATASectionState -> {
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(RIGHT_SQUARE_BRACKET)) {
                        switchTo(TokenizationState.CDATASectionBracketState)
                    } else if (consumedCharacter.isEndOfFile()) {
                        parseError("eof-in-doctype")
                        emitEndOfFileToken()
                    } else {
                        emitAsACharacterToken(consumedCharacter)
                        //Note: NULL characters are handled later, this will only appear in the foreign content insertion mode, the only place where CDATA appear
                    }
                }
                TokenizationState.CDATASectionBracketState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(RIGHT_SQUARE_BRACKET)) {
                        switchTo(TokenizationState.CDATASectionEndState)
                    } else {
                        emitACharacterToken(RIGHT_SQUARE_BRACKET)
                        reconsumeIn(TokenizationState.CDATASectionState)
                    }
                }
                TokenizationState.CDATASectionEndState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.matches(RIGHT_SQUARE_BRACKET)) {
                        emitACharacterToken(RIGHT_SQUARE_BRACKET)
                    } else if (consumedCharacter.matches(GREATER_THAN_SIGN)) {
                        switchTo(TokenizationState.DataState)
                    } else {
                        emitACharacterToken(RIGHT_SQUARE_BRACKET)
                        emitACharacterToken(RIGHT_SQUARE_BRACKET)
                        reconsumeIn(TokenizationState.CDATASectionState)
                    }
                }
                TokenizationState.CharacterReferenceState -> {
                    temporaryBuffer = ""
                    temporaryBuffer += AMPERSAND

                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAsciiAlphaNumeric()) {
                        reconsumeIn(TokenizationState.NamedCharacterReferenceState)
                    } else if (consumedCharacter.matches(NUMBER_SIGN)) {
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
                        val lastCharacterMatched = possibleMatch.characters.toCharArray().asList().last()

                        if (wasConsumedAsPartOfAnAttribute()
                            && lastCharacterMatched != SEMICOLON
                            && (nextInputCharacterIs(EQUALS_SIGN) || nextInputCharacterIsAnAsciiAlphanumeric())
                        ) {
                            //For historical reasons
                            flushCodePointsConsumedAsACharacterReference()
                            switchToReturnState()
                        } else {
                            if (lastCharacterMatched != SEMICOLON) {
                                parseError("missing-semicolon-after-character-reference")
                            }

                            temporaryBuffer = ""
                            possibleMatch.codepoints.forEach {
                                temporaryBuffer += Char(it)
                            }

                            flushCodePointsConsumedAsACharacterReference()
                            switchToReturnState()
                        }
                    } else {
                        flushCodePointsConsumedAsACharacterReference()
                        switchTo(TokenizationState.AmbiguousAmpersandState)
                    }
                }
                TokenizationState.AmbiguousAmpersandState -> {
                    inputStream.mark(1)
                    val consumedCharacter = consumeCharacter()

                    if (consumedCharacter.isAsciiAlphaNumeric()) {
                        if (wasConsumedAsPartOfAnAttribute()) {
                            (currentAttribute as Attribute).value += consumedCharacter.character
                        } else {
                            emitAsACharacterToken(consumedCharacter)
                        }
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
                    switchToReturnState()
                }
            }
        }

        return emittedTokens.removeFirst()
    }

    private fun wasConsumedAsPartOfAnAttribute(): Boolean {
        return listOf(
            TokenizationState.AttributeValueDoubleQuotedState,
            TokenizationState.AttributeValueSingleQuotedState,
            TokenizationState.AttributeValueUnquotedState
        ).contains(returnState)
    }

    private fun isAnAdjustedCurrentNodeAndItIsNotAnElementInTheHTMLNamespace(): Boolean {
        //FIXME: implement
        return false
    }

    private fun AttributeValueUnquotedStateAnythingElse(consumedCharacter: InputCharacter) {
        (currentAttribute as Attribute).value += consumedCharacter.character
    }

    private fun startANewAttributeIn(tagToken: TagToken, attributeName: String = "", value: String = "") {
        currentAttribute = Attribute(attributeName, value)
        tagToken.attributes.add(currentAttribute as Attribute)
    }

    private fun AttributeNameStateAnythingElse(consumedCharacter: InputCharacter) {
        (currentAttribute as Attribute).attributeName += consumedCharacter.character
    }

    private fun ScriptDataEscapedEndTagNameStateAnythingElse() {
        emitACharacterToken(LESS_THAN_SIGN)
        emitACharacterToken(SOLIDUS)
        temporaryBuffer.toCharArray().forEach { emitACharacterToken(it) }

        reconsumeIn(TokenizationState.ScriptDataEscapedState)
    }

    private fun ScriptDataEndTagNameStateAnythingElse() {
        emitACharacterToken(LESS_THAN_SIGN)
        emitACharacterToken(SOLIDUS)
        temporaryBuffer.toCharArray().forEach { emitACharacterToken(it) }

        reconsumeIn(TokenizationState.ScriptDataState)
    }

    private fun RAWTEXTEndTagNameStateAnythingElse() {
        emitACharacterToken(LESS_THAN_SIGN)
        emitACharacterToken(SOLIDUS)
        temporaryBuffer.toCharArray().forEach { emitACharacterToken(it) }

        reconsumeIn(TokenizationState.RAWTEXTState)
    }

    private fun RCDATAEndTagNameStateAnythingElse() {
        emitACharacterToken(LESS_THAN_SIGN)
        emitACharacterToken(SOLIDUS)
        temporaryBuffer.toCharArray().map { emitACharacterToken(it) }

        reconsumeIn(TokenizationState.RCDATAState)
    }

    private fun isAnAppropriateEndTagToken(endTagToken: EndTagToken): Boolean {
        return lastEmittedStartTagToken != null && lastEmittedStartTagToken!!.tagName == endTagToken.tagName
    }

    private fun checkCharacterReferenceCode() {
        //FIXME: check all cases
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

    private fun matchInCharacterReferenceTable(): NamedChararcterReference? {
        var index = 0
        var filteredList = NamedChararcterReferenceContainer().namedCharacters.toMutableList()

        while (true) {
            val consumedCharacter = consumeCharacter()
            temporaryBuffer += consumedCharacter.character

            filteredList = filteredList
                .filter { it.matchableName().length > index }
                .filter { it.matchableName()[index] == consumedCharacter.character } as MutableList<NamedChararcterReference>

            if (filteredList.isEmpty()) {
                return null
            } else if (filteredList.size == 1) {
                val referenceNameLength = filteredList.first().matchableName().length
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
        if (wasConsumedAsPartOfAnAttribute()) {
            (currentAttribute as Attribute).value += temporaryBuffer
        } else {
            temporaryBuffer.toCharArray().map { emitACharacterToken(it) }
        }
    }

    data class ParseError(val errorMessage: String, val inputCharacter: InputCharacter?)

    private fun parseError(errorMessage: String, inputCharacter: InputCharacter? = null) {
        parseErrors.add(ParseError(errorMessage, inputCharacter))
    }

    private fun reconsumeIn(newState: TokenizationState) {
        inputStream.reset()
        switchTo(newState)
    }

    private fun switchToReturnState() {
        switchTo(returnState as TokenizationState)
    }

    internal fun switchTo(state: TokenizationState) {
        if (this.state == TokenizationState.AttributeNameState) {
            val latestAttribute = (currentToken as TagToken).attributes.last()
            val matchingNames = (currentToken as TagToken).attributes
                .filter { it.attributeName == latestAttribute.attributeName }
                .count()

            if (matchingNames > 1) {
                parseError("duplicate-attribute")
                (currentToken as TagToken).attributes.removeLast()
                // Note: currentAttribute still points to the now orphaned attribute, and any value read will be consumed but ignored
            }
        }

        this.state = state
    }

    private fun emitCurrentToken() {
        val local = currentToken!!
        currentToken = null
        emit(local)
    }

    private fun emitEndOfFileToken() {
        emit(EndOfFileToken())
    }

    private fun emitACharacterToken(character: Char) {
        emit(CharacterToken(character))
    }

    private fun emitAsACharacterToken(consumedCharacter: InputCharacter) {
        emit(CharacterToken(consumedCharacter))
    }

    private fun emit(token: Token, reprocess: Boolean = false) {
        if (token is StartTagToken) {
            lastEmittedStartTagToken = token
        }

        if (reprocess) {
            emittedTokens.add(0, token)
        } else {
            emittedTokens.add(token)
        }
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


    private fun nextInputCharacterIs(expectedChar: Char): Boolean {
        inputStream.mark(1)
        val peek = Char(inputStream.read())
        inputStream.reset()

        return peek == expectedChar
    }


    private fun nextInputCharacterIsAnAsciiAlphanumeric(): Boolean {
        inputStream.mark(1)
        val peek = consumeCharacter()
        inputStream.reset()

        return peek.isAsciiAlphaNumeric()
    }

    private fun nextFewCharactersMatch(needle: String, haystack: InputStream, ignoreCase: Boolean = false): Boolean {
        haystack.mark(needle.length)

        for (c in needle.toCharArray()) {
            if (haystack.available() <= 0) {
                return false
            }
            val peek = Char(haystack.read())
            if (!peek.toString().equals(c.toString(), ignoreCase = ignoreCase)) {
                haystack.reset()
                return false
            }
        }
        haystack.reset()
        return true
    }

    internal fun reprocess(token: Token) {
        emit(token, reprocess = true)
    }
}