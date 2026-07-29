package com.kroegerama.openapi.kmp.gen.companion

import com.kroegerama.openapi.kmp.gen.companion.AuthPlugin.Plugin.authKeys
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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

    private class RetryHarness(
        val engine: MockEngine,
        val client: HttpClient
    )

    private fun retryHarness(
        resolver: AuthItemResolver,
        handler: UnauthorizedHandler? = null,
        expectSuccess: Boolean = false,
        responder: MockRequestHandleScope.(callIndex: Int) -> HttpResponseData
    ): RetryHarness {
        var calls = 0
        val engine = MockEngine { responder(calls++) }
        val client = HttpClient(engine) {
            this.expectSuccess = expectSuccess
            install(AuthPlugin) {
                authItem(resolver)
                if (handler != null) {
                    onUnauthorized(handler)
                }
            }
        }
        return RetryHarness(engine, client)
    }

    private fun MockRequestHandleScope.respondUnauthorizedThenOk(callIndex: Int): HttpResponseData =
        if (callIndex == 0) respond("nope", HttpStatusCode.Unauthorized) else respond("ok")

    @Test
    fun unauthorizedRetriesOnceWithFreshValues() = runTest {
        var resolverCalls = 0
        val handlerItems = mutableListOf<Map<String, AuthItem>>()
        val harness = retryHarness(
            resolver = { AuthItem.Bearer("token-${++resolverCalls}") },
            handler = { items ->
                handlerItems += items
                true
            },
            responder = { respondUnauthorizedThenOk(it) }
        )

        val response = harness.client.get("https://example.com/path") { authKeys("k") }

        assertEquals(HttpStatusCode.OK, response.status)
        val history = harness.engine.requestHistory
        assertEquals(2, history.size)
        assertEquals("Bearer token-1", history[0].headers[HttpHeaders.Authorization])
        assertEquals("Bearer token-2", history[1].headers[HttpHeaders.Authorization])
        assertEquals(listOf(mapOf<String, AuthItem>("k" to AuthItem.Bearer("token-1"))), handlerItems)
        harness.client.close()
    }

    @Test
    fun unauthorizedWithDecliningHandlerIsNotRetried() = runTest {
        val harness = retryHarness(
            resolver = { AuthItem.Bearer("token") },
            handler = { false },
            responder = { respondUnauthorizedThenOk(it) }
        )

        val response = harness.client.get("https://example.com/path") { authKeys("k") }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(1, harness.engine.requestHistory.size)
        harness.client.close()
    }

    @Test
    fun unauthorizedWithoutHandlerIsNotRetried() = runTest {
        val harness = retryHarness(
            resolver = { AuthItem.Bearer("token") },
            responder = { respondUnauthorizedThenOk(it) }
        )

        val response = harness.client.get("https://example.com/path") { authKeys("k") }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(1, harness.engine.requestHistory.size)
        harness.client.close()
    }

    @Test
    fun unauthorizedRetriesAtMostOnce() = runTest {
        var handlerCalls = 0
        val harness = retryHarness(
            resolver = { AuthItem.Bearer("token") },
            handler = {
                handlerCalls++
                true
            },
            responder = { respond("nope", HttpStatusCode.Unauthorized) }
        )

        val response = harness.client.get("https://example.com/path") { authKeys("k") }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(2, harness.engine.requestHistory.size)
        assertEquals(1, handlerCalls)
        harness.client.close()
    }

    @Test
    fun otherErrorStatusDoesNotInvokeHandler() = runTest {
        var handlerCalls = 0
        val harness = retryHarness(
            resolver = { AuthItem.Bearer("token") },
            handler = {
                handlerCalls++
                true
            },
            responder = { respond("nope", HttpStatusCode.Forbidden) }
        )

        val response = harness.client.get("https://example.com/path") { authKeys("k") }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(1, harness.engine.requestHistory.size)
        assertEquals(0, handlerCalls)
        harness.client.close()
    }

    @Test
    fun requestWithoutAuthKeysIsNotRetried() = runTest {
        var handlerCalls = 0
        val harness = retryHarness(
            resolver = { AuthItem.Bearer("token") },
            handler = {
                handlerCalls++
                true
            },
            responder = { respondUnauthorizedThenOk(it) }
        )

        val response = harness.client.get("https://example.com/path")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(1, harness.engine.requestHistory.size)
        assertEquals(0, handlerCalls)
        harness.client.close()
    }

    @Test
    fun unresolvedKeysReachHandlerAsEmptyMap() = runTest {
        val handlerItems = mutableListOf<Map<String, AuthItem>>()
        val harness = retryHarness(
            resolver = { null },
            handler = { items ->
                handlerItems += items
                false
            },
            responder = { respondUnauthorizedThenOk(it) }
        )

        val response = harness.client.get("https://example.com/path") { authKeys("k") }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(listOf(emptyMap<String, AuthItem>()), handlerItems)
        assertEquals(1, harness.engine.requestHistory.size)
        harness.client.close()
    }

    @Test
    fun retryReplacesAllAppliedValues() = runTest {
        var round = 0
        val harness = retryHarness(
            resolver = { key ->
                val suffix = if (round == 0) "1" else "2"
                when (key) {
                    "bearer" -> AuthItem.Bearer("t$suffix")
                    "header" -> AuthItem.ApiKey(AuthItem.Position.Header, "X-API-Key", "h$suffix")
                    "query" -> AuthItem.ApiKey(AuthItem.Position.Query, "api_key", "q$suffix")
                    "cookie" -> AuthItem.ApiKey(AuthItem.Position.Cookie, "session", "c$suffix")
                    else -> null
                }
            },
            handler = {
                round = 1
                true
            },
            responder = { respondUnauthorizedThenOk(it) }
        )

        val response = harness.client.get("https://example.com/path") {
            authKeys("bearer", "header", "query", "cookie")
            cookie("other", "keep")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val retried = harness.engine.requestHistory[1]
        assertEquals(listOf("Bearer t2"), retried.headers.getAll(HttpHeaders.Authorization))
        assertEquals(listOf("h2"), retried.headers.getAll("X-API-Key"))
        assertEquals(listOf("q2"), retried.url.parameters.getAll("api_key"))
        val cookieHeaders = retried.headers.getAll(HttpHeaders.Cookie).orEmpty()
        assertEquals(1, cookieHeaders.size, cookieHeaders.toString())
        val cookieHeader = cookieHeaders.single()
        assertTrue("session=c2" in cookieHeader, cookieHeader)
        assertTrue("other=keep" in cookieHeader, cookieHeader)
        assertFalse("c1" in cookieHeader, cookieHeader)
        harness.client.close()
    }

    @Test
    fun retryWorksWithExpectSuccess() = runTest {
        var resolverCalls = 0
        val harness = retryHarness(
            resolver = { AuthItem.Bearer("token-${++resolverCalls}") },
            handler = { true },
            expectSuccess = true,
            responder = { respondUnauthorizedThenOk(it) }
        )

        val response = harness.client.get("https://example.com/path") { authKeys("k") }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(2, harness.engine.requestHistory.size)
        harness.client.close()
    }

    @Test
    fun exhaustedRetryStillThrowsWithExpectSuccess() = runTest {
        val harness = retryHarness(
            resolver = { AuthItem.Bearer("token") },
            handler = { true },
            expectSuccess = true,
            responder = { respond("nope", HttpStatusCode.Unauthorized) }
        )

        assertFailsWith<ClientRequestException> {
            harness.client.get("https://example.com/path") { authKeys("k") }
        }
        assertEquals(2, harness.engine.requestHistory.size)
        harness.client.close()
    }

    @Test
    fun retryResendsRequestBody() = runTest {
        var resolverCalls = 0
        val harness = retryHarness(
            resolver = { AuthItem.Bearer("token-${++resolverCalls}") },
            handler = { true },
            responder = { respondUnauthorizedThenOk(it) }
        )

        val response = harness.client.post("https://example.com/path") {
            authKeys("k")
            setBody("payload")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val bodies = harness.engine.requestHistory.map { it.body.toByteArray().decodeToString() }
        assertEquals(listOf("payload", "payload"), bodies)
        harness.client.close()
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
