package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.headersOf
import io.ktor.http.parameters
import io.ktor.http.quote
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Encodes this object as URL-encoded form data with one entry per top-level property, skipping `null` values.
 * Array properties are sent in OpenAPI form style, nested objects as JSON text.
 *
 * @param explode sends an array as one pair per item (`tags=a&tags=b`) instead of one comma-joined pair (`tags=a,b`).
 * @param json the [Json] instance for encoding.
 */
public inline fun <reified T : Any> T.asFormDataContent(
    explode: Boolean = true,
    json: Json
): FormDataContent = createFormDataContent(json.encodeToJsonElement(this), explode)

@PublishedApi
internal fun createFormDataContent(element: JsonElement, explode: Boolean): FormDataContent {
    val formData = parameters {
        element.jsonObject.forEach { (key, value) ->
            when (value) {
                JsonNull -> Unit
                is JsonPrimitive -> append(key, value.content)
                is JsonArray -> if (value.any { it !is JsonNull }) {
                    serializeForm(key, value, explode) { name, content ->
                        if (content != null) append(name, content)
                    }
                }

                is JsonObject -> append(key, value.toString())
            }
        }
    }
    return FormDataContent(formData)
}

/**
 * Encodes this object as multipart form data, appending each top-level property as [appendSerializedPart] does.
 * Binary parts are not supported; build file uploads with `formData { appendFilePart(...) }` instead.
 *
 * @param json the [Json] instance for encoding.
 */
public inline fun <reified T : Any> T.asMultiPartFormDataContent(
    json: Json
): MultiPartFormDataContent = createMultiPartFormDataContent(json.encodeToJsonElement(this))

@PublishedApi
internal fun createMultiPartFormDataContent(element: JsonElement): MultiPartFormDataContent {
    val parts = formData {
        element.jsonObject.forEach { (key, value) ->
            appendSerializedElementPart(key, value)
        }
    }
    return MultiPartFormDataContent(parts)
}

/**
 * Content of one multipart file part, read from a fresh [ByteReadChannel] on every send.
 *
 * ```kotlin
 * FilePart(bytes, filename = "report.pdf", contentType = ContentType.Application.Pdf)
 * ```
 *
 * @property size the number of bytes [provider] yields, or `null` if unknown; a known size adds a `Content-Length` header to the part.
 * @property filename the `filename` of the `Content-Disposition` header, or `null` to send the part name.
 * @property contentType the `Content-Type` of the part, or `null` to use the default of the operation.
 * @property provider returns a new channel with the file content on every call, because a request can be sent more than once.
 */
public class FilePart(
    public val size: Long? = null,
    public val filename: String? = null,
    public val contentType: ContentType? = null,
    public val provider: () -> ByteReadChannel
) {
    /**
     * Creates a file part that sends [bytes].
     */
    public constructor(
        bytes: ByteArray,
        filename: String? = null,
        contentType: ContentType? = null
    ) : this(
        size = bytes.size.toLong(),
        filename = filename,
        contentType = contentType,
        provider = { ByteReadChannel(bytes) }
    )
}

/**
 * Appends [value] as one file part with its filename and content type.
 *
 * @param name the name of the part, also sent as `filename` when [FilePart.filename] is `null`.
 * @param value the content of the part; `null` appends nothing.
 * @param defaultContentType the `Content-Type` sent when [FilePart.contentType] is `null`.
 */
public fun FormBuilder.appendFilePart(
    name: String,
    value: FilePart?,
    defaultContentType: ContentType
) {
    if (value == null) return
    val partHeaders = headers {
        append(HttpHeaders.ContentDisposition, "filename=${(value.filename ?: name).quote()}")
        append(HttpHeaders.ContentType, (value.contentType ?: defaultContentType).toString())
    }
    append(name, ChannelProvider(value.size, value.provider), partHeaders)
}

/**
 * Appends [value] as multipart parts under [name]; `null` and [JsonNull] values append nothing.
 * A primitive is one text part, an array one part per non-null item, and an object, or an array item that is an object or array,
 * one `application/json` part with its JSON text.
 *
 * @param name the name of every appended part.
 * @param value the value to encode.
 * @param json the [Json] instance for encoding.
 */
public inline fun <reified T> FormBuilder.appendSerializedPart(
    name: String,
    value: T,
    json: Json
) {
    if (value == null) return
    appendSerializedElementPart(name, json.encodeToJsonElement(value))
}

/**
 * Variant of [appendSerializedPart] with an explicit [serializer], preserving custom
 * serializers of annotated typealiases such as [SerializableISO8601Instant].
 */
public fun <T : Any> FormBuilder.appendSerializedPart(
    name: String,
    value: T?,
    serializer: SerializationStrategy<T>,
    json: Json
) {
    if (value == null) return
    appendSerializedElementPart(name, json.encodeToJsonElement(serializer, value))
}

@PublishedApi
internal fun FormBuilder.appendSerializedElementPart(name: String, element: JsonElement) {
    when (element) {
        JsonNull -> Unit
        is JsonPrimitive -> append(name, element.content)
        is JsonArray -> element.forEach { item ->
            when (item) {
                JsonNull -> Unit
                is JsonPrimitive -> append(name, item.content)
                is JsonArray, is JsonObject -> appendJsonPart(name, item)
            }
        }

        is JsonObject -> appendJsonPart(name, element)
    }
}

private val jsonPartHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

private fun FormBuilder.appendJsonPart(name: String, element: JsonElement) {
    append(name, element.toString(), jsonPartHeaders)
}
