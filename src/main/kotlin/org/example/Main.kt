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
    val result = getRequest(uri)
    println(result)
    val tokens = Tokenizer(result).tokenize()

    println("tokenized to $tokens")

    val document = Parser().parse(result)
    DOMDebugger.printDOMTree(document)
}

private fun getRequest(uri: URI): String {
    val request = HttpRequest.newBuilder(uri)
        .GET()
        .build()
    val client = HttpClient.newBuilder().build()

    val response = client.send(request, BodyHandlers.ofString())

    if (response.statusCode() == 301) {
        val possibleLocation = response.headers().firstValue("Location")
        if (possibleLocation.isPresent) {
            println("Redirected to... ${possibleLocation.get()}")
            return getRequest(URI(possibleLocation.get()))
        }
    }
    return response.body()
}

