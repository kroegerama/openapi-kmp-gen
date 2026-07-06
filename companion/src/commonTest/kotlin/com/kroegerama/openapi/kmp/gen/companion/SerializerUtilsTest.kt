package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SerializerUtilsTest {

    @Serializable
    private data class Color(val R: Int, val G: Int, val B: Int)

    private val color = Color(R = 100, G = 200, B = 150)

    @Test
    fun pathPrimitive() {
        assertEquals("blue", createSerializedPathSegment("blue"))
        assertEquals("5", createSerializedPathSegment(5))
    }

    @Test
    fun pathArray() {
        // simple style: arrays are comma-joined regardless of explode
        assertEquals("blue,black,brown", createSerializedPathSegment(listOf("blue", "black", "brown"), explode = false))
        assertEquals("blue,black,brown", createSerializedPathSegment(listOf("blue", "black", "brown"), explode = true))
    }

    @Test
    fun pathObjectNotExploded() {
        // simple style, explode = false → k1,v1,k2,v2
        assertEquals("R,100,G,200,B,150", createSerializedPathSegment(color, explode = false))
    }

    @Test
    fun pathObjectExploded() {
        // simple style, explode = true → k1=v1,k2=v2
        assertEquals("R=100,G=200,B=150", createSerializedPathSegment(color, explode = true))
    }

    @Test
    fun pathNull() {
        val nothing: String? = null
        assertEquals("", createSerializedPathSegment(nothing))
    }

    @Test
    fun headerObjectSharesSimpleStyle() {
        val builder = HttpRequestBuilder()
        builder.appendSerializedHeaderParameter("X-Color", color, explode = true)
        assertEquals("R=100,G=200,B=150", builder.headers["X-Color"])
    }

    @Test
    fun queryArrayExploded() {
        // form style, explode = true → one parameter per item
        val builder = HttpRequestBuilder()
        builder.appendSerializedQueryParameter("c", listOf("blue", "black", "brown"), explode = true)
        assertEquals(listOf("blue", "black", "brown"), builder.url.parameters.getAll("c"))
    }

    @Test
    fun queryArrayNotExploded() {
        // form style, explode = false → single comma-joined parameter
        val builder = HttpRequestBuilder()
        builder.appendSerializedQueryParameter("c", listOf("blue", "black", "brown"), explode = false)
        assertEquals("blue,black,brown", builder.url.parameters["c"])
    }

    @Test
    fun queryObjectExploded() {
        // form style, explode = true → one parameter per property, keyed by property name; `name` dropped
        val builder = HttpRequestBuilder()
        builder.appendSerializedQueryParameter("color", color, explode = true)
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
        builder.appendSerializedQueryParameter("color", color, explode = false)
        assertEquals("R,100,G,200,B,150", builder.url.parameters["color"])
    }

    @Test
    fun cookieObjectExploded() {
        // form style, explode = true → one cookie per property, keyed by property name
        val builder = HttpRequestBuilder()
        builder.appendSerializedCookieParameter("color", color, explode = true)
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
        builder.appendSerializedCookieParameter("color", color, explode = false)
        val cookieHeader = builder.headers["Cookie"].orEmpty()
        assertTrue(cookieHeader.startsWith("color=R"), cookieHeader)
        assertTrue(!cookieHeader.contains("{"), cookieHeader)
    }
}
