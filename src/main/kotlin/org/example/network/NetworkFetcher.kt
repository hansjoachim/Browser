package org.example.network

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class NetworkFetcher(private val client: HttpClient) {
    constructor() : this(HttpClient.newBuilder().build())

    companion object {
        fun urlString(url: String): String {
            if (isAbsolute(url)) {
                return url
            }
            return "http://$url"
        }

        private fun isAbsolute(url: String) = url.contains("://")
    }

    fun getRequest(addresse: String): String {
        val uri = URI(urlString(addresse))
        val request = HttpRequest.newBuilder(uri)
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        val redirectStatusCodesWithLocation = listOf(301, 302, 303, 307, 308)
        if (redirectStatusCodesWithLocation.contains(response.statusCode())) {
            val possibleLocation = response.headers().firstValue("Location")
            if (possibleLocation.isPresent) {

                val locationValue = possibleLocation.get()
                val newLocation = when (isAbsolute(locationValue)) {
                    true -> locationValue
                    false -> addresse + locationValue
                }

                println("Redirected to... $newLocation (based on $locationValue)")
                return getRequest(newLocation)
            }
        }
        //TODO: determine charset and based on MIME-type send to parser
        return response.body()
    }
}