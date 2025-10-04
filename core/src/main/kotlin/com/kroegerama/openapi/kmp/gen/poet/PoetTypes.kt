package com.kroegerama.openapi.kmp.gen.poet

import com.kroegerama.openapi.kmp.gen.OptionSet
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

private const val COMPANION_PACKAGE = "com.kroegerama.openapi.kmp.gen.companion"

class PoetTypes(
    private val options: OptionSet
) {
    val api = ClassName(options.packageName, "Api")
    val auth = ClassName(options.packageName, "Auth")

    fun modelName(vararg name: String) = ClassName(options.modelPackage, *name)

    fun apiName(name: String) = ClassName(options.apiPackage, name + "Api")

    companion object {
        private const val KTOR_HTTP_PACKAGE = "io.ktor.http"
        val KtorUrl = ClassName(KTOR_HTTP_PACKAGE, "Url")
        val HttpMethod = ClassName(KTOR_HTTP_PACKAGE, "HttpMethod")
        val HttpResponse = ClassName("io.ktor.client.statement", "HttpResponse")
        val HttpRequestBuilder = ClassName("io.ktor.client.request", "HttpRequestBuilder")
        val ListOfKtorUrl = LIST.parameterizedBy(KtorUrl)

        private const val KTOR_CLIENT_REQUEST_FORMS = "io.ktor.client.request.forms"
        val MultiPartFormDataContent = ClassName(KTOR_CLIENT_REQUEST_FORMS, "MultiPartFormDataContent")
        val FormDataContent = ClassName(KTOR_CLIENT_REQUEST_FORMS, "FormDataContent")


        val ApiHolder = ClassName(COMPANION_PACKAGE, "ApiHolder")
        val AuthItem = ClassName(COMPANION_PACKAGE, "AuthItem")
        val AuthItemBasic = ClassName(COMPANION_PACKAGE, "AuthItem", "Basic")
        val AuthItemBearer = ClassName(COMPANION_PACKAGE, "AuthItem", "Bearer")
        val AuthItemApiKey = ClassName(COMPANION_PACKAGE, "AuthItem", "ApiKey")
        val AuthItemPosition = ClassName(COMPANION_PACKAGE, "AuthItem", "Position")

        val LocalDate = ClassName("kotlinx.datetime", "LocalDate")
        val LocalTime = ClassName("kotlinx.datetime", "LocalTime")
        val Instant = ClassName("kotlin.time", "Instant")
        val Uuid = ClassName("kotlin.uuid", "Uuid")
        val SerializableBase64 = ClassName(COMPANION_PACKAGE, "SerializableBase64")

        private val CALL_EXCEPTION = ClassName(COMPANION_PACKAGE, "CallException")
        private val CALL_RESPONSE = ClassName(COMPANION_PACKAGE, "HttpCallResponse")

        private const val KTX_SERIALIZATION = "kotlinx.serialization"
        private const val KTX_SERIALIZATION_JSON = "kotlinx.serialization.json"

        val Serializable = ClassName(KTX_SERIALIZATION, "Serializable")
        val SerialName = ClassName(KTX_SERIALIZATION, "SerialName")
        val JsonElement = ClassName(KTX_SERIALIZATION_JSON, "JsonElement")
        val JsonClassDiscriminator = ClassName(KTX_SERIALIZATION_JSON, "JsonClassDiscriminator")

        val Deprecated = ClassName("kotlin", "Deprecated")
        val Immutable = ClassName("androidx.compose.runtime", "Immutable")

        private val Either = ClassName("arrow.core", "Either")

        fun either(cn: TypeName) = Either.parameterizedBy(
            CALL_EXCEPTION,
            CALL_RESPONSE.parameterizedBy(cn)
        )
    }
}

object PoetMembers {
    private const val KTOR_CLIENT_REQUEST_PACKAGE = "io.ktor.client.request"
    val EitherRequest = MemberName(COMPANION_PACKAGE, "eitherRequest")
    val AppendSerializedQueryParameter = MemberName(COMPANION_PACKAGE, "appendSerializedQueryParameter")
    val AppendSerializedHeaderParameter = MemberName(COMPANION_PACKAGE, "appendSerializedHeaderParameter")
    val AppendSerializedCookieParameter = MemberName(COMPANION_PACKAGE, "appendSerializedCookieParameter")
    val CreateSerializedPathSegment = MemberName(COMPANION_PACKAGE, "createSerializedPathSegment")
    val AuthKeys = ClassName(COMPANION_PACKAGE, "AuthPlugin", "Plugin").member("authKeys")
    val AppendPathSegments = MemberName("io.ktor.http", "appendPathSegments")
    val TakeFrom = MemberName("io.ktor.http", "takeFrom")
    val RequestSetBody = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "setBody")
}
