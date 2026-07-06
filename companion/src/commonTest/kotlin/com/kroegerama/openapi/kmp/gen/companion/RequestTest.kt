package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestTest {

    @Serializable
    private data class Dto(val value: Int)

    @Serializable
    private data class ApiError(val code: String)

    private val jsonContentType = headersOf(HttpHeaders.ContentType, "application/json")

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
            respond("""{"value":42}""", HttpStatusCode.OK, jsonContentType)
        }
        val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
        assertTrue(result.isRight(), result.toString())
        val response = result.getOrNull()!!
        assertEquals(Dto(42), response.data)
        assertEquals(200, response.code)
        assertTrue(response.isSuccessful)
    }

    @Test
    fun eitherRequestErrorStatusExpectSuccess() = runTest {
        // expectSuccess = true -> Ktor throws a ResponseException, mapped via Throwable.asCallException
        val client = jsonClient {
            respond("""{"code":"NOT_FOUND"}""", HttpStatusCode.NotFound, jsonContentType)
        }
        val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
        val left = result.leftOrNull()
        assertTrue(left is HttpCallException, left.toString())
        assertEquals(404, left.code)
    }

    @Test
    fun eitherRequestErrorStatusManualBranch() = runTest {
        // expectSuccess = false -> the manual status guard in eitherRequest raises instead
        val client = jsonClient(expectSuccess = false) {
            respond("""{"code":"NOT_FOUND"}""", HttpStatusCode.NotFound, jsonContentType)
        }
        val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
        val left = result.leftOrNull()
        assertTrue(left is HttpCallException, left.toString())
        assertEquals(404, left.code)
    }

    @Test
    fun eitherRequestSerializationError() = runTest {
        // body is valid JSON but misses the required `value` field -> ContentConvertException
        val client = jsonClient {
            respond("""{"wrong":true}""", HttpStatusCode.OK, jsonContentType)
        }
        val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
        assertTrue(result.leftOrNull() is CallSerializationException, result.toString())
    }

    @Test
    fun eitherRequestIOError() = runTest {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) { json() }
            engine {
                addHandler { throw IOException("connection reset") }
            }
        }
        val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
        assertTrue(result.leftOrNull() is IOCallException, result.toString())
    }

    @Test
    fun typedDecodesErrorBody() = runTest {
        // With expectSuccess = true the validator save()s the response, so the error body is re-readable
        val client = jsonClient {
            respond("""{"code":"NOT_FOUND"}""", HttpStatusCode.NotFound, jsonContentType)
        }
        val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
            .typed<Dto, ApiError>()
        val left = result.leftOrNull()
        assertTrue(left is TypedHttpCallException<*>, left.toString())
        assertEquals(ApiError("NOT_FOUND"), left.error)
        assertEquals(404, left.code)
    }

    @Test
    fun typedKeepsSuccess() = runTest {
        val client = jsonClient {
            respond("""{"value":42}""", HttpStatusCode.OK, jsonContentType)
        }
        val result = client.eitherRequest<Dto> { url("https://example.com/dto") }
            .typed<Dto, ApiError>()
        assertTrue(result.isRight(), result.toString())
        assertEquals(Dto(42), result.getOrNull()!!.data)
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
