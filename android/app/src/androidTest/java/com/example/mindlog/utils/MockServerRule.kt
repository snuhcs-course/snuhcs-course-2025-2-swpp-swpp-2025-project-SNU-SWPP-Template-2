package com.example.mindlog.utils

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.ExternalResource

class MockServerRule : ExternalResource() {
    lateinit var server: MockWebServer
    val baseUrl: String get() = server.url("/").toString()

    override fun before() {
        server = MockWebServer()
        server.start()
    }

    override fun after() {
        server.shutdown()
    }

    fun enqueueResponse(body: String, code: Int = 200) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setBody(body)
        )
    }
}