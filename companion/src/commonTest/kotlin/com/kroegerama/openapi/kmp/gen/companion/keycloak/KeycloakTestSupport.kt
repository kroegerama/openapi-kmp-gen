package com.kroegerama.openapi.kmp.gen.companion.keycloak

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlin.io.encoding.Base64
import kotlin.time.Clock

private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

private fun encode(json: String): String = base64.encode(json.encodeToByteArray())

/** Builds an unsigned JWT that [com.kroegerama.openapi.kmp.gen.companion.JWT.parse] accepts. */
internal fun unsignedJwt(exp: Long? = null, iat: Long? = null, sub: String? = null): String {
    val claims = buildList {
        exp?.let { add(""""exp":$it""") }
        iat?.let { add(""""iat":$it""") }
        sub?.let { add(""""sub":"$it"""") }
    }
    return "${encode("""{"alg":"none","typ":"JWT"}""")}.${encode(claims.joinToString(",", "{", "}"))}.sig"
}

internal fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

internal fun tokenResponseJson(
    accessToken: String,
    refreshToken: String? = null,
    expiresIn: Long = 300,
    refreshExpiresIn: Long = 1800
): String = buildString {
    append("""{"access_token":"$accessToken","expires_in":$expiresIn""")
    if (refreshToken != null) {
        append(""","refresh_token":"$refreshToken","refresh_expires_in":$refreshExpiresIn""")
    }
    append(""","token_type":"Bearer"}""")
}

internal fun testHttpClient(engine: MockEngine): HttpClient = createKeycloakHttpClient(engine)
