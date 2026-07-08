package com.kroegerama.openapi.kmp.gen.companion

import arrow.core.left
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class CallResponseTest {

    @Serializable
    private data class Dto(val value: Int)

    @Serializable
    private data class ApiError(val code: String)

    private suspend fun mockResponse(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = ""
    ): HttpResponse {
        val client = HttpClient(MockEngine) {
            expectSuccess = false
            engine {
                addHandler {
                    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
        }
        return client.get("https://example.com/")
    }

    private fun nonHttpExceptions(): List<CallException> = listOf(
        IOCallException(null, IOException("io")),
        CallSerializationException(null, ContentConvertException("convert")),
        UnexpectedCallException(null, null)
    )

    @Test
    fun httpCallResponseExposesRawProperties() = runTest {
        val raw = mockResponse(HttpStatusCode.NotFound)
        val response = HttpCallResponse(data = "payload", raw = raw)
        assertEquals(404, response.code)
        assertEquals("Not Found", response.message)
        assertEquals("application/json", response.headers[HttpHeaders.ContentType])
        assertFalse(response.isSuccessful)
    }

    @Test
    fun mapTransformsDataAndKeepsRaw() = runTest {
        val raw = mockResponse()
        val mapped = HttpCallResponse(data = 41, raw = raw).map { it + 1 }
        assertEquals(42, mapped.data)
        assertSame(raw, mapped.raw)
    }

    @Test
    fun httpResponseAsCallException() = runTest {
        val raw = mockResponse(HttpStatusCode.BadGateway)
        val exception = raw.asCallException()
        assertTrue(exception is HttpCallException, exception.toString())
        assertEquals(502, exception.code)
        assertNull(exception.cause)
        assertSame(raw, exception.raw)
    }

    @Test
    fun onResponseInvokedForHttpExceptions() = runTest {
        val raw = mockResponse(HttpStatusCode.NotFound)
        var calls = 0
        HttpCallException(raw = raw, cause = null).onResponse {
            calls++
            assertSame(raw, it)
        }
        TypedHttpCallException(error = "err", raw = raw, cause = null).onResponse {
            calls++
            assertSame(raw, it)
        }
        assertEquals(2, calls)
    }

    @Test
    fun onResponseSkippedForNonHttpExceptions() {
        nonHttpExceptions().forEach { exception ->
            exception.onResponse { fail("onResponse must not be invoked for $exception") }
        }
    }

    @Test
    fun onCodeInvokedForHttpExceptions() = runTest {
        val raw = mockResponse(HttpStatusCode.NotFound)
        var calls = 0
        HttpCallException(raw = raw, cause = null).onCode {
            calls++
            assertEquals(404, it)
        }
        TypedHttpCallException(error = "err", raw = raw, cause = null).onCode {
            calls++
            assertEquals(404, it)
        }
        assertEquals(2, calls)
    }

    @Test
    fun onCodeSkippedForNonHttpExceptions() {
        nonHttpExceptions().forEach { exception ->
            exception.onCode { fail("onCode must not be invoked for $exception") }
        }
    }

    @Test
    fun typedPassesThroughNonHttpExceptions() = runTest {
        nonHttpExceptions().forEach { exception ->
            val either: EitherCallResponse<Dto> = exception.left()
            assertSame(exception, either.typed<Dto, ApiError>().leftOrNull())
        }
    }
}
