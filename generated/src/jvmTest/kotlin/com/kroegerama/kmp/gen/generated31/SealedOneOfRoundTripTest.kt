package com.kroegerama.kmp.gen.generated31

import com.kroegerama.kmp.gen.generated31.models.NullableUnion
import com.kroegerama.kmp.gen.generated31.models.NullableUnionChildA
import com.kroegerama.kmp.gen.generated31.models.NullableUnionChildB
import com.kroegerama.kmp.gen.generated31.models.SealedClass1
import com.kroegerama.kmp.gen.generated31.models.SealedClass1Child1
import com.kroegerama.kmp.gen.generated31.models.SealedClass1Child2
import com.kroegerama.kmp.gen.generated31.models.SealedClass2
import com.kroegerama.kmp.gen.generated31.models.SealedClass2Child1
import com.kroegerama.kmp.gen.generated31.models.SealedClass2Child2
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SealedOneOfRoundTripTest {

    private val json = createDefaultJson()

    private inline fun <reified T> roundTrip(value: T, discriminatorProperty: String, expectedDiscriminator: String) {
        val encoded = json.encodeToString(value)
        val discriminator = json.parseToJsonElement(encoded).jsonObject.getValue(discriminatorProperty).jsonPrimitive.content
        assertEquals(expectedDiscriminator, discriminator)
        assertEquals(value, json.decodeFromString<T>(encoded))
    }

    @Test
    fun sealedClass1VariantsRoundTrip() {
        roundTrip<SealedClass1>(
            SealedClass1Child1(commonAttr = "common", child1Only = 1L),
            discriminatorProperty = "#discriminator",
            expectedDiscriminator = "C1",
        )
        roundTrip<SealedClass1>(
            SealedClass1Child2(commonAttr = "common", child2Only = "two"),
            discriminatorProperty = "#discriminator",
            expectedDiscriminator = "C2",
        )
    }

    @Test
    fun sealedClass2VariantsRoundTrip() {
        roundTrip<SealedClass2>(
            SealedClass2Child1(commonAttr = "common", child1Only = 1L),
            discriminatorProperty = "#discriminator",
            expectedDiscriminator = "C1",
        )
        roundTrip<SealedClass2>(
            SealedClass2Child2(commonAttr = "common", child2Only = "two"),
            discriminatorProperty = "#discriminator",
            expectedDiscriminator = "C2",
        )
    }

    @Test
    fun nullableUnionVariantsRoundTrip() {
        roundTrip<NullableUnion>(
            NullableUnionChildA(aValue = "a"),
            discriminatorProperty = "kind",
            expectedDiscriminator = "a",
        )
        roundTrip<NullableUnion>(
            NullableUnionChildB(bValue = 2),
            discriminatorProperty = "kind",
            expectedDiscriminator = "b",
        )
    }

    @Test
    fun nullableUnionEncodesAndDecodesNull() {
        assertEquals("null", json.encodeToString<NullableUnion?>(null))
        assertNull(json.decodeFromString<NullableUnion?>("null"))

        val restored = json.decodeFromString<NullableUnion?>("""{"kind":"a","aValue":"a"}""")
        assertEquals(NullableUnionChildA(aValue = "a"), restored)
    }
}
