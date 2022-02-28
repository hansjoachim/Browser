package org.example.html

import org.w3c.dom.html.HTMLHeadingElement

class HTMLHeadingElementImpl(tagName: String) : HTMLHeadingElement, HTMLElementImpl(tagName = tagName) {
    override fun getAlign(): String {
        TODO("Not yet implemented")
    }

    override fun setAlign(align: String?) {
        TODO("Not yet implemented")
    }
}