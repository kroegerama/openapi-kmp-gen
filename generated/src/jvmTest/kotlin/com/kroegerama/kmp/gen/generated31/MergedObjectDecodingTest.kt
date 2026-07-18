package com.kroegerama.kmp.gen.generated31

import com.kroegerama.kmp.gen.generated31.models.CombinedAnyOf
import com.kroegerama.kmp.gen.generated31.models.ExtendedPhoto
import com.kroegerama.kmp.gen.generated31.models.MergedNullableMember
import com.kroegerama.kmp.gen.generated31.models.MergedRequiredTest
import com.kroegerama.kmp.gen.generated31.models.NullableExtendedPhoto
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Schemas built from allOf/anyOf combinations without a discriminator are flattened into a
 * single class; these tests pin the merged property sets on the wire.
 */
class MergedObjectDecodingTest {

    private val json = createDefaultJson()

    @Test
    fun extendedPhotoDecodesBaseAndSiblingProperties() {
        val decoded = json.decodeFromString<ExtendedPhoto>(
            """{"albumId":1,"id":2,"title":"t","url":"u","thumbnailUrl":"tu","extendedAttr":"extra"}"""
        )
        assertEquals(
            ExtendedPhoto(albumId = 1, id = 2, title = "t", url = "u", thumbnailUrl = "tu", extendedAttr = "extra"),
            decoded,
        )
    }

    @Test
    fun combinedAnyOfDecodesMergedProperties() {
        val decoded = json.decodeFromString<CombinedAnyOf>(
            """{"id":7,"combinedExtraAttr":"extra"}"""
        )
        assertEquals(CombinedAnyOf(id = 7, combinedExtraAttr = "extra"), decoded)
    }

    @Test
    fun mergedNullableMemberDecodesMergedProperties() {
        val decoded = json.decodeFromString<MergedNullableMember>(
            """{"id":7,"mergedAttr":"merged"}"""
        )
        assertEquals(MergedNullableMember(id = 7, mergedAttr = "merged"), decoded)
    }

    @Test
    fun nullableExtendedPhotoDecodesSiblingAttr() {
        val decoded = json.decodeFromString<NullableExtendedPhoto>(
            """{"id":7,"siblingAttr":"sibling"}"""
        )
        assertEquals(NullableExtendedPhoto(id = 7, siblingAttr = "sibling"), decoded)
    }

    @Test
    fun mergedRequiredTestKeepsInnerRequired() {
        val decoded = json.decodeFromString<MergedRequiredTest>(
            """{"innerRequired":"inner","outerAttr":"outer"}"""
        )
        assertEquals(MergedRequiredTest(innerRequired = "inner", outerAttr = "outer"), decoded)
    }
}
