package org.example.html

import org.w3c.dom.Attr
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.w3c.dom.TypeInfo

open class ElementImpl(
    private val tagName: String
) : NodeImpl(nodeName = tagName), Element {
    override fun getTagName(): String {
        return tagName
    }

    override fun getAttribute(name: String?): String {
        TODO("Not yet implemented")
    }

    override fun setAttribute(name: String?, value: String?) {
        TODO("Not yet implemented")
    }

    override fun removeAttribute(name: String?) {
        TODO("Not yet implemented")
    }

    override fun getAttributeNode(name: String?): Attr {
        TODO("Not yet implemented")
    }

    override fun setAttributeNode(newAttr: Attr?): Attr {
        TODO("Not yet implemented")
    }

    override fun removeAttributeNode(oldAttr: Attr?): Attr {
        TODO("Not yet implemented")
    }

    override fun getElementsByTagName(name: String?): NodeList {
        TODO("Not yet implemented")
    }

    override fun getAttributeNS(namespaceURI: String?, localName: String?): String {
        TODO("Not yet implemented")
    }

    override fun setAttributeNS(namespaceURI: String?, qualifiedName: String?, value: String?) {
        TODO("Not yet implemented")
    }

    override fun removeAttributeNS(namespaceURI: String?, localName: String?) {
        TODO("Not yet implemented")
    }

    override fun getAttributeNodeNS(namespaceURI: String?, localName: String?): Attr {
        TODO("Not yet implemented")
    }

    override fun setAttributeNodeNS(newAttr: Attr?): Attr {
        TODO("Not yet implemented")
    }

    override fun getElementsByTagNameNS(namespaceURI: String?, localName: String?): NodeList {
        TODO("Not yet implemented")
    }

    override fun hasAttribute(name: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasAttributeNS(namespaceURI: String?, localName: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getSchemaTypeInfo(): TypeInfo {
        TODO("Not yet implemented")
    }

    override fun setIdAttribute(name: String?, isId: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setIdAttributeNS(namespaceURI: String?, localName: String?, isId: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setIdAttributeNode(idAttr: Attr?, isId: Boolean) {
        TODO("Not yet implemented")
    }
}