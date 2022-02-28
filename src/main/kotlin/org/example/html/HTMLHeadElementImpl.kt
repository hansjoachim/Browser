package org.example.html

import org.w3c.dom.html.HTMLHeadElement

class HTMLHeadElementImpl : HTMLHeadElement, HTMLElementImpl(tagName = "head") {
    override fun getProfile(): String {
        TODO("Not yet implemented")
    }

    override fun setProfile(profile: String?) {
        TODO("Not yet implemented")
    }
}