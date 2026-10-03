package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.request.HttpRequestData
import io.ktor.http.Url
import kotlinx.serialization.json.Json

internal class TestApiHolder : ApiHolder() {
    override var baseUrl: Url = Url("https://api.example.com/v1/")

    // expose the protected registration hooks for assertions
    fun register(id: String, provider: AuthItemProvider) = setAuthProvider(id, provider)
    fun unregister(id: String) = clearAuthProvider(id)
}

/** Records each request in [requests] and answers it with [body] as `application/json`. */
internal fun recordingJsonHandler(
    requests: MutableList<HttpRequestData>,
    body: String = ""
): MockRequestHandler = { request ->
    requests += request
    respondJson(body)
}

internal fun mockApiClient(
    json: Json,
    requests: MutableList<HttpRequestData> = mutableListOf(),
    body: String = ""
): HttpClient = createApiHttpClient(MockEngine, json, userAgent = null) {
    engine {
        addHandler(recordingJsonHandler(requests, body))
    }
}
