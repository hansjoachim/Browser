package org.example.parsing

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TokenizerTest {

    @Test
    fun should_tokenize_simple_example() {
        val simpleExample = "<!DOCTYPE html><html><body></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html"),
            StartTagToken("body"),
            EndTagToken("body"),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_lowercase_doctype() {
        val simpleExample = "<!doctype html><html><body></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html"),
            StartTagToken("body"),
            EndTagToken("body"),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_simple_example_with_text() {
        val simpleExample = "<!DOCTYPE html><html><body><p>hello</p></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html"),
            StartTagToken("body"),
            StartTagToken("p"),
            CharacterToken('h'),
            CharacterToken('e'),
            CharacterToken('l'),
            CharacterToken('l'),
            CharacterToken('o'),
            EndTagToken("p"),
            EndTagToken("body"),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_comment() {
        val simpleExample = "<!DOCTYPE html><html><body><!-- Ignored comment --></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html"),
            StartTagToken("body"),
            CommentToken(" Ignored comment "),
            EndTagToken("body"),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_comment_one_dash_should_not_be_the_end_of_it() {
        val simpleExample = "<!DOCTYPE html><html><body><!-- Comment with - in the middle --></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html"),
            StartTagToken("body"),
            CommentToken(" Comment with - in the middle "),
            EndTagToken("body"),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_attribute() {
        val simpleExample = "<!DOCTYPE html><html lang=en></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html", mutableListOf(Attribute("lang", "en"))),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_attribute_with_single_quotes() {
        val simpleExample = "<!DOCTYPE html><html lang='en'></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html", mutableListOf(Attribute("lang", "en"))),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_attribute_with_double_quotes() {
        val simpleExample = "<!DOCTYPE html><html lang=\"en\"></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html", mutableListOf(Attribute("lang", "en"))),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_multiple_attributes() {
        val simpleExample = "<!DOCTYPE html><html><body id=\"test\" class='some class' lang=en-US></body></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html"),
            StartTagToken(
                "body", mutableListOf(
                    Attribute("id", "test"),
                    Attribute("class", "some class"),
                    Attribute("lang", "en-US"),
                )
            ),
            EndTagToken("body"),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_squashed_attributes() {
        val simpleExample = "<!DOCTYPE html><html><body id='test'class='some class'></body></html>"

        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html"),
            StartTagToken(
                "body", mutableListOf(
                    Attribute("id", "test"),
                    Attribute("class", "some class"),
                )
            ),
            EndTagToken("body"),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_tags_with_numbers_in_names() {
        val simpleExample = "<!DOCTYPE html><html><h1>My title</h1></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html"),
            StartTagToken("h1"),
            CharacterToken('M'),
            CharacterToken('y'),
            CharacterToken(' '),
            CharacterToken('t'),
            CharacterToken('i'),
            CharacterToken('t'),
            CharacterToken('l'),
            CharacterToken('e'),
            EndTagToken("h1"),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_self_closing_tag() {
        val simpleExample = "<!DOCTYPE html><html><br/></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html"),
            StartTagToken("br", selfClosing = true),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_self_closing_tag_this_time_with_space() {
        val simpleExample = "<!DOCTYPE html><html><br /></html>"
        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            DOCTYPEToken("html"),
            StartTagToken("html"),
            StartTagToken("br", selfClosing = true),
            EndTagToken("html"),
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    @Test
    fun should_tokenize_eof_empty_respons() {
        val simpleExample = ""

        val tokenizer = Tokenizer(simpleExample)

        val tokens = allTokens(tokenizer)

        val expectedTokens = listOf(
            EndOfFileToken()
        )

        assertThat(tokens).containsExactlyElementsOf(expectedTokens)
    }

    //TODO: ampersand (in &nbsp; )
    //TODO: bogus comment


    /**
     * Only works when testing simple cases.
     * This methode will NOT handle all cases which depend on the parser adjusting tokenization state!
     */
    private fun allTokens(tokenizer: Tokenizer): List<Token> {
        val allTokens: MutableList<Token> = mutableListOf()

        while (EndOfFileToken() !in allTokens) {
            allTokens.add(tokenizer.nextToken())
        }

        return allTokens
    }
}
