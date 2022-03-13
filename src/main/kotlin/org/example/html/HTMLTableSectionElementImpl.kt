package org.example.html

import org.w3c.dom.html.HTMLCollection
import org.w3c.dom.html.HTMLElement
import org.w3c.dom.html.HTMLTableSectionElement

class HTMLTableSectionElementImpl(tagName:String): HTMLTableSectionElement, HTMLElementImpl(tagName) {
    override fun getAlign(): String {
        TODO("Not yet implemented")
    }

    override fun setAlign(align: String?) {
        TODO("Not yet implemented")
    }

    override fun getCh(): String {
        TODO("Not yet implemented")
    }

    override fun setCh(ch: String?) {
        TODO("Not yet implemented")
    }

    override fun getChOff(): String {
        TODO("Not yet implemented")
    }

    override fun setChOff(chOff: String?) {
        TODO("Not yet implemented")
    }

    override fun getVAlign(): String {
        TODO("Not yet implemented")
    }

    override fun setVAlign(vAlign: String?) {
        TODO("Not yet implemented")
    }

    override fun getRows(): HTMLCollection {
        TODO("Not yet implemented")
    }

    override fun insertRow(index: Int): HTMLElement {
        TODO("Not yet implemented")
    }

    override fun deleteRow(index: Int) {
        TODO("Not yet implemented")
    }
}