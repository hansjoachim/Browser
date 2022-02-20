package org.example

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TokenizerTest {

    @Test
    fun should_parse_simple_example() {
        val simpleExample = "<!DOCTYPE html><html><body></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html"),
            Token.StartTagToken("body"),
            Token.EndTagToken("body"),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_parse_simple_example_with_text() {
        val simpleExample = "<!DOCTYPE html><html><body><p>hello</p></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html"),
            Token.StartTagToken("body"),
            Token.StartTagToken("p"),
            Token.CharacterToken('h'),
            Token.CharacterToken('e'),
            Token.CharacterToken('l'),
            Token.CharacterToken('l'),
            Token.CharacterToken('o'),
            Token.EndTagToken("p"),
            Token.EndTagToken("body"),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_parse_simple_example_with_comment() {
        val simpleExample = "<!DOCTYPE html><html><body><!-- Ignored comment --></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html"),
            Token.StartTagToken("body"),
            Token.CommentToken(" Ignored comment "),
            Token.EndTagToken("body"),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }
}
