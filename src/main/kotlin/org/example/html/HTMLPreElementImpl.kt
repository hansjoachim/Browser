package org.example.html

import org.w3c.dom.html.HTMLPreElement

class HTMLPreElementImpl(tagName:String): HTMLPreElement, HTMLElementImpl(tagName = tagName) {
    override fun getWidth(): Int {
        TODO("Not yet implemented")
    }

    override fun setWidth(width: Int) {
        TODO("Not yet implemented")
    }
}