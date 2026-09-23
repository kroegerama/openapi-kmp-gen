package com.kroegerama.kmp.gen.generated31

import com.kroegerama.kmp.gen.generated31.models.BucketOnly
import com.kroegerama.kmp.gen.generated31.models.ClosedAlias
import com.kroegerama.kmp.gen.generated31.models.ClosedEmpty
import com.kroegerama.kmp.gen.generated31.models.ClosedObject
import com.kroegerama.kmp.gen.generated31.models.ClosedRedeclaration
import com.kroegerama.kmp.gen.generated31.models.CollidingHybrid
import com.kroegerama.kmp.gen.generated31.models.FreeFormHybrid
import com.kroegerama.kmp.gen.generated31.models.FreeFormMap
import com.kroegerama.kmp.gen.generated31.models.HybridBody200Response
import com.kroegerama.kmp.gen.generated31.models.HybridBodyRequest
import com.kroegerama.kmp.gen.generated31.models.HybridOwner
import com.kroegerama.kmp.gen.generated31.models.HybridUnion
import com.kroegerama.kmp.gen.generated31.models.HybridUnionA
import com.kroegerama.kmp.gen.generated31.models.HybridUnionB
import com.kroegerama.kmp.gen.generated31.models.InheritedHybrid
import com.kroegerama.kmp.gen.generated31.models.MapProperties
import com.kroegerama.kmp.gen.generated31.models.MapWithSiblings
import com.kroegerama.kmp.gen.generated31.models.NestedHybrid
import com.kroegerama.kmp.gen.generated31.models.NestedNameHybrid
import com.kroegerama.kmp.gen.generated31.models.NullableFreeFormMap
import com.kroegerama.kmp.gen.generated31.models.NullableMapWithSiblings
import com.kroegerama.kmp.gen.generated31.models.NullableValueHybrid
import com.kroegerama.kmp.gen.generated31.models.ObjectValueHybrid
import com.kroegerama.kmp.gen.generated31.models.OverridingHybrid
import com.kroegerama.kmp.gen.generated31.models.Photo
import com.kroegerama.kmp.gen.generated31.models.SealedHybridBase
import com.kroegerama.kmp.gen.generated31.models.SealedHybridChild1
import com.kroegerama.kmp.gen.generated31.models.SealedHybridChild2
import com.kroegerama.kmp.gen.generated31.models.TypedHybrid
import com.kroegerama.kmp.gen.generated31.models.UntaggedHybridA
import com.kroegerama.kmp.gen.generated31.models.UntaggedHybridB
import com.kroegerama.kmp.gen.generated31.models.UntaggedHybridUnion
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

class AdditionalPropertiesRoundTripTest {

    private val json = createDefaultJson()
    private val strictJson = Json(json) { ignoreUnknownKeys = false }

    private inline fun <reified T> assertRoundTrip(value: T) {
        assertEquals(value, json.decodeFromString<T>(json.encodeToString(value)))
        assertEquals(value, json.decodeFromJsonElement<T>(json.encodeToJsonElement(value)))
    }

    @Test
    fun typedHybridDecodesDeclaredPropertiesAndBucket() {
        val decoded = json.decodeFromString<TypedHybrid>(
            """{"id":1,"name":"first","created":"2026-05-05T10:15:30Z","updated":"2026-05-06T10:15:30Z"}"""
        )

        assertEquals(1L, decoded.id)
        assertEquals("first", decoded.name)
        assertEquals(
            mapOf(
                "created" to Instant.parse("2026-05-05T10:15:30Z"),
                "updated" to Instant.parse("2026-05-06T10:15:30Z"),
            ),
            decoded.additionalProperties,
        )
    }

    @Test
    fun typedHybridEncodesBucketEntriesAtTopLevel() {
        val value = TypedHybrid(
            id = 1L,
            name = "first",
            additionalProperties = mapOf("created" to Instant.parse("2026-05-05T10:15:30Z")),
        )

        val encoded = json.encodeToJsonElement(value).jsonObject

        assertEquals(setOf("id", "name", "created"), encoded.keys)
        assertEquals("2026-05-05T10:15:30Z", encoded.getValue("created").jsonPrimitive.content)
        assertFalse("additionalProperties" in encoded)
    }

    @Test
    fun hybridsRoundTrip() {
        assertRoundTrip(TypedHybrid(id = 1L, additionalProperties = mapOf("created" to Instant.parse("2026-05-05T10:15:30Z"))))
        assertRoundTrip(
            FreeFormHybrid(
                id = 2L,
                additionalProperties = buildJsonObject {
                    put("text", "value")
                    put("number", 3)
                    put("nested", buildJsonObject { put("flag", true) })
                    put("list", buildJsonArray { add(JsonPrimitive(1)); add(JsonNull) })
                },
            )
        )
        assertRoundTrip(NullableValueHybrid(id = 3L, additionalProperties = mapOf("present" to "yes", "absent" to null)))
        assertRoundTrip(
            InheritedHybrid(id = 4L, extra = true, additionalProperties = mapOf("created" to Instant.parse("2026-05-05T10:15:30Z")))
        )
        assertRoundTrip(OverridingHybrid(id = 5L, name = "five", additionalProperties = mapOf("first" to 1L, "second" to 2L)))
        assertRoundTrip(
            ObjectValueHybrid(
                id = 6L,
                additionalProperties = mapOf("entry" to ObjectValueHybrid.AdditionalProperty(label = "label", count = 7L)),
            )
        )
        assertRoundTrip(HybridBodyRequest(id = 8L, additionalProperties = mapOf("extra" to "value")))
        assertRoundTrip(HybridBody200Response(count = 9L, additionalProperties = buildJsonObject { put("extra", "value") }))
    }

    @Test
    fun nullableValueHybridKeepsNullEntries() {
        val decoded = json.decodeFromString<NullableValueHybrid>("""{"id":1,"present":"yes","absent":null}""")

        assertEquals(mapOf("present" to "yes", "absent" to null), decoded.additionalProperties)
        assertEquals("""{"id":1,"present":"yes","absent":null}""", json.encodeToString(decoded))
    }

    @Test
    fun typedBucketValuesUseTheCompanionSerializer() {
        val decoded = json.decodeFromString<TypedHybrid>("""{"id":1,"created":"2026-05-05T12:15:30+02:00"}""")

        assertEquals(Instant.parse("2026-05-05T10:15:30Z"), decoded.additionalProperties.getValue("created"))
        assertEquals("""{"id":1,"created":"2026-05-05T10:15:30Z"}""", json.encodeToString(decoded))
    }

    @Test
    fun collidingHybridSeparatesDeclaredPropertyFromBucket() {
        val decoded = json.decodeFromString<CollidingHybrid>(
            """{"id":1,"additionalProperties":"declared","other":"extra","additionalProperties_":"captured"}"""
        )

        assertEquals("declared", decoded.additionalProperties)
        assertEquals(mapOf("other" to "extra", "additionalProperties_" to "captured"), decoded.additionalProperties_)
        assertRoundTrip(decoded)

        val encoded = json.encodeToJsonElement(decoded).jsonObject
        assertEquals(setOf("id", "additionalProperties", "other", "additionalProperties_"), encoded.keys)
        assertEquals("declared", encoded.getValue("additionalProperties").jsonPrimitive.content)
    }

    @Test
    fun discriminatedHybridVariantsKeepDiscriminatorOutOfBucket() {
        val inputA = """{"kind":"a","aValue":"a","extra":"value"}"""
        val expectedA = HybridUnionA(aValue = "a", additionalProperties = mapOf("extra" to "value"))
        val inputB = """{"kind":"b","bValue":2,"extra":{"nested":true}}"""
        val expectedB = HybridUnionB(bValue = 2L, additionalProperties = buildJsonObject { put("extra", buildJsonObject { put("nested", true) }) })

        assertEquals(expectedA, strictJson.decodeFromString<HybridUnion>(inputA))
        assertEquals(expectedA, strictJson.decodeFromJsonElement<HybridUnion>(strictJson.parseToJsonElement(inputA)))
        assertEquals(expectedA, strictJson.decodeFromString<HybridUnion>("""{"aValue":"a","extra":"value","kind":"a"}"""))
        assertEquals(expectedB, strictJson.decodeFromString<HybridUnion>(inputB))
        assertEquals(expectedB, strictJson.decodeFromJsonElement<HybridUnion>(strictJson.parseToJsonElement(inputB)))
        assertEquals(expectedB, strictJson.decodeFromString<HybridUnion>("""{"bValue":2,"extra":{"nested":true},"kind":"b"}"""))

        assertEquals("""{"kind":"a","aValue":"a","extra":"value"}""", json.encodeToString<HybridUnion>(expectedA))
        assertEquals(json.parseToJsonElement("""{"kind":"a","aValue":"a","extra":"value"}"""), json.encodeToJsonElement<HybridUnion>(expectedA))
        assertEquals("""{"kind":"b","bValue":2,"extra":{"nested":true}}""", json.encodeToString<HybridUnion>(expectedB))
        assertEquals(json.parseToJsonElement(inputB), json.encodeToJsonElement<HybridUnion>(expectedB))
    }

    @Test
    fun untaggedHybridVariantsKeepFallbackDiscriminatorOutOfBucket() {
        val inputA = """{"type":"UntaggedHybridA","aValue":"a","extra":"value"}"""
        val expectedA = UntaggedHybridA(aValue = "a", additionalProperties = mapOf("extra" to "value"))
        val inputB = """{"type":"UntaggedHybridB","bValue":2,"extra":[1,2]}"""
        val expectedB = UntaggedHybridB(
            bValue = 2L,
            additionalProperties = buildJsonObject { put("extra", buildJsonArray { add(JsonPrimitive(1)); add(JsonPrimitive(2)) }) },
        )

        assertEquals(expectedA, strictJson.decodeFromString<UntaggedHybridUnion>(inputA))
        assertEquals(expectedA, strictJson.decodeFromJsonElement<UntaggedHybridUnion>(strictJson.parseToJsonElement(inputA)))
        assertEquals(expectedA, strictJson.decodeFromString<UntaggedHybridUnion>("""{"aValue":"a","extra":"value","type":"UntaggedHybridA"}"""))
        assertEquals(expectedB, strictJson.decodeFromString<UntaggedHybridUnion>(inputB))
        assertEquals(expectedB, strictJson.decodeFromJsonElement<UntaggedHybridUnion>(strictJson.parseToJsonElement(inputB)))
        assertEquals(expectedB, strictJson.decodeFromString<UntaggedHybridUnion>("""{"bValue":2,"extra":[1,2],"type":"UntaggedHybridB"}"""))

        assertEquals(inputA, json.encodeToString<UntaggedHybridUnion>(expectedA))
        assertEquals(json.parseToJsonElement(inputA), json.encodeToJsonElement<UntaggedHybridUnion>(expectedA))
        assertEquals(inputB, json.encodeToString<UntaggedHybridUnion>(expectedB))
        assertEquals(json.parseToJsonElement(inputB), json.encodeToJsonElement<UntaggedHybridUnion>(expectedB))
    }

    @Test
    fun bucketEntryNamedLikeDeclaredPropertyFailsEncoding() {
        val value = TypedHybrid(id = 1L, additionalProperties = mapOf("name" to Instant.parse("2026-05-05T10:15:30Z")))

        assertFailsWith<SerializationException> { json.encodeToString(value) }
        assertFailsWith<SerializationException> { json.encodeToJsonElement(value) }
    }

    @Test
    fun bucketEntryNamedLikeDiscriminatorFailsEncoding() {
        val declared = HybridUnionA(additionalProperties = mapOf("kind" to "x"))
        val fallback = UntaggedHybridA(additionalProperties = mapOf("type" to "x"))

        assertFailsWith<SerializationException> { json.encodeToString<HybridUnion>(declared) }
        assertFailsWith<SerializationException> { json.encodeToJsonElement<HybridUnion>(declared) }
        assertFailsWith<SerializationException> { json.encodeToString<UntaggedHybridUnion>(fallback) }
        assertFailsWith<SerializationException> { json.encodeToJsonElement<UntaggedHybridUnion>(fallback) }
    }

    @Test
    fun closedObjectDropsUnknownKeys() {
        val decoded = json.decodeFromString<ClosedObject>("""{"id":1,"unknown":"dropped"}""")

        assertEquals(ClosedObject(id = 1L), decoded)
        assertEquals("""{"id":1}""", json.encodeToString(decoded))
    }

    @Test
    fun freeFormMapRoundTripsNestedValues() {
        val value: FreeFormMap = buildJsonObject {
            put("text", "value")
            put("nested", buildJsonObject { put("list", buildJsonArray { add(JsonPrimitive(1)); add(buildJsonObject { put("deep", false) }) }) })
        }

        assertEquals(value, json.decodeFromString<FreeFormMap>(json.encodeToString(value)))
        assertEquals(value, json.decodeFromJsonElement<FreeFormMap>(json.encodeToJsonElement(value)))
    }

    @Test
    fun nullableFreeFormMapRoundTripsNullEntry() {
        val input = """{"present":{"flag":true},"absent":null}"""

        val decoded = json.decodeFromString<NullableFreeFormMap>(input)

        assertEquals(JsonNull, decoded.getValue("absent"))
        assertEquals(input, json.encodeToString(decoded))
        assertEquals(decoded, json.decodeFromJsonElement<NullableFreeFormMap>(json.encodeToJsonElement(decoded)))
    }

    @Test
    fun nestedNameHybridRoundTrips() {
        val decoded = json.decodeFromString<NestedNameHybrid>(
            """{"additionalProperty":{"inner":"a"},"serializer":{"inner":"b"},"extra":{"value":"c"}}"""
        )

        assertEquals(NestedNameHybrid.AdditionalProperty(inner = "a"), decoded.additionalProperty)
        assertEquals(NestedNameHybrid.Serializer(inner = "b"), decoded.serializer)
        assertEquals(mapOf("extra" to NestedNameHybrid.AdditionalProperty_(value = "c")), decoded.additionalProperties)
        assertEquals("""{"additionalProperty":{"inner":"a"},"serializer":{"inner":"b"},"extra":{"value":"c"}}""", json.encodeToString(decoded))
        assertRoundTrip(decoded)
    }

    @Test
    fun emptyBucketEncodesNothingExtra() {
        assertEquals("""{"id":1}""", json.encodeToString(TypedHybrid(id = 1L)))
        assertEquals("""{}""", json.encodeToString(FreeFormHybrid()))
        assertEquals(FreeFormHybrid(), json.decodeFromString<FreeFormHybrid>("""{}"""))
        assertNull(json.decodeFromString<FreeFormHybrid>("""{}""").id)
        assertEquals(JsonObject(emptyMap()), json.decodeFromString<FreeFormHybrid>("""{}""").additionalProperties)
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
        assertEquals("""{}""", json.encodeToString(BucketOnly()))
        assertRoundTrip(bucketOnly)

        val siblings = MapWithSiblings(id = 1L, additionalProperties = mapOf("a" to "1"))
        assertEquals("""{"id":1,"a":"1"}""", json.encodeToString(siblings))
        assertEquals(siblings, json.decodeFromString<MapWithSiblings>("""{"id":1,"a":"1"}"""))
        assertRoundTrip(siblings)

        val nullable = NullableMapWithSiblings(id = 1L, additionalProperties = mapOf("a" to "1"))
        assertEquals("""{"id":1,"a":"1"}""", json.encodeToString(nullable))
        assertEquals(nullable, json.decodeFromString<NullableMapWithSiblings>("""{"id":1,"a":"1"}"""))
        assertNull(json.decodeFromString<NullableMapWithSiblings?>("null"))
    }

    @Test
    fun closedShapesStayPlain() {
        val redeclared = json.decodeFromString<ClosedRedeclaration>("""{"type":"t","value":"v","unknown":1}""")
        assertEquals(ClosedRedeclaration(type = "t", value = "v"), redeclared)
        assertEquals("""{"type":"t","value":"v"}""", json.encodeToString(redeclared))

        val alias: ClosedAlias = ClosedObject(id = 1L)
        assertEquals(alias, json.decodeFromString<ClosedAlias>("""{"id":1,"unknown":"dropped"}"""))
        assertEquals("""{"id":1}""", json.encodeToString(alias))

        val empty: ClosedEmpty = json.parseToJsonElement("""{"any":[1,"two",null]}""")
        assertEquals(empty, json.decodeFromString<ClosedEmpty>(json.encodeToString(empty)))
        assertEquals(JsonPrimitive(1), json.decodeFromString<ClosedEmpty>("1"))
    }

    @Test
    fun nestedHybridValuesRoundTrip() {
        val input = """{"id":1,"child":{"id":2,"deep":"x"},"other":{"flag":true}}"""
        val expected = NestedHybrid(
            id = 1L,
            additionalProperties = mapOf(
                "child" to FreeFormHybrid(id = 2L, additionalProperties = buildJsonObject { put("deep", "x") }),
                "other" to FreeFormHybrid(additionalProperties = buildJsonObject { put("flag", true) }),
            ),
        )

        assertEquals(expected, json.decodeFromString<NestedHybrid>(input))
        assertEquals(expected, json.decodeFromJsonElement<NestedHybrid>(json.parseToJsonElement(input)))
        assertEquals(input, json.encodeToString(expected))
        assertEquals(json.parseToJsonElement(input), json.encodeToJsonElement(expected))
    }

    @Test
    fun hybridCollectionsUseTheClassSerializer() {
        val listInput = """[{"id":1,"created":"2026-05-05T10:15:30Z"},{"id":2}]"""
        val first = TypedHybrid(id = 1L, additionalProperties = mapOf("created" to Instant.parse("2026-05-05T10:15:30Z")))
        val second = TypedHybrid(id = 2L)

        assertEquals(listOf(first, second), json.decodeFromString<List<TypedHybrid>>(listInput))
        assertEquals(listInput, json.encodeToString(listOf(first, second)))

        val mapInput = """{"one":{"id":1,"created":"2026-05-05T10:15:30Z"},"two":{"id":2}}"""
        assertEquals(mapOf("one" to first, "two" to second), json.decodeFromString<Map<String, TypedHybrid>>(mapInput))
        assertEquals(mapInput, json.encodeToString(mapOf("one" to first, "two" to second)))
    }

    @Test
    fun mapPropertiesRoundTrip() {
        val input = """{"meta":{"a":1,"b":[true]},"photos":{"p":{"id":1,"title":"t"}}}"""
        val expected = MapProperties(
            meta = buildJsonObject { put("a", 1); put("b", buildJsonArray { add(JsonPrimitive(true)) }) },
            photos = mapOf("p" to Photo(id = 1, title = "t")),
        )

        assertEquals(expected, json.decodeFromString<MapProperties>(input))
        assertEquals(input, json.encodeToString(expected))
        assertEquals("""{}""", json.encodeToString(MapProperties()))
        assertRoundTrip(expected)
    }

    @Test
    fun sealedSiblingBucketIsInheritedByVariants() {
        val input1 = """{"kind":"one","one":1,"extra":"value"}"""
        val expected1 = SealedHybridChild1(one = 1L, additionalProperties = mapOf("extra" to "value"))
        val input2 = """{"kind":"two","two":"t","extra":{"nested":1}}"""
        val expected2 = SealedHybridChild2(two = "t", additionalProperties = buildJsonObject { put("extra", buildJsonObject { put("nested", 1) }) })

        assertEquals(expected1, strictJson.decodeFromString<SealedHybridBase>(input1))
        assertEquals(expected1, strictJson.decodeFromJsonElement<SealedHybridBase>(strictJson.parseToJsonElement(input1)))
        assertEquals(expected1, strictJson.decodeFromString<SealedHybridBase>("""{"one":1,"extra":"value","kind":"one"}"""))
        assertEquals(expected2, strictJson.decodeFromString<SealedHybridBase>(input2))
        assertEquals(expected2, strictJson.decodeFromJsonElement<SealedHybridBase>(strictJson.parseToJsonElement(input2)))
        assertEquals(expected2, strictJson.decodeFromString<SealedHybridBase>("""{"two":"t","extra":{"nested":1},"kind":"two"}"""))

        assertEquals(input1, json.encodeToString<SealedHybridBase>(expected1))
        assertEquals(json.parseToJsonElement(input1), json.encodeToJsonElement<SealedHybridBase>(expected1))
        assertEquals(input2, json.encodeToString<SealedHybridBase>(expected2))
        assertEquals(json.parseToJsonElement(input2), json.encodeToJsonElement<SealedHybridBase>(expected2))

        assertFailsWith<SerializationException> {
            json.encodeToString<SealedHybridBase>(SealedHybridChild1(additionalProperties = mapOf("kind" to "x")))
        }
    }
}
