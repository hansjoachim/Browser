package org.example

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TokenizerTest {

    @Test
    fun should_tokenize_simple_example() {
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
    fun should_tokenize_lowercase_doctype() {
        val simpleExample = "<!doctype html><html><body></body></html>"
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
    fun should_tokenize_simple_example_with_text() {
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
    fun should_tokenize_comment() {
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

    @Test
    fun should_tokenize_comment_one_dash_should_not_be_the_end_of_it() {
        val simpleExample = "<!DOCTYPE html><html><body><!-- Comment with - in the middle --></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html"),
            Token.StartTagToken("body"),
            Token.CommentToken(" Comment with - in the middle "),
            Token.EndTagToken("body"),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_attribute() {
        val simpleExample = "<!DOCTYPE html><html lang=en></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html", mutableListOf(Token.Attribute("lang", "en"))),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_attribute_with_single_quotes() {
        val simpleExample = "<!DOCTYPE html><html lang='en'></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html", mutableListOf(Token.Attribute("lang", "en"))),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_attribute_with_double_quotes() {
        val simpleExample = "<!DOCTYPE html><html lang=\"en\"></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html", mutableListOf(Token.Attribute("lang", "en"))),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_multiple_attributes() {
        val simpleExample = "<!DOCTYPE html><html><body id=\"test\" class='some class' lang=en-US></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html"),
            Token.StartTagToken("body", mutableListOf(
                Token.Attribute("id", "test"),
                Token.Attribute("class", "some class"),
                Token.Attribute("lang", "en-US"),
            )),
            Token.EndTagToken("body"),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_numbers_in_names() {
        val simpleExample = "<!DOCTYPE html><html><h1>My title</h1></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html"),
            Token.StartTagToken("h1"),
            Token.CharacterToken('M'),
            Token.CharacterToken('y'),
            Token.CharacterToken(' '),
            Token.CharacterToken('t'),
            Token.CharacterToken('i'),
            Token.CharacterToken('t'),
            Token.CharacterToken('l'),
            Token.CharacterToken('e'),
            Token.EndTagToken("h1"),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_self_closing_tag() {
        val simpleExample = "<!DOCTYPE html><html><br/></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html"),
            Token.StartTagToken("br", selfClosing = true),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_self_closing_tag_this_time_with_space() {
        val simpleExample = "<!DOCTYPE html><html><br /></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = tokenizer.tokenize()

        val expectedTokens = listOf(
            Token.DOCTYPEToken("html"),
            Token.StartTagToken("html"),
            Token.StartTagToken("br", selfClosing = true),
            Token.EndTagToken("html"),
            Token.EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }
}
