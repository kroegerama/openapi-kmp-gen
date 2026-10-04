package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FormDataContentTest {

    @Serializable
    private data class Form(
        val name: String,
        val age: Int,
        val nickname: String? = null
    )

    @Serializable
    private data class Nested(
        val id: Int,
        val tags: List<String>
    )

    @Serializable
    private data class Tagged(
        val tags: List<String?>
    )

    @Serializable
    private data class Matrix(
        val m: List<List<String>>
    )

    @Serializable
    private data class ObjectItems(
        val items: List<Nested>
    )

    @Serializable
    private data class WithObject(
        val id: Int,
        val nested: Nested
    )

    @Serializable
    private data class WithDefault(
        val name: String,
        val role: String = "user"
    )

    @Test
    fun formDataEncodesPrimitives() {
        val content = Form(name = "Alice", age = 30).asFormDataContent(json = ApiJson)
        assertEquals("Alice", content.formData["name"])
        assertEquals("30", content.formData["age"])
    }

    @Test
    fun formDataStringsAreUnquoted() {
        // JsonPrimitive.content is used, so string values must not carry JSON quotes
        val content = Form(name = "Alice", age = 30).asFormDataContent(json = ApiJson)
        assertEquals("Alice", content.formData["name"])
    }

    @Test
    fun formDataSkipsNulls() {
        val content = Form(name = "Alice", age = 30, nickname = null).asFormDataContent(json = ApiJson)
        assertNull(content.formData["nickname"])
        assertFalse("nickname" in content.formData.names())
    }

    @Test
    fun formDataExplodesArrays() {
        val content = Tagged(tags = listOf("a", null, "b")).asFormDataContent(json = ApiJson)
        assertEquals(listOf("a", "b"), content.formData.getAll("tags"))
    }

    @Test
    fun formDataJoinsArraysWithoutExplode() {
        val content = Tagged(tags = listOf("a", null, "b")).asFormDataContent(explode = false, json = ApiJson)
        assertEquals(listOf("a,b"), content.formData.getAll("tags"))
    }

    @Test
    fun formDataSkipsEmptyArrays() {
        listOf(true, false).forEach { explode ->
            val content = Tagged(tags = emptyList()).asFormDataContent(explode = explode, json = ApiJson)
            assertFalse("tags" in content.formData.names(), "explode=$explode")
        }
    }

    @Test
    fun formDataSkipsAllNullArrays() {
        listOf(true, false).forEach { explode ->
            val content = Tagged(tags = listOf(null, null)).asFormDataContent(explode = explode, json = ApiJson)
            assertFalse("tags" in content.formData.names(), "explode=$explode")
        }
    }

    @Test
    fun formDataExplodesArrayItemsAsJson() {
        val content = Matrix(m = listOf(listOf("a"), listOf("b"))).asFormDataContent(json = ApiJson)
        assertEquals(listOf("""["a"]""", """["b"]"""), content.formData.getAll("m"))
    }

    @Test
    fun formDataJoinsArrayItemsAsJsonWithoutExplode() {
        val content = Matrix(m = listOf(listOf("a"), listOf("b"))).asFormDataContent(explode = false, json = ApiJson)
        assertEquals(listOf("""["a"],["b"]"""), content.formData.getAll("m"))
    }

    @Test
    fun formDataExplodesObjectItemsAsJson() {
        val items = listOf(Nested(id = 1, tags = listOf("a")), Nested(id = 2, tags = emptyList()))
        val content = ObjectItems(items = items).asFormDataContent(json = ApiJson)
        assertEquals(listOf("""{"id":1,"tags":["a"]}""", """{"id":2,"tags":[]}"""), content.formData.getAll("items"))
    }

    @Test
    fun formDataJoinsObjectItemsAsJsonWithoutExplode() {
        val items = listOf(Nested(id = 1, tags = listOf("a")), Nested(id = 2, tags = emptyList()))
        val content = ObjectItems(items = items).asFormDataContent(explode = false, json = ApiJson)
        assertEquals(listOf("""{"id":1,"tags":["a"]},{"id":2,"tags":[]}"""), content.formData.getAll("items"))
    }

    @Test
    fun formDataNestedObjectsStayJson() {
        val content = WithObject(id = 1, nested = Nested(id = 2, tags = listOf("a", "b"))).asFormDataContent(json = ApiJson)
        assertEquals("1", content.formData["id"])
        assertEquals("""{"id":2,"tags":["a","b"]}""", content.formData["nested"])
    }

    @Test
    fun formDataThrowsForNonObject() {
        // a bare primitive does not encode to a JsonObject -> the jsonObject cast fails
        assertFailsWith<IllegalArgumentException> {
            42.asFormDataContent(json = ApiJson)
        }
    }

    @Test
    fun formDataThrowsForListReceiver() {
        assertFailsWith<IllegalArgumentException> {
            listOf("a", "b").asFormDataContent(json = ApiJson)
        }
    }

    @Test
    fun multiPartThrowsForNonObject() {
        assertFailsWith<IllegalArgumentException> {
            "plain".asMultiPartFormDataContent(ApiJson)
        }
    }

    @Test
    fun multiPartConstructsForObject() {
        val content = Form(name = "Alice", age = 30).asMultiPartFormDataContent(ApiJson)
        assertTrue(content.contentType.toString().startsWith("multipart/form-data"))
    }

    @Test
    fun multiPartEncodesValues() = runTest {
        var body = ""
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    body = request.body.toByteArray().decodeToString()
                    respond("ok")
                }
            }
        }
        client.post("https://example.com/upload") {
            setBody(Form(name = "Alice", age = 30).asMultiPartFormDataContent(ApiJson))
        }
        client.close()

        assertTrue("""name="name"""" in body, body)
        assertTrue("Alice" in body, body)
        assertTrue("""name="age"""" in body, body)
        assertTrue("30" in body, body)
        assertFalse("""name="nickname"""" in body, body)
    }

    @Test
    fun formDataUsesProvidedJson() {
        // ApiJson encodes defaulted properties, a Json without encodeDefaults omits them
        val withDefaults = WithDefault(name = "Alice").asFormDataContent(json = ApiJson)
        assertEquals("user", withDefaults.formData["role"])

        val withoutDefaults = WithDefault(name = "Alice").asFormDataContent(json = Json { encodeDefaults = false })
        assertNull(withoutDefaults.formData["role"])
    }

    @Test
    fun multiPartUsesProvidedJson() = runTest {
        var body = ""
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    body = request.body.toByteArray().decodeToString()
                    respond("ok")
                }
            }
        }
        try {
            client.post("https://example.com/upload") {
                setBody(Person("Ada").asMultiPartFormDataContent(snakeCaseJson))
            }
        } finally {
            client.close()
        }

        assertTrue("""name="first_name"""" in body, body)
        assertTrue("Ada" in body, body)
    }
}
