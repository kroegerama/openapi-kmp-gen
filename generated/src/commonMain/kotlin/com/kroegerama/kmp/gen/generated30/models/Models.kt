/* 
 * NOTE: This file is auto generated. Do not edit the file manually!
 * 
 * Test API
 * Test API Description
 * Version 1.0.0-SNAPSHOT
 * 
 * Generated Mon, 1 Jun 2026 13:00:00 GMT
 * OpenAPI KMP Gen (version 1.6.1) by kroegerama
 */
@file:Suppress("ArrayInDataClass", "RedundantVisibilityModifier", "unused", "ConstPropertyName")

package com.kroegerama.kmp.gen.generated30.models

import androidx.compose.runtime.Immutable
import com.kroegerama.openapi.kmp.gen.`companion`.AdditionalPropertiesSerializer
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableBase64
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableEpochMilliseconds
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableEpochSeconds
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableISO8601Instant
import kotlin.Boolean
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.emptyList
import kotlin.collections.emptyMap
import kotlin.collections.setOf
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * @param attr1 nullable=false -> not null
 * @param attr2 required -> not null
 * @param attr3 nullable=true -> nullable
 * @param attr4 required + nullable=true -> nullable
 */
@Serializable
@Immutable
public data class NullableAttrTest(
  /**
   * nullable=false -> not null
   */
  @SerialName("attr1")
  public val attr1: Photo,
  /**
   * required -> not null
   */
  @SerialName("attr2")
  public val attr2: Photo,
  /**
   * nullable=true -> nullable
   */
  @SerialName("attr3")
  public val attr3: Photo? = null,
  /**
   * required + nullable=true -> nullable
   */
  @SerialName("attr4")
  public val attr4: Photo? = null,
  @SerialName("attr5")
  public val attr5: NullableInlineObject? = null,
)

/**
 * nullable inline object -> class, nullable at reference sites
 */
@Serializable
@Immutable
public data class NullableInlineObject(
  @SerialName("value")
  public val `value`: String? = null,
)

@Serializable
@Immutable
public data class Photo(
  @SerialName("albumId")
  public val albumId: Int? = null,
  @SerialName("id")
  public val id: Int? = null,
  @SerialName("title")
  public val title: String? = null,
  @SerialName("url")
  public val url: String? = null,
  @SerialName("thumbnailUrl")
  public val thumbnailUrl: String? = null,
)

@Serializable
@Immutable
public data class SerialTest(
  @SerialName("date")
  public val date: LocalDate? = null,
  @SerialName("time")
  public val time: LocalTime? = null,
  @SerialName("instant")
  public val instant: SerializableISO8601Instant? = null,
  @SerialName("duration")
  public val duration: Duration? = null,
  @SerialName("uuid")
  public val uuid: Uuid? = null,
  @SerialName("base64")
  public val base64: SerializableBase64? = null,
  @SerialName("epochSeconds")
  public val epochSeconds: SerializableEpochSeconds? = null,
  @SerialName("epochMillis")
  public val epochMillis: SerializableEpochMilliseconds? = null,
)

@Serializable
@Immutable
public data class IntegerTest(
  @SerialName("unknown")
  public val unknown: Long? = null,
  @SerialName("int32")
  public val int32: Int? = null,
  @SerialName("int64")
  public val int64: Long? = null,
  @SerialName("float")
  public val float: Float? = null,
  @SerialName("double")
  public val double: Double? = null,
)

@Serializable
@Immutable
public data class NumberTest(
  @SerialName("unknown")
  public val unknown: Double? = null,
  @SerialName("float")
  public val float: Float? = null,
  @SerialName("double")
  public val double: Double? = null,
  @SerialName("int32")
  public val int32: Int? = null,
  @SerialName("int64")
  public val int64: Long? = null,
)

@Serializable
@Immutable
public data class DefaultValue(
  @SerialName("nullableString")
  public val nullableString: String? = null,
  @SerialName("nullableList")
  public val nullableList: List<String>? = null,
  @SerialName("requiredList")
  public val requiredList: List<String> = emptyList(),
  @SerialName("requiredMap")
  public val requiredMap: Map<String, String> = emptyMap(),
)

@Serializable
@Immutable
public enum class SealedClassType {
  @SerialName("C1")
  C_1,
  @SerialName("C2")
  C_2,
}

@Serializable
@Immutable
@JsonClassDiscriminator("#discriminator")
public sealed interface SealedClass1

@Serializable
@Immutable
@SerialName("C1")
public data class SealedClass1Child1(
  @SerialName("commonAttr")
  public val commonAttr: String? = null,
  @SerialName("child1Only")
  public val child1Only: Long? = null,
) : SealedClass1

@Serializable
@Immutable
@SerialName("C2")
public data class SealedClass1Child2(
  @SerialName("commonAttr")
  public val commonAttr: String? = null,
  @SerialName("child2Only")
  public val child2Only: String? = null,
) : SealedClass1

@Serializable
@Immutable
@JsonClassDiscriminator("#discriminator")
public sealed interface SealedClass2

@Serializable
@Immutable
@SerialName("C1")
public data class SealedClass2Child1(
  @SerialName("commonAttr")
  public val commonAttr: String? = null,
  @SerialName("child1Only")
  public val child1Only: Long? = null,
) : SealedClass2

@Serializable
@Immutable
@SerialName("C2")
public data class SealedClass2Child2(
  @SerialName("commonAttr")
  public val commonAttr: String? = null,
  @SerialName("child2Only")
  public val child2Only: String? = null,
) : SealedClass2

/**
 * anyOf without discriminator -> object with merged properties
 */
@Serializable
@Immutable
public data class CombinedAnyOf(
  @SerialName("albumId")
  public val albumId: Int? = null,
  @SerialName("id")
  public val id: Int? = null,
  @SerialName("title")
  public val title: String? = null,
  @SerialName("url")
  public val url: String? = null,
  @SerialName("thumbnailUrl")
  public val thumbnailUrl: String? = null,
  @SerialName("combinedExtraAttr")
  public val combinedExtraAttr: String? = null,
)

/**
 * single allOf with sibling properties -> object with merged properties
 */
@Serializable
@Immutable
public data class ExtendedPhoto(
  @SerialName("albumId")
  public val albumId: Int? = null,
  @SerialName("id")
  public val id: Int? = null,
  @SerialName("title")
  public val title: String? = null,
  @SerialName("url")
  public val url: String? = null,
  @SerialName("thumbnailUrl")
  public val thumbnailUrl: String? = null,
  @SerialName("extendedAttr")
  public val extendedAttr: String? = null,
)

@Serializable
@Immutable
@JsonClassDiscriminator("kind")
public sealed interface ActionResponse

@Serializable
@Immutable
@SerialName("create")
public data class CreateActionResponse(
  @SerialName("itemId")
  public val itemId: String,
) : ActionResponse

@Serializable
@Immutable
@SerialName("update")
public data class UpdateActionResponse(
  @SerialName("oldItemId")
  public val oldItemId: String,
  @SerialName("newItemId")
  public val newItemId: String,
) : ActionResponse

@Serializable
@Immutable
@SerialName("delete")
public data class DeleteActionResponse(
  @SerialName("itemId")
  public val itemId: String,
) : ActionResponse

@Serializable
@Immutable
@SerialName("create")
public data class CreateEventNotification(
  @SerialName("eventId")
  public val eventId: String,
) : EventNotification

@Serializable
@Immutable
@SerialName("delete")
public data class DeleteEventNotification(
  @SerialName("eventId")
  public val eventId: String,
) : EventNotification

@Serializable
@Immutable
@JsonClassDiscriminator("kind")
public sealed interface EventNotification

@Serializable
@Immutable
public data class AuditedAction(
  @SerialName("auditId")
  public val auditId: String,
)

/**
 * additionalProperties false -> plain class without bucket
 */
@Serializable
@Immutable
public data class ClosedObject(
  @SerialName("id")
  public val id: Long? = null,
)

/**
 * typed bucket with a companion serializer for the values
 */
@Serializable(with = TypedHybrid.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class TypedHybrid(
  @SerialName("id")
  public val id: Long,
  @SerialName("name")
  public val name: String? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, SerializableISO8601Instant> = emptyMap(),
) {
  public object Serializer : AdditionalPropertiesSerializer<TypedHybrid>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * free-form bucket next to declared properties
 */
@Serializable(with = FreeFormHybrid.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class FreeFormHybrid(
  @SerialName("id")
  public val id: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: JsonObject = JsonObject(emptyMap()),
) {
  public object Serializer : AdditionalPropertiesSerializer<FreeFormHybrid>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * bucket with nullable values
 */
@Serializable(with = NullableValueHybrid.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class NullableValueHybrid(
  @SerialName("id")
  public val id: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, String?> = emptyMap(),
) {
  public object Serializer : AdditionalPropertiesSerializer<NullableValueHybrid>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * declared nested types occupy the default names of the bucket value type and the serializer object
 */
@Serializable(with = NestedNameHybrid.Serializer_::class)
@KeepGeneratedSerializer
@Immutable
public data class NestedNameHybrid(
  @SerialName("additionalProperty")
  public val additionalProperty: AdditionalProperty? = null,
  @SerialName("serializer")
  public val serializer: Serializer? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, AdditionalProperty_> = emptyMap(),
) {
  @Serializable
  @Immutable
  public data class AdditionalProperty(
    @SerialName("inner")
    public val `inner`: String? = null,
  )

  @Serializable
  @Immutable
  public data class Serializer(
    @SerialName("inner")
    public val `inner`: String? = null,
  )

  @Serializable
  @Immutable
  public data class AdditionalProperty_(
    @SerialName("value")
    public val `value`: String? = null,
  )

  public object Serializer_ : AdditionalPropertiesSerializer<NestedNameHybrid>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * bucket inherited through allOf
 */
@Serializable(with = InheritedHybrid.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class InheritedHybrid(
  @SerialName("id")
  public val id: Long,
  @SerialName("name")
  public val name: String? = null,
  @SerialName("extra")
  public val extra: Boolean? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, SerializableISO8601Instant> = emptyMap(),
) {
  public object Serializer : AdditionalPropertiesSerializer<InheritedHybrid>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * later allOf member overrides the inherited bucket
 */
@Serializable(with = OverridingHybrid.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class OverridingHybrid(
  @SerialName("id")
  public val id: Long,
  @SerialName("name")
  public val name: String? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, Long> = emptyMap(),
) {
  public object Serializer : AdditionalPropertiesSerializer<OverridingHybrid>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * inline object value type -> nested AdditionalProperty class
 */
@Serializable(with = ObjectValueHybrid.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class ObjectValueHybrid(
  @SerialName("id")
  public val id: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, AdditionalProperty> = emptyMap(),
) {
  @Serializable
  @Immutable
  public data class AdditionalProperty(
    @SerialName("label")
    public val label: String? = null,
    @SerialName("count")
    public val count: Long? = null,
  )

  public object Serializer : AdditionalPropertiesSerializer<ObjectValueHybrid>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * declared property occupies the default bucket name
 */
@Serializable(with = CollidingHybrid.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class CollidingHybrid(
  @SerialName("id")
  public val id: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: String? = null,
  @SerialName("additionalProperties_")
  public val additionalProperties_: Map<String, String> = emptyMap(),
) {
  public object Serializer : AdditionalPropertiesSerializer<CollidingHybrid>(tSerializer = generatedSerializer(), bucketName = "additionalProperties_")
}

/**
 * discriminated oneOf whose variants are hybrids
 */
@Serializable
@Immutable
@JsonClassDiscriminator("kind")
public sealed interface HybridUnion

@Serializable(with = HybridUnionA.Serializer::class)
@KeepGeneratedSerializer
@Immutable
@SerialName("a")
public data class HybridUnionA(
  @SerialName("aValue")
  public val aValue: String? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, String> = emptyMap(),
) : HybridUnion {
  public object Serializer : AdditionalPropertiesSerializer<HybridUnionA>(tSerializer = generatedSerializer(), bucketName = "additionalProperties", ignoredKeys = setOf("kind"))
}

@Serializable(with = HybridUnionB.Serializer::class)
@KeepGeneratedSerializer
@Immutable
@SerialName("b")
public data class HybridUnionB(
  @SerialName("bValue")
  public val bValue: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: JsonObject = JsonObject(emptyMap()),
) : HybridUnion {
  public object Serializer : AdditionalPropertiesSerializer<HybridUnionB>(tSerializer = generatedSerializer(), bucketName = "additionalProperties", ignoredKeys = setOf("kind"))
}

/**
 * oneOf without discriminator whose variants are hybrids
 */
@Serializable
@Immutable
@JsonClassDiscriminator("type")
public sealed interface UntaggedHybridUnion

@Serializable(with = UntaggedHybridA.Serializer::class)
@KeepGeneratedSerializer
@Immutable
@SerialName("UntaggedHybridA")
public data class UntaggedHybridA(
  @SerialName("aValue")
  public val aValue: String? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, String> = emptyMap(),
) : UntaggedHybridUnion {
  public object Serializer : AdditionalPropertiesSerializer<UntaggedHybridA>(tSerializer = generatedSerializer(), bucketName = "additionalProperties", ignoredKeys = setOf("type"))
}

@Serializable(with = UntaggedHybridB.Serializer::class)
@KeepGeneratedSerializer
@Immutable
@SerialName("UntaggedHybridB")
public data class UntaggedHybridB(
  @SerialName("bValue")
  public val bValue: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: JsonObject = JsonObject(emptyMap()),
) : UntaggedHybridUnion {
  public object Serializer : AdditionalPropertiesSerializer<UntaggedHybridB>(tSerializer = generatedSerializer(), bucketName = "additionalProperties", ignoredKeys = setOf("type"))
}

@Serializable
@Immutable
public data class TypedThing(
  @SerialName("type")
  public val type: String? = null,
  @SerialName("value")
  public val `value`: String? = null,
)

/**
 * inline hybrid property -> nested class with its own serializer object
 */
@Serializable
@Immutable
public data class HybridOwner(
  @SerialName("inline")
  public val `inline`: Inline? = null,
) {
  @Serializable(with = Inline.Serializer::class)
  @KeepGeneratedSerializer
  @Immutable
  public data class Inline(
    @SerialName("id")
    public val id: Long? = null,
    @SerialName("additionalProperties")
    public val additionalProperties: Map<String, String> = emptyMap(),
  ) {
    public object Serializer : AdditionalPropertiesSerializer<Inline>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
  }
}

/**
 * allOf of two maps -> data class with only a bucket, later member wins
 */
@Serializable(with = BucketOnly.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class BucketOnly(
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, String> = emptyMap(),
) {
  public object Serializer : AdditionalPropertiesSerializer<BucketOnly>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * map ref with sibling properties -> hybrid
 */
@Serializable(with = MapWithSiblings.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class MapWithSiblings(
  @SerialName("id")
  public val id: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, String> = emptyMap(),
) {
  public object Serializer : AdditionalPropertiesSerializer<MapWithSiblings>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * hybrid values inside a bucket
 */
@Serializable(with = NestedHybrid.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class NestedHybrid(
  @SerialName("id")
  public val id: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, FreeFormHybrid> = emptyMap(),
) {
  public object Serializer : AdditionalPropertiesSerializer<NestedHybrid>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * free-form and typed map properties
 */
@Serializable
@Immutable
public data class MapProperties(
  @SerialName("meta")
  public val meta: JsonObject? = null,
  @SerialName("photos")
  public val photos: Map<String, Photo>? = null,
)

/**
 * sealed schema with an additionalProperties sibling -> variants inherit the bucket
 */
@Serializable
@Immutable
@JsonClassDiscriminator("kind")
public sealed interface SealedHybridBase

@Serializable(with = SealedHybridChild1.Serializer::class)
@KeepGeneratedSerializer
@Immutable
@SerialName("one")
public data class SealedHybridChild1(
  @SerialName("one")
  public val one: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, String> = emptyMap(),
) : SealedHybridBase {
  public object Serializer : AdditionalPropertiesSerializer<SealedHybridChild1>(tSerializer = generatedSerializer(), bucketName = "additionalProperties", ignoredKeys = setOf("kind"))
}

@Serializable(with = SealedHybridChild2.Serializer::class)
@KeepGeneratedSerializer
@Immutable
@SerialName("two")
public data class SealedHybridChild2(
  @SerialName("two")
  public val two: String? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: JsonObject = JsonObject(emptyMap()),
) : SealedHybridBase {
  public object Serializer : AdditionalPropertiesSerializer<SealedHybridChild2>(tSerializer = generatedSerializer(), bucketName = "additionalProperties", ignoredKeys = setOf("kind"))
}

@Serializable(with = HybridBodyRequest.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class HybridBodyRequest(
  @SerialName("id")
  public val id: Long,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, String> = emptyMap(),
) {
  public object Serializer : AdditionalPropertiesSerializer<HybridBodyRequest>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

@Serializable(with = HybridBody200Response.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class HybridBody200Response(
  @SerialName("count")
  public val count: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: JsonObject = JsonObject(emptyMap()),
) {
  public object Serializer : AdditionalPropertiesSerializer<HybridBody200Response>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

public typealias DateTime = SerializableISO8601Instant

/**
 * Nullable Photo via allOf
 */
public typealias NullableTestAllOfTypealias = Photo?

public typealias RefTypealias = Photo

/**
 * additionalProperties true -> JsonObject typealias
 */
public typealias FreeFormMap = JsonObject

/**
 * empty value schema -> JsonObject typealias
 */
public typealias EmptySchemaMap = JsonObject

/**
 * nullable empty value schema -> JsonObject typealias
 */
public typealias NullableFreeFormMap = JsonObject

/**
 * typed map referenced by the bucket-only fixtures
 */
public typealias StringMap = Map<String, String>

/**
 * additionalProperties false without properties -> JsonElement
 */
public typealias ClosedEmpty = JsonElement

/**
 * additionalProperties false next to a single ref -> typealias
 */
public typealias ClosedAlias = ClosedObject
