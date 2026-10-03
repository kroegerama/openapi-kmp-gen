package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.serialization.json.Json

/**
 * The [Json] configuration used by generated APIs unless another instance is passed to [ApiHolder.updateClient].
 *
 * ```kotlin
 * val customJson = Json(ApiJson) { prettyPrint = true }
 * ```
 */
public val ApiJson: Json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    allowStructuredMapKeys = true
    prettyPrint = false
    explicitNulls = false
    coerceInputValues = true
    useArrayPolymorphism = false
    allowSpecialFloatingPointValues = true
}
