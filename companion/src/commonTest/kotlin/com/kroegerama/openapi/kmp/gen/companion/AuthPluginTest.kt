package com.kroegerama.openapi.kmp.gen.companion

import com.kroegerama.openapi.kmp.gen.companion.AuthPlugin.Plugin.authKeys
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.plugins.cookies.get
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
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
        beforeAuthPlugin: HttpClientConfig<*>.() -> Unit = {},
        afterAuthPlugin: HttpClientConfig<*>.() -> Unit = {},
        responder: MockRequestHandleScope.(callIndex: Int) -> HttpResponseData
    ): RetryHarness {
        var calls = 0
        val engine = MockEngine { responder(calls++) }
        val client = HttpClient(engine) {
            this.expectSuccess = expectSuccess
            beforeAuthPlugin()
            install(AuthPlugin) {
                authItem(resolver)
                if (handler != null) {
                    onUnauthorized(handler)
                }
            }
            afterAuthPlugin()
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

    private enum class HttpCookiesInstall { None, BeforeAuthPlugin, AfterAuthPlugin }

    private fun cookieHarness(
        cookies: HttpCookiesInstall,
        resolver: AuthItemResolver,
        handler: UnauthorizedHandler? = null,
        responder: MockRequestHandleScope.(callIndex: Int) -> HttpResponseData = { respond("ok") }
    ): RetryHarness {
        val installCookies: HttpClientConfig<*>.() -> Unit = {
            install(HttpCookies) {
                default {
                    addCookie(Url("https://example.com/"), Cookie("stored", "yes", path = "/"))
                }
            }
        }
        return retryHarness(
            resolver = resolver,
            handler = handler,
            beforeAuthPlugin = { if (cookies == HttpCookiesInstall.BeforeAuthPlugin) installCookies() },
            afterAuthPlugin = { if (cookies == HttpCookiesInstall.AfterAuthPlugin) installCookies() },
            responder = responder
        )
    }

    private fun HttpRequestData.sortedCookies(): List<String> =
        headers.getAll(HttpHeaders.Cookie).orEmpty().flatMap { it.split("; ") }.sorted()

    private fun expectedCookies(cookies: HttpCookiesInstall, vararg sent: String): List<String> =
        (sent.toList() + if (cookies == HttpCookiesInstall.None) emptyList() else listOf("stored=yes")).sorted()

    @Test
    fun cookieAuthIsSentWithOtherCookiesAndStaysOutOfTheCookieStorage() = runTest {
        for (cookies in HttpCookiesInstall.entries) {
            val harness = cookieHarness(
                cookies = cookies,
                resolver = { AuthItem.ApiKey(AuthItem.Position.Cookie, "session", "abc") }
            )

            harness.client.get("https://example.com/path") {
                authKeys("k")
                cookie("other", "keep")
            }

            val request = harness.engine.requestHistory.single()
            assertEquals(expectedCookies(cookies, "other=keep", "session=abc"), request.sortedCookies(), cookies.name)
            assertNull(harness.client.cookies("https://example.com/path")["session"], cookies.name)
            harness.client.close()
        }
    }

    @Test
    fun cookieAuthIsNoLongerSentOnceTheKeyResolvesToNothing() = runTest {
        for (cookies in HttpCookiesInstall.entries) {
            var item: AuthItem? = AuthItem.ApiKey(AuthItem.Position.Cookie, "session", "abc")
            val harness = cookieHarness(cookies = cookies, resolver = { item })

            harness.client.get("https://example.com/path") { authKeys("k") }
            item = null
            harness.client.get("https://example.com/path") { authKeys("k") }

            val history = harness.engine.requestHistory
            assertEquals(expectedCookies(cookies, "session=abc"), history[0].sortedCookies(), cookies.name)
            assertEquals(expectedCookies(cookies), history[1].sortedCookies(), cookies.name)
            harness.client.close()
        }
    }

    @Test
    fun unauthorizedRetrySendsOnlyTheRefreshedCookie() = runTest {
        for (cookies in HttpCookiesInstall.entries) {
            var resolverCalls = 0
            val harness = cookieHarness(
                cookies = cookies,
                resolver = { AuthItem.ApiKey(AuthItem.Position.Cookie, "session", "c${++resolverCalls}") },
                handler = { true },
                responder = { respondUnauthorizedThenOk(it) }
            )

            val response = harness.client.get("https://example.com/path") {
                authKeys("k")
                cookie("other", "keep")
            }

            val history = harness.engine.requestHistory
            assertEquals(HttpStatusCode.OK, response.status, cookies.name)
            assertEquals(2, history.size, cookies.name)
            assertEquals(expectedCookies(cookies, "other=keep", "session=c1"), history[0].sortedCookies(), cookies.name)
            assertEquals(expectedCookies(cookies, "other=keep", "session=c2"), history[1].sortedCookies(), cookies.name)
            assertNull(harness.client.cookies("https://example.com/path")["session"], cookies.name)
            harness.client.close()
        }
    }

    private fun MockRequestHandleScope.respondRedirect(location: String): HttpResponseData =
        respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, location))

    // sends a request with a cookie auth key that is redirected to location, returns the original and the redirected request
    private suspend fun redirectedRequests(cookies: HttpCookiesInstall, location: String): List<HttpRequestData> {
        val harness = cookieHarness(
            cookies = cookies,
            resolver = { AuthItem.ApiKey(AuthItem.Position.Cookie, "session", "abc") },
            responder = { callIndex -> if (callIndex == 0) respondRedirect(location) else respond("ok") }
        )
        try {
            harness.client.get("https://example.com/path") { authKeys("k") }
        } finally {
            harness.client.close()
        }
        val history = harness.engine.requestHistory
        assertEquals(2, history.size, cookies.name)
        return history
    }

    @Test
    fun cookieAuthIsNotSentToAnotherHostOnRedirect() = runTest {
        for (cookies in HttpCookiesInstall.entries) {
            val (original, redirected) = redirectedRequests(cookies, location = "https://other.test/target")

            assertEquals("other.test", redirected.url.host, cookies.name)
            assertEquals(expectedCookies(cookies, "session=abc"), original.sortedCookies(), cookies.name)
            assertEquals(emptyList(), redirected.sortedCookies(), cookies.name)
        }
    }

    @Test
    fun cookieAuthIsNotSentToAnotherPortOnRedirect() = runTest {
        for (cookies in HttpCookiesInstall.entries) {
            val (original, redirected) = redirectedRequests(cookies, location = "https://example.com:8443/target")

            assertEquals(8443, redirected.url.port, cookies.name)
            assertEquals(expectedCookies(cookies, "session=abc"), original.sortedCookies(), cookies.name)
            assertFalse("session=abc" in redirected.sortedCookies(), cookies.name)
        }
    }

    @Test
    fun cookieAuthIsSentToTheSameOriginOnRedirect() = runTest {
        for (cookies in HttpCookiesInstall.entries) {
            val (original, redirected) = redirectedRequests(cookies, location = "https://example.com/target")

            assertEquals("/target", redirected.url.encodedPath, cookies.name)
            assertEquals(expectedCookies(cookies, "session=abc"), original.sortedCookies(), cookies.name)
            assertEquals(expectedCookies(cookies, "session=abc"), redirected.sortedCookies(), cookies.name)
        }
    }

    @Test
    fun redirectToTheExplicitDefaultPortCountsAsTheSameOrigin() = runTest {
        for (cookies in HttpCookiesInstall.entries) {
            val (original, redirected) = redirectedRequests(cookies, location = "https://example.com:443/target")

            assertEquals("/target", redirected.url.encodedPath, cookies.name)
            assertEquals(expectedCookies(cookies, "session=abc"), original.sortedCookies(), cookies.name)
            assertEquals(expectedCookies(cookies, "session=abc"), redirected.sortedCookies(), cookies.name)
        }
    }

    @Test
    fun storedCookieWithTheAuthCookieNameIsStillSentToItsOwnOrigin() = runTest {
        val engine = MockEngine { request ->
            if (request.url.host == "example.com") respondRedirect("https://other.test/target") else respond("ok")
        }
        val client = HttpClient(engine) {
            install(HttpCookies) {
                default {
                    addCookie(Url("https://other.test/"), Cookie("session", "theirs", path = "/"))
                }
            }
            install(AuthPlugin) {
                authItem { AuthItem.ApiKey(AuthItem.Position.Cookie, "session", "abc") }
            }
        }
        try {
            client.get("https://example.com/path") { authKeys("k") }
        } finally {
            client.close()
        }

        val (original, redirected) = engine.requestHistory
        assertEquals(listOf("session=abc"), original.sortedCookies())
        assertEquals(listOf("session=theirs"), redirected.sortedCookies())
    }

    private fun allPositionsResolver(suffix: () -> String = { "" }): AuthItemResolver = { key ->
        when (key) {
            "bearer" -> AuthItem.Bearer("t${suffix()}")
            "header" -> AuthItem.ApiKey(AuthItem.Position.Header, "X-API-Key", "h${suffix()}")
            "query" -> AuthItem.ApiKey(AuthItem.Position.Query, "api_key", "q${suffix()}")
            "cookie" -> AuthItem.ApiKey(AuthItem.Position.Cookie, "session", "c${suffix()}")
            else -> null
        }
    }

    private suspend fun HttpClient.getWithAllAuthPositions() = get("https://example.com/path") {
        authKeys("bearer", "header", "query", "cookie")
    }

    @Test
    fun authValuesAreNotSentToAnotherOriginOnRedirect() = runTest {
        val harness = retryHarness(
            resolver = allPositionsResolver(),
            responder = { callIndex -> if (callIndex == 0) respondRedirect("https://other.test/target") else respond("ok") }
        )

        val response = harness.client.getWithAllAuthPositions()

        assertEquals(HttpStatusCode.OK, response.status)
        val (original, redirected) = harness.engine.requestHistory
        assertEquals("Bearer t", original.headers[HttpHeaders.Authorization])
        assertEquals("h", original.headers["X-API-Key"])
        assertEquals("q", original.url.parameters["api_key"])
        assertEquals(listOf("session=c"), original.sortedCookies())
        assertEquals("other.test", redirected.url.host)
        assertNull(redirected.headers[HttpHeaders.Authorization])
        assertNull(redirected.headers["X-API-Key"])
        assertNull(redirected.url.parameters["api_key"])
        assertEquals(emptyList(), redirected.sortedCookies())
        harness.client.close()
    }

    @Test
    fun authValuesAreSentAgainWhenARedirectChainReturnsToTheOrigin() = runTest {
        val harness = retryHarness(
            resolver = allPositionsResolver(),
            responder = { callIndex ->
                when (callIndex) {
                    0 -> respondRedirect("https://other.test/hop")
                    1 -> respondRedirect("https://example.com/back")
                    else -> respond("ok")
                }
            }
        )

        val response = harness.client.getWithAllAuthPositions()

        assertEquals(HttpStatusCode.OK, response.status)
        val history = harness.engine.requestHistory
        assertEquals(listOf("example.com", "other.test", "example.com"), history.map { it.url.host })
        val foreign = history[1]
        assertNull(foreign.headers[HttpHeaders.Authorization])
        assertNull(foreign.headers["X-API-Key"])
        assertNull(foreign.url.parameters["api_key"])
        assertEquals(emptyList(), foreign.sortedCookies())
        val returned = history[2]
        assertEquals("/back", returned.url.encodedPath)
        assertEquals(listOf("Bearer t"), returned.headers.getAll(HttpHeaders.Authorization))
        assertEquals(listOf("h"), returned.headers.getAll("X-API-Key"))
        assertEquals(listOf("q"), returned.url.parameters.getAll("api_key"))
        assertEquals(listOf("session=c"), returned.sortedCookies())
        harness.client.close()
    }

    @Test
    fun unauthorizedFromAnotherOriginIsReturnedWithoutRetryOrHandlerCall() = runTest {
        var handlerCalls = 0
        val harness = retryHarness(
            resolver = allPositionsResolver(),
            handler = {
                handlerCalls++
                true
            },
            responder = { callIndex ->
                if (callIndex == 0) respondRedirect("https://other.test/target") else respond("nope", HttpStatusCode.Unauthorized)
            }
        )

        val response = harness.client.getWithAllAuthPositions()

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(listOf("example.com", "other.test"), harness.engine.requestHistory.map { it.url.host })
        assertEquals(0, handlerCalls)
        harness.client.close()
    }

    @Test
    fun authValuesAreSentToTheSameOriginOnRedirect() = runTest {
        val harness = retryHarness(
            resolver = allPositionsResolver(),
            responder = { callIndex -> if (callIndex == 0) respondRedirect("https://example.com/target?page=2") else respond("ok") }
        )

        val response = harness.client.getWithAllAuthPositions()

        assertEquals(HttpStatusCode.OK, response.status)
        val (original, redirected) = harness.engine.requestHistory
        assertEquals(listOf("q"), original.url.parameters.getAll("api_key"))
        assertEquals("/target", redirected.url.encodedPath)
        assertEquals("2", redirected.url.parameters["page"])
        assertEquals(listOf("q"), redirected.url.parameters.getAll("api_key"))
        assertEquals(listOf("h"), redirected.headers.getAll("X-API-Key"))
        assertEquals(listOf("Bearer t"), redirected.headers.getAll(HttpHeaders.Authorization))
        assertEquals(listOf("session=c"), redirected.sortedCookies())
        harness.client.close()
    }

    @Test
    fun unauthorizedAfterASameOriginRedirectIsRetriedWithFreshValues() = runTest {
        var round = 1
        val harness = retryHarness(
            resolver = allPositionsResolver { round.toString() },
            handler = {
                round++
                true
            },
            responder = { callIndex ->
                when (callIndex) {
                    0 -> respondRedirect("https://example.com/target")
                    1 -> respond("nope", HttpStatusCode.Unauthorized)
                    else -> respond("ok")
                }
            }
        )

        val response = harness.client.getWithAllAuthPositions()

        assertEquals(HttpStatusCode.OK, response.status)
        val history = harness.engine.requestHistory
        assertEquals(listOf("/path", "/target", "/target"), history.map { it.url.encodedPath })
        assertEquals(listOf("q1"), history[1].url.parameters.getAll("api_key"))
        val retried = history[2]
        assertEquals(listOf("q2"), retried.url.parameters.getAll("api_key"))
        assertEquals(listOf("h2"), retried.headers.getAll("X-API-Key"))
        assertEquals(listOf("Bearer t2"), retried.headers.getAll(HttpHeaders.Authorization))
        assertEquals(listOf("session=c2"), retried.sortedCookies())
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
