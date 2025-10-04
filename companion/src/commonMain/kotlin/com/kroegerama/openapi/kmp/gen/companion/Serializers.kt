package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

public typealias SerializableBase64 = @Serializable(Base64Serializer::class) ByteArray

public object Base64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.kroegerama.openapi.kmp.gen.companion.Base64Serializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, `value`: ByteArray): Unit = encoder.encodeString(Base64.encode(value))
    override fun deserialize(decoder: Decoder): ByteArray = Base64.decode(decoder.decodeString())
}

public inline fun <reified T> Json.encodeToPrimitiveString(value: T): String? {
    if (value == null) return null
    return when (val e = encodeToJsonElement(value)) {
        is JsonPrimitive -> e.content
        is JsonArray -> e.toString()
        is JsonObject -> e.toString()
        JsonNull -> null
    }
}

@PublishedApi
internal fun serializeInner(inner: JsonElement): String? = when (inner) {
    is JsonPrimitive -> inner.content
    is JsonArray -> inner.toString()
    is JsonObject -> inner.toString()
    JsonNull -> null
}

public inline fun <reified T> HttpRequestBuilder.appendSerializedPathSegment(
    value: T,
    explode: Boolean = false,
    json: Json = Json
) {
    if (value == null) return
    val content = when (val e = json.encodeToJsonElement(value)) {
        JsonNull -> return
        is JsonPrimitive -> e.content
        is JsonArray -> e.mapNotNull { serializeInner(it) }.joinToString(",")
        is JsonObject -> e.toString()
    }
    url.appendPathSegments(content)
}

public inline fun <reified T> createSerializedPathSegment(
    value: T,
    explode: Boolean = false,
    json: Json = Json
): String {
    if (value == null) return ""
    return when (val e = json.encodeToJsonElement(value)) {
        JsonNull -> return ""
        is JsonPrimitive -> e.content
        is JsonArray -> e.mapNotNull { serializeInner(it) }.joinToString(",")
        is JsonObject -> e.toString()
    }
}

public inline fun <reified T> HttpRequestBuilder.appendSerializedQueryParameter(
    name: String,
    value: T,
    explode: Boolean = true,
    json: Json = Json
) {
    if (value == null) return
    when (val e = json.encodeToJsonElement(value)) {
        JsonNull -> return
        is JsonPrimitive -> parameter(name, e.content)

        is JsonArray -> {
            if (explode) {
                e.forEach {
                    parameter(name, serializeInner(it))
                }
            } else {
                parameter(name, e.mapNotNull { serializeInner(it) }.joinToString(","))
            }
        }

        is JsonObject -> parameter(name, e.toString())
    }
}

public inline fun <reified T> HttpRequestBuilder.appendSerializedHeaderParameter(
    name: String,
    value: T,
    explode: Boolean = false,
    json: Json = Json
) {
    if (value == null) return
    when (val e = json.encodeToJsonElement(value)) {
        JsonNull -> return
        is JsonPrimitive -> header(name, e.content)
        is JsonArray -> header(name, e.mapNotNull { serializeInner(it) }.joinToString(","))
        is JsonObject -> header(name, e.toString())
    }
}

public inline fun <reified T> HttpRequestBuilder.appendSerializedCookieParameter(
    name: String,
    value: T,
    explode: Boolean = true,
    json: Json = Json
) {
    if (value == null) return
    when (val e = json.encodeToJsonElement(value)) {
        JsonNull -> return
        is JsonPrimitive -> cookie(name, e.content)
        is JsonArray -> cookie(name, e.mapNotNull { serializeInner(it) }.joinToString(","))
        is JsonObject -> cookie(name, e.toString())
    }
}
