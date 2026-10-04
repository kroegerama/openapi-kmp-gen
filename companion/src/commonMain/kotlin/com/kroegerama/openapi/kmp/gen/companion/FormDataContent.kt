package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.parameters
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
 * Encodes this object as multipart form data with one text part per top-level property, skipping `null` values.
 * Binary parts are not supported; build file uploads with Ktor's `formData { }` instead.
 *
 * @param json the [Json] instance for encoding.
 */
public inline fun <reified T : Any> T.asMultiPartFormDataContent(
    json: Json
): MultiPartFormDataContent {
    val jsonObject = json.encodeToJsonElement(this).jsonObject
    val parts = formData {
        jsonObject.forEach { (key, value) ->
            val content = when (value) {
                JsonNull -> return@forEach
                is JsonPrimitive -> value.content
                else -> value.toString()
            }
            append(key, content)
        }
    }
    return MultiPartFormDataContent(parts)
}
