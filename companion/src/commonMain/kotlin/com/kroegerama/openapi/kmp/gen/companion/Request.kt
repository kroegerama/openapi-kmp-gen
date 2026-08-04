package com.kroegerama.openapi.kmp.gen.companion

import arrow.core.Either
import arrow.core.raise.either
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.call.save
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

public suspend inline fun <reified T> HttpClient.eitherRequest(
    noinline block: HttpRequestBuilder.() -> Unit
): Either<CallException, HttpCallResponse<T>> = eitherRequestImpl(block) { response ->
    response.body<T>()
}

/**
 * Variant of [eitherRequest] with an explicit [deserializer], preserving custom serializers of
 * annotated typealiases such as [SerializableISO8601Instant]: the reified variant resolves the
 * serializer from the reified type, which drops `@Serializable(with = …)` type annotations
 * carried by such typealiases.
 *
 * The success body is decoded from the response text via [json] instead of ktor's
 * `ContentNegotiation` plugin.
 */
public suspend fun <T> HttpClient.eitherRequest(
    deserializer: DeserializationStrategy<T>,
    json: Json = Json,
    block: HttpRequestBuilder.() -> Unit
): Either<CallException, HttpCallResponse<T>> = eitherRequestImpl(block) { response ->
    json.decodeFromString(deserializer, response.bodyAsText())
}

@PublishedApi
internal suspend fun <T> HttpClient.eitherRequestImpl(
    block: HttpRequestBuilder.() -> Unit,
    readBody: suspend (HttpResponse) -> T
): Either<CallException, HttpCallResponse<T>> = either {
    val response = Either.catch {
        request(block)
    }.mapLeft {
        it.asCallException()
    }.bind()
    if (!response.status.isSuccess()) {
        // Reached only when the default response validation is disabled (expectSuccess = false);
        // otherwise Ktor already threw a ResponseException above. save() buffers the response so a
        // later typed<E>() can still read the error body off a non-streaming copy.
        raise(
            HttpCallException(
                raw = response.call.save().response,
                cause = null
            )
        )
    }
    val successBody: T = Either.catch {
        readBody(response)
    }.mapLeft {
        it.asCallException()
    }.bind()
    HttpCallResponse(
        data = successBody,
        raw = response
    )
}
