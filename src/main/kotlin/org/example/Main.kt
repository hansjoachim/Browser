package org.example

import org.example.network.NetworkFetcher
import org.example.parsing.DOMDebugger
import org.example.parsing.Parser
import org.example.parsing.Tokenizer

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("You didn't give me a URI as the first parameter")
        return
    }
    goTo(args[0])
}

val network = NetworkFetcher()

private fun goTo(address: String) {
    val result = network.getRequest(address)
    println(result)

    val document = Parser(result).parse()
    DOMDebugger.printDOMTree(document)
}


