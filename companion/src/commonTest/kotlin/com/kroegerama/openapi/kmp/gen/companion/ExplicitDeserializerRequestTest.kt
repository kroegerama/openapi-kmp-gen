package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Instant

class ExplicitDeserializerRequestTest {

    private val instant = Instant.parse("2024-02-23T09:50:31Z")

    @Test
    fun responseBodyUsesExplicitDeserializer() = runTest {
        val client = plainJsonClient("1708681831")
        try {
            val result = client.eitherRequest(deserializer = EpochSecondsSerializer, json = ApiJson) {}
            assertEquals(instant, result.getOrNull()?.data)
        } finally {
            client.close()
        }
    }

    @Test
    fun listResponseBodyUsesExplicitDeserializer() = runTest {
        val client = plainJsonClient("""["2024-02-23T09:50:31Z","2024-02-23T10:50:31Z"]""")
        try {
            val result = client.eitherRequest(deserializer = ListSerializer(ISO8601InstantSerializer), json = ApiJson) {}
            assertEquals(
                listOf(instant, instant + Duration.parse("1h")),
                result.getOrNull()?.data
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun responseBodyIsDecodedWithTheGivenJson() = runTest {
        // the client decodes snake case through ContentNegotiation, so only the given Json accepts the kebab case body
        val client = mockApiClient(snakeCaseJson, body = """{"first-name":"Ada"}""")
        try {
            val result = client.eitherRequest(deserializer = Person.serializer(), json = kebabCaseJson) {}
            assertEquals(Person("Ada"), result.getOrNull()?.data, result.toString())
        } finally {
            client.close()
        }
    }

    @Test
    fun malformedResponseBodyMapsToSerializationCallException() = runTest {
        val client = plainJsonClient("\"not a number\"")
        try {
            val result = client.eitherRequest(deserializer = EpochSecondsSerializer, json = ApiJson) {}
            val left = assertIs<SerializationCallException>(result.leftOrNull(), result.toString())
            assertIs<SerializationException>(left.cause.cause)
        } finally {
            client.close()
        }
    }

    @Test
    fun valueRejectedByTheDeserializerMapsToSerializationCallException() = runTest {
        val client = plainJsonClient("\"not a date\"")
        try {
            val result = client.eitherRequest(deserializer = ISO8601InstantSerializer, json = ApiJson) {}
            assertIs<SerializationCallException>(result.leftOrNull(), result.toString())
        } finally {
            client.close()
        }
    }

    @Test
    fun bodyTheGivenJsonCannotDecodeMapsToSerializationCallException() = runTest {
        val client = mockApiClient(ApiJson, body = """{"firstName":"Ada"}""")
        try {
            val result = client.eitherRequest(deserializer = Person.serializer(), json = snakeCaseJson) {}
            assertIs<SerializationCallException>(result.leftOrNull(), result.toString())
        } finally {
            client.close()
        }
    }

    @Test
    fun jsonNullBodyDecodesToNullInBothVariants() = runTest {
        val client = mockApiClient(ApiJson, body = "null")
        try {
            val explicit = client.eitherRequest(deserializer = Person.serializer().nullable, json = ApiJson) {}
            val reified = client.eitherRequest<Person?> {}

            assertTrue(explicit.isRight(), explicit.toString())
            assertNull(explicit.getOrNull()?.data)
            assertTrue(reified.isRight(), reified.toString())
            assertNull(reified.getOrNull()?.data)
        } finally {
            client.close()
        }
    }

    @Test
    fun emptyBodyWithANullableTypeMapsToSerializationCallExceptionInBothVariants() = runTest {
        val client = mockApiClient(ApiJson, body = "")
        try {
            val explicit = client.eitherRequest(deserializer = Person.serializer().nullable, json = ApiJson) {}
            val reified = client.eitherRequest<Person?> {}

            assertIs<SerializationCallException>(explicit.leftOrNull(), explicit.toString())
            assertIs<SerializationCallException>(reified.leftOrNull(), reified.toString())
        } finally {
            client.close()
        }
    }

    @Test
    fun emptyBodyWithANonNullableDeserializerMapsToSerializationCallException() = runTest {
        val client = mockApiClient(ApiJson, body = "")
        try {
            val result = client.eitherRequest(deserializer = Person.serializer(), json = ApiJson) {}
            assertIs<SerializationCallException>(result.leftOrNull(), result.toString())
        } finally {
            client.close()
        }
    }

    @Test
    fun errorStatusMapsToCallException() = runTest {
        val client = plainJsonClient("1708681831", HttpStatusCode.BadRequest)
        try {
            val result = client.eitherRequest(deserializer = EpochSecondsSerializer, json = ApiJson) {}
            assertTrue(result.isLeft())
        } finally {
            client.close()
        }
    }
}
