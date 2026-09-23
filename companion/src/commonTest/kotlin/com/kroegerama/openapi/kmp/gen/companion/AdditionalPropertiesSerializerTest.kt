package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AdditionalPropertiesSerializerTest {

    @Serializable(with = TypedHybrid.Serializer::class)
    @KeepGeneratedSerializer
    data class TypedHybrid(
        @SerialName("id")
        val id: Long,
        @SerialName("name")
        val name: String? = null,
        @SerialName("additionalProperties")
        val additionalProperties: Map<String, String> = emptyMap(),
    ) {
        object Serializer : AdditionalPropertiesSerializer<TypedHybrid>(
            tSerializer = generatedSerializer(),
            bucketName = "additionalProperties",
        )
    }

    @Serializable(with = FreeFormHybrid.Serializer::class)
    @KeepGeneratedSerializer
    data class FreeFormHybrid(
        @SerialName("id")
        val id: Long,
        @SerialName("additionalProperties")
        val additionalProperties: JsonObject = JsonObject(emptyMap()),
    ) {
        object Serializer : AdditionalPropertiesSerializer<FreeFormHybrid>(
            tSerializer = generatedSerializer(),
            bucketName = "additionalProperties",
        )
    }

    @Serializable(with = AliasHybrid.Serializer::class)
    @KeepGeneratedSerializer
    data class AliasHybrid(
        @SerialName("id")
        @JsonNames("identifier", "ident")
        val id: Long,
        @SerialName("additionalProperties")
        val additionalProperties: Map<String, String> = emptyMap(),
    ) {
        object Serializer : AdditionalPropertiesSerializer<AliasHybrid>(
            tSerializer = generatedSerializer(),
            bucketName = "additionalProperties",
        )
    }

    @Serializable(with = IgnoringHybrid.Serializer::class)
    @KeepGeneratedSerializer
    data class IgnoringHybrid(
        @SerialName("id")
        val id: Long,
        @SerialName("additionalProperties")
        val additionalProperties: Map<String, String> = emptyMap(),
    ) {
        object Serializer : AdditionalPropertiesSerializer<IgnoringHybrid>(
            tSerializer = generatedSerializer(),
            bucketName = "additionalProperties",
            ignoredKeys = setOf("kind"),
        )
    }

    @Serializable(with = NullableBucketHybrid.Serializer::class)
    @KeepGeneratedSerializer
    data class NullableBucketHybrid(
        @SerialName("id")
        val id: Long,
        @SerialName("additionalProperties")
        val additionalProperties: Map<String, String>? = null,
    ) {
        object Serializer : AdditionalPropertiesSerializer<NullableBucketHybrid>(
            tSerializer = generatedSerializer(),
            bucketName = "additionalProperties",
        )
    }

    @Serializable(with = RequiredBucketHybrid.Serializer::class)
    @KeepGeneratedSerializer
    data class RequiredBucketHybrid(
        @SerialName("id")
        val id: Long,
        @SerialName("additionalProperties")
        val additionalProperties: Map<String, String>,
    ) {
        object Serializer : AdditionalPropertiesSerializer<RequiredBucketHybrid>(
            tSerializer = generatedSerializer(),
            bucketName = "additionalProperties",
        )
    }

    @Serializable(with = DeclaredIgnoredHybrid.Serializer::class)
    @KeepGeneratedSerializer
    data class DeclaredIgnoredHybrid(
        @SerialName("id")
        val id: Long,
        @SerialName("kind")
        val kind: String? = null,
        @SerialName("additionalProperties")
        val additionalProperties: Map<String, String> = emptyMap(),
    ) {
        object Serializer : AdditionalPropertiesSerializer<DeclaredIgnoredHybrid>(
            tSerializer = generatedSerializer(),
            bucketName = "additionalProperties",
            ignoredKeys = setOf("kind"),
        )
    }

    @Serializable
    data class Plain(
        @SerialName("id")
        val id: Long,
    )

    @Serializable
    data class StringBucket(
        @SerialName("id")
        val id: Long,
        @SerialName("additionalProperties")
        val additionalProperties: String = "",
    )

    private val json = createDefaultJson()

    @Test
    fun extraKeysAreCapturedAndReEmitted() {
        val input = """{"id":1,"name":"a","x":"1","y":"2"}"""
        val decoded = json.decodeFromString<TypedHybrid>(input)
        assertEquals(TypedHybrid(id = 1, name = "a", additionalProperties = mapOf("x" to "1", "y" to "2")), decoded)

        val encoded = json.encodeToJsonElement(decoded) as JsonObject
        assertEquals(JsonPrimitive(1), encoded["id"])
        assertEquals(JsonPrimitive("a"), encoded["name"])
        assertEquals(JsonPrimitive("1"), encoded["x"])
        assertEquals(JsonPrimitive("2"), encoded["y"])
        assertFalse("additionalProperties" in encoded)
        assertEquals(input, json.encodeToString(decoded))
    }

    @Test
    fun roundTripWithoutExtraKeys() {
        val value = TypedHybrid(id = 7)
        val encoded = json.encodeToString(value)
        assertEquals("""{"id":7}""", encoded)
        assertEquals(value, json.decodeFromString<TypedHybrid>(encoded))
    }

    @Test
    fun freeFormBucketKeepsNestedValues() {
        val input = """{"id":1,"nested":{"a":[1,2,null]},"flag":true,"nothing":null}"""
        val decoded = json.decodeFromString<FreeFormHybrid>(input)
        val expectedBucket = buildJsonObject {
            put("nested", buildJsonObject { put("a", Json.parseToJsonElement("[1,2,null]")) })
            put("flag", true)
            put("nothing", null as String?)
        }
        assertEquals(FreeFormHybrid(id = 1, additionalProperties = expectedBucket), decoded)
        assertEquals(input, json.encodeToString(decoded))
        assertEquals(decoded, json.decodeFromJsonElement<FreeFormHybrid>(json.encodeToJsonElement(decoded)))
    }

    @Test
    fun incomingKeyEqualToBucketNameLandsInBucket() {
        val input = """{"id":1,"additionalProperties":"inner"}"""
        val decoded = json.decodeFromString<TypedHybrid>(input)
        assertEquals(mapOf("additionalProperties" to "inner"), decoded.additionalProperties)
        assertEquals(input, json.encodeToString(decoded))
        assertEquals(decoded, json.decodeFromString<TypedHybrid>(json.encodeToString(decoded)))
    }

    @Test
    fun collisionWithDeclaredKeyThrowsOnEncode() {
        val value = TypedHybrid(id = 1, additionalProperties = mapOf("name" to "clash"))
        val failure = assertFailsWith<SerializationException> { json.encodeToString(value) }
        assertEquals(
            "additional property 'name' collides with a declared property of ${TypedHybrid.generatedSerializer().descriptor.serialName}",
            failure.message,
        )
        assertFailsWith<SerializationException> { json.encodeToJsonElement(value) }
    }

    @Test
    fun collisionWithAliasThrowsOnEncode() {
        val value = AliasHybrid(id = 1, additionalProperties = mapOf("ident" to "clash"))
        assertFailsWith<SerializationException> { json.encodeToString(value) }
    }

    @Test
    fun collisionWithIgnoredKeyThrowsOnEncode() {
        val value = IgnoringHybrid(id = 1, additionalProperties = mapOf("kind" to "clash"))
        val failure = assertFailsWith<SerializationException> { json.encodeToString(value) }
        assertEquals(
            "additional property 'kind' collides with an ignored key of ${IgnoringHybrid.generatedSerializer().descriptor.serialName}",
            failure.message,
        )
    }

    @Test
    fun nullableBucketDecodesToEmptyMapWithoutExtraKeys() {
        val decoded = json.decodeFromString<NullableBucketHybrid>("""{"id":1}""")
        assertEquals(NullableBucketHybrid(id = 1, additionalProperties = emptyMap()), decoded)
        assertEquals("""{"id":1}""", json.encodeToString(decoded))

        val withExtra = json.decodeFromString<NullableBucketHybrid>("""{"id":1,"x":"1"}""")
        assertEquals(NullableBucketHybrid(id = 1, additionalProperties = mapOf("x" to "1")), withExtra)
        assertEquals("""{"id":1,"x":"1"}""", json.encodeToString(withExtra))
    }

    @Test
    fun nullBucketEncodesNothingExtra() {
        val explicit = Json(json) { explicitNulls = true }
        val value = NullableBucketHybrid(id = 1, additionalProperties = null)
        assertEquals("""{"id":1}""", json.encodeToString(value))
        assertEquals("""{"id":1}""", explicit.encodeToString(value))
        assertEquals("""{"id":1}""", explicit.encodeToJsonElement(value).toString())
    }

    @Test
    fun requiredBucketIsSatisfiedWithoutExtraKeys() {
        val decoded = json.decodeFromString<RequiredBucketHybrid>("""{"id":1}""")
        assertEquals(RequiredBucketHybrid(id = 1, additionalProperties = emptyMap()), decoded)
        assertEquals("""{"id":1}""", json.encodeToString(decoded))
        assertEquals(decoded, json.decodeFromJsonElement<RequiredBucketHybrid>(json.encodeToJsonElement(decoded)))
    }

    @Test
    fun declaredPropertyNamedLikeIgnoredKeyIsDropped() {
        val strict = Json(json) { ignoreUnknownKeys = false }
        val decoded = strict.decodeFromString<DeclaredIgnoredHybrid>("""{"id":1,"kind":"thing","extra":"e"}""")
        assertEquals(DeclaredIgnoredHybrid(id = 1, kind = null, additionalProperties = mapOf("extra" to "e")), decoded)

        val fromTree = strict.decodeFromJsonElement<DeclaredIgnoredHybrid>(Json.parseToJsonElement("""{"id":1,"kind":"thing"}"""))
        assertEquals(DeclaredIgnoredHybrid(id = 1), fromTree)
    }

    @Test
    fun nonObjectInputFailsInPluginSerializer() {
        assertFailsWith<SerializationException> { json.decodeFromString<TypedHybrid>("[1,2]") }
        assertFailsWith<SerializationException> { json.decodeFromString<TypedHybrid>("\"text\"") }
        assertFailsWith<SerializationException> { json.decodeFromJsonElement<TypedHybrid>(JsonPrimitive(3)) }
    }

    @Test
    fun aliasIsDecodedIntoDeclaredProperty() {
        val decoded = json.decodeFromString<AliasHybrid>("""{"identifier":5,"extra":"e"}""")
        assertEquals(AliasHybrid(id = 5, additionalProperties = mapOf("extra" to "e")), decoded)
        assertEquals("""{"id":5,"extra":"e"}""", json.encodeToString(decoded))
    }

    @Test
    fun ignoredKeyIsNeitherCapturedNorForwarded() {
        val strict = Json(json) { ignoreUnknownKeys = false }
        val decoded = strict.decodeFromString<IgnoringHybrid>("""{"kind":"thing","id":2,"extra":"e"}""")
        assertEquals(IgnoringHybrid(id = 2, additionalProperties = mapOf("extra" to "e")), decoded)
        assertEquals("""{"id":2,"extra":"e"}""", strict.encodeToString(decoded))

        val fromTree = strict.decodeFromJsonElement<IgnoringHybrid>(Json.parseToJsonElement("""{"id":2,"kind":"thing"}"""))
        assertEquals(IgnoringHybrid(id = 2), fromTree)
    }

    @Test
    fun unknownBucketNameFailsConstruction() {
        val failure = assertFailsWith<IllegalArgumentException> {
            AdditionalPropertiesSerializer(Plain.serializer(), bucketName = "missing")
        }
        assertEquals("${Plain.serializer().descriptor.serialName} has no property 'missing'", failure.message)
    }

    @Test
    fun nonMapBucketFailsConstruction() {
        val failure = assertFailsWith<IllegalArgumentException> {
            AdditionalPropertiesSerializer(StringBucket.serializer(), bucketName = "additionalProperties")
        }
        assertEquals(
            "${StringBucket.serializer().descriptor.serialName}.additionalProperties must be a map, but has kind STRING",
            failure.message,
        )
    }
}
