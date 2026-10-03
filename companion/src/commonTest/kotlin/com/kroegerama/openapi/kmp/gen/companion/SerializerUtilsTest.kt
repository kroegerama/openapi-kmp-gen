package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SerializerUtilsTest {

    @Serializable
    private data class Color(val R: Int, val G: Int, val B: Int)

    @Serializable
    private data class WithNull(val a: String?, val b: Int)

    private val color = Color(R = 100, G = 200, B = 150)

    @Test
    fun pathSegmentHonorsExplicitJson() {
        assertEquals("first_name,Ada", createSerializedPathSegment(Person("Ada"), json = snakeCaseJson))
    }

    @Test
    fun pathPrimitive() {
        assertEquals("blue", createSerializedPathSegment("blue", json = ApiJson))
        assertEquals("5", createSerializedPathSegment(5, json = ApiJson))
    }

    @Test
    fun pathArray() {
        // simple style: arrays are comma-joined regardless of explode
        assertEquals("blue,black,brown", createSerializedPathSegment(listOf("blue", "black", "brown"), explode = false, json = ApiJson))
        assertEquals("blue,black,brown", createSerializedPathSegment(listOf("blue", "black", "brown"), explode = true, json = ApiJson))
    }

    @Test
    fun pathObjectNotExploded() {
        // simple style, explode = false → k1,v1,k2,v2
        assertEquals("R,100,G,200,B,150", createSerializedPathSegment(color, explode = false, json = ApiJson))
    }

    @Test
    fun pathObjectExploded() {
        // simple style, explode = true → k1=v1,k2=v2
        assertEquals("R=100,G=200,B=150", createSerializedPathSegment(color, explode = true, json = ApiJson))
    }

    @Test
    fun pathNull() {
        val nothing: String? = null
        assertEquals("", createSerializedPathSegment(nothing, json = ApiJson))
    }

    @Test
    fun pathArraySkipsNullElements() {
        assertEquals("blue,brown", createSerializedPathSegment(listOf("blue", null, "brown"), json = ApiJson))
    }

    @Test
    fun pathObjectWithNullValues() {
        // ApiJson omits null properties
        assertEquals("b,1", createSerializedPathSegment(WithNull(a = null, b = 1), explode = false, json = ApiJson))
        assertEquals("b=1", createSerializedPathSegment(WithNull(a = null, b = 1), explode = true, json = ApiJson))
        // a Json with explicit nulls writes them; simple style renders them as empty values
        assertEquals("a,,b,1", createSerializedPathSegment(WithNull(a = null, b = 1), explode = false, json = Json))
        assertEquals("a=,b=1", createSerializedPathSegment(WithNull(a = null, b = 1), explode = true, json = Json))
    }

    @Test
    fun appendPathSegmentAppendsToUrl() {
        val builder = HttpRequestBuilder()
        builder.appendSerializedPathSegment(listOf("blue", "black"), json = ApiJson)
        assertEquals("blue,black", builder.url.pathSegments.last())
    }

    @Test
    fun encodeToPrimitiveStringPrimitives() {
        // strings are unquoted, other primitives use their JSON text
        assertEquals("blue", Json.encodeToPrimitiveString("blue"))
        assertEquals("5", Json.encodeToPrimitiveString(5))
        assertEquals("true", Json.encodeToPrimitiveString(true))
    }

    @Test
    fun encodeToPrimitiveStringComposites() {
        assertEquals("""["a","b"]""", Json.encodeToPrimitiveString(listOf("a", "b")))
        assertEquals("""{"R":100,"G":200,"B":150}""", Json.encodeToPrimitiveString(color))
    }

    @Test
    fun encodeToPrimitiveStringNulls() {
        assertNull(Json.encodeToPrimitiveString<String?>(null))
        assertNull(Json.encodeToPrimitiveString(JsonNull))
    }

    @Test
    fun nullValuesAreSkippedEverywhere() {
        val builder = HttpRequestBuilder()
        val nothing: String? = null
        builder.appendSerializedPathSegment(nothing, json = ApiJson)
        builder.appendSerializedQueryParameter("q", nothing, json = ApiJson)
        builder.appendSerializedHeaderParameter("X-Q", nothing, json = ApiJson)
        builder.appendSerializedCookieParameter("c", nothing, json = ApiJson)
        assertEquals(emptyList(), builder.url.pathSegments.filter { it.isNotEmpty() })
        assertNull(builder.url.parameters["q"])
        assertNull(builder.headers["X-Q"])
        assertNull(builder.headers["Cookie"])
    }

    @Test
    fun headerPrimitiveAndArray() {
        val builder = HttpRequestBuilder()
        builder.appendSerializedHeaderParameter("X-P", "v", json = ApiJson)
        builder.appendSerializedHeaderParameter("X-A", listOf("a", "b"), json = ApiJson)
        assertEquals("v", builder.headers["X-P"])
        assertEquals("a,b", builder.headers["X-A"])
    }

    @Test
    fun queryPrimitive() {
        val builder = HttpRequestBuilder()
        builder.appendSerializedQueryParameter("q", 5, json = ApiJson)
        assertEquals("5", builder.url.parameters["q"])
    }

    @Test
    fun queryArrayExplodedSkipsNullElements() {
        val builder = HttpRequestBuilder()
        builder.appendSerializedQueryParameter("c", listOf("blue", null), explode = true, json = ApiJson)
        assertEquals(listOf("blue"), builder.url.parameters.getAll("c"))
    }

    @Test
    fun headerObjectSharesSimpleStyle() {
        val builder = HttpRequestBuilder()
        builder.appendSerializedHeaderParameter("X-Color", color, explode = true, json = ApiJson)
        assertEquals("R=100,G=200,B=150", builder.headers["X-Color"])
    }

    @Test
    fun queryArrayExploded() {
        // form style, explode = true → one parameter per item
        val builder = HttpRequestBuilder()
        builder.appendSerializedQueryParameter("c", listOf("blue", "black", "brown"), explode = true, json = ApiJson)
        assertEquals(listOf("blue", "black", "brown"), builder.url.parameters.getAll("c"))
    }

    @Test
    fun queryArrayNotExploded() {
        // form style, explode = false → single comma-joined parameter
        val builder = HttpRequestBuilder()
        builder.appendSerializedQueryParameter("c", listOf("blue", "black", "brown"), explode = false, json = ApiJson)
        assertEquals("blue,black,brown", builder.url.parameters["c"])
    }

    @Test
    fun queryObjectExploded() {
        // form style, explode = true → one parameter per property, keyed by property name; `name` dropped
        val builder = HttpRequestBuilder()
        builder.appendSerializedQueryParameter("color", color, explode = true, json = ApiJson)
        val params = builder.url.parameters
        assertEquals("100", params["R"])
        assertEquals("200", params["G"])
        assertEquals("150", params["B"])
        assertNull(params["color"])
    }

    @Test
    fun queryObjectNotExploded() {
        // form style, explode = false → single parameter `name=k1,v1,k2,v2`
        val builder = HttpRequestBuilder()
        builder.appendSerializedQueryParameter("color", color, explode = false, json = ApiJson)
        assertEquals("R,100,G,200,B,150", builder.url.parameters["color"])
    }

    @Test
    fun cookieObjectExploded() {
        // form style, explode = true → one cookie per property, keyed by property name
        val builder = HttpRequestBuilder()
        builder.appendSerializedCookieParameter("color", color, explode = true, json = ApiJson)
        val cookieHeader = builder.headers["Cookie"].orEmpty()
        assertTrue(cookieHeader.contains("R=100"), cookieHeader)
        assertTrue(cookieHeader.contains("G=200"), cookieHeader)
        assertTrue(cookieHeader.contains("B=150"), cookieHeader)
    }

    @Test
    fun cookieObjectNotExploded() {
        // form style, explode = false → a single cookie under `name` (not raw JSON).
        // Ktor URL-encodes the comma-joined value in the Cookie header, so assert structurally.
        val builder = HttpRequestBuilder()
        builder.appendSerializedCookieParameter("color", color, explode = false, json = ApiJson)
        val cookieHeader = builder.headers["Cookie"].orEmpty()
        assertTrue(cookieHeader.startsWith("color=R"), cookieHeader)
        assertTrue(!cookieHeader.contains("{"), cookieHeader)
    }
}
