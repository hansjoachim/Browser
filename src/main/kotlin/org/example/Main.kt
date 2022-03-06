package org.example

import org.example.gui.BrowserWindow
import org.example.network.NetworkFetcher
import org.example.parsing.DOMDebugger
import org.example.parsing.Parser

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        BrowserWindow()
        return
    }
    goTo(args[0])
}

private fun goTo(address: String) {
    val result = NetworkFetcher().getRequest(address)
    println(result)

    val document = Parser(result).parse()
    DOMDebugger.printDOMTree(document)
}


