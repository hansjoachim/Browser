package org.example.layout

import org.example.parsing.DOMDebugger
import org.example.parsing.Parser
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.html.HTMLImageElement

class LayoutEngine {

    fun render(page: String): String {
        val document = Parser(page).parse()

        //TODO: generate CSSOM and combine the two

        return getDOMTree(document)
    }

    fun debug(page: String): String {
        val document = Parser(page).parse()
        return DOMDebugger.getDOMTree(document)
    }

    private fun getDOMTree(node: Node, indentation: String = ""): String {
        var children = ""

        for (i in 0 until node.childNodes.length) {
            children += getDOMTree(node.childNodes.item(i), indentation + "\t")
        }

        val currentNodeInformation = when (node) {
            is Text -> {
                node.data
            }
            is HTMLImageElement -> {
                node.alt
            }
            else -> {
                ""
            }
        }

        return currentNodeInformation + children
    }
}