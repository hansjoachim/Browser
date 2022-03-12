package org.example.html

import org.w3c.dom.html.HTMLFormElement
import org.w3c.dom.html.HTMLOptionElement

class HTMLOptionElementImpl: HTMLOptionElement, HTMLElementImpl(tagName = "option") {
    override fun getForm(): HTMLFormElement {
        TODO("Not yet implemented")
    }

    override fun getDefaultSelected(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setDefaultSelected(defaultSelected: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getText(): String {
        TODO("Not yet implemented")
    }

    override fun getIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun getDisabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setDisabled(disabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getLabel(): String {
        TODO("Not yet implemented")
    }

    override fun setLabel(label: String?) {
        TODO("Not yet implemented")
    }

    override fun getSelected(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setSelected(selected: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getValue(): String {
        TODO("Not yet implemented")
    }

    override fun setValue(value: String?) {
        TODO("Not yet implemented")
    }
}