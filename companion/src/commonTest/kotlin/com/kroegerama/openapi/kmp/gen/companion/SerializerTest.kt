package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.expect
import kotlin.time.Instant

@Suppress("ArrayInDataClass")
class SerializerTest {

    @Serializable
    private data class Base64(
        val bytes: SerializableBase64
    )

    @Test
    fun base64SerializerTest() {
        val payload = Base64("Hello World".encodeToByteArray())
        val json = """{"bytes":"SGVsbG8gV29ybGQ="}"""
        expect(json) { Json.encodeToString(payload) }
        val decoded = Json.decodeFromString<Base64>(json)
        assertContentEquals(payload.bytes, decoded.bytes)
    }

    @Serializable
    private data class EpochSeconds(
        val dateTime: SerializableEpochSeconds
    )

    @Test
    fun epochSecondsTest() {
        val payload = EpochSeconds(Instant.fromEpochSeconds(1772535600))
        val json = """{"dateTime":1772535600}"""
        expect(json) { Json.encodeToString(payload) }
        val decoded = Json.decodeFromString<EpochSeconds>(json)
        assertEquals(payload, decoded)
    }

    @Serializable
    private data class EpochMilliseconds(
        val dateTime: SerializableEpochMilliseconds
    )

    @Test
    fun epochMillisecondsTest() {
        val payload = EpochMilliseconds(Instant.fromEpochMilliseconds(1772535600000))
        val json = """{"dateTime":1772535600000}"""
        expect(json) { Json.encodeToString(payload) }
        val decoded = Json.decodeFromString<EpochMilliseconds>(json)
        assertEquals(payload, decoded)
    }

    @Serializable
    private data class ImmutableList(
        val strings: SerializableImmutableList<String>,
        val dates: SerializableImmutableList<Instant>,
        val seconds: SerializableImmutableList<SerializableEpochSeconds>
    )

    @Test
    fun immutableListTest() {
        val instantList = persistentListOf(
            Instant.fromEpochSeconds(1772535600),
            Instant.fromEpochSeconds(1772539200)
        )
        val payload = ImmutableList(
            strings = persistentListOf("Hello", "World"),
            dates = instantList,
            seconds = instantList
        )
        val json = """{"strings":["Hello","World"],"dates":["2026-03-03T11:00:00Z","2026-03-03T12:00:00Z"],"seconds":[1772535600,1772539200]}"""
        expect(json) { Json.encodeToString(payload) }
        val decoded = Json.decodeFromString<ImmutableList>(json)
        assertEquals(payload, decoded)
    }
}
