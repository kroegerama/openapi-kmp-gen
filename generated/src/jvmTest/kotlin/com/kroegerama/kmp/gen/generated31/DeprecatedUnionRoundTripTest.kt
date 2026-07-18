@file:Suppress("DEPRECATION")

package com.kroegerama.kmp.gen.generated31

import com.kroegerama.kmp.gen.generated31.models.DeprecatedUnion
import com.kroegerama.kmp.gen.generated31.models.DeprecatedUnionChildA
import com.kroegerama.kmp.gen.generated31.models.DeprecatedUnionChildB
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [DeprecatedUnion] has no discriminator mapping in the spec, so its children use the
 * generated class names as discriminator values.
 */
class DeprecatedUnionRoundTripTest {

    private val json = createDefaultJson()

    private inline fun <reified T> roundTrip(value: T, expectedDiscriminator: String) {
        val encoded = json.encodeToString(value)
        val discriminator = json.parseToJsonElement(encoded).jsonObject.getValue("kind").jsonPrimitive.content
        assertEquals(expectedDiscriminator, discriminator)
        assertEquals(value, json.decodeFromString<T>(encoded))
    }

    @Test
    fun childrenUseClassNameDiscriminators() {
        roundTrip<DeprecatedUnion>(DeprecatedUnionChildA(aValue = "a"), expectedDiscriminator = "DeprecatedUnionChildA")
        roundTrip<DeprecatedUnion>(DeprecatedUnionChildB(bValue = "b"), expectedDiscriminator = "DeprecatedUnionChildB")
    }
}
