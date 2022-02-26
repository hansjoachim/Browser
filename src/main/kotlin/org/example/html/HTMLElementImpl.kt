package org.example.html

import org.w3c.dom.html.HTMLElement

open class HTMLElementImpl(
    tagName: String
) : HTMLElement, ElementImpl(tagName) {
    override fun getId(): String {
        TODO("Not yet implemented")
    }

    override fun setId(id: String?) {
        TODO("Not yet implemented")
    }

    override fun getTitle(): String {
        TODO("Not yet implemented")
    }

    override fun setTitle(title: String?) {
        TODO("Not yet implemented")
    }

    override fun getLang(): String {
        TODO("Not yet implemented")
    }

    override fun setLang(lang: String?) {
        TODO("Not yet implemented")
    }

    override fun getDir(): String {
        TODO("Not yet implemented")
    }

    override fun setDir(dir: String?) {
        TODO("Not yet implemented")
    }

    override fun getClassName(): String {
        TODO("Not yet implemented")
    }

    override fun setClassName(className: String?) {
        TODO("Not yet implemented")
    }

}
