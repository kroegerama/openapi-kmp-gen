package com.kroegerama.kmp.gen.generated30

import com.kroegerama.kmp.gen.generated30.models.BucketOnly
import com.kroegerama.kmp.gen.generated30.models.ClosedAlias
import com.kroegerama.kmp.gen.generated30.models.ClosedEmpty
import com.kroegerama.kmp.gen.generated30.models.ClosedObject
import com.kroegerama.kmp.gen.generated30.models.CollidingHybrid
import com.kroegerama.kmp.gen.generated30.models.FreeFormHybrid
import com.kroegerama.kmp.gen.generated30.models.FreeFormMap
import com.kroegerama.kmp.gen.generated30.models.HybridOwner
import com.kroegerama.kmp.gen.generated30.models.HybridUnion
import com.kroegerama.kmp.gen.generated30.models.HybridUnionA
import com.kroegerama.kmp.gen.generated30.models.InheritedHybrid
import com.kroegerama.kmp.gen.generated30.models.MapProperties
import com.kroegerama.kmp.gen.generated30.models.MapWithSiblings
import com.kroegerama.kmp.gen.generated30.models.NestedHybrid
import com.kroegerama.kmp.gen.generated30.models.NestedNameHybrid
import com.kroegerama.kmp.gen.generated30.models.NullableFreeFormMap
import com.kroegerama.kmp.gen.generated30.models.NullableValueHybrid
import com.kroegerama.kmp.gen.generated30.models.OverridingHybrid
import com.kroegerama.kmp.gen.generated30.models.Photo
import com.kroegerama.kmp.gen.generated30.models.SealedHybridBase
import com.kroegerama.kmp.gen.generated30.models.SealedHybridChild1
import com.kroegerama.kmp.gen.generated30.models.SealedHybridChild2
import com.kroegerama.kmp.gen.generated30.models.TypedHybrid
import com.kroegerama.kmp.gen.generated30.models.UntaggedHybridB
import com.kroegerama.kmp.gen.generated30.models.UntaggedHybridUnion
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Instant

class AdditionalPropertiesRoundTripTest {

    private val json = createDefaultJson()
    private val strictJson = Json(json) { ignoreUnknownKeys = false }

    private inline fun <reified T> assertRoundTrip(value: T) {
        assertEquals(value, json.decodeFromString<T>(json.encodeToString(value)))
        assertEquals(value, json.decodeFromJsonElement<T>(json.encodeToJsonElement(value)))
    }

    @Test
    fun typedHybridDecodesAndEncodesBucket() {
        val decoded = json.decodeFromString<TypedHybrid>("""{"id":1,"name":"first","created":"2026-05-05T10:15:30Z"}""")

        assertEquals(1L, decoded.id)
        assertEquals("first", decoded.name)
        assertEquals(mapOf("created" to Instant.parse("2026-05-05T10:15:30Z")), decoded.additionalProperties)

        val encoded = json.encodeToJsonElement(decoded).jsonObject
        assertEquals(setOf("id", "name", "created"), encoded.keys)
        assertFalse("additionalProperties" in encoded)
    }

    @Test
    fun hybridsRoundTrip() {
        assertRoundTrip(TypedHybrid(id = 1L, additionalProperties = mapOf("created" to Instant.parse("2026-05-05T10:15:30Z"))))
        assertRoundTrip(FreeFormHybrid(id = 2L, additionalProperties = buildJsonObject { put("nested", buildJsonObject { put("flag", true) }) }))
        assertRoundTrip(NullableValueHybrid(id = 3L, additionalProperties = mapOf("present" to "yes", "absent" to null)))
        assertRoundTrip(
            InheritedHybrid(id = 4L, extra = true, additionalProperties = mapOf("created" to Instant.parse("2026-05-05T10:15:30Z")))
        )
        assertRoundTrip(OverridingHybrid(id = 5L, additionalProperties = mapOf("first" to 1L)))
    }

    @Test
    fun collidingHybridSeparatesDeclaredPropertyFromBucket() {
        val decoded = json.decodeFromString<CollidingHybrid>(
            """{"id":1,"additionalProperties":"declared","other":"extra","additionalProperties_":"captured"}"""
        )

        assertEquals("declared", decoded.additionalProperties)
        assertEquals(mapOf("other" to "extra", "additionalProperties_" to "captured"), decoded.additionalProperties_)
        assertRoundTrip(decoded)
    }

    @Test
    fun sealedHybridVariantsKeepDiscriminatorOutOfBucket() {
        val declared = HybridUnionA(aValue = "a", additionalProperties = mapOf("extra" to "value"))
        val declaredJson = """{"kind":"a","aValue":"a","extra":"value"}"""
        val fallback = UntaggedHybridB(bValue = 2L, additionalProperties = buildJsonObject { put("extra", buildJsonArray { add(JsonNull) }) })
        val fallbackJson = """{"type":"UntaggedHybridB","bValue":2,"extra":[null]}"""

        assertEquals(declared, strictJson.decodeFromString<HybridUnion>(declaredJson))
        assertEquals(declared, strictJson.decodeFromJsonElement<HybridUnion>(strictJson.parseToJsonElement(declaredJson)))
        assertEquals(declared, strictJson.decodeFromString<HybridUnion>("""{"aValue":"a","extra":"value","kind":"a"}"""))
        assertEquals(declaredJson, json.encodeToString<HybridUnion>(declared))
        assertEquals(json.parseToJsonElement(declaredJson), json.encodeToJsonElement<HybridUnion>(declared))

        assertEquals(fallback, strictJson.decodeFromString<UntaggedHybridUnion>(fallbackJson))
        assertEquals(fallback, strictJson.decodeFromJsonElement<UntaggedHybridUnion>(strictJson.parseToJsonElement(fallbackJson)))
        assertEquals(fallback, strictJson.decodeFromString<UntaggedHybridUnion>("""{"bValue":2,"extra":[null],"type":"UntaggedHybridB"}"""))
        assertEquals(fallbackJson, json.encodeToString<UntaggedHybridUnion>(fallback))
        assertEquals(json.parseToJsonElement(fallbackJson), json.encodeToJsonElement<UntaggedHybridUnion>(fallback))
    }

    @Test
    fun collidingBucketEntriesFailEncoding() {
        assertFailsWith<SerializationException> {
            json.encodeToString(TypedHybrid(id = 1L, additionalProperties = mapOf("id" to Instant.parse("2026-05-05T10:15:30Z"))))
        }
        assertFailsWith<SerializationException> {
            json.encodeToString<HybridUnion>(HybridUnionA(additionalProperties = mapOf("kind" to "x")))
        }
    }

    @Test
    fun closedObjectDropsUnknownKeys() {
        assertEquals(ClosedObject(id = 1L), json.decodeFromString<ClosedObject>("""{"id":1,"unknown":"dropped"}"""))
    }

    @Test
    fun freeFormMapsRoundTrip() {
        val value: FreeFormMap = buildJsonObject { put("nested", buildJsonObject { put("list", buildJsonArray { add(JsonNull) }) }) }
        assertEquals(value, json.decodeFromString<FreeFormMap>(json.encodeToString(value)))

        val nullEntry = """{"present":{"flag":true},"absent":null}"""
        val decoded = json.decodeFromString<NullableFreeFormMap>(nullEntry)
        assertEquals(JsonNull, decoded.getValue("absent"))
        assertEquals(nullEntry, json.encodeToString(decoded))
    }

    @Test
    fun nestedNameHybridRoundTrips() {
        val input = """{"additionalProperty":{"inner":"a"},"serializer":{"inner":"b"},"extra":{"value":"c"}}"""

        val decoded = json.decodeFromString<NestedNameHybrid>(input)

        assertEquals(NestedNameHybrid.AdditionalProperty(inner = "a"), decoded.additionalProperty)
        assertEquals(NestedNameHybrid.Serializer(inner = "b"), decoded.serializer)
        assertEquals(mapOf("extra" to NestedNameHybrid.AdditionalProperty_(value = "c")), decoded.additionalProperties)
        assertEquals(input, json.encodeToString(decoded))
    }

    @Test
    fun inlineHybridPropertyRoundTrips() {
        val input = """{"inline":{"id":1,"extra":"value"}}"""
        val expected = HybridOwner(inline = HybridOwner.Inline(id = 1L, additionalProperties = mapOf("extra" to "value")))

        assertEquals(expected, json.decodeFromString<HybridOwner>(input))
        assertEquals(input, json.encodeToString(expected))
        assertRoundTrip(expected)
    }

    @Test
    fun bucketOnlyStructuresRoundTrip() {
        val bucketOnly = BucketOnly(additionalProperties = mapOf("a" to "1", "b" to "2"))
        assertEquals("""{"a":"1","b":"2"}""", json.encodeToString(bucketOnly))
        assertEquals(bucketOnly, json.decodeFromString<BucketOnly>("""{"a":"1","b":"2"}"""))
        assertRoundTrip(bucketOnly)

        val siblings = MapWithSiblings(id = 1L, additionalProperties = mapOf("a" to "1"))
        assertEquals("""{"id":1,"a":"1"}""", json.encodeToString(siblings))
        assertEquals(siblings, json.decodeFromString<MapWithSiblings>("""{"id":1,"a":"1"}"""))
    }

    @Test
    fun closedShapesStayPlain() {
        val alias: ClosedAlias = ClosedObject(id = 1L)
        assertEquals(alias, json.decodeFromString<ClosedAlias>("""{"id":1,"unknown":"dropped"}"""))

        val empty: ClosedEmpty = json.parseToJsonElement("""{"any":[1,"two",null]}""")
        assertEquals(empty, json.decodeFromString<ClosedEmpty>(json.encodeToString(empty)))
    }

    @Test
    fun nestedHybridValuesRoundTrip() {
        val input = """{"id":1,"child":{"id":2,"deep":"x"}}"""
        val expected = NestedHybrid(
            id = 1L,
            additionalProperties = mapOf("child" to FreeFormHybrid(id = 2L, additionalProperties = buildJsonObject { put("deep", "x") })),
        )

        assertEquals(expected, json.decodeFromString<NestedHybrid>(input))
        assertEquals(input, json.encodeToString(expected))
        assertRoundTrip(expected)
    }

    @Test
    fun hybridCollectionsUseTheClassSerializer() {
        val input = """[{"id":1,"created":"2026-05-05T10:15:30Z"},{"id":2}]"""
        val expected = listOf(
            TypedHybrid(id = 1L, additionalProperties = mapOf("created" to Instant.parse("2026-05-05T10:15:30Z"))),
            TypedHybrid(id = 2L),
        )

        assertEquals(expected, json.decodeFromString<List<TypedHybrid>>(input))
        assertEquals(input, json.encodeToString(expected))
    }

    @Test
    fun mapPropertiesRoundTrip() {
        val input = """{"meta":{"a":1,"b":[null]},"photos":{"p":{"id":1,"title":"t"}}}"""
        val expected = MapProperties(
            meta = buildJsonObject { put("a", 1); put("b", buildJsonArray { add(JsonNull) }) },
            photos = mapOf("p" to Photo(id = 1, title = "t")),
        )

        assertEquals(expected, json.decodeFromString<MapProperties>(input))
        assertEquals(input, json.encodeToString(expected))
        assertEquals("""{}""", json.encodeToString(MapProperties()))
    }

    @Test
    fun sealedSiblingBucketIsInheritedByVariants() {
        val input1 = """{"kind":"one","one":1,"extra":"value"}"""
        val expected1 = SealedHybridChild1(one = 1L, additionalProperties = mapOf("extra" to "value"))
        val input2 = """{"kind":"two","two":"t","extra":{"nested":1}}"""
        val expected2 = SealedHybridChild2(two = "t", additionalProperties = buildJsonObject { put("extra", buildJsonObject { put("nested", 1) }) })

        assertEquals(expected1, strictJson.decodeFromString<SealedHybridBase>(input1))
        assertEquals(expected1, strictJson.decodeFromString<SealedHybridBase>("""{"one":1,"extra":"value","kind":"one"}"""))
        assertEquals(expected2, strictJson.decodeFromJsonElement<SealedHybridBase>(strictJson.parseToJsonElement(input2)))
        assertEquals(input1, json.encodeToString<SealedHybridBase>(expected1))
        assertEquals(json.parseToJsonElement(input2), json.encodeToJsonElement<SealedHybridBase>(expected2))
        assertFailsWith<SerializationException> {
            json.encodeToString<SealedHybridBase>(SealedHybridChild1(additionalProperties = mapOf("kind" to "x")))
        }
    }
}
