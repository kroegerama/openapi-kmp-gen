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
    private data class WithDefault(
        val name: String,
        val role: String = "user"
    )

    @Test
    fun formDataEncodesPrimitives() {
        val content = Form(name = "Alice", age = 30).asFormDataContent()
        assertEquals("Alice", content.formData["name"])
        assertEquals("30", content.formData["age"])
    }

    @Test
    fun formDataStringsAreUnquoted() {
        // JsonPrimitive.content is used, so string values must not carry JSON quotes
        val content = Form(name = "Alice", age = 30).asFormDataContent()
        assertEquals("Alice", content.formData["name"])
    }

    @Test
    fun formDataSkipsNulls() {
        val content = Form(name = "Alice", age = 30, nickname = null).asFormDataContent()
        assertNull(content.formData["nickname"])
        assertFalse("nickname" in content.formData.names())
    }

    @Test
    fun formDataNestedValuesFallBackToJson() {
        val content = Nested(id = 1, tags = listOf("a", "b")).asFormDataContent()
        assertEquals("1", content.formData["id"])
        // arrays/objects are not JsonPrimitive -> toString() yields JSON text
        assertEquals("[\"a\",\"b\"]", content.formData["tags"])
    }

    @Test
    fun formDataThrowsForNonObject() {
        // a bare primitive does not encode to a JsonObject -> the jsonObject cast fails
        assertFailsWith<IllegalArgumentException> {
            42.asFormDataContent()
        }
    }

    @Test
    fun multiPartThrowsForNonObject() {
        assertFailsWith<IllegalArgumentException> {
            "plain".asMultiPartFormDataContent()
        }
    }

    @Test
    fun multiPartConstructsForObject() {
        val content = Form(name = "Alice", age = 30).asMultiPartFormDataContent()
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
            setBody(Form(name = "Alice", age = 30).asMultiPartFormDataContent())
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
        // the default Json omits defaulted properties; a caller-provided Json controls encoding
        val default = WithDefault(name = "Alice").asFormDataContent()
        assertNull(default.formData["role"])

        val withDefaults = WithDefault(name = "Alice").asFormDataContent(Json { encodeDefaults = true })
        assertEquals("user", withDefaults.formData["role"])
    }
}
