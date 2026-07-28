package com.kroegerama.openapi.kmp.gen.companion.keycloak

import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.io.encoding.Base64
import kotlin.time.Clock

private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

private fun encode(json: String): String = base64.encode(json.encodeToByteArray())

/** Builds an unsigned JWT that [com.kroegerama.openapi.kmp.gen.companion.JWT.parse] accepts. */
internal fun unsignedJwt(exp: Long? = null, iat: Long? = null): String {
    val claims = buildList {
        exp?.let { add(""""exp":$it""") }
        iat?.let { add(""""iat":$it""") }
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

internal fun testHttpClient(engine: MockEngine): HttpClient = HttpClient(engine) {
    // The production client (createKeycloakHttpClient -> defaultConfig) sets expectSuccess = true,
    // so error responses surface as ResponseException. Match it here so the tests exercise the
    // same error path, including re-reading the error body after Ktor's validator consumed it.
    expectSuccess = true
    install(ContentNegotiation) {
        json(createDefaultJson())
    }
}

internal fun MockRequestHandleScope.respondJson(
    json: String,
    status: HttpStatusCode = HttpStatusCode.OK
): HttpResponseData = respond(
    content = json,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
)
