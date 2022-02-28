package org.example.html

import org.w3c.dom.Attr
import org.w3c.dom.Element
import org.w3c.dom.TypeInfo

class AttrImpl(
    private val name: String,
    private val value: String,
    private val ownerElement: Element
) : Attr,
    NodeImpl(
        nodeName = name
    ) {

    override fun getName(): String {
        return name
    }

    override fun getSpecified(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getValue(): String {
        return value
    }

    override fun setValue(value: String?) {
        TODO("Not yet implemented")
    }

    override fun getOwnerElement(): Element {
        return ownerElement
    }

    override fun getSchemaTypeInfo(): TypeInfo {
        TODO("Not yet implemented")
    }

    override fun isId(): Boolean {
        TODO("Not yet implemented")
    }
}