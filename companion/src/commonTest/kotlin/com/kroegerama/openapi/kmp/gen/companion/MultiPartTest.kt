package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class MultiPartTest {

    @Serializable
    private data class Item(
        val id: Int,
        val tags: List<String>
    )

    @Test
    fun filePartSendsFilenameContentTypeAndBytes() = runTest {
        val bytes = byteArrayOf(0, 1, 127, -128, -1)
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendFilePart(
                        name = "file",
                        value = FilePart(bytes, filename = "report.pdf", contentType = ContentType.Application.Pdf),
                        defaultContentType = ContentType.Application.OctetStream
                    )
                }
            )
        )

        val part = parts.single()
        assertEquals("file", part.name)
        assertEquals("""form-data; name="file"; filename="report.pdf"""", part.header("Content-Disposition"))
        assertEquals("application/pdf", part.header("Content-Type"))
        assertEquals("5", part.header("Content-Length"))
        assertEquals(bytes.toLatin1String(), part.body)
    }

    @Test
    fun filePartWithoutFilenameSendsPartNameAndDefaultContentType() = runTest {
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendFilePart(name = "image", value = FilePart("png".encodeToByteArray()), defaultContentType = ContentType.Image.PNG)
                }
            )
        )

        val part = parts.single()
        assertEquals("""form-data; name="image"; filename="image"""", part.header("Content-Disposition"))
        assertEquals("image/png", part.header("Content-Type"))
        assertEquals("png", part.body)
    }

    @Test
    fun filePartQuotesFilename() = runTest {
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendFilePart(
                        name = "file",
                        value = FilePart(byteArrayOf(1), filename = "a\"b.txt"),
                        defaultContentType = ContentType.Application.OctetStream
                    )
                }
            )
        )

        assertEquals("""form-data; name="file"; filename="a\"b.txt"""", parts.single().header("Content-Disposition"))
    }

    @Test
    fun filePartsAppendedPerItem() = runTest {
        val files = listOf(FilePart("a".encodeToByteArray(), filename = "a.txt"), FilePart("b".encodeToByteArray(), filename = "b.txt"))
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    files.forEach { appendFilePart(name = "files", value = it, defaultContentType = ContentType.Text.Plain) }
                }
            )
        )

        assertEquals(listOf("files", "files"), parts.map { it.name })
        assertEquals(listOf("a", "b"), parts.map { it.body })
        assertEquals(listOf("text/plain", "text/plain"), parts.map { it.header("Content-Type") })
    }

    @Test
    fun nullFilePartAppendsNothing() = runTest {
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendFilePart(name = "file", value = null, defaultContentType = ContentType.Application.OctetStream)
                    appendSerializedPart(name = "description", value = "Q3", json = ApiJson)
                }
            )
        )

        assertEquals(listOf("description"), parts.map { it.name })
    }

    @Test
    fun filePartWithoutSizeHasNoContentLength() = runTest {
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendFilePart(
                        name = "file",
                        value = FilePart { ByteReadChannel("streamed") },
                        defaultContentType = ContentType.Application.OctetStream
                    )
                }
            )
        )

        val part = parts.single()
        assertNull(part.header("Content-Length"))
        assertEquals("streamed", part.body)
    }

    @Test
    fun filePartProviderIsCalledPerSend() = runTest {
        var calls = 0
        val file = FilePart(size = 3, filename = "abc.txt") {
            calls++
            ByteReadChannel("abc")
        }
        val content = MultiPartFormDataContent(
            formData {
                appendFilePart(name = "file", value = file, defaultContentType = ContentType.Text.Plain)
            }
        )

        val bodies = renderMultiPart(content, content)

        assertEquals(2, calls)
        assertEquals(bodies[0], bodies[1])
        assertEquals("abc", parseParts(bodies[1]).single().body)
    }

    @Test
    fun byteArrayFilePartIsReadPerSend() = runTest {
        val content = MultiPartFormDataContent(
            formData {
                appendFilePart(name = "file", value = FilePart("abc".encodeToByteArray()), defaultContentType = ContentType.Text.Plain)
            }
        )

        val bodies = renderMultiPart(content, content)

        assertEquals(listOf("abc", "abc"), bodies.map { parseParts(it).single().body })
    }

    @Test
    fun serializedPartSendsPrimitivesAsText() = runTest {
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendSerializedPart(name = "name", value = "Alice", json = ApiJson)
                    appendSerializedPart(name = "age", value = 30, json = ApiJson)
                    appendSerializedPart(name = "active", value = true, json = ApiJson)
                }
            )
        )

        assertEquals(listOf("name", "age", "active"), parts.map { it.name })
        assertEquals(listOf("Alice", "30", "true"), parts.map { it.body })
        assertTrue(parts.all { it.header("Content-Type") == null }, parts.toString())
    }

    @Test
    fun serializedPartSendsOnePartPerArrayItem() = runTest {
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendSerializedPart(name = "tags", value = listOf("a", null, "b"), json = ApiJson)
                }
            )
        )

        assertEquals(listOf("tags", "tags"), parts.map { it.name })
        assertEquals(listOf("a", "b"), parts.map { it.body })
        assertTrue(parts.all { it.header("Content-Type") == null }, parts.toString())
    }

    @Test
    fun serializedPartSendsObjectsAsJson() = runTest {
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendSerializedPart(name = "item", value = Item(id = 1, tags = listOf("a")), json = ApiJson)
                }
            )
        )

        val part = parts.single()
        assertEquals("item", part.name)
        assertEquals("application/json", part.header("Content-Type"))
        assertEquals("""{"id":1,"tags":["a"]}""", part.body)
    }

    @Test
    fun serializedPartSendsObjectAndArrayItemsAsJson() = runTest {
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendSerializedPart(name = "items", value = listOf(Item(id = 1, tags = emptyList()), null), json = ApiJson)
                    appendSerializedPart(name = "matrix", value = listOf(listOf("a"), listOf("b")), json = ApiJson)
                }
            )
        )

        assertEquals(listOf("items", "matrix", "matrix"), parts.map { it.name })
        assertEquals(listOf("""{"id":1,"tags":[]}""", """["a"]""", """["b"]"""), parts.map { it.body })
        assertTrue(parts.all { it.header("Content-Type") == "application/json" }, parts.toString())
    }

    @Test
    fun serializedPartSkipsNullValues() = runTest {
        val missing: String? = null
        val missingInstant: Instant? = null
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendSerializedPart(name = "missing", value = missing, json = ApiJson)
                    appendSerializedPart(name = "jsonNull", value = JsonNull, json = ApiJson)
                    appendSerializedPart(name = "explicit", value = missingInstant, serializer = ISO8601InstantSerializer, json = ApiJson)
                }
            )
        )

        assertTrue(parts.isEmpty(), parts.toString())
    }

    @Test
    fun serializedPartUsesExplicitSerializer() = runTest {
        val instant = Instant.parse("2024-02-23T09:50:31Z")
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendSerializedPart(name = "iso", value = instant, serializer = ISO8601InstantSerializer, json = ApiJson)
                    appendSerializedPart(name = "seconds", value = instant, serializer = EpochSecondsSerializer, json = ApiJson)
                }
            )
        )

        assertEquals(listOf("2024-02-23T09:50:31Z", "1708681831"), parts.map { it.body })
    }

    @Test
    fun serializedPartUsesProvidedJson() = runTest {
        val parts = renderParts(
            MultiPartFormDataContent(
                formData {
                    appendSerializedPart(name = "person", value = Person("Ada"), json = snakeCaseJson)
                }
            )
        )

        assertEquals("""{"first_name":"Ada"}""", parts.single().body)
    }
}
