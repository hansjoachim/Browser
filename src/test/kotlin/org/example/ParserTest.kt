package org.example

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ParserTest {

    @Test
    fun should_parse_simple_example() {
        val simpleExample = "<!DOCTYPE html><html><body></body></html>"
        val parser = Parser()
        val document = parser.parse(simpleExample)

        assertThat(document).isNotNull
        assertThat(document.children).isNotEmpty
    }
}