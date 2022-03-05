package org.example.html

import org.w3c.dom.html.HTMLElement

class HTMLUnknownElementImpl(tagName: String) : HTMLElement, HTMLElementImpl(tagName = tagName)