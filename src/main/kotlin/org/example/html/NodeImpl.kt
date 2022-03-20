package org.example.html

import org.w3c.dom.*
import org.w3c.dom.DOMException.NOT_FOUND_ERR

open class NodeImpl(private val nodeName: String) : Node {
    private val childNodes: MutableList<Node> = mutableListOf()

    override fun getNodeName(): String {
        return nodeName
    }

    override fun getNodeValue(): String {
        TODO("Not yet implemented")
    }

    override fun setNodeValue(nodeValue: String?) {
        TODO("Not yet implemented")
    }

    override fun getNodeType(): Short {
        TODO("Not yet implemented")
    }

    override fun getParentNode(): Node {
        TODO("Not yet implemented")
    }

    override fun getChildNodes(): NodeList {
        return NodeListImpl(childNodes)
    }

    override fun getFirstChild(): Node {
        return childNodes.first()
    }

    override fun getLastChild(): Node {
        return childNodes.last()
    }

    override fun getPreviousSibling(): Node {
        TODO("Not yet implemented")
    }

    override fun getNextSibling(): Node {
        TODO("Not yet implemented")
    }

    override fun getAttributes(): NamedNodeMap {
        TODO("Not yet implemented")
    }

    override fun getOwnerDocument(): Document {
        TODO("Not yet implemented")
    }

    override fun insertBefore(newChild: Node?, refChild: Node?): Node {
        TODO("Not yet implemented")
    }

    override fun replaceChild(newChild: Node?, oldChild: Node?): Node {
        TODO("Not yet implemented")
    }

    override fun removeChild(oldChild: Node?): Node {
        if (oldChild is Node) {
            childNodes.remove(oldChild)
            return oldChild
        }

        throw DOMException(NOT_FOUND_ERR, "Could not find matching child node")
    }

    override fun appendChild(newChild: Node): Node {
        childNodes.add(newChild)
        return newChild
    }

    override fun hasChildNodes(): Boolean {
        return childNodes.isNotEmpty()
    }

    override fun cloneNode(deep: Boolean): Node {
        TODO("Not yet implemented")
    }

    override fun normalize() {
        TODO("Not yet implemented")
    }

    override fun isSupported(feature: String?, version: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getNamespaceURI(): String {
        TODO("Not yet implemented")
    }

    override fun getPrefix(): String {
        TODO("Not yet implemented")
    }

    override fun setPrefix(prefix: String?) {
        TODO("Not yet implemented")
    }

    override fun getLocalName(): String {
        TODO("Not yet implemented")
    }

    override fun hasAttributes(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getBaseURI(): String {
        TODO("Not yet implemented")
    }

    override fun compareDocumentPosition(other: Node?): Short {
        TODO("Not yet implemented")
    }

    override fun getTextContent(): String {
        TODO("Not yet implemented")
    }

    override fun setTextContent(textContent: String?) {
        TODO("Not yet implemented")
    }

    override fun isSameNode(other: Node?): Boolean {
        TODO("Not yet implemented")
    }

    override fun lookupPrefix(namespaceURI: String?): String {
        TODO("Not yet implemented")
    }

    override fun isDefaultNamespace(namespaceURI: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun lookupNamespaceURI(prefix: String?): String {
        TODO("Not yet implemented")
    }

    override fun isEqualNode(arg: Node?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getFeature(feature: String?, version: String?): Any {
        TODO("Not yet implemented")
    }

    override fun setUserData(key: String?, data: Any?, handler: UserDataHandler?): Any {
        TODO("Not yet implemented")
    }

    override fun getUserData(key: String?): Any {
        TODO("Not yet implemented")
    }
}