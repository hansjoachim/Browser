package org.example.parsing

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ParserTest {

    @Test
    fun should_parse_simple_example() {
        val simpleExample = "<!DOCTYPE html><html><body><div></div></body></html>"
        val document = Parser(simpleExample).parse()

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
    fun should_parse_xhtml_example() {
        val simpleExample = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
<head>
<title>XHTML 1.0 Strict Example</title>
</head>
<body>
</body>
</html>"""
        val document = Parser(simpleExample).parse()

        val expectedDOM = """#document
	#comment
	html
		head
			#text
			title
				#text
			#text
		#text
		body
			#text
"""
        val tree = DOMDebugger.getDOMTree(document)
        assertThat(tree).isEqualTo(expectedDOM)
    }

    @Test
    fun should_parse_with_missing_doctype() {
        val simpleExample = "<html><body /></html>"
        val document = Parser(simpleExample).parse()

        val expectedDOM = """#document
	html
		head
		body
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
        val document = Parser(simpleExample).parse()

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
        val document = Parser(simpleExample).parse()

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
        val document = Parser(simpleExample).parse()

        val expectedDOM = """#document
	html
		head
			#text
			meta
			#text
			meta
			#text
			link
			#text
		#text
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
        val document = Parser(simpleExample).parse()

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

    @Test
    fun should_parse_with_style() {
        val simpleExample =
            "<html><head><style>abc</style><noscript><style>xyz</style></noscript></head><body><style>123</style></body></html>"
        val document = Parser(simpleExample).parse()

        val expectedDOM = """#document
	html
		head
			style
				#text
			noscript
				style
					#text
		body
			style
				#text
"""
        val tree = DOMDebugger.getDOMTree(document)
        assertThat(tree).isEqualTo(expectedDOM)
    }

    //FIXME: expand with script tags in body
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
<body>
</body>
</html>
"""
        val document = Parser(simpleExample).parse()

        val expectedDOM = """#document
	html
		head
			#text
			script
				#text
			#text
		#text
		body
			#text
"""
        val tree = DOMDebugger.getDOMTree(document)
        assertThat(tree).isEqualTo(expectedDOM)
    }

    @Test
    fun should_parse_plaintext() {
        val simpleExample = """
<!DOCTYPE html>
<html>
<body>
<div></div>
<plaintext>
This tag can contain lots of fun stuff, and it never even ends
</plaintext>
<div><!-- That's why this won't show up --></div>
</body>
</html>
"""
        val document = Parser(simpleExample).parse()

        val expectedDOM = """#document
	html
		head
		body
			#text
			div
			#text
			plaintext
				#text
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
        val document = Parser(simpleExample).parse()

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

//TODO: emit more than one thing to a list/stream?
}
