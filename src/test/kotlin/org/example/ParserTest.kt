package org.example

import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
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

    @Ignore("FIXME: support meta and link tags")
    @Test
    fun should_parse_example_with_meta_tags() {
        val simpleExample = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="referrer" content="origin">
    <link href="main.css" rel="stylesheet">
</head>
<body />
</html>
"""
        val parser = Parser()
        val document = parser.parse(simpleExample)

        val expectedDOM = """#document
	html
		head
			meta
			meta
			link
		body
			#text
"""
        val tree = DOMDebugger.getDOMTree(document)
        assertThat(tree).isEqualTo(expectedDOM)
    }

    @Test
    fun should_parse_noscript() {
        val simpleExample = """
<!DOCTYPE html>
<html>
<head>
<noscript><!-- This is only added when scripting is disabled --></noscript>
</head>
<body>
<noscript><!-- ditto --></noscript>
</body>
</html>
"""
        val parser = Parser()
        val document = parser.parse(simpleExample)

        val expectedDOM = """#document
	html
		head
			#text
			noscript
				#comment
			#text
		#text
		body
			#text
			noscript
				#comment
			#text
"""
        val tree = DOMDebugger.getDOMTree(document)
        assertThat(tree).isEqualTo(expectedDOM)
    }

    //TODO : this case is in parser instead of tokenizer since the parser is the one giving the signal to the tokenizer
    @Ignore("FIXME: right now script content can break in strange ways if it stumble across < symbols. Should parse this as pure text to fix those errors ")
    @Test
    fun should_parse_script() {
        val simpleExample = """
<!DOCTYPE html>
<html>
<head>
<script>/*Javascript comment. And it can even include <!-- ignored html comments --> */
function comparison(a, b)  {
    return a < b
}
</script>
</head>
</body>
</html>
"""
        val parser = Parser()
        val document = parser.parse(simpleExample)

        val expectedDOM = """#document
	html
		head
			script
		body
"""
        val tree = DOMDebugger.getDOMTree(document)
        assertThat(tree).isEqualTo(expectedDOM)
    }


    //TODO: expected this would break with the simple popping of current element. Let's revisit this in a while
    @Test
    fun should_append_nested_elements_correctly() {
        val simpleExample = """
<!DOCTYPE html>
<html>
<body>
    <div>
        <div>
            <div></div>
        </div>
        <div>
            <div></div>
            <div></div>
        </div>
    </div>
    <div>
        <div>
            <div></div>
            <div></div>
        </div>
        <div>
            <div></div>
        </div>
    </div>
</body>
</html>
"""
        val parser = Parser()
        val document = parser.parse(simpleExample)

        val expectedDOM = """#document
	html
		head
		body
			#text
			div
				#text
				div
					#text
					div
					#text
				#text
				div
					#text
					div
					#text
					div
					#text
				#text
			#text
			div
				#text
				div
					#text
					div
					#text
					div
					#text
				#text
				div
					#text
					div
					#text
				#text
			#text
"""
        val tree = DOMDebugger.getDOMTree(document)
        assertThat(tree).isEqualTo(expectedDOM)
    }
}