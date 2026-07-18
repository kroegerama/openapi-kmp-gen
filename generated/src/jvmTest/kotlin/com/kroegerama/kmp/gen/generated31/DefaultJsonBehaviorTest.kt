package com.kroegerama.kmp.gen.generated31

import com.kroegerama.kmp.gen.generated31.models.ActionResponse
import com.kroegerama.kmp.gen.generated31.models.CreateActionResponse
import com.kroegerama.kmp.gen.generated31.models.DefaultValue
import com.kroegerama.kmp.gen.generated31.models.IntegerTest
import com.kroegerama.kmp.gen.generated31.models.NumberTest
import com.kroegerama.kmp.gen.generated31.models.Photo
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the behavior of the [createDefaultJson] configuration used by the generated APIs
 * at runtime: `ignoreUnknownKeys`, `explicitNulls = false`, `encodeDefaults = true`,
 * `coerceInputValues`, `isLenient` and `allowSpecialFloatingPointValues`.
 */
class DefaultJsonBehaviorTest {

    private val json = createDefaultJson()

    @Test
    fun unknownKeysAreIgnored() {
        val decoded = json.decodeFromString<Photo>("""{"id":1,"unexpected":{"nested":true},"other":[1,2]}""")
        assertEquals(Photo(id = 1), decoded)
    }

    @Test
    fun unknownKeysAreIgnoredInSealedVariants() {
        val decoded = json.decodeFromString<ActionResponse>("""{"kind":"create","itemId":"item-1","extra":"x"}""")
        assertEquals(CreateActionResponse(itemId = "item-1"), decoded)
    }

    @Test
    fun nullValuesAreOmittedFromEncodedOutput() {
        val encoded = json.parseToJsonElement(json.encodeToString(Photo(id = 1))).jsonObject
        assertEquals(setOf("id"), encoded.keys)
    }

    @Test
    fun nonNullDefaultsAreEncoded() {
        val encoded = json.parseToJsonElement(json.encodeToString(DefaultValue())).jsonObject
        assertEquals(setOf("requiredList", "requiredMap"), encoded.keys)
    }

    @Test
    fun nullCoercesToDefaultForNonNullableProperties() {
        val decoded = json.decodeFromString<DefaultValue>("""{"requiredList":null,"requiredMap":null}""")
        assertEquals(emptyList(), decoded.requiredList)
        assertEquals(emptyMap(), decoded.requiredMap)
    }

    @Test
    fun lenientModeDecodesQuotedNumbers() {
        val decoded = json.decodeFromString<IntegerTest>("""{"int32":"5","int64":"9007199254740993"}""")
        assertEquals(5, decoded.int32)
        assertEquals(9007199254740993L, decoded.int64)
    }

    @Test
    fun specialFloatingPointValuesRoundTrip() {
        val encoded = json.encodeToString(NumberTest(double = Double.NaN, float = Float.POSITIVE_INFINITY))
        val decoded = json.decodeFromString<NumberTest>(encoded)
        assertTrue(decoded.double!!.isNaN())
        assertEquals(Float.POSITIVE_INFINITY, decoded.float)
    }
}
