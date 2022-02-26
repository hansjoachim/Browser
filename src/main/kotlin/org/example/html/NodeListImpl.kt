package org.example.html

import org.w3c.dom.Node
import org.w3c.dom.NodeList

class NodeListImpl(private val list: List<Node>) : NodeList {
    override fun item(index: Int): Node {
        return list[index]
    }

    override fun getLength(): Int {
        return list.size
    }
}