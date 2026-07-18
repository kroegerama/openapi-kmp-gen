/* 
 * NOTE: This file is auto generated. Do not edit the file manually!
 * 
 * Test API
 * Test API Description
 * Version 1.0.0-SNAPSHOT
 * 
 * Generated Mon, 1 Jun 2026 13:00:00 GMT
 * OpenAPI KMP Gen (version 1.6.0) by kroegerama
 */
@file:Suppress("ArrayInDataClass", "RedundantVisibilityModifier", "unused", "ConstPropertyName")

package com.kroegerama.kmp.gen.generated31.models

import androidx.compose.runtime.Immutable
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableBase64
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableEpochMilliseconds
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableEpochSeconds
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableISO8601Instant
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
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

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

@Serializable
@Immutable
public data class NullableResponse200Response(
  @SerialName("payload")
  public val payload: String? = null,
)

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
