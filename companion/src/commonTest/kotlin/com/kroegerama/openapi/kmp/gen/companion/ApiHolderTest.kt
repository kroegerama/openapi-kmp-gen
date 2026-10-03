package com.kroegerama.openapi.kmp.gen.companion

import com.kroegerama.openapi.kmp.gen.companion.AuthPlugin.Plugin.authKeys
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ApiHolderTest {

    @Serializable
    private data class Dto(val value: Int)

    private val requests = mutableListOf<HttpRequestData>()

    private val recordingHandler: MockRequestHandler = { request ->
        requests += request
        respond("")
    }

    private fun TestApiHolder.updateMockClient(
        json: Json = ApiJson,
        userAgent: String? = null,
        handler: MockRequestHandler = recordingHandler,
        block: HttpClientConfig<MockEngineConfig>.() -> Unit = {}
    ) {
        updateClient(MockEngine, json, userAgent) {
            engine {
                addHandler(handler)
            }
            block()
        }
    }

    private fun holder(
        json: Json = ApiJson,
        userAgent: String? = null,
        handler: MockRequestHandler = recordingHandler,
        block: HttpClientConfig<MockEngineConfig>.() -> Unit = {}
    ): TestApiHolder = TestApiHolder().apply {
        updateMockClient(json, userAgent, handler, block)
    }

    @Test
    fun jsonReturnsConfiguredInstance() {
        val json = Json { prettyPrint = true }
        val holder = holder(json = json)
        try {
            assertSame(json, holder.json)
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun jsonIsApiJsonBeforeFirstUpdate() {
        assertSame(ApiJson, TestApiHolder().json)
    }

    @Test
    fun clientIsCreatedOnFirstAccess() {
        val holder = TestApiHolder()
        val client = holder.client
        try {
            assertSame(client, holder.client)
            assertSame(ApiJson, holder.json)
        } finally {
            client.close()
        }
    }

    @Test
    fun clientIsStableAcrossReads() {
        val holder = holder()
        try {
            assertSame(holder.client, holder.client)
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun updateClientClosesPreviousClient() {
        // the replaced client must be closed so its resources are released
        val holder = holder()
        try {
            val first = holder.client
            holder.updateMockClient()
            assertFalse(first.isActive, "previous client should be closed")
            assertNotSame(first, holder.client)
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun parallelFirstAccessYieldsOneClient() = runTest {
        val holder = TestApiHolder()
        val clients = withContext(Dispatchers.Default) {
            List(16) { async { holder.client } }.awaitAll()
        }
        try {
            assertEquals(1, clients.toSet().size)
        } finally {
            clients.forEach { it.close() }
        }
    }

    // suspends a MockEngine handler until the test opens the gate
    private class SuspendedCall {
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()

        suspend fun awaitGate() {
            started.complete(Unit)
            gate.await()
        }
    }

    @Test
    fun updateClientClosesThePreviousClientWhileACallIsInFlight() = runTest {
        val suspended = SuspendedCall()
        val holder = holder(
            handler = {
                suspended.awaitGate()
                respond("old")
            }
        )
        val old = holder.client
        try {
            val inFlight = async { old.eitherRequest<String> { url("slow") } }
            suspended.started.await()

            holder.updateMockClient()

            assertFalse(suspended.gate.isCompleted, "the handler should still be suspended")
            assertFalse(old.isActive, "the previous client should be closed although a call is running on it")
            assertNotSame(old, holder.client)

            suspended.gate.complete(Unit)
            val result = inFlight.await()
            assertEquals("old", result.getOrNull()?.data, result.toString())

            val left = old.eitherRequest<String> { url("x") }.leftOrNull()
            assertTrue(left is UnexpectedCallException, left.toString())
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun replacedClientWithARequestTimeoutRejectsNewRequestsWhileACallIsInFlight() = runTest {
        val suspended = SuspendedCall()
        var providerCalls = 0
        val holder = holder(
            handler = { request ->
                requests += request
                suspended.awaitGate()
                respond("old")
            }
        ) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
            }
        }
        holder.register("k") {
            providerCalls++
            null
        }
        val old = holder.client
        try {
            val inFlight = async { old.eitherRequest<String> { url("slow") } }
            suspended.started.await()

            holder.updateMockClient()

            val left = old.eitherRequest<String> {
                url("x")
                authKeys("k")
            }.leftOrNull()
            assertTrue(left is UnexpectedCallException, left.toString())
            assertEquals(listOf("/v1/slow"), requests.map { it.url.encodedPath })
            assertEquals(0, providerCalls)

            suspended.gate.complete(Unit)
            val result = inFlight.await()
            assertEquals("old", result.getOrNull()?.data, result.toString())
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun updateClientReplacesJson() {
        val holder = holder()
        try {
            val newJson = Json { prettyPrint = true }
            holder.updateMockClient(json = newJson)
            assertSame(newJson, holder.json)
        } finally {
            holder.client.close()
        }
    }

    // mirrors generated service functions: holder.client for the call, holder.json for the helpers and the explicit deserializer
    private suspend fun requestPersonInBothVariants(holder: TestApiHolder, person: Person) = Pair(
        holder.client.eitherRequest<Person> {
            url.appendPathSegments("people", createSerializedPathSegment(value = person, explode = true, json = holder.json))
            appendSerializedQueryParameter(name = "person", value = person, json = holder.json)
        },
        holder.client.eitherRequest(deserializer = Person.serializer(), json = holder.json) {
            appendSerializedQueryParameter(name = "person", value = person, serializer = Person.serializer(), json = holder.json)
            contentType(ContentType.Application.Json)
            setSerializedBody(value = person, serializer = Person.serializer(), json = holder.json)
        }
    )

    // requests a person in both variants and expects key as the property name in the responses, path, query and body
    private suspend fun assertPersonIsSerializedWithKey(holder: TestApiHolder, key: String) {
        val person = Person("Ada")
        val (reified, explicit) = requestPersonInBothVariants(holder, person)

        assertEquals(person, reified.getOrNull()?.data, reified.toString())
        assertEquals(person, explicit.getOrNull()?.data, explicit.toString())
        assertEquals("/v1/people/$key=Ada", requests[0].url.encodedPath)
        assertEquals("Ada", requests[0].url.parameters[key])
        assertEquals("Ada", requests[1].url.parameters[key])
        assertEquals("""{"$key":"Ada"}""", (requests[1].body as TextContent).text)
    }

    @Test
    fun jsonMatchesTheContentNegotiationOfTheCurrentClient() = runTest {
        val holder = holder(json = snakeCaseJson, handler = recordingJsonHandler(requests, """{"first_name":"Ada"}"""))
        try {
            assertPersonIsSerializedWithKey(holder, "first_name")
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun jsonMatchesTheContentNegotiationAfterUpdateClient() = runTest {
        val holder = holder(json = snakeCaseJson, handler = recordingJsonHandler(requests, """{"first_name":"Ada"}"""))
        try {
            holder.updateMockClient(json = kebabCaseJson, handler = recordingJsonHandler(requests, """{"first-name":"Ada"}"""))
            assertPersonIsSerializedWithKey(holder, "first-name")
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun eitherRequestReportsABodyTheCurrentJsonCannotDecode() = runTest {
        val holder = holder(json = kebabCaseJson, handler = recordingJsonHandler(requests, """{"first_name":"Ada"}"""))
        try {
            val reified = holder.client.eitherRequest<Person> { url("x") }
            val explicit = holder.client.eitherRequest(deserializer = Person.serializer(), json = holder.json) { url("x") }

            assertTrue(reified.leftOrNull() is SerializationCallException, reified.toString())
            assertTrue(explicit.leftOrNull() is SerializationCallException, explicit.toString())
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun engineInstanceOverloadUsesTheGivenEngine() = runTest {
        val engine = MockEngine(recordingHandler)
        val json = Json { prettyPrint = true }
        val holder = TestApiHolder().apply {
            updateClient(engine, json, userAgent = "test-agent/1.0") {
                defaultRequest {
                    header(HttpHeaders.AcceptLanguage, "de")
                }
            }
        }
        try {
            holder.client.get("photos/42")

            assertSame(engine, holder.client.engine)
            assertSame(json, holder.json)
            val request = requests.single()
            assertEquals("api.example.com", request.url.host)
            assertEquals("/v1/photos/42", request.url.encodedPath)
            assertEquals("test-agent/1.0", request.headers[HttpHeaders.UserAgent])
            assertEquals("de", request.headers[HttpHeaders.AcceptLanguage])
        } finally {
            holder.client.close()
            engine.close()
        }
    }

    @Test
    fun replacingTheClientLeavesTheEngineInstanceUsable() = runTest {
        val engine = MockEngine(recordingHandler)
        val holder = TestApiHolder().apply { updateClient(engine) }
        try {
            val first = holder.client

            holder.updateClient(engine)

            assertFalse(first.isActive, "previous client should be closed")
            assertTrue(engine.isActive, "the engine instance should stay open")
            holder.client.get("x")
            assertEquals(1, requests.size)
        } finally {
            holder.client.close()
            engine.close()
        }
    }

    private suspend fun assertRequestIsRejectedBeforeTheEngine(replaced: HttpClient) {
        val left = replaced.eitherRequest<String> { url("x") }.leftOrNull()

        assertTrue(left is UnexpectedCallException, left.toString())
        assertFalse(left.cause is CancellationException, left.cause.toString())
        assertTrue(requests.isEmpty(), "the request should not reach the engine")
    }

    @Test
    fun requestStartedOnAReplacedClientReturnsUnexpectedCallException() = runTest {
        val holder = holder()
        try {
            val old = holder.client
            holder.updateMockClient()

            assertRequestIsRejectedBeforeTheEngine(old)
            assertTrue(holder.client.eitherRequest<String> { url("x") }.isRight())
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun requestStartedOnAReplacedClientReturnsUnexpectedCallExceptionOnASharedEngine() = runTest {
        val engine = MockEngine(recordingHandler)
        val holder = TestApiHolder().apply { updateClient(engine) }
        try {
            val old = holder.client
            holder.updateClient(engine)

            assertRequestIsRejectedBeforeTheEngine(old)
        } finally {
            holder.client.close()
            engine.close()
        }
    }

    @Test
    fun baseUrlIsAppliedToRequests() = runTest {
        // the holder's DefaultRequest applies baseUrl to relative request paths
        val holder = holder()
        try {
            holder.client.get("photos/42")
            val request = requests.last()
            assertEquals("api.example.com", request.url.host)
            assertEquals("/v1/photos/42", request.url.encodedPath)
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun baseUrlIsReadPerRequest() = runTest {
        val holder = holder()
        try {
            holder.client.get("x")
            holder.baseUrl = Url("https://other.example.com/v2/")
            holder.client.get("x")
            assertEquals("api.example.com", requests[0].url.host)
            assertEquals("other.example.com", requests[1].url.host)
            assertEquals("/v2/x", requests[1].url.encodedPath)
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun registeredAuthProviderIsResolvedPerRequest() = runTest {
        // providers registered after client creation are read live via the atomic map
        val holder = holder()
        try {
            holder.register("bearer") { AuthItem.Bearer("tok") }

            holder.client.get("x") { authKeys("bearer") }
            assertEquals("Bearer tok", requests.last().headers[HttpHeaders.Authorization])
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun clearedAuthProviderIsRemoved() = runTest {
        val holder = holder()
        try {
            holder.register("bearer") { AuthItem.Bearer("tok") }
            holder.unregister("bearer")

            holder.client.get("x") { authKeys("bearer") }
            assertNull(requests.last().headers[HttpHeaders.Authorization])
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun blockIsAppliedOnTopOfApiPlugins() = runTest {
        val holder = holder {
            defaultRequest {
                header("X-Decorated", "yes")
            }
        }
        try {
            holder.client.get("x")
            val request = requests.last()
            assertEquals("yes", request.headers["X-Decorated"])
            // the block's DefaultRequest must merge with the holder's, not replace it
            assertEquals("api.example.com", request.url.host)
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun blockCanInstallCookies() = runTest {
        val holder = holder(
            handler = { request ->
                requests += request
                respond("", headers = headersOf(HttpHeaders.SetCookie, "session=abc"))
            }
        ) {
            install(HttpCookies)
        }
        try {
            holder.client.get("x")
            holder.client.get("x")
            assertNull(requests[0].headers[HttpHeaders.Cookie])
            assertEquals("session=abc", requests[1].headers[HttpHeaders.Cookie])
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun userAgentIsApplied() = runTest {
        val holder = holder(userAgent = "test-agent/1.0")
        try {
            holder.client.get("x")
            assertEquals("test-agent/1.0", requests.last().headers[HttpHeaders.UserAgent])
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun nullUserAgentSkipsTheUserAgentPlugin() {
        val holder = holder(userAgent = null)
        try {
            assertNull(holder.client.pluginOrNull(UserAgent))
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun errorStatusThrowsByDefault() = runTest {
        // createApiHttpClient sets expectSuccess = true
        val holder = holder(handler = { respond("nope", HttpStatusCode.NotFound) })
        try {
            assertFailsWith<ClientRequestException> {
                holder.client.get("x")
            }
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun unauthorizedHandlerRetriesThroughFullStack() = runTest {
        // Uses the full production stack (createApiHttpClient sets expectSuccess = true), so
        // this also proves the retry happens before Ktor's response validation throws.
        var calls = 0
        val holder = holder(
            handler = { request ->
                requests += request
                calls++
                if (calls == 1) respond("nope", HttpStatusCode.Unauthorized) else respond("")
            }
        )
        try {
            var token = "t1"
            val handledItems = mutableListOf<Map<String, AuthItem>>()
            holder.register("bearer") { AuthItem.Bearer(token) }
            holder.setUnauthorizedHandler { appliedItems ->
                handledItems += appliedItems
                token = "t2"
                true
            }

            val response = holder.client.get("x") { authKeys("bearer") }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf(mapOf<String, AuthItem>("bearer" to AuthItem.Bearer("t1"))), handledItems)
            assertEquals("Bearer t1", requests[requests.size - 2].headers[HttpHeaders.Authorization])
            assertEquals("Bearer t2", requests.last().headers[HttpHeaders.Authorization])
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun clearedUnauthorizedHandlerStopsRetrying() = runTest {
        var calls = 0
        val holder = holder(
            handler = {
                calls++
                respond("nope", HttpStatusCode.Unauthorized)
            }
        )
        try {
            holder.register("bearer") { AuthItem.Bearer("tok") }
            holder.setUnauthorizedHandler { true }
            holder.setUnauthorizedHandler(null)

            assertFailsWith<ClientRequestException> {
                holder.client.get("x") { authKeys("bearer") }
            }
            assertEquals(1, calls)
        } finally {
            holder.client.close()
        }
    }

    @Test
    fun contentNegotiationUsesHolderJson() = runTest {
        val handler: MockRequestHandler = {
            respondJson("""{"value":1,"extra":true}""")
        }

        // ApiJson sets ignoreUnknownKeys = true, so the extra field must not fail decoding
        val lenient = holder(handler = handler)
        try {
            assertEquals(Dto(1), lenient.client.get("x").body())
        } finally {
            lenient.client.close()
        }

        // a strict Json must reject the extra field - proving ContentNegotiation is wired to the
        // holder's json instance rather than a default of its own
        val strict = holder(json = Json { ignoreUnknownKeys = false }, handler = handler)
        try {
            assertFailsWith<ContentConvertException> {
                strict.client.get("x").body<Dto>()
            }
        } finally {
            strict.client.close()
        }
    }
}
