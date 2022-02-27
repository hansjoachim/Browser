package org.example.parsing

import org.w3c.dom.Node

class DOMDebugger {
    companion object {
        fun printDOMTree(node: Node) {
            val tree = getDOMTree(node)
            println(tree)
        }

        fun getDOMTree(node: Node, indentation: String = ""): String {
            var children = ""

            for (i in 0 until node.childNodes.length) {
                children += getDOMTree(node.childNodes.item(i), indentation + "\t")
            }

            return indentation + node.nodeName + "\n" + children
        }
    }
}
