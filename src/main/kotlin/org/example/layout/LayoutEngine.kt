package org.example.layout

import org.example.parsing.DOMDebugger
import org.example.parsing.Parser

class LayoutEngine {

    //TODO: should replace debug DOM with real rendering
    fun render(page:String): String {
        val document = Parser(page).parse()
        return DOMDebugger.getDOMTree(document)
    }
}