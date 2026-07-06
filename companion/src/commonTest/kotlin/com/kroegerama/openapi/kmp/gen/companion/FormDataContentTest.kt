package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.serialization.Serializable
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
}
