package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("You didn't give me a URI as the first parameter")
        return
    }
    val uri = URI(args[0])
    goTo(uri)
}

private fun goTo(uri: URI) {
    val request = HttpRequest.newBuilder(uri)
        .GET()
        .build()
    val client = HttpClient.newBuilder().build()

    val result = client.send(request, BodyHandlers.ofString()).body()
    println(result)
    val tokens = Tokenizer(result).tokenize()

    println("tokenized to $tokens")

    val document = Parser().parse(result)
    DOMDebugger.printDOMTree(document)
}

