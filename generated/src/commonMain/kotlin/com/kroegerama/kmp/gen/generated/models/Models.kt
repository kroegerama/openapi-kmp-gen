/* 
 * NOTE: This file is auto generated. Do not edit the file manually!
 * 
 * Test API
 * Test API Description
 * Version 1.0.0-SNAPSHOT
 * 
 * Generated Mon, 18 May 2026 20:14:40 +0200
 * OpenAPI KMP Gen (version 1.3.1) by kroegerama
 */
@file:Suppress("ArrayInDataClass", "RedundantVisibilityModifier", "unused", "ConstPropertyName")

package com.kroegerama.kmp.gen.generated.models

import androidx.compose.runtime.Immutable
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableBase64
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
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * @param attr1 nullable=false -> not null
 * @param attr2 required -> not null
 * @param attr3 has null type -> nullable
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
   * has null type -> nullable
   */
  @SerialName("attr3")
  public val attr3: Photo? = null,
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
  public val instant: Instant? = null,
  @SerialName("uuid")
  public val uuid: Uuid? = null,
  @SerialName("base64")
  public val base64: SerializableBase64? = null,
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
 * Nullable Photo via anyOf
 */
public typealias NullableTestAnyOfTypealias = Photo?

/**
 * Nullable Photo via allOf
 */
public typealias NullableTestOneOfTypealias = Photo?

public typealias RefTypealias = Photo
