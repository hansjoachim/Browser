package org.example.html

import org.w3c.dom.html.HTMLStyleElement

class HTMLStyleElementImpl: HTMLStyleElement, HTMLElementImpl(tagName = "style") {
    override fun getDisabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setDisabled(disabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getMedia(): String {
        TODO("Not yet implemented")
    }

    override fun setMedia(media: String?) {
        TODO("Not yet implemented")
    }

    override fun getType(): String {
        TODO("Not yet implemented")
    }

    override fun setType(type: String?) {
        TODO("Not yet implemented")
    }
}