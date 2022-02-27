package org.example.html

import org.w3c.dom.html.HTMLParagraphElement

class HTMLParagraphElementImpl: HTMLParagraphElement, HTMLElementImpl(tagName = "p") {
    override fun getAlign(): String {
        TODO("Not yet implemented")
    }

    override fun setAlign(align: String?) {
        TODO("Not yet implemented")
    }
}