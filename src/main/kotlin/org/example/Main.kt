package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

fun main(args: Array<String>) {
    val request = HttpRequest.newBuilder(URI(args[0]))
        .GET()
        .build()
    val client = HttpClient.newBuilder().build()

    val result = client.send(request, BodyHandlers.ofString()).body()
    println(result)
    val tokens = Tokenizer(result).tokenize()

    println("tokenized to $tokens")
}

