package org.example

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ParserTest {

    @Test
    fun should_parse_simple_example() {
        val simpleExample = "<!DOCTYPE html><html><body><div></div></body></html>"
        val parser = Parser()
        val document = parser.parse(simpleExample)

        val expectedDOM = """#document
	html
		head
		body
			div
"""
        val tree = DOMDebugger.getDOMTree(document)
        assertThat(tree).isEqualTo(expectedDOM)
    }

    @Test
    fun should_parse_example_with_text() {
        val simpleExample = """
<!DOCTYPE html>
<html>
<head>
    <title>My title</title>
</head>
<body>
<h1>Welcome!</h1>
</body>
</html>
"""
        val parser = Parser()
        val document = parser.parse(simpleExample)

        val expectedDOM = """#document
	html
		head
			#text
			title
				#text
			#text
		#text
		body
			#text
			h1
				#text
			#text
"""
        val tree = DOMDebugger.getDOMTree(document)
        assertThat(tree).isEqualTo(expectedDOM)
    }

    @Test
    fun should_parse_example_with_comments() {
        val simpleExample = """
<!-- In the beginning... -->
<!DOCTYPE html>
<!-- Some comment after DOCTYPE -->
<html>
<!-- Some comment before head -->
<head>
<!-- Some comment in head -->
</head>
<!-- Some comment after head -->
<body>
<!-- Some comment in body -->
</body>
<!-- Some comment after body -->
</html>
<!-- Some comment at the end -->
"""
        val parser = Parser()
        val document = parser.parse(simpleExample)

        val expectedDOM = """#document
	#comment
	#comment
	html
		#comment
		head
			#text
			#comment
			#text
		#text
		#comment
		#text
		body
			#text
			#comment
			#text
		#comment
	#comment
"""
        val tree = DOMDebugger.getDOMTree(document)
        assertThat(tree).isEqualTo(expectedDOM)
    }

    //TODO: meta tags

}