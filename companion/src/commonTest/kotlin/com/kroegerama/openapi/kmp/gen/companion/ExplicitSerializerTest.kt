package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.reflect.typeInfo
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Instant

class ExplicitSerializerTest {

    private val instant = Instant.parse("2024-02-23T09:50:31Z")

    @Test
    fun pathSegmentUsesExplicitSerializer() {
        assertEquals(
            "2024-02-23T09:50:31Z",
            createSerializedPathSegment(value = instant, serializer = ISO8601InstantSerializer, json = ApiJson)
        )
        assertEquals(
            "1708681831",
            createSerializedPathSegment(value = instant, serializer = EpochSecondsSerializer, json = ApiJson)
        )
    }

    @Test
    fun queryParameterUsesExplicitSerializer() {
        val builder = HttpRequestBuilder()
        builder.appendSerializedQueryParameter("instant", instant, serializer = ISO8601InstantSerializer, json = ApiJson)
        builder.appendSerializedQueryParameter("seconds", instant, serializer = EpochSecondsSerializer, json = ApiJson)
        builder.appendSerializedQueryParameter("millis", instant, serializer = EpochMillisecondsSerializer, json = ApiJson)
        builder.appendSerializedQueryParameter("bytes", "Hello World".encodeToByteArray(), serializer = Base64Serializer, json = ApiJson)
        assertEquals("2024-02-23T09:50:31Z", builder.url.parameters["instant"])
        assertEquals("1708681831", builder.url.parameters["seconds"])
        assertEquals("1708681831000", builder.url.parameters["millis"])
        assertEquals("SGVsbG8gV29ybGQ=", builder.url.parameters["bytes"])
    }

    @Test
    fun queryParameterListUsesExplicitSerializer() {
        val builder = HttpRequestBuilder()
        val values = listOf(instant, instant + Duration.parse("1h"))
        builder.appendSerializedQueryParameter(
            name = "instants",
            value = values,
            serializer = ListSerializer(ISO8601InstantSerializer),
            explode = true,
            json = ApiJson
        )
        assertEquals(
            listOf("2024-02-23T09:50:31Z", "2024-02-23T10:50:31Z"),
            builder.url.parameters.getAll("instants")
        )
    }

    @Test
    fun headerAndCookieUseExplicitSerializer() {
        val builder = HttpRequestBuilder()
        builder.appendSerializedHeaderParameter("X-Instant", instant, serializer = ISO8601InstantSerializer, json = ApiJson)
        builder.appendSerializedCookieParameter("seconds", instant, serializer = EpochSecondsSerializer, json = ApiJson)
        assertEquals("2024-02-23T09:50:31Z", builder.headers["X-Instant"])
        assertEquals("seconds=1708681831", builder.headers["Cookie"])
    }

    @Test
    fun nullValuesAreSkipped() {
        val builder = HttpRequestBuilder()
        val nothing: Instant? = null
        builder.appendSerializedPathSegment(nothing, serializer = ISO8601InstantSerializer, json = ApiJson)
        builder.appendSerializedQueryParameter("q", nothing, serializer = ISO8601InstantSerializer, json = ApiJson)
        builder.appendSerializedHeaderParameter("X-Q", nothing, serializer = ISO8601InstantSerializer, json = ApiJson)
        builder.appendSerializedCookieParameter("c", nothing, serializer = ISO8601InstantSerializer, json = ApiJson)
        assertEquals("", createSerializedPathSegment(nothing, serializer = ISO8601InstantSerializer, json = ApiJson))
        assertEquals(emptyList(), builder.url.pathSegments.filter { it.isNotEmpty() })
        assertNull(builder.url.parameters["q"])
        assertNull(builder.headers["X-Q"])
        assertNull(builder.headers["Cookie"])
    }

    @Test
    fun encodeNullableToJsonElement() {
        assertEquals(
            JsonPrimitive("2024-02-23T09:50:31Z"),
            Json.encodeNullableToJsonElement(ISO8601InstantSerializer, instant)
        )
        assertEquals(JsonNull, Json.encodeNullableToJsonElement(ISO8601InstantSerializer, null))
    }

    @Test
    fun setSerializedBodyUsesExplicitSerializer() {
        val builder = HttpRequestBuilder()
        builder.setSerializedBody(value = instant, serializer = ISO8601InstantSerializer, json = ApiJson)
        assertEquals(JsonPrimitive("2024-02-23T09:50:31Z"), builder.body)
        assertEquals(typeInfo<JsonElement>(), builder.bodyType)
    }

    @Test
    fun setSerializedBodySendsJsonNullForNull() {
        val builder = HttpRequestBuilder()
        builder.setSerializedBody(value = null, serializer = ISO8601InstantSerializer, json = ApiJson)
        assertEquals(JsonNull, builder.body)
        assertEquals(typeInfo<JsonElement>(), builder.bodyType)
    }

    @Test
    fun setSerializedBodyHonorsExplicitJson() {
        val builder = HttpRequestBuilder()
        builder.setSerializedBody(value = Person("Ada"), serializer = Person.serializer(), json = snakeCaseJson)
        assertEquals(buildJsonObject { put("first_name", "Ada") }, builder.body)
    }
}
