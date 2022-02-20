package org.example

import org.junit.Test
import kotlin.test.assertEquals

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

        //assertTrue(tokens.containsAll(expectedTokens))
        // and nothing else
        assertEquals(tokens.size, expectedTokens.size)
    }
}
