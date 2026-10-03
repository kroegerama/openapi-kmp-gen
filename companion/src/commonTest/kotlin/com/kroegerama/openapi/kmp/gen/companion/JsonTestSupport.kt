package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

internal fun MockRequestHandleScope.respondJson(
    json: String,
    status: HttpStatusCode = HttpStatusCode.OK
): HttpResponseData = respond(
    content = json,
    status = status,
    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
)

// plain HttpClient without the library defaults and without content negotiation, answers every request with body as JSON
internal fun plainJsonClient(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK
): HttpClient = HttpClient(MockEngine) {
    engine {
        addHandler { respondJson(body, status) }
    }
}

// the property name shows which Json encoded or decoded a value: firstName, first_name or first-name
@Serializable
internal data class Person(val firstName: String)

internal val snakeCaseJson = Json { namingStrategy = JsonNamingStrategy.SnakeCase }

internal val kebabCaseJson = Json { namingStrategy = JsonNamingStrategy.KebabCase }
