package com.kroegerama.openapi.kmp.gen.companion

import androidx.compose.runtime.Immutable
import arrow.core.Either
import arrow.core.getOrElse
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.isSuccess
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public typealias EitherCallResponse<T> = Either<CallException, HttpCallResponse<T>>
public typealias EitherTypedCallResponse<E, T> = Either<TypedCallException<E>, HttpCallResponse<T>>

public fun HttpResponse.asCallException(): CallException {
    return HttpCallException(
        raw = this,
        cause = null
    )
}

public fun Throwable.asCallException(): CallException {
    return when (this) {
        is ResponseException -> HttpCallException(
            raw = response,
            cause = this
        )

        is ContentConvertException -> SerializationException(
            message = message,
            cause = this
        )

        is IOException -> IOCallException(null, this)

        else -> UnexpectedCallException(null, this)
    }
}

public suspend inline fun <T, reified E> EitherCallResponse<T>.typed(): EitherTypedCallResponse<E, T> {
    return mapLeft<TypedCallException<E>> { callException ->
        when (callException) {
            is HttpCallException -> {
                val errorBody: E = Either.catch {
                    callException.raw.body<E>()
                }.getOrElse {
                    return@mapLeft callException
                }
                TypedHttpCallException(
                    error = errorBody,
                    raw = callException.raw,
                    cause = callException
                )
            }

            is IOCallException -> callException
            is SerializationException -> callException
            is UnexpectedCallException -> callException
        }
    }
}

@Immutable
public data class HttpCallResponse<out T>(
    val data: T,
    val raw: HttpResponse
) {
    val code: Int = raw.status.value
    val message: String = raw.status.description
    val headers: Headers = raw.headers
    val isSuccessful: Boolean = raw.status.isSuccess()
}

@Immutable
public sealed interface TypedCallException<out E>

@Immutable
public sealed class CallException : RuntimeException(), TypedCallException<Nothing>

public data class TypedHttpCallException<out E>(
    val error: E,
    val raw: HttpResponse,
    val cause: CallException?
) : TypedCallException<E> {
    val code: Int = raw.status.value
    val headers: Headers = raw.headers
}

public data class HttpCallException(
    val raw: HttpResponse,
    override val cause: Throwable?
) : CallException() {
    val code: Int = raw.status.value
    override val message: String = raw.status.description
    val headers: Headers = raw.headers
}

public data class SerializationException(
    override val message: String?,
    override val cause: ContentConvertException
) : CallException()

public data class IOCallException(
    override val message: String?,
    override val cause: IOException
) : CallException()

public data class UnexpectedCallException(
    override val message: String?,
    override val cause: Throwable?
) : CallException()

public inline fun TypedCallException<*>.onResponse(
    block: (HttpResponse) -> Unit
) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    when (this) {
        is TypedHttpCallException<*> -> raw
        is HttpCallException -> raw
        is SerializationException -> return
        is IOCallException -> return
        is UnexpectedCallException -> return
    }.let(block)
}

public inline fun TypedCallException<*>.onCode(
    block: (Int) -> Unit
) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    when (this) {
        is TypedHttpCallException<*> -> code
        is HttpCallException -> code
        is SerializationException -> return
        is IOCallException -> return
        is UnexpectedCallException -> return
    }.let(block)
}
