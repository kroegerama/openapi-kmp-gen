package com.kroegerama.openapi.kmp.gen.companion

import com.kroegerama.openapi.kmp.gen.companion.AuthPlugin.Plugin.authKeys
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthPluginTest {

    private suspend fun captureRequest(
        resolver: AuthItemResolver,
        block: HttpRequestBuilder.() -> Unit
    ): HttpRequestData {
        var captured: HttpRequestData? = null
        val client = HttpClient(MockEngine) {
            install(AuthPlugin) {
                authItem(resolver)
            }
            engine {
                addHandler { request ->
                    captured = request
                    respond("ok")
                }
            }
        }
        client.get("https://example.com/path", block)
        client.close()
        return requireNotNull(captured) { "MockEngine handler was not invoked" }
    }

    @Test
    fun apiKeyInHeader() = runTest {
        val request = captureRequest(
            resolver = { AuthItem.ApiKey(AuthItem.Position.Header, "X-API-Key", "secret") }
        ) {
            authKeys("k")
        }
        assertEquals("secret", request.headers["X-API-Key"])
    }

    @Test
    fun apiKeyInQuery() = runTest {
        val request = captureRequest(
            resolver = { AuthItem.ApiKey(AuthItem.Position.Query, "api_key", "secret") }
        ) {
            authKeys("k")
        }
        assertEquals("secret", request.url.parameters["api_key"])
    }

    @Test
    fun apiKeyInCookie() = runTest {
        val request = captureRequest(
            resolver = { AuthItem.ApiKey(AuthItem.Position.Cookie, "session", "abc") }
        ) {
            authKeys("k")
        }
        val cookieHeader = request.headers[HttpHeaders.Cookie].orEmpty()
        assertTrue(cookieHeader.contains("session=abc"), cookieHeader)
    }

    @Test
    fun basicAuth() = runTest {
        val request = captureRequest(
            resolver = { AuthItem.Basic("user", "pass") }
        ) {
            authKeys("k")
        }
        // base64("user:pass") == "dXNlcjpwYXNz"
        assertEquals("Basic dXNlcjpwYXNz", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun bearerAuth() = runTest {
        val request = captureRequest(
            resolver = { AuthItem.Bearer("token123") }
        ) {
            authKeys("k")
        }
        assertEquals("Bearer token123", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun noAuthKeysAddsNothing() = runTest {
        val request = captureRequest(
            resolver = { AuthItem.Bearer("token123") }
        ) {
            // no authKeys(...) call -> interceptor returns early
        }
        assertNull(request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun unresolvedKeyAddsNothing() = runTest {
        val request = captureRequest(
            resolver = { null }
        ) {
            authKeys("k")
        }
        assertNull(request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun resolverReceivesRequestedKeysInOrder() = runTest {
        val seen = mutableListOf<String>()
        captureRequest(
            resolver = { key ->
                seen += key
                null
            }
        ) {
            authKeys("a", "b", "a")
        }
        assertEquals(listOf("a", "b", "a"), seen)
    }

    @Test
    fun partiallyResolvedKeysApplyOnlyResolved() = runTest {
        val request = captureRequest(
            resolver = { key -> if (key == "good") AuthItem.Bearer("tok") else null }
        ) {
            authKeys("missing", "good")
        }
        assertEquals("Bearer tok", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun multipleKeysAllApplied() = runTest {
        val request = captureRequest(
            resolver = { key ->
                when (key) {
                    "bearer" -> AuthItem.Bearer("token123")
                    "apikey" -> AuthItem.ApiKey(AuthItem.Position.Header, "X-API-Key", "secret")
                    else -> null
                }
            }
        ) {
            authKeys("bearer", "apikey")
        }
        assertEquals("Bearer token123", request.headers[HttpHeaders.Authorization])
        assertEquals("secret", request.headers["X-API-Key"])
    }
}
