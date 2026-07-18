package com.kroegerama.kmp.gen.generated30

import com.kroegerama.kmp.gen.generated30.models.DefaultValue
import com.kroegerama.kmp.gen.generated30.models.NullableAttrTest
import com.kroegerama.kmp.gen.generated30.models.NullableInlineObject
import com.kroegerama.kmp.gen.generated30.models.Photo
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NullableAndDefaultDecodingTest {

    private val json = createDefaultJson()

    @Test
    fun defaultValueDecodesFromEmptyObject() {
        val decoded = json.decodeFromString<DefaultValue>("{}")

        assertNull(decoded.nullableString)
        assertNull(decoded.nullableList)
        assertEquals(emptyList(), decoded.requiredList)
        assertEquals(emptyMap(), decoded.requiredMap)
    }

    @Test
    fun defaultValueRoundTrips() {
        val value = DefaultValue(
            nullableString = "text",
            nullableList = listOf("a", "b"),
            requiredList = listOf("c"),
            requiredMap = mapOf("key" to "value"),
        )
        assertEquals(value, json.decodeFromString<DefaultValue>(json.encodeToString(value)))
    }

    @Test
    fun nullableAttrsDecodeFromExplicitNulls() {
        val decoded = json.decodeFromString<NullableAttrTest>(
            """
            {
              "attr1": {"id": 1},
              "attr2": {"id": 2},
              "attr3": null,
              "attr4": null,
              "attr5": null
            }
            """.trimIndent()
        )

        assertEquals(Photo(id = 1), decoded.attr1)
        assertEquals(Photo(id = 2), decoded.attr2)
        assertNull(decoded.attr3)
        assertNull(decoded.attr4)
        assertNull(decoded.attr5)
    }

    @Test
    fun nullableAttrsDecodeFromAbsentKeys() {
        val decoded = json.decodeFromString<NullableAttrTest>("""{"attr1": {"id": 1}, "attr2": {"id": 2}}""")

        assertNull(decoded.attr3)
        assertNull(decoded.attr4)
        assertNull(decoded.attr5)
    }

    @Test
    fun nullableAttrsDecodeFromValues() {
        val decoded = json.decodeFromString<NullableAttrTest>(
            """
            {
              "attr1": {"id": 1},
              "attr2": {"id": 2},
              "attr3": {"id": 3},
              "attr4": {"id": 4},
              "attr5": {"value": "five"}
            }
            """.trimIndent()
        )

        assertEquals(Photo(id = 3), decoded.attr3)
        assertEquals(Photo(id = 4), decoded.attr4)
        assertEquals(NullableInlineObject(value = "five"), decoded.attr5)
    }
}
