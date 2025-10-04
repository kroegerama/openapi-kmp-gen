/* 
 * NOTE: This file is auto generated. Do not edit the file manually!
 * 
 * Test API
 * Test API Description
 * Version 1.0.0-SNAPSHOT
 * 
 * Generated Sat, 4 Oct 2025 22:37:04 +0200
 * OpenAPI KMP Gen (version 1.0.0-alpha01) by kroegerama
 */
package com.kroegerama.kmp.gen.generated.api

import arrow.core.Either
import com.kroegerama.kmp.gen.generated.Api
import com.kroegerama.kmp.gen.generated.Auth
import com.kroegerama.kmp.gen.generated.models.IntegerTest
import com.kroegerama.kmp.gen.generated.models.NumberTest
import com.kroegerama.kmp.gen.generated.models.Photo
import com.kroegerama.kmp.gen.generated.models.SerialTest
import com.kroegerama.openapi.kmp.gen.`companion`.AuthPlugin.Plugin.authKeys
import com.kroegerama.openapi.kmp.gen.`companion`.CallException
import com.kroegerama.openapi.kmp.gen.`companion`.HttpCallResponse
import com.kroegerama.openapi.kmp.gen.`companion`.createSerializedPathSegment
import com.kroegerama.openapi.kmp.gen.`companion`.eitherRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import kotlin.Int
import kotlin.Unit

public object DefaultApi {
  /**
   * `GET /photos/{id}`
   *
   * @return OK
   */
  public suspend fun getPhoto(id: Int, decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<Photo>> = Api.client.eitherRequest {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "photos",
      createSerializedPathSegment(value = id, explode = false, json = Api.json),
    )
    decorator()
  }

  /**
   * `GET /serialTest`
   *
   * @return OK
   */
  public suspend fun getSerialTest(decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<SerialTest>> = Api.client.eitherRequest {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "serialTest",
    )
    decorator()
  }

  /**
   * `GET /integerTest`
   *
   * @return OK
   */
  public suspend fun getIntegerTest(decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<IntegerTest>> = Api.client.eitherRequest {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "integerTest",
    )
    decorator()
  }

  /**
   * `GET /numberTest`
   *
   * @return OK
   */
  public suspend fun getNumberTest(decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<NumberTest>> = Api.client.eitherRequest {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "numberTest",
    )
    decorator()
  }

  /**
   * `POST /multipart`
   *
   * @return OK
   */
  public suspend fun multipart(body: MultiPartFormDataContent? = null, decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<Unit>> = Api.client.eitherRequest {
    method = HttpMethod.parse("POST")
    authKeys(
      Auth.BasicAuth.ID,
      Auth.TokenAuth.ID,
      Auth.APIKeyAuth.ID,
    )
    url.takeFrom("https://example.com/")
    url.appendPathSegments(
      "multipart",
    )
    setBody(body)
    decorator()
  }

  /**
   * `POST /urlencoded`
   *
   * @return OK
   */
  public suspend fun urlEncoded(body: FormDataContent? = null, decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<Unit>> = Api.client.eitherRequest {
    method = HttpMethod.parse("POST")
    authKeys(
      Auth.BearerAuth.ID,
      Auth.TokenAuth.ID,
      Auth.APIKeyAuth.ID,
    )
    url.takeFrom("https://example.com/")
    url.appendPathSegments(
      "urlencoded",
    )
    setBody(body)
    decorator()
  }
}
