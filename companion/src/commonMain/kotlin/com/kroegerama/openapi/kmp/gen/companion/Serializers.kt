package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.cookie
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.io.encoding.Base64
import kotlin.time.Instant

public typealias SerializableBase64 = @Serializable(Base64Serializer::class) ByteArray

public object Base64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.kroegerama.openapi.kmp.gen.companion.Base64Serializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, `value`: ByteArray): Unit = encoder.encodeString(Base64.encode(value))
    override fun deserialize(decoder: Decoder): ByteArray = Base64.decode(decoder.decodeString())
}

public typealias SerializableEpochSeconds = @Serializable(EpochSecondsSerializer::class) Instant

public object EpochSecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.kroegerama.openapi.kmp.gen.companion.EpochSecondsSerializer", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant): Unit = encoder.encodeLong(value.epochSeconds)
    override fun deserialize(decoder: Decoder): Instant = Instant.fromEpochSeconds(decoder.decodeLong())
}

public typealias SerializableEpochMilliseconds = @Serializable(EpochMillisecondsSerializer::class) Instant

public object EpochMillisecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.kroegerama.openapi.kmp.gen.companion.EpochMillisecondsSerializer", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant): Unit = encoder.encodeLong(value.toEpochMilliseconds())
    override fun deserialize(decoder: Decoder): Instant = Instant.fromEpochMilliseconds(decoder.decodeLong())
}

public typealias SerializableImmutableList<T> = @Serializable(ImmutableListSerializer::class) ImmutableList<T>

public class ImmutableListSerializer<T>(
    elementSerializer: KSerializer<T>
) : KSerializer<ImmutableList<T>> {
    private val listSerializer: KSerializer<List<T>> = ListSerializer(elementSerializer)

    override val descriptor: SerialDescriptor =
        SerialDescriptor("ImmutableList", listSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: ImmutableList<T>): Unit = listSerializer.serialize(encoder, value.toList())
    override fun deserialize(decoder: Decoder): ImmutableList<T> = listSerializer.deserialize(decoder).toImmutableList()
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
