package com.kroegerama.openapi.kmp.gen.companion

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.DelicateRaiseApi
import arrow.core.raise.RaiseCancellationException
import arrow.core.raise.either
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.call.save
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

public suspend inline fun <reified T> HttpClient.eitherRequest(
    noinline block: HttpRequestBuilder.() -> Unit
): Either<CallException, HttpCallResponse<T>> = eitherRequestImpl(block) { response ->
    response.body<T>()
}

/**
 * Variant of [eitherRequest] with an explicit [deserializer], preserving custom serializers of annotated typealiases such as
 * [SerializableISO8601Instant]. A body that cannot be decoded is reported as a [SerializationCallException].
 *
 * @param deserializer decodes the success body from the response text; the `ContentNegotiation` plugin is not used.
 * @param json the [Json] instance for decoding the success body.
 */
public suspend fun <T> HttpClient.eitherRequest(
    deserializer: DeserializationStrategy<T>,
    json: Json,
    block: HttpRequestBuilder.() -> Unit
): Either<CallException, HttpCallResponse<T>> = eitherRequestImpl(block) { response ->
    val text = response.bodyAsText()
    try {
        json.decodeFromString(deserializer, text)
    } catch (e: Exception) {
        // same wrapping as ktor's ContentNegotiation converter, so both variants report a SerializationCallException
        throw JsonConvertException("Illegal input: ${e.message}", e)
    }
}

@PublishedApi
internal suspend fun <T> HttpClient.eitherRequestImpl(
    block: HttpRequestBuilder.() -> Unit,
    readBody: suspend (HttpResponse) -> T
): Either<CallException, HttpCallResponse<T>> = either {
    // built outside catchCall, so a cancellation thrown by the block is never taken for a closed client
    val builder = Either.catch {
        HttpRequestBuilder().apply(block)
    }.mapLeft {
        it.asCallException()
    }.bind()
    val response = catchCall {
        request(builder)
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
    val successBody: T = catchCall {
        readBody(response)
    }.bind()
    HttpCallResponse(
        data = successBody,
        raw = response
    )
}

// A closed client cancels the engine call although the caller is still active, which is reported as a failed call. The pipeline still
// runs on a closed client, so a timeout or an Arrow raise thrown by a plugin is rethrown like a caller cancellation.
private suspend inline fun <T> HttpClient.catchCall(block: () -> T): Either<CallException, T> = try {
    Either.catch {
        block()
    }.mapLeft {
        // Ktor unwraps a cancellation caused by a failure, so the caller can be cancelled although no CancellationException arrives
        currentCoroutineContext().ensureActive()
        it.asCallException()
    }
} catch (e: CancellationException) {
    currentCoroutineContext().ensureActive()
    val clientClosed = !this.coroutineContext.job.isActive
    if (!clientClosed || e.isTimeoutOrRaise()) throw e
    UnexpectedCallException(e.message, e).left()
}

@OptIn(DelicateRaiseApi::class)
private fun Throwable.isTimeoutOrRaise(): Boolean = this is TimeoutCancellationException || this is RaiseCancellationException
