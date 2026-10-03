package com.kroegerama.openapi.kmp.gen.companion

import arrow.core.raise.either
import com.kroegerama.openapi.kmp.gen.companion.AuthPlugin.Plugin.authKeys
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class RequestTest {

    @Serializable
    private data class Dto(val value: Int)

    @Serializable
    private data class ApiError(val code: String)

    private fun jsonClient(
        expectSuccess: Boolean = true,
        handler: MockRequestHandler
    ): HttpClient = HttpClient(MockEngine) {
        this.expectSuccess = expectSuccess
        install(ContentNegotiation) {
            json()
        }
        engine {
            addHandler(handler)
        }
    }

    @Test
    fun eitherRequestSuccess() = runTest {
        val client = jsonClient {
            respondJson("""{"value":42}""")
        }
        try {
            val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
            assertTrue(result.isRight(), result.toString())
            val response = result.getOrNull()!!
            assertEquals(Dto(42), response.data)
            assertEquals(200, response.code)
            assertTrue(response.isSuccessful)
        } finally {
            client.close()
        }
    }

    private suspend fun assertNotFoundMapsToHttpCallException(expectSuccess: Boolean) {
        val client = jsonClient(expectSuccess) {
            respondJson("""{"code":"NOT_FOUND"}""", HttpStatusCode.NotFound)
        }
        try {
            val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
            val left = result.leftOrNull()
            assertTrue(left is HttpCallException, left.toString())
            assertEquals(404, left.code)
        } finally {
            client.close()
        }
    }

    @Test
    fun eitherRequestErrorStatusExpectSuccess() = runTest {
        // expectSuccess = true -> Ktor throws a ResponseException, mapped via Throwable.asCallException
        assertNotFoundMapsToHttpCallException(expectSuccess = true)
    }

    @Test
    fun eitherRequestErrorStatusManualBranch() = runTest {
        // expectSuccess = false -> the manual status guard in eitherRequest raises instead
        assertNotFoundMapsToHttpCallException(expectSuccess = false)
    }

    @Test
    fun eitherRequestSerializationError() = runTest {
        // body is valid JSON but misses the required `value` field -> ContentConvertException
        val client = jsonClient {
            respondJson("""{"wrong":true}""")
        }
        try {
            val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
            assertTrue(result.leftOrNull() is SerializationCallException, result.toString())
        } finally {
            client.close()
        }
    }

    @Test
    fun eitherRequestIOError() = runTest {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json() }
            engine {
                addHandler { throw IOException("connection reset") }
            }
        }
        try {
            val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
            assertTrue(result.leftOrNull() is IOCallException, result.toString())
        } finally {
            client.close()
        }
    }

    @Test
    fun eitherRequestOnClosedClientReturnsUnexpected() = runTest {
        val client = jsonClient {
            respondJson("""{"value":42}""")
        }
        client.close()
        val left = client.eitherRequest<Dto> { url("https://example.com/dto") }.leftOrNull()
        assertTrue(left is UnexpectedCallException, left.toString())
        assertTrue(left.cause is CancellationException, left.cause.toString())
    }

    @Test
    fun eitherRequestRethrowsCallerCancellation() = runTest {
        val requestReceived = CompletableDeferred<Unit>()
        val client = jsonClient {
            requestReceived.complete(Unit)
            awaitCancellation()
        }
        try {
            var outcome: Result<Any?>? = null
            val job = launch {
                outcome = runCatching { client.eitherRequest<Dto> { url("https://example.com/dto") } }
            }
            requestReceived.await()
            job.cancelAndJoin()

            assertTrue(outcome?.exceptionOrNull() is CancellationException, outcome.toString())
        } finally {
            client.close()
        }
    }

    @Test
    fun eitherRequestRethrowsARaiseFromTheBlock() = runTest {
        val open = jsonClient { respondJson("""{"value":42}""") }
        val closed = jsonClient { respondJson("""{"value":42}""") }
        closed.close()

        try {
            for (client in listOf(open, closed)) {
                val result = either {
                    client.eitherRequest<Dto> {
                        url("https://example.com/dto")
                        raise("stop")
                    }
                }
                assertEquals("stop", result.leftOrNull(), result.toString())
            }
        } finally {
            open.close()
        }
    }

    @Test
    fun eitherRequestRethrowsATimeoutFromAnAuthProvider() = runTest {
        val client = HttpClient(MockEngine) {
            install(AuthPlugin) {
                authItem {
                    withTimeout(100.milliseconds) {
                        awaitCancellation()
                    }
                }
            }
            engine {
                addHandler { respond("") }
            }
        }

        try {
            assertFailsWith<TimeoutCancellationException> {
                client.eitherRequest<String> {
                    url("https://example.com/")
                    authKeys("k")
                }
            }
        } finally {
            client.close()
        }
    }

    private fun authClient(resolver: AuthItemResolver): HttpClient = HttpClient(MockEngine) {
        install(AuthPlugin) {
            authItem(resolver)
        }
        engine {
            addHandler { respond("") }
        }
    }

    // closes the client while its auth provider is suspended, the provider then ends with finish
    private fun TestScope.authClientClosedDuringAuth(finish: suspend () -> AuthItem?): HttpClient {
        val suspended = CompletableDeferred<Unit>()
        val closed = CompletableDeferred<Unit>()
        val client = authClient {
            suspended.complete(Unit)
            closed.await()
            finish()
        }
        launch {
            suspended.await()
            client.close()
            closed.complete(Unit)
        }
        return client
    }

    private suspend fun HttpClient.eitherAuthRequest() = eitherRequest<String> {
        url("https://example.com/")
        authKeys("k")
    }

    @Test
    fun eitherRequestRethrowsARaiseFromAnAuthProvider() = runTest {
        for (closeFirst in listOf(false, true)) {
            val result = either {
                val client = authClient { raise("stop") }
                if (closeFirst) client.close()
                try {
                    client.eitherAuthRequest()
                } finally {
                    client.close()
                }
            }
            assertEquals("stop", result.leftOrNull(), "closeFirst=$closeFirst: $result")
        }
    }

    @Test
    fun eitherRequestRethrowsARaiseFromAnAuthProviderOnAClientClosedDuringAuth() = runTest {
        val result = either {
            val client = authClientClosedDuringAuth { raise("stop") }
            try {
                client.eitherAuthRequest()
            } finally {
                client.close()
            }
        }
        assertEquals("stop", result.leftOrNull(), result.toString())
    }

    @Test
    fun eitherRequestRethrowsATimeoutFromAnAuthProviderOnAClosedClient() = runTest {
        val client = authClient {
            withTimeout(100.milliseconds) {
                awaitCancellation()
            }
        }
        client.close()

        assertFailsWith<TimeoutCancellationException> {
            client.eitherAuthRequest()
        }
    }

    @Test
    fun eitherRequestOnAClientClosedDuringAuthReturnsUnexpected() = runTest {
        val client = authClientClosedDuringAuth { null }
        try {
            val left = client.eitherAuthRequest().leftOrNull()
            assertTrue(left is UnexpectedCallException, left.toString())
            assertTrue(left.cause is CancellationException, left.cause.toString())
        } finally {
            client.close()
        }
    }

    // fails a sibling coroutine once the request is suspended and expects the request to end without a result
    private suspend fun assertSiblingFailureCancelsTheRequest(client: HttpClient, requestSuspended: CompletableDeferred<Unit>) {
        var result: Any? = null
        try {
            val failure = assertFailsWith<IllegalStateException> {
                coroutineScope {
                    launch {
                        result = client.eitherAuthRequest()
                    }
                    launch {
                        requestSuspended.await()
                        throw IllegalStateException("sibling failed")
                    }
                }
            }
            assertEquals("sibling failed", failure.message)
            assertNull(result, "the cancelled request should not return a result")
        } finally {
            client.close()
        }
    }

    @Test
    fun eitherRequestReturnsNoLeftWhenASiblingFailsDuringTheEngineCall() = runTest {
        val requestSuspended = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine) {
            engine {
                addHandler {
                    requestSuspended.complete(Unit)
                    awaitCancellation()
                }
            }
        }

        assertSiblingFailureCancelsTheRequest(client, requestSuspended)
    }

    @Test
    fun eitherRequestReturnsNoLeftWhenASiblingFailsDuringAuth() = runTest {
        val requestSuspended = CompletableDeferred<Unit>()
        val client = authClient {
            requestSuspended.complete(Unit)
            awaitCancellation()
        }

        assertSiblingFailureCancelsTheRequest(client, requestSuspended)
    }

    private suspend fun assertTypedDecodesNotFoundBody(expectSuccess: Boolean) {
        val client = jsonClient(expectSuccess) {
            respondJson("""{"code":"NOT_FOUND"}""", HttpStatusCode.NotFound)
        }
        try {
            val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
                .typed<Dto, ApiError>()
            val left = result.leftOrNull()
            assertTrue(left is TypedHttpCallException<*>, left.toString())
            assertEquals(ApiError("NOT_FOUND"), left.error)
            assertEquals(404, left.code)
        } finally {
            client.close()
        }
    }

    @Test
    fun typedDecodesErrorBody() = runTest {
        // With expectSuccess = true the validator save()s the response, so the error body is re-readable
        assertTypedDecodesNotFoundBody(expectSuccess = true)
    }

    @Test
    fun typedDecodesErrorBodyManualBranch() = runTest {
        // expectSuccess = false -> the manual guard in eitherRequest save()s the response,
        // so typed() must still be able to read the error body afterwards
        assertTypedDecodesNotFoundBody(expectSuccess = false)
    }

    @Test
    fun typedFallsBackWhenErrorBodyUndecodable() = runTest {
        // the error body does not match ApiError -> typed() keeps the original HttpCallException
        val client = jsonClient {
            respondJson("""{"unexpected":1}""", HttpStatusCode.NotFound)
        }
        try {
            val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
                .typed<Dto, ApiError>()
            val left = result.leftOrNull()
            assertTrue(left is HttpCallException, left.toString())
            assertEquals(404, left.code)
        } finally {
            client.close()
        }
    }

    @Test
    fun typedKeepsSuccess() = runTest {
        val client = jsonClient {
            respondJson("""{"value":42}""")
        }
        try {
            val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
                .typed<Dto, ApiError>()
            assertTrue(result.isRight(), result.toString())
            assertEquals(Dto(42), result.getOrNull()!!.data)
        } finally {
            client.close()
        }
    }

    private val person = Person("Ada")
    private val requests = mutableListOf<HttpRequestData>()

    // sends one request built by block on a snake case client and expects the kebab case names in path, query, header and cookie
    private suspend fun assertParametersUseKebabCase(block: HttpRequestBuilder.() -> Unit): HttpRequestData {
        val client = mockApiClient(snakeCaseJson, requests)
        try {
            val result = client.eitherRequest<String> {
                url("https://api.example.com/")
                block()
            }

            assertTrue(result.isRight(), result.toString())
            val request = requests.single()
            assertEquals(listOf("first-name=Ada", "first-name,Ada"), request.url.segments)
            assertEquals("Ada", request.url.parameters["first-name"])
            assertEquals("first-name=Ada", request.headers["X-Person"])
            assertEquals("first-name=Ada", request.headers[HttpHeaders.Cookie])
            return request
        } finally {
            client.close()
        }
    }

    @Test
    fun helpersEncodeWithTheJsonPassedIn() = runTest {
        // the client negotiates snake case, so the kebab case names come from the Json passed to the helpers
        assertParametersUseKebabCase {
            url.appendPathSegments(createSerializedPathSegment(value = person, explode = true, json = kebabCaseJson))
            appendSerializedPathSegment(value = person, json = kebabCaseJson)
            appendSerializedQueryParameter(name = "person", value = person, json = kebabCaseJson)
            appendSerializedHeaderParameter(name = "X-Person", value = person, explode = true, json = kebabCaseJson)
            appendSerializedCookieParameter(name = "person", value = person, json = kebabCaseJson)
        }
    }

    @Test
    fun explicitSerializerHelpersEncodeWithTheJsonPassedIn() = runTest {
        val serializer = Person.serializer()
        val request = assertParametersUseKebabCase {
            url.appendPathSegments(createSerializedPathSegment(value = person, serializer = serializer, explode = true, json = kebabCaseJson))
            appendSerializedPathSegment(value = person, serializer = serializer, json = kebabCaseJson)
            appendSerializedQueryParameter(name = "person", value = person, serializer = serializer, json = kebabCaseJson)
            appendSerializedHeaderParameter(name = "X-Person", value = person, serializer = serializer, explode = true, json = kebabCaseJson)
            appendSerializedCookieParameter(name = "person", value = person, serializer = serializer, json = kebabCaseJson)
            contentType(ContentType.Application.Json)
            setSerializedBody(value = person, serializer = serializer, json = kebabCaseJson)
        }

        assertEquals("""{"first-name":"Ada"}""", (request.body as TextContent).text)
    }

    @Test
    fun asCallExceptionMapsIOException() {
        val mapped: CallException = IOException("boom").asCallException()
        assertTrue(mapped is IOCallException)
    }

    @Test
    fun asCallExceptionMapsUnexpected() {
        val mapped: CallException = IllegalStateException("boom").asCallException()
        assertTrue(mapped is UnexpectedCallException)
    }
}
