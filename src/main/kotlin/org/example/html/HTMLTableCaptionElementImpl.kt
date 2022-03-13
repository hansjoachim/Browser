package org.example.html

import org.w3c.dom.html.HTMLTableCaptionElement

class HTMLTableCaptionElementImpl: HTMLTableCaptionElement, HTMLElementImpl(tagName = "caption") {
    override fun getAlign(): String {
        TODO("Not yet implemented")
    }

    override fun setAlign(align: String?) {
        TODO("Not yet implemented")
    }
}