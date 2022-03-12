package org.example.network

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpResponse
import java.util.function.BiPredicate

class NetworkFetcherTest {

    @Test
    fun should_fetch_response() {
        val mockClient = mockk<HttpClient>()
        every {
            mockClient.send(
                any(),
                eq(HttpResponse.BodyHandlers.ofString())
            )
        } returns mockHttpResponse(body = "Hello world!")
        val network = NetworkFetcher(mockClient)

        val response = network.getRequest("http://example.com")

        assertThat(response).isEqualTo("Hello world!")
        verify(exactly = 1) { mockClient.send(any(), eq(HttpResponse.BodyHandlers.ofString())) }
    }

    @Test
    fun should_follow_redirects() {
        val mockClient = mockk<HttpClient>()
        every { mockClient.send(any(), eq(HttpResponse.BodyHandlers.ofString())) } returns mockHttpResponse(
            301,
            "First",
            mapOf(Pair("Location", listOf("http://moved.example.com")))
        ) andThen mockHttpResponse(
            200,
            "Second"
        )
        val network = NetworkFetcher(mockClient)

        val response = network.getRequest("http://example.com")

        assertThat(response).isEqualTo("Second")

        verifyOrder {
            mockClient.send(match { it.uri().host == "example.com" }, eq(HttpResponse.BodyHandlers.ofString()))
            mockClient.send(match { it.uri().host == "moved.example.com" }, eq(HttpResponse.BodyHandlers.ofString()))
        }
    }

    @Test
    fun should_follow_relative_redirects() {
        val mockClient = mockk<HttpClient>()
        every { mockClient.send(any(), eq(HttpResponse.BodyHandlers.ofString())) } returns mockHttpResponse(
            301,
            "First",
            mapOf(Pair("Location", listOf("/index.html")))
        ) andThen mockHttpResponse(
            200,
            "Second"
        )
        val network = NetworkFetcher(mockClient)

        val response = network.getRequest("http://example.com")

        assertThat(response).isEqualTo("Second")

        verifyOrder {
            mockClient.send(match { it.uri().host == "example.com" && it.uri().path.isBlank() }, eq(HttpResponse.BodyHandlers.ofString()))
            mockClient.send(match { it.uri().host == "example.com" && it.uri().path == "/index.html" }, eq(HttpResponse.BodyHandlers.ofString()))
        }
    }

    @Test
    fun should_prepend_protocol_to_raw_domain() {
        val url = NetworkFetcher.urlString("www.example.com")

        assertThat(url).isEqualTo("http://www.example.com")
    }

    @Test
    fun should_keep_existing_http_protocol() {
        val url = NetworkFetcher.urlString("http://www.example.com")

        assertThat(url).isEqualTo("http://www.example.com")
    }

    @Test
    fun should_keep_existing_https_protocol() {
        val url = NetworkFetcher.urlString("https://www.example.com")

        assertThat(url).isEqualTo("https://www.example.com")
    }

    private fun mockHttpResponse(
        statusCode: Int = 200,
        body: String,
        headers: Map<String, List<String>> = mapOf()
    ): HttpResponse<String> {
        val mockResponse = mockk<HttpResponse<String>>()
        every { mockResponse.statusCode() } returns statusCode
        every { mockResponse.body() } returns body
        every { mockResponse.headers() } returns HttpHeaders.of(headers, AlwaysTrueFilter())
        return mockResponse
    }
}

class AlwaysTrueFilter : BiPredicate<String, String> {
    override fun test(t: String, u: String): Boolean {
        return true
    }
}
