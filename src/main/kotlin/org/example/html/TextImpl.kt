package org.example.html

import org.w3c.dom.Text


class TextImpl(data: String = "") : Text, CharacterDataImpl(nodeName = "#text", data = data) {
    override fun splitText(offset: Int): Text {
        TODO("Not yet implemented")
    }

    override fun isElementContentWhitespace(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getWholeText(): String {
        TODO("Not yet implemented")
    }

    override fun replaceWholeText(content: String?): Text {
        TODO("Not yet implemented")
    }
}