/* 
 * NOTE: This file is auto generated. Do not edit the file manually!
 * 
 * Test API
 * Test API Description
 * Version 1.0.0-SNAPSHOT
 * 
 * Generated Thu, 26 Feb 2026 13:42:55 +0100
 * OpenAPI KMP Gen (version 1.1.1) by kroegerama
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
