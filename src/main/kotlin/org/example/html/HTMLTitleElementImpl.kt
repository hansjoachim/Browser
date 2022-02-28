package org.example.html

import org.w3c.dom.html.HTMLTitleElement

class HTMLTitleElementImpl : HTMLTitleElement, HTMLElementImpl(tagName = "title") {
    override fun getText(): String {
        TODO("Not yet implemented")
    }

    override fun setText(text: String?) {
        TODO("Not yet implemented")
    }
}