package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.cookie
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Encodes [value] to a single scalar string, or `null` when it has no scalar form.
 *
 * A [JsonPrimitive] yields its raw [content][JsonPrimitive.content] (strings are unquoted);
 * arrays and objects yield their compact JSON text; `null` and [JsonNull] yield `null`.
 */
public inline fun <reified T> Json.encodeToPrimitiveString(value: T): String? {
    if (value == null) return null
    return when (val e = encodeToJsonElement(value)) {
        JsonNull -> null
        is JsonPrimitive -> e.content
        is JsonArray -> e.toString()
        is JsonObject -> e.toString()
    }
}

@PublishedApi
internal fun serializeInner(inner: JsonElement): String? = when (inner) {
    JsonNull -> null
    is JsonPrimitive -> inner.content
    is JsonArray -> inner.toString()
    is JsonObject -> inner.toString()
}

/**
 * Serializes [element] using OpenAPI **simple** style - the default for `path` and `header`
 * parameters. Primitives and arrays are unaffected by [explode] in this style (arrays are always
 * comma-joined); objects honor it: `k1=v1,k2=v2` when exploded, otherwise `k1,v1,k2,v2`.
 * Returns `null` for [JsonNull] so callers can skip the parameter entirely.
 */
@PublishedApi
internal fun serializeSimple(element: JsonElement, explode: Boolean): String? = when (element) {
    JsonNull -> null
    is JsonPrimitive -> element.content
    is JsonArray -> element.mapNotNull { serializeInner(it) }.joinToString(",")
    is JsonObject -> if (explode) {
        element.entries.joinToString(",") { (key, value) -> "$key=${serializeInner(value).orEmpty()}" }
    } else {
        element.entries.flatMap { (key, value) -> listOf(key, serializeInner(value).orEmpty()) }.joinToString(",")
    }
}

/**
 * Serializes [element] using OpenAPI **form** style - the default for `query` and `cookie`
 * parameters - invoking [emit] once per resulting name/value pair (`value` is `null` for a
 * [JsonNull] element, which callers should skip). Behavior by [explode]:
 * - primitive → a single `name`/value pair
 * - array, explode → one pair per item under [name]; non-explode → one comma-joined pair
 * - object, explode → one pair per property keyed by the **property name** ([name] is dropped);
 *   non-explode → one pair under [name] rendered as `k1,v1,k2,v2`
 *
 * The `deepObject` style (`name[key]=value`) is not supported.
 */
@PublishedApi
internal fun serializeForm(
    name: String,
    element: JsonElement,
    explode: Boolean,
    emit: (name: String, value: String?) -> Unit
) {
    when (element) {
        JsonNull -> return
        is JsonPrimitive -> emit(name, element.content)
        is JsonArray -> if (explode) {
            element.forEach { emit(name, serializeInner(it)) }
        } else {
            emit(name, element.mapNotNull { serializeInner(it) }.joinToString(","))
        }

        is JsonObject -> if (explode) {
            element.forEach { (key, value) -> emit(key, serializeInner(value)) }
        } else {
            emit(name, element.entries.flatMap { (key, value) -> listOf(key, serializeInner(value).orEmpty()) }.joinToString(","))
        }
    }
}

/**
 * Appends [value] to the request URL as a single OpenAPI **simple**-style path segment (see
 * [serializeSimple] for how [explode] affects arrays and objects). `null` and [JsonNull] values are
 * skipped, appending nothing.
 *
 * This is the [HttpRequestBuilder] counterpart to [createSerializedPathSegment]. Generated code uses
 * [createSerializedPathSegment], which composes several segments into one `url.appendPathSegments(...)`
 * call; this variant is provided for hand-written requests that append a single segment directly.
 */
public inline fun <reified T> HttpRequestBuilder.appendSerializedPathSegment(
    value: T,
    explode: Boolean = false,
    json: Json = Json
) {
    if (value == null) return
    val content = serializeSimple(json.encodeToJsonElement(value), explode) ?: return
    url.appendPathSegments(content)
}

/**
 * Variant of [appendSerializedPathSegment] with an explicit [serializer].
 *
 * The reified variants resolve the serializer from the reified type, which drops
 * `@Serializable(with = …)` type annotations carried by typealiases such as
 * [SerializableISO8601Instant]. Use this overload to keep the custom serializer:
 *
 * ```kotlin
 * appendSerializedPathSegment(value = instant, serializer = ISO8601InstantSerializer)
 * ```
 */
public fun <T : Any> HttpRequestBuilder.appendSerializedPathSegment(
    value: T?,
    serializer: SerializationStrategy<T>,
    explode: Boolean = false,
    json: Json = Json
) {
    if (value == null) return
    val content = serializeSimple(json.encodeToJsonElement(serializer, value), explode) ?: return
    url.appendPathSegments(content)
}

public inline fun <reified T> createSerializedPathSegment(
    value: T,
    explode: Boolean = false,
    json: Json = Json
): String {
    if (value == null) return ""
    return serializeSimple(json.encodeToJsonElement(value), explode).orEmpty()
}

/**
 * Variant of [createSerializedPathSegment] with an explicit [serializer], preserving custom
 * serializers of annotated typealiases such as [SerializableISO8601Instant].
 */
public fun <T : Any> createSerializedPathSegment(
    value: T?,
    serializer: SerializationStrategy<T>,
    explode: Boolean = false,
    json: Json = Json
): String {
    if (value == null) return ""
    return serializeSimple(json.encodeToJsonElement(serializer, value), explode).orEmpty()
}

public inline fun <reified T> HttpRequestBuilder.appendSerializedQueryParameter(
    name: String,
    value: T,
    explode: Boolean = true,
    json: Json = Json
) {
    if (value == null) return
    serializeForm(name, json.encodeToJsonElement(value), explode) { key, content ->
        parameter(key, content)
    }
}

/**
 * Variant of [appendSerializedQueryParameter] with an explicit [serializer], preserving custom
 * serializers of annotated typealiases such as [SerializableISO8601Instant].
 */
public fun <T : Any> HttpRequestBuilder.appendSerializedQueryParameter(
    name: String,
    value: T?,
    serializer: SerializationStrategy<T>,
    explode: Boolean = true,
    json: Json = Json
) {
    if (value == null) return
    serializeForm(name, json.encodeToJsonElement(serializer, value), explode) { key, content ->
        parameter(key, content)
    }
}

public inline fun <reified T> HttpRequestBuilder.appendSerializedHeaderParameter(
    name: String,
    value: T,
    explode: Boolean = false,
    json: Json = Json
) {
    if (value == null) return
    val content = serializeSimple(json.encodeToJsonElement(value), explode) ?: return
    header(name, content)
}

/**
 * Variant of [appendSerializedHeaderParameter] with an explicit [serializer], preserving custom
 * serializers of annotated typealiases such as [SerializableISO8601Instant].
 */
public fun <T : Any> HttpRequestBuilder.appendSerializedHeaderParameter(
    name: String,
    value: T?,
    serializer: SerializationStrategy<T>,
    explode: Boolean = false,
    json: Json = Json
) {
    if (value == null) return
    val content = serializeSimple(json.encodeToJsonElement(serializer, value), explode) ?: return
    header(name, content)
}

public inline fun <reified T> HttpRequestBuilder.appendSerializedCookieParameter(
    name: String,
    value: T,
    explode: Boolean = true,
    json: Json = Json
) {
    if (value == null) return
    serializeForm(name, json.encodeToJsonElement(value), explode) { key, content ->
        if (content != null) cookie(key, content)
    }
}

/**
 * Variant of [appendSerializedCookieParameter] with an explicit [serializer], preserving custom
 * serializers of annotated typealiases such as [SerializableISO8601Instant].
 */
public fun <T : Any> HttpRequestBuilder.appendSerializedCookieParameter(
    name: String,
    value: T?,
    serializer: SerializationStrategy<T>,
    explode: Boolean = true,
    json: Json = Json
) {
    if (value == null) return
    serializeForm(name, json.encodeToJsonElement(serializer, value), explode) { key, content ->
        if (content != null) cookie(key, content)
    }
}

/**
 * Encodes [value] with an explicit [serializer], returning [JsonNull] for `null` values.
 *
 * Intended for request bodies whose type is an annotated typealias such as
 * [SerializableISO8601Instant]: passing such a value to `setBody` directly would resolve the
 * serializer from the underlying type and drop the `@Serializable(with = …)` annotation.
 */
public fun <T : Any> Json.encodeNullableToJsonElement(
    serializer: SerializationStrategy<T>,
    value: T?
): JsonElement {
    if (value == null) return JsonNull
    return encodeToJsonElement(serializer, value)
}
