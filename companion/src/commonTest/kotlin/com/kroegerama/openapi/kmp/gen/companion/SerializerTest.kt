package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.expect
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
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

    @Serializable
    private data class ISO8601Instant(
        val dateTime: SerializableISO8601Instant
    )

    @Test
    fun iso8601InstantSerializeTest() {
        val instant = Instant.parse("2024-02-23T09:50:31Z")
        val payload = ISO8601Instant(instant)
        val json = """{"dateTime":"2024-02-23T09:50:31Z"}"""
        expect(json) { Json.encodeToString(payload) }
        val decoded = Json.decodeFromString<ISO8601Instant>(json)
        assertEquals(payload, decoded)
    }

    @Test
    fun iso8601InstantParseVariantsTest() {
        val base = Instant.parse("2024-02-23T09:50:31Z")
        val offsetH2 = 2.hours
        val offsetH2M3 = 2.hours + 3.minutes
        val offsetH2M3S4 = 2.hours + 3.minutes + 4.seconds
        val nanos1 = 123_000_000.nanoseconds
        val nanos2 = 123_456_789.nanoseconds

        val cases = listOf(
            "2024-02-23T09:50:31Z" to base,
            "2024-02-23T09:50:31.123Z" to base + nanos1,

            "2024-02-23T09:50:31+00:00" to base,
            "2024-02-23T09:50:31+02:00" to base - offsetH2,
            "2024-02-23T09:50:31+02:03:04" to base - offsetH2M3S4,
            "2024-02-23T09:50:31+0000" to base,
            "2024-02-23T09:50:31+02" to base - offsetH2,
            "2024-02-23T09:50:31+0200" to base - offsetH2,
            "2024-02-23T09:50:31+020304" to base - offsetH2M3S4,

            "2024-02-23T09:50:31.123+00:00" to base + nanos1,
            "2024-02-23T09:50:31.123+02:00" to base - offsetH2 + nanos1,
            "2024-02-23T09:50:31.123+02:03:04" to base - offsetH2M3S4 + nanos1,
            "2024-02-23T09:50:31.123+0000" to base + nanos1,
            "2024-02-23T09:50:31.123+02" to base - offsetH2 + nanos1,
            "2024-02-23T09:50:31.123+0203" to base - offsetH2M3 + nanos1,
            "2024-02-23T09:50:31.123+020304" to base - offsetH2M3S4 + nanos1,

            "2024-02-23T09:50:31-02:00" to base + offsetH2,
            "2024-02-23T09:50:31.123-0200" to base + offsetH2 + nanos1,

            "2024-02-23T09:50:31.123456789Z" to base + nanos2,

            "2024-02-23t09:50:31Z" to base,
            "2024-02-23T09:50:31z" to base,
            "2024-02-23t09:50:31z" to base,
        )

        for ((input, expected) in cases) {
            val json = """{"dateTime":"$input"}"""
            val decoded = Json.decodeFromString<ISO8601Instant>(json)
            assertEquals(expected, decoded.dateTime, "Failed for input: $input")
        }
    }

    @Test
    fun iso8601InstantParseFailuresTest() {
        val invalidInputs = listOf(
            "2024-02-23T09:50:31",
            "2024-02-23T09:50:31.123",

            "2024-02-23",
            "09:50:31Z",
            "2024-02-23 09:50:31Z",
            "1708678231",

            "",
            "not-a-date",

            "2024-02-23T09:50:31+2:00",
            "2024-02-23T09:50:31+002",

            "2024-13-23T09:50:31Z", // invalid month
            "2024-02-30T09:50:31Z", // invalid day
            "2024-02-23T25:50:31Z", // invalid hour
            "2024-02-23T09:60:31Z", // invalid minute
            "2024-02-23T09:50:61Z", // invalid second
        )

        for (input in invalidInputs) {
            val json = """{"dateTime":"$input"}"""
            assertFails("Expected failure for input: $input") {
                Json.decodeFromString<ISO8601Instant>(json)
            }
        }
    }

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
