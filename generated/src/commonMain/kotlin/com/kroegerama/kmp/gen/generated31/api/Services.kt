/* 
 * NOTE: This file is auto generated. Do not edit the file manually!
 * 
 * Test API
 * Test API Description
 * Version 1.0.0-SNAPSHOT
 * 
 * Generated Mon, 1 Jun 2026 13:00:00 GMT
 * OpenAPI KMP Gen (version 1.6.0-RC03) by kroegerama
 */
@file:Suppress("ArrayInDataClass", "RedundantVisibilityModifier", "unused", "ConstPropertyName")

package com.kroegerama.kmp.gen.generated31.api

import arrow.core.Either
import com.kroegerama.kmp.gen.generated31.Api
import com.kroegerama.kmp.gen.generated31.Auth
import com.kroegerama.kmp.gen.generated31.models.DateTime
import com.kroegerama.kmp.gen.generated31.models.DefaultValue
import com.kroegerama.kmp.gen.generated31.models.IntegerTest
import com.kroegerama.kmp.gen.generated31.models.NullableResponse200Response
import com.kroegerama.kmp.gen.generated31.models.NumberTest
import com.kroegerama.kmp.gen.generated31.models.Photo
import com.kroegerama.kmp.gen.generated31.models.SerialTest
import com.kroegerama.openapi.kmp.gen.`companion`.AuthPlugin.Plugin.authKeys
import com.kroegerama.openapi.kmp.gen.`companion`.Base64Serializer
import com.kroegerama.openapi.kmp.gen.`companion`.CallException
import com.kroegerama.openapi.kmp.gen.`companion`.EpochMillisecondsSerializer
import com.kroegerama.openapi.kmp.gen.`companion`.EpochSecondsSerializer
import com.kroegerama.openapi.kmp.gen.`companion`.HttpCallResponse
import com.kroegerama.openapi.kmp.gen.`companion`.ISO8601InstantSerializer
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableBase64
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableEpochMilliseconds
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableEpochSeconds
import com.kroegerama.openapi.kmp.gen.`companion`.SerializableISO8601Instant
import com.kroegerama.openapi.kmp.gen.`companion`.appendSerializedHeaderParameter
import com.kroegerama.openapi.kmp.gen.`companion`.appendSerializedQueryParameter
import com.kroegerama.openapi.kmp.gen.`companion`.createSerializedPathSegment
import com.kroegerama.openapi.kmp.gen.`companion`.eitherRequest
import com.kroegerama.openapi.kmp.gen.`companion`.encodeNullableToJsonElement
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.emptyList
import kotlin.collections.emptyMap
import kotlinx.serialization.builtins.ListSerializer

public object DefaultApi {
  /**
   * `POST /refTest`
   *
   * @return OK
   */
  public suspend fun refTest(
    testParam: DateTime? = null,
    body: List<String> = emptyList(),
    decorator: HttpRequestBuilder.() -> Unit = {},
  ): Either<CallException, HttpCallResponse<Photo>> = Api.client.eitherRequest {
    method = HttpMethod.parse("POST")
    contentType(ContentType.Application.Json)
    url.appendPathSegments(
      "refTest",
    )
    appendSerializedQueryParameter(name = "testParam", value = testParam, serializer = ISO8601InstantSerializer, explode = true, json = Api.json)
    setBody(body)
    decorator()
  }

  /**
   * `GET /{status}`
   *
   * @return OK
   */
  public suspend fun redirectTest(
    status: String,
    count: Int? = null,
    decorator: HttpRequestBuilder.() -> Unit = {},
  ): Either<CallException, HttpCallResponse<Unit>> = Api.client.eitherRequest {
    method = HttpMethod.parse("GET")
    url.takeFrom("https://mock.httpstatus.io/")
    url.appendPathSegments(
      createSerializedPathSegment(value = status, explode = false, json = Api.json),
    )
    appendSerializedQueryParameter(name = "count", value = count, explode = true, json = Api.json)
    decorator()
  }

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
   * `GET /defaultValue`
   *
   * @return OK
   */
  public suspend fun defaultValue(decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<DefaultValue>> = Api.client.eitherRequest {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "defaultValue",
    )
    decorator()
  }

  /**
   * `GET /nullableResponse`
   *
   * @return OK
   */
  public suspend fun nullableResponse(decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<NullableResponse200Response?>> = Api.client.eitherRequest {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "nullableResponse",
    )
    decorator()
  }

  /**
   * `GET /rawResponse`
   *
   * @return media type without schema -> raw response, not null
   */
  public suspend fun rawResponse(decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<HttpResponse>> = Api.client.eitherRequest {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "rawResponse",
    )
    decorator()
  }

  /**
   * `GET /nullableBinaryResponse`
   *
   * @return nullable non-json schema -> raw response, not null
   */
  public suspend fun nullableBinaryResponse(decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<HttpResponse>> = Api.client.eitherRequest {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "nullableBinaryResponse",
    )
    decorator()
  }

  /**
   * `POST /nullableBody`
   *
   * @return required body with null type variant -> nullable body parameter
   */
  public suspend fun nullableBody(body: Photo? = null, decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<Unit>> = Api.client.eitherRequest {
    method = HttpMethod.parse("POST")
    contentType(ContentType.Application.Json)
    url.appendPathSegments(
      "nullableBody",
    )
    setBody(body)
    decorator()
  }

  /**
   * `POST /listBody`
   *
   * @return OK
   */
  public suspend fun listBody(body: List<Long> = emptyList(), decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<Unit>> = Api.client.eitherRequest {
    method = HttpMethod.parse("POST")
    contentType(ContentType.Application.Json)
    url.appendPathSegments(
      "listBody",
    )
    setBody(body)
    decorator()
  }

  /**
   * `POST /mapBody`
   *
   * @return OK
   */
  public suspend fun mapBody(body: Map<String, Long> = emptyMap(), decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<Unit>> = Api.client.eitherRequest {
    method = HttpMethod.parse("POST")
    contentType(ContentType.Application.Json)
    url.appendPathSegments(
      "mapBody",
    )
    setBody(body)
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
      Auth.OAuth.ID,
      Auth.OIDCAuth.ID,
    )
    url.takeFrom("https://example.com/")
    url.appendPathSegments(
      "urlencoded",
    )
    setBody(body)
    decorator()
  }

  /**
   * `GET /serializedParams/{pathInstant}`
   *
   * @return OK
   */
  public suspend fun serializedParams(
    xEpochMillis: SerializableEpochMilliseconds? = null,
    pathInstant: SerializableISO8601Instant,
    queryInstant: SerializableISO8601Instant? = null,
    queryEpochSeconds: SerializableEpochSeconds? = null,
    queryBase64: SerializableBase64? = null,
    queryInstantList: List<SerializableISO8601Instant>? = null,
    decorator: HttpRequestBuilder.() -> Unit = {},
  ): Either<CallException, HttpCallResponse<Unit>> = Api.client.eitherRequest {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "serializedParams",
      createSerializedPathSegment(value = pathInstant, serializer = ISO8601InstantSerializer, explode = false, json = Api.json),
    )
    appendSerializedHeaderParameter(name = "X-Epoch-Millis", value = xEpochMillis, serializer = EpochMillisecondsSerializer, explode = false, json = Api.json)
    appendSerializedQueryParameter(name = "queryInstant", value = queryInstant, serializer = ISO8601InstantSerializer, explode = true, json = Api.json)
    appendSerializedQueryParameter(name = "queryEpochSeconds", value = queryEpochSeconds, serializer = EpochSecondsSerializer, explode = true, json = Api.json)
    appendSerializedQueryParameter(name = "queryBase64", value = queryBase64, serializer = Base64Serializer, explode = true, json = Api.json)
    appendSerializedQueryParameter(name = "queryInstantList", value = queryInstantList, serializer = ListSerializer(ISO8601InstantSerializer), explode = true, json = Api.json)
    decorator()
  }

  /**
   * `POST /instantBody`
   *
   * @return OK
   */
  public suspend fun instantBody(body: SerializableISO8601Instant, decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<Unit>> = Api.client.eitherRequest {
    method = HttpMethod.parse("POST")
    contentType(ContentType.Application.Json)
    url.appendPathSegments(
      "instantBody",
    )
    setBody(Api.json.encodeNullableToJsonElement(serializer = ISO8601InstantSerializer, value = body))
    decorator()
  }

  /**
   * `GET /epochSecondsResponse`
   *
   * @return OK
   */
  public suspend fun epochSecondsResponse(decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<SerializableEpochSeconds>> = Api.client.eitherRequest(deserializer = EpochSecondsSerializer, json = Api.json) {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "epochSecondsResponse",
    )
    decorator()
  }

  /**
   * `GET /instantListResponse`
   *
   * @return OK
   */
  public suspend fun instantListResponse(decorator: HttpRequestBuilder.() -> Unit = {}): Either<CallException, HttpCallResponse<List<SerializableISO8601Instant>>> = Api.client.eitherRequest(deserializer = ListSerializer(ISO8601InstantSerializer), json = Api.json) {
    method = HttpMethod.parse("GET")
    url.appendPathSegments(
      "instantListResponse",
    )
    decorator()
  }
}
