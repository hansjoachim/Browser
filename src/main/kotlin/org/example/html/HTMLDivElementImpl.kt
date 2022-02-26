package org.example.html

import org.w3c.dom.html.HTMLDivElement

class HtmlDivElementImpl : HTMLDivElement, HTMLElementImpl(tagName = "div") {

    override fun getAlign(): String {
        TODO("Not yet implemented")
    }

    override fun setAlign(align: String?) {
        TODO("Not yet implemented")
    }
}