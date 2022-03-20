package org.example.layout

import org.example.parsing.DOMDebugger
import org.example.parsing.Parser
import org.w3c.dom.Comment
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.html.HTMLImageElement
import org.w3c.dom.html.HTMLScriptElement
import org.w3c.dom.html.HTMLStyleElement
import java.awt.FlowLayout
import javax.swing.*

class LayoutEngine {

    fun renderHacky(page: String): JComponent {
        val document = Parser(page).parse()

        //TODO: generate CSSOM and combine the two
        val renderTree = getRenderTree(document)

        return paint(renderTree)
    }

    private fun getRenderTree(DOMTree: Node): Node {
        //FIXME: really don't like to manipulate parameters, but not sure atm how to implement Node.clone() properly
        val renderTree = DOMTree

        val children = mutableListOf<Node>()
        for (i in 0 until DOMTree.childNodes.length) {
            val child = getRenderTree(DOMTree.childNodes.item(i))
            children.add(child)
        }

        while (renderTree.hasChildNodes()) {
            renderTree.removeChild(renderTree.lastChild)
        }

        //Remove non-visible elements
        children
            .filter { it !is HTMLScriptElement }
            .filter { it !is HTMLStyleElement }
            .filter { it !is Comment }
            .map { renderTree.appendChild(it) }

        return renderTree
    }

    fun renderAsString(page: String): String {
        val document = Parser(page).parse()

        //TODO: generate CSSOM and combine the two

        return getDOMTreeAsString(document)
    }

    fun debug(page: String): String {
        val document = Parser(page).parse()
        return DOMDebugger.getDOMTree(document)
    }

    private fun getDOMTreeAsString(node: Node, indentation: String = ""): String {
        var children = ""

        for (i in 0 until node.childNodes.length) {
            children += getDOMTreeAsString(node.childNodes.item(i), indentation + "\t")
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

    private fun paint(node: Node): JComponent {

        val currentNodeInformation = when (node) {
//            is HTMLAnchorElement -> {
//                //FIXME: this should really be a wrapper around the underlying child nodes
//                val link = JLabel("Should be a link")
//                link.toolTipText = node.href
//                link.foreground = Color.BLUE
//                return link
//            }
            is Text -> {
                val textContent = JTextArea(node.data)
                textContent.lineWrap = true
                return textContent
            }
            is HTMLImageElement -> {
                JTextField("[image: " + node.alt + "]")
            }
            else -> {
                container()
            }
        }

        for (i in 0 until node.childNodes.length) {
            val child = paint(node.childNodes.item(i))
            currentNodeInformation.add(child)
        }

        return currentNodeInformation
    }

    private fun container(): JPanel {
        val container = JPanel()
        container.layout = FlowLayout()
        return container
    }
}