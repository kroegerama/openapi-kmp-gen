package com.kroegerama.openapi.kmp.gen.companion

import arrow.core.Either
import arrow.core.raise.either
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.http.isSuccess

public suspend inline fun <reified T> HttpClient.eitherRequest(
    block: HttpRequestBuilder.() -> Unit
): Either<CallException, HttpCallResponse<T>> = either {
    val response = Either.catch {
        request(block)
    }.mapLeft {
        it.asCallException()
    }.bind()
    if (!response.status.isSuccess()) {
        raise(
            HttpCallException(
                raw = response,
                cause = null
            )
        )
    }
    val successBody: T = Either.catch {
        response.body<T>()
    }.mapLeft {
        it.asCallException()
    }.bind()
    HttpCallResponse(
        data = successBody,
        raw = response
    )
}
