package org.example

import org.example.network.NetworkFetcher

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
    val tokens = Tokenizer(result).tokenize()

    println("tokenized to $tokens")

    val document = Parser().parse(result)
    DOMDebugger.printDOMTree(document)
}


