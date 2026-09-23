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

package com.kroegerama.kmp.gen.generated31.models

import androidx.compose.runtime.Immutable
import com.kroegerama.openapi.kmp.gen.`companion`.AdditionalPropertiesSerializer
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableBase64
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableEpochMilliseconds
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableEpochSeconds
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableISO8601Instant
import kotlin.Boolean
import kotlin.Deprecated
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
 * @param attr1 required -> not null
 * @param attr2 required allOf -> not null
 * @param attr3 has null type -> nullable
 * @param attr4 required oneOf with null type -> nullable
 * @param attr5 required oneOf with null type and primitive -> nullable String
 * @param attr6 required oneOf with null type and inline object -> nullable generated class
 * @param attr7 required oneOf with null type around single allOf -> nullable
 * @param attr8 required with null in type list -> nullable
 * @param attr9 required array with nullable items via oneOf
 * @param attr10 required ref to intrinsically nullable named schema -> nullable
 * @param attr11 required ref to nullable schema with merged properties -> nullable
 * @param attr12 deprecated oneOf variant -> deprecated nullable property
 */
@Serializable
@Immutable
public data class NullableAttrTest(
  /**
   * required -> not null
   */
  @SerialName("attr1")
  public val attr1: Photo,
  /**
   * required allOf -> not null
   */
  @SerialName("attr2")
  public val attr2: Photo,
  /**
   * has null type -> nullable
   */
  @SerialName("attr3")
  public val attr3: Photo? = null,
  /**
   * required oneOf with null type -> nullable
   */
  @SerialName("attr4")
  public val attr4: Photo? = null,
  /**
   * required oneOf with null type and primitive -> nullable String
   */
  @SerialName("attr5")
  public val attr5: String? = null,
  /**
   * required oneOf with null type and inline object -> nullable generated class
   */
  @SerialName("attr6")
  public val attr6: Attr6? = null,
  /**
   * required oneOf with null type around single allOf -> nullable
   */
  @SerialName("attr7")
  public val attr7: Photo? = null,
  /**
   * required with null in type list -> nullable
   */
  @SerialName("attr8")
  public val attr8: String? = null,
  /**
   * required array with nullable items via oneOf
   */
  @SerialName("attr9")
  public val attr9: List<Photo?> = emptyList(),
  /**
   * required ref to intrinsically nullable named schema -> nullable
   */
  @SerialName("attr10")
  public val attr10: NullableInlineObject? = null,
  /**
   * required ref to nullable schema with merged properties -> nullable
   */
  @SerialName("attr11")
  public val attr11: NullableExtendedPhoto? = null,
  /**
   * deprecated oneOf variant -> deprecated nullable property
   */
  @SerialName("attr12")
  @Deprecated("Deprecated via OpenAPI Spec")
  public val attr12: Photo? = null,
) {
  @Serializable
  @Immutable
  public data class Attr6(
    @SerialName("innerAttr")
    public val innerAttr: String? = null,
  )
}

/**
 * oneOf with null type and sibling properties -> object with merged properties
 */
@Serializable
@Immutable
public data class NullableExtendedPhoto(
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
  @SerialName("siblingAttr")
  public val siblingAttr: String? = null,
)

/**
 * variant required list is merged -> innerRequired stays non-null
 */
@Serializable
@Immutable
public data class MergedRequiredTest(
  @SerialName("innerRequired")
  public val innerRequired: String,
  @SerialName("outerAttr")
  public val outerAttr: String? = null,
)

/**
 * named oneOf with null type and inline object -> class, nullable at reference sites
 */
@Serializable
@Immutable
public data class NullableInlineObject(
  @SerialName("value")
  public val `value`: String? = null,
)

/**
 * allOf member wrapped in oneOf with null type -> object with merged properties
 */
@Serializable
@Immutable
public data class MergedNullableMember(
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
  @SerialName("mergedAttr")
  public val mergedAttr: String? = null,
)

/**
 * anyOf without null type -> object with merged properties
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

/**
 * oneOf with null type and multiple variants -> sealed interface without extra null child
 */
@Serializable
@Immutable
@JsonClassDiscriminator("kind")
public sealed interface NullableUnion

@Serializable
@Immutable
@SerialName("a")
public data class NullableUnionChildA(
  @SerialName("aValue")
  public val aValue: String? = null,
) : NullableUnion

@Serializable
@Immutable
@SerialName("b")
public data class NullableUnionChildB(
  @SerialName("bValue")
  public val bValue: Int? = null,
) : NullableUnion

/**
 * deprecated oneOf -> deprecated sealed interface
 */
@Serializable
@Deprecated("Deprecated via OpenAPI Spec")
@Immutable
@JsonClassDiscriminator("kind")
public sealed interface DeprecatedUnion

@Serializable
@Immutable
@SerialName("DeprecatedUnionChildA")
public data class DeprecatedUnionChildA(
  @SerialName("aValue")
  public val aValue: String? = null,
) : DeprecatedUnion

@Serializable
@Immutable
@SerialName("DeprecatedUnionChildB")
public data class DeprecatedUnionChildB(
  @SerialName("bValue")
  public val bValue: String? = null,
) : DeprecatedUnion

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
) : EventNotification,
    NullableEventNotification

@Serializable
@Immutable
@SerialName("delete")
public data class DeleteEventNotification(
  @SerialName("eventId")
  public val eventId: String,
) : EventNotification,
    NullableEventNotification

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

@Serializable
@Immutable
@JsonClassDiscriminator("kind")
public sealed interface NullableEventNotification

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
 * nullable wrapper around a map with sibling properties -> hybrid, nullable at reference sites
 */
@Serializable(with = NullableMapWithSiblings.Serializer::class)
@KeepGeneratedSerializer
@Immutable
public data class NullableMapWithSiblings(
  @SerialName("id")
  public val id: Long? = null,
  @SerialName("additionalProperties")
  public val additionalProperties: Map<String, String> = emptyMap(),
) {
  public object Serializer : AdditionalPropertiesSerializer<NullableMapWithSiblings>(tSerializer = generatedSerializer(), bucketName = "additionalProperties")
}

/**
 * property redeclared with additionalProperties false keeps the inherited type
 */
@Serializable
@Immutable
public data class ClosedRedeclaration(
  @SerialName("type")
  public val type: String? = null,
  @SerialName("value")
  public val `value`: String? = null,
)

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

@Serializable
@Immutable
public data class NullableResponse200Response(
  @SerialName("payload")
  public val payload: String? = null,
)

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
 * ref to intrinsically nullable named schema -> nullable typealias
 */
public typealias NullableRefTypealias = NullableInlineObject?

/**
 * Nullable Photo via anyOf
 */
public typealias NullableTestAnyOfTypealias = Photo?

/**
 * Nullable Photo via oneOf
 */
public typealias NullableTestOneOfTypealias = Photo?

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
 * nullable wrapper around a schema with a type property -> nullable typealias
 */
public typealias NullableTypedThing = TypedThing?

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
