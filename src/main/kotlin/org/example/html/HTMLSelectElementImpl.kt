package org.example.html

import org.w3c.dom.html.HTMLCollection
import org.w3c.dom.html.HTMLElement
import org.w3c.dom.html.HTMLFormElement
import org.w3c.dom.html.HTMLSelectElement

class HTMLSelectElementImpl: HTMLSelectElement, HTMLElementImpl(tagName = "select") {
    override fun getType(): String {
        TODO("Not yet implemented")
    }

    override fun getSelectedIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun setSelectedIndex(selectedIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun getValue(): String {
        TODO("Not yet implemented")
    }

    override fun setValue(value: String?) {
        TODO("Not yet implemented")
    }

    override fun getLength(): Int {
        TODO("Not yet implemented")
    }

    override fun getForm(): HTMLFormElement {
        TODO("Not yet implemented")
    }

    override fun getOptions(): HTMLCollection {
        TODO("Not yet implemented")
    }

    override fun getDisabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setDisabled(disabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getMultiple(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setMultiple(multiple: Boolean) {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun setName(name: String?) {
        TODO("Not yet implemented")
    }

    override fun getSize(): Int {
        TODO("Not yet implemented")
    }

    override fun setSize(size: Int) {
        TODO("Not yet implemented")
    }

    override fun getTabIndex(): Int {
        TODO("Not yet implemented")
    }

    override fun setTabIndex(tabIndex: Int) {
        TODO("Not yet implemented")
    }

    override fun add(element: HTMLElement?, before: HTMLElement?) {
        TODO("Not yet implemented")
    }

    override fun remove(index: Int) {
        TODO("Not yet implemented")
    }

    override fun blur() {
        TODO("Not yet implemented")
    }

    override fun focus() {
        TODO("Not yet implemented")
    }
}