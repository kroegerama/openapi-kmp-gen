package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Instant

class ExplicitDeserializerRequestTest {

    private val instant = Instant.parse("2024-02-23T09:50:31Z")

    private fun jsonClient(body: String, status: HttpStatusCode = HttpStatusCode.OK) = HttpClient(
        MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
    )

    @Test
    fun responseBodyUsesExplicitDeserializer() = runTest {
        val result = jsonClient("1708681831").eitherRequest(deserializer = EpochSecondsSerializer) {}
        assertEquals(instant, result.getOrNull()?.data)
    }

    @Test
    fun listResponseBodyUsesExplicitDeserializer() = runTest {
        val result = jsonClient("""["2024-02-23T09:50:31Z","2024-02-23T10:50:31Z"]""")
            .eitherRequest(deserializer = ListSerializer(ISO8601InstantSerializer)) {}
        assertEquals(
            listOf(instant, instant + Duration.parse("1h")),
            result.getOrNull()?.data
        )
    }

    @Test
    fun malformedResponseBodyMapsToCallException() = runTest {
        val result = jsonClient("\"not a number\"").eitherRequest(deserializer = EpochSecondsSerializer) {}
        assertTrue(result.isLeft())
    }

    @Test
    fun errorStatusMapsToCallException() = runTest {
        val result = jsonClient("1708681831", HttpStatusCode.BadRequest)
            .eitherRequest(deserializer = EpochSecondsSerializer) {}
        assertTrue(result.isLeft())
    }
}
