package org.example.html

import org.w3c.dom.html.HTMLCollection
import org.w3c.dom.html.HTMLElement
import org.w3c.dom.html.HTMLTableRowElement

class HTMLTableRowElementImpl : HTMLTableRowElement, HTMLElementImpl("tr") {
    override fun getRowIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun getSectionRowIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun getCells(): HTMLCollection {
        TODO("Not yet implemented")
    }

    override fun getAlign(): String {
        TODO("Not yet implemented")
    }

    override fun setAlign(align: String?) {
        TODO("Not yet implemented")
    }

    override fun getBgColor(): String {
        TODO("Not yet implemented")
    }

    override fun setBgColor(bgColor: String?) {
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

    override fun insertCell(index: Int): HTMLElement {
        TODO("Not yet implemented")
    }

    override fun deleteCell(index: Int) {
        TODO("Not yet implemented")
    }
}