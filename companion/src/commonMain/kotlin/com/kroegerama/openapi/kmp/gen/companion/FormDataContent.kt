package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

public inline fun <reified T> T.asFormDataContent(
    json: Json = Json
): FormDataContent {
    val jsonObject = json.encodeToJsonElement(this).jsonObject
    val formData = parameters {
        jsonObject.forEach { (key, value) ->
            val content = when (value) {
                JsonNull -> return@forEach
                is JsonPrimitive -> value.content
                else -> value.toString()
            }
            append(key, content)
        }
    }
    return FormDataContent(formData)
}

public inline fun <reified T> T.asMultiPartFormDataContent(
    json: Json = Json
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
