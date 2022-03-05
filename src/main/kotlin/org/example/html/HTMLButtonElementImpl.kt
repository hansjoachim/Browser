package org.example.html

import org.w3c.dom.html.HTMLButtonElement
import org.w3c.dom.html.HTMLFormElement

class HTMLButtonElementImpl: HTMLButtonElement, HTMLElementImpl("button") {
    override fun getForm(): HTMLFormElement {
        TODO("Not yet implemented")
    }

    override fun getAccessKey(): String {
        TODO("Not yet implemented")
    }

    override fun setAccessKey(accessKey: String?) {
        TODO("Not yet implemented")
    }

    override fun getDisabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setDisabled(disabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun setName(name: String?) {
        TODO("Not yet implemented")
    }

    override fun getTabIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun setTabIndex(tabIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun getType(): String {
        TODO("Not yet implemented")
    }

    override fun getValue(): String {
        TODO("Not yet implemented")
    }

    override fun setValue(value: String?) {
        TODO("Not yet implemented")
    }
}