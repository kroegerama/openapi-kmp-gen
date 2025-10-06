package main

import com.kroegerama.kmp.gen.generated.Api
import com.kroegerama.kmp.gen.generated.Auth
import com.kroegerama.kmp.gen.generated.api.DefaultApi
import com.kroegerama.kmp.gen.generated.models.IntegerTest
import com.kroegerama.openapi.kmp.gen.companion.AuthItem
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Url
import io.ktor.http.parameters
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

fun main() {
    runBlocking {
        Api.setAuthProvider(Auth.TokenAuth {
            "tokenAuthValue_" + Clock.System.now().toEpochMilliseconds()
        })
        Api.setAuthProvider(Auth.APIKeyAuth {
            "apiKeyAuthValue_" + Clock.System.now().toEpochMilliseconds()
        })
        Api.setAuthProvider(Auth.BasicAuth {
            AuthItem.Basic("user", "password")
        })
        Api.setAuthProvider(Auth.BearerAuth {
            AuthItem.Bearer(Clock.System.now().toString())
        })

        println(
            DefaultApi.redirectTest("301")
        )
        println(
            DefaultApi.getPhoto(1)
        )

        Api.baseUrl = Url("https://example.com/base/replaced/")
        println(
            DefaultApi.getPhoto(1)
        )

        println(
            DefaultApi.multipart(
                MultiPartFormDataContent(
                    formData {
                        append("hello", "world")
                    }
                )
            )
        )
        println(
            DefaultApi.urlEncoded(
                FormDataContent(
                    parameters {
                        append("hello", "world")
                    }
                )
            )
        )
    }
}
