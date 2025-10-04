package generated.api

import com.kroegerama.openapi.kmp.gen.companion.AuthPlugin.Plugin.authKeys
import com.kroegerama.openapi.kmp.gen.companion.appendSerializedCookieParameter
import com.kroegerama.openapi.kmp.gen.companion.appendSerializedHeaderParameter
import com.kroegerama.openapi.kmp.gen.companion.appendSerializedPathSegment
import com.kroegerama.openapi.kmp.gen.companion.appendSerializedQueryParameter
import com.kroegerama.openapi.kmp.gen.companion.eitherRequest
import com.kroegerama.openapi.kmp.gen.companion.encodeToPrimitiveString
import generated.Api
import generated.Auth
import generated.models.Photo
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.statement.*
import io.ktor.http.*

object DefaultApi {
    suspend fun photo(
        id: Int,
        decorator: HttpRequestBuilder.() -> Unit = {}
    ) = Api.client.eitherRequest<Photo> {
        method = HttpMethod.Get

        url.appendPathSegments("photos")
        appendSerializedPathSegment(
            value = id,
            explode = false,
            json = Api.json
        )
        appendSerializedQueryParameter(
            name = "noExp",
            value = listOf("hello", "world"),
            explode = false,
            json = Api.json
        )
        appendSerializedQueryParameter(
            name = "exp",
            value = listOf("hello", "world"),
            explode = true,
            json = Api.json
        )

        appendSerializedHeaderParameter("header", "testo")
        appendSerializedCookieParameter("cookie", "monster")

        authKeys(
            Auth.APIKeyAuth.ID,
            Auth.TokenAuth.ID,
            Auth.BasicAuth.ID
        )
        decorator()
    }

    suspend fun photo2(
        id: Int,
        decorator: HttpRequestBuilder.() -> Unit = {}
    ) = Api.client.eitherRequest<Photo> {
        method = HttpMethod.Get

        url.takeFrom("https://example.com/api")
        url.appendPathSegments(
            listOf(
                "photos",
                Api.json.encodeToPrimitiveString(id) ?: "null"
            )
        )
        parameter("query", "hello")
        header("header", "world")
        cookie("cookie", "monster")
        authKeys(
            Auth.APIKeyAuth.ID,
            Auth.TokenAuth.ID,
            Auth.BasicAuth.ID
        )
        decorator()
    }

    suspend fun photo3(
        id: Int,
        decorator: HttpRequestBuilder.() -> Unit = {}
    ) = Api.client.eitherRequest<HttpResponse> {
        method = HttpMethod.Get

        url.appendPathSegments(
            listOf(
                "photos",
                Api.json.encodeToPrimitiveString(id) ?: "null"
            )
        )
        parameter("query", "hello")
        header("header", "world")
        cookie("cookie", "monster")
        authKeys(
            Auth.APIKeyAuth.ID,
            Auth.TokenAuth.ID,
            Auth.BasicAuth.ID
        )
        decorator()
    }

    suspend fun multipart(
        body: MultiPartFormDataContent,
        decorator: HttpRequestBuilder.() -> Unit = {}
    ) = Api.client.eitherRequest<HttpResponse> {
        method = HttpMethod.Get

        url.takeFrom("https://example.com")
        parameter("query", "hello")
        header("header", "world")
        cookie("cookie", "monster")
        authKeys(
            Auth.APIKeyAuth.ID,
            Auth.TokenAuth.ID,
            Auth.BasicAuth.ID
        )
        setBody(body)
        decorator()
    }

    suspend fun urlencoded(
        body: FormDataContent,
        decorator: HttpRequestBuilder.() -> Unit = {}
    ) = Api.client.eitherRequest<HttpResponse> {
        method = HttpMethod.Get

        url.takeFrom("https://example.com")
        parameter("query", "hello")
        header("header", "world")
        cookie("cookie", "monster")
        authKeys(
            Auth.APIKeyAuth.ID,
            Auth.TokenAuth.ID,
            Auth.BasicAuth.ID
        )
        setBody(body)
        decorator()
    }
}
