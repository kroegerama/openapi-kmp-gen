package com.kroegerama.kmp.gen.generated31

import arrow.core.Either
import com.kroegerama.kmp.gen.generated31.api.DefaultApi
import com.kroegerama.kmp.gen.generated31.models.MultipartTypedRequest
import com.kroegerama.kmp.gen.generated31.models.UrlencodedTypedRequest
import com.kroegerama.openapi.kmp.gen.companion.FilePart
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class FormRequestBodyTest {

    @Test
    fun requiredMultipartBodySendsTypedParts() = runTest {
        val pdf = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x00, -1)
        val request = capture {
            DefaultApi.multipartTyped(
                description = "query",
                file = FilePart(pdf, filename = "report.pdf"),
                attachments = listOf(
                    FilePart(byteArrayOf(1, 2), filename = "a.png"),
                    FilePart(byteArrayOf(3), filename = "b.jpg", contentType = ContentType.Image.JPEG)
                ),
                avatar = FilePart(byteArrayOf(4, 5, 6)),
                metadata = MultipartTypedRequest.Metadata(title = "Q3", pages = 2),
                options = buildJsonObject { put("flag", true) },
                url = "https://example.org/x",
                bodyDescription = "body"
            )
        }

        assertEquals("/multipartTyped", request.url.encodedPath)
        assertEquals("query", request.url.parameters["description"])
        assertTrue(request.body.contentType!!.match(ContentType.MultiPart.FormData), request.body.contentType.toString())

        val parts = request.renderParts()
        assertEquals(
            listOf("file", "attachments", "attachments", "avatar", "metadata", "options", "url", "description"),
            parts.map { it.name }
        )

        val (file, png, jpg, avatar, metadata) = parts
        assertEquals("""form-data; name="file"; filename="report.pdf"""", file.header("Content-Disposition"))
        assertEquals("application/pdf", file.header("Content-Type"))
        assertEquals(pdf.toLatin1String(), file.body)

        assertEquals("""form-data; name="attachments"; filename="a.png"""", png.header("Content-Disposition"))
        assertEquals("image/png", png.header("Content-Type"))
        assertEquals(byteArrayOf(1, 2).toLatin1String(), png.body)
        assertEquals("image/jpeg", jpg.header("Content-Type"))
        assertEquals(byteArrayOf(3).toLatin1String(), jpg.body)

        assertEquals("""form-data; name="avatar"; filename="avatar"""", avatar.header("Content-Disposition"))
        assertEquals("application/octet-stream", avatar.header("Content-Type"))
        assertEquals(byteArrayOf(4, 5, 6).toLatin1String(), avatar.body)

        assertEquals("application/json", metadata.header("Content-Type"))
        assertEquals("""{"title":"Q3","pages":2}""", metadata.body)

        val options = parts[5]
        assertEquals("application/json", options.header("Content-Type"))
        assertEquals("""{"flag":true}""", options.body)

        val (url, description) = parts.drop(6)
        assertNull(url.header("Content-Type"))
        assertEquals("https://example.org/x", url.body)
        assertNull(description.header("Content-Type"))
        assertEquals("body", description.body)
    }

    @Test
    fun requiredMultipartBodySkipsAbsentParts() = runTest {
        val request = capture {
            DefaultApi.multipartTyped(file = FilePart(byteArrayOf(1)), url = "u")
        }

        assertNull(request.url.parameters["description"])
        assertEquals(listOf("file", "url"), request.renderParts().map { it.name })
    }

    @Test
    fun contentMediaTypeIsDefaultContentType() = runTest {
        val request = capture {
            DefaultApi.multipartMediaTypes(
                image = FilePart(byteArrayOf(1)),
                document = FilePart(byteArrayOf(2)),
                files = listOf(FilePart(byteArrayOf(3)), FilePart(byteArrayOf(4), contentType = ContentType.Text.Plain))
            )
        }

        val parts = request.renderParts()
        assertEquals(listOf("image", "document", "files", "files"), parts.map { it.name })
        assertEquals(
            listOf("image/png", "application/pdf", "application/octet-stream", "text/plain"),
            parts.map { it.header("Content-Type") }
        )
    }

    @Test
    fun wildcardContentMediaTypeFallsBackToOctetStream() = runTest {
        val request = capture {
            DefaultApi.multipartMediaTypes(image = FilePart(byteArrayOf(1)), wildcard = FilePart(byteArrayOf(2)))
        }

        val wildcard = request.renderParts().last()
        assertEquals("wildcard", wildcard.name)
        assertEquals("application/octet-stream", wildcard.header("Content-Type"))
    }

    @Test
    fun encodingContentTypeKeepsParameters() = runTest {
        val request = capture {
            DefaultApi.multipartMediaTypes(image = FilePart(byteArrayOf(1)), notes = FilePart(byteArrayOf(2)))
        }

        val notes = request.renderParts().last()
        assertEquals("notes", notes.name)
        assertEquals("text/plain; charset=utf-8", notes.header("Content-Type"))
    }

    @Test
    fun contentEncodingMakesTextPartWithOrWithoutType() = runTest {
        val request = capture {
            DefaultApi.multipartMediaTypes(image = FilePart(byteArrayOf(1)), encoded = "AQID", encodedTyped = "BAUG")
        }

        val (encoded, encodedTyped) = request.renderParts().drop(1)
        assertEquals("""form-data; name="encoded"""", encoded.header("Content-Disposition"))
        assertNull(encoded.header("Content-Type"))
        assertEquals("AQID", encoded.body)
        assertEquals("""form-data; name="encodedTyped"""", encodedTyped.header("Content-Disposition"))
        assertNull(encodedTyped.header("Content-Type"))
        assertEquals("BAUG", encodedTyped.body)
    }

    @Test
    fun formatWinsOverContentMediaType() = runTest {
        val token = Uuid.parse("123e4567-e89b-12d3-a456-426614174000")
        val request = capture {
            DefaultApi.multipartMediaTypes(image = FilePart(byteArrayOf(1)), token = token)
        }

        val part = request.renderParts().last()
        assertEquals("""form-data; name="token"""", part.header("Content-Disposition"))
        assertNull(part.header("Content-Type"))
        assertEquals(token.toString(), part.body)
    }

    @Test
    fun wildcardContentTypeOnValuePartKeepsMultiPartFormDataContentParameter() = runTest {
        val request = capture {
            DefaultApi.multipartWildcard(body = MultiPartFormDataContent(formData { append("photo", "raw") }))
        }

        val part = request.renderParts().single()
        assertEquals("photo", part.name)
        assertEquals("raw", part.body)
    }

    @Test
    fun optionalMultipartBodySendsPresentParts() = runTest {
        val request = capture {
            DefaultApi.multipartOptional(file = FilePart(byteArrayOf(7, 8), filename = "data.bin"))
        }

        val part = request.renderParts().single()
        assertEquals("""form-data; name="file"; filename="data.bin"""", part.header("Content-Disposition"))
        assertEquals("application/octet-stream", part.header("Content-Type"))
        assertEquals(byteArrayOf(7, 8).toLatin1String(), part.body)
    }

    @Test
    fun optionalMultipartBodyWithTextPartOnly() = runTest {
        val request = capture {
            DefaultApi.multipartOptional(note = "hello")
        }

        val part = request.renderParts().single()
        assertEquals("note", part.name)
        assertEquals("hello", part.body)
    }

    @Test
    fun optionalMultipartBodyWithoutPartsSendsNoBody() = runTest {
        val request = capture {
            DefaultApi.multipartOptional()
        }

        assertIs<EmptyContent>(request.body)
    }

    @Test
    fun typedUrlEncodedBody() = runTest {
        val request = capture {
            DefaultApi.urlEncodedTyped(UrlencodedTypedRequest(name = "a b&c", count = 3, tags = listOf("x", "y")))
        }

        assertEquals(ContentType.Application.FormUrlEncoded, request.body.contentType?.withoutParameters())
        assertEquals("name=a+b%26c&count=3&tags=x&tags=y", request.body.toByteArray().decodeToString())
    }

    private suspend fun capture(call: suspend () -> Either<*, *>): HttpRequestData {
        var captured: HttpRequestData? = null
        Api.updateClient(MockEngine { request ->
            captured = request
            respond("")
        })
        val result = try {
            call()
        } finally {
            Api.updateClient()
        }
        assertTrue(result.isRight(), result.toString())
        return assertNotNull(captured)
    }
}

private class RenderedPart(
    val headers: List<String>,
    val body: String
) {
    val name: String? get() = NAME_REGEX.find(header("Content-Disposition").orEmpty())?.groupValues?.get(1)

    fun header(name: String): String? = headers.firstOrNull { it.startsWith("$name: ", ignoreCase = true) }?.substringAfter(": ")

    override fun toString(): String = "$headers\n$body"

    private companion object {
        val NAME_REGEX = Regex("""(?:^|; )name="([^"]*)"""")
    }
}

// splits the rendered multipart body into parts, one byte per char
private suspend fun HttpRequestData.renderParts(): List<RenderedPart> {
    val rendered = body.toByteArray().toLatin1String()
    val delimiter = rendered.substringBefore("\r\n")
    return rendered.split(delimiter).drop(1).dropLast(1).map { raw ->
        val part = raw.removePrefix("\r\n").removeSuffix("\r\n")
        RenderedPart(
            headers = part.substringBefore("\r\n\r\n").split("\r\n"),
            body = part.substringAfter("\r\n\r\n")
        )
    }
}

private fun ByteArray.toLatin1String(): String = buildString(size) {
    this@toLatin1String.forEach { append((it.toInt() and 0xFF).toChar()) }
}
