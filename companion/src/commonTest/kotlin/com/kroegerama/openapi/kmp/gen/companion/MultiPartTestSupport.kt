package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal data class RenderedPart(
    val headers: List<String>,
    val body: String
) {
    val name: String? get() = NAME_REGEX.find(header("Content-Disposition").orEmpty())?.groupValues?.get(1)

    fun header(name: String): String? = headers.firstOrNull { it.startsWith("$name: ", ignoreCase = true) }?.substringAfter(": ")

    private companion object {
        val NAME_REGEX = Regex("""(?:^|; )name="([^"]*)"""")
    }
}

// sends every content through one MockEngine client and returns the rendered request bodies, one byte per char
internal suspend fun renderMultiPart(vararg contents: MultiPartFormDataContent): List<String> {
    val bodies = mutableListOf<String>()
    val client = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                bodies += request.body.toByteArray().toLatin1String()
                respond("ok")
            }
        }
    }
    try {
        contents.forEach { content ->
            client.post("https://example.com/upload") {
                setBody(content)
            }
        }
    } finally {
        client.close()
    }
    return bodies
}

internal suspend fun renderParts(content: MultiPartFormDataContent): List<RenderedPart> = parseParts(renderMultiPart(content).single())

internal fun parseParts(body: String): List<RenderedPart> {
    val delimiter = body.substringBefore("\r\n")
    return body.split(delimiter).drop(1).dropLast(1).map { raw ->
        val part = raw.removePrefix("\r\n").removeSuffix("\r\n")
        RenderedPart(
            headers = part.substringBefore("\r\n\r\n").split("\r\n"),
            body = part.substringAfter("\r\n\r\n")
        )
    }
}

internal fun ByteArray.toLatin1String(): String = buildString(size) {
    this@toLatin1String.forEach { append((it.toInt() and 0xFF).toChar()) }
}
