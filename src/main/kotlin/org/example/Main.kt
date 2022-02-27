package org.example

import org.example.network.NetworkFetcher
import java.net.URI

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("You didn't give me a URI as the first parameter")
        return
    }
    val uri = URI(args[0])
    goTo(uri)
}

val network = NetworkFetcher()

private fun goTo(uri: URI) {
    val result = network.getRequest(uri)
    println(result)
    val tokens = Tokenizer(result).tokenize()

    println("tokenized to $tokens")

    val document = Parser().parse(result)
    DOMDebugger.printDOMTree(document)
}


