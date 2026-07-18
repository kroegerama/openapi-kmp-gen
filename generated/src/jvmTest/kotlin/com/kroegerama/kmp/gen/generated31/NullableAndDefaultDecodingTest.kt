package com.kroegerama.kmp.gen.generated31

import com.kroegerama.kmp.gen.generated31.models.DefaultValue
import com.kroegerama.kmp.gen.generated31.models.NullableAttrTest
import com.kroegerama.kmp.gen.generated31.models.Photo
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlin.test.Test
import kotlin.test.assertContentEquals
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
              "attr5": null,
              "attr6": null,
              "attr7": null,
              "attr8": null,
              "attr9": [null, {"id": 9}],
              "attr10": null,
              "attr11": null,
              "attr12": null
            }
            """.trimIndent()
        )

        assertEquals(Photo(id = 1), decoded.attr1)
        assertEquals(Photo(id = 2), decoded.attr2)
        assertNull(decoded.attr3)
        assertNull(decoded.attr4)
        assertNull(decoded.attr5)
        assertNull(decoded.attr6)
        assertNull(decoded.attr7)
        assertNull(decoded.attr8)
        assertContentEquals(listOf(null, Photo(id = 9)), decoded.attr9)
        assertNull(decoded.attr10)
        assertNull(decoded.attr11)
    }

    @Test
    fun nullableAttrsDecodeFromAbsentKeys() {
        val decoded = json.decodeFromString<NullableAttrTest>("""{"attr1": {"id": 1}, "attr2": {"id": 2}}""")

        assertNull(decoded.attr3)
        assertNull(decoded.attr8)
        assertEquals(emptyList(), decoded.attr9)
        assertNull(decoded.attr10)
    }
}
