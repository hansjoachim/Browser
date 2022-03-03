package org.example.network

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class NetworkFetcher(private val client: HttpClient) {
    constructor() : this(HttpClient.newBuilder().build())

    fun getRequest(address:String): String {
        val uri = URI(address)
        val request = HttpRequest.newBuilder(uri)
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 301 || response.statusCode() == 302) {
            val possibleLocation = response.headers().firstValue("Location")
            if (possibleLocation.isPresent) {
                println("Redirected to... ${possibleLocation.get()}")
                return getRequest(possibleLocation.get())
            }
        }
        //TODO: determine charset and based on MIME-type send to parser
        return response.body()
    }
}