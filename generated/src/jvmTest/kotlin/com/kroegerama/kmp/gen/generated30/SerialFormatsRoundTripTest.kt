package com.kroegerama.kmp.gen.generated30

import com.kroegerama.kmp.gen.generated30.models.SerialTest
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SerialFormatsRoundTripTest {

    private val json = createDefaultJson()

    @Test
    fun allFormatsDecodeFromIsoStrings() {
        val decoded = json.decodeFromString<SerialTest>(
            """
            {
              "date": "2026-05-05",
              "time": "10:15:30",
              "instant": "2026-05-05T10:15:30Z",
              "duration": "PT1H30M",
              "uuid": "550e8400-e29b-41d4-a716-446655440000",
              "base64": "AQID"
            }
            """.trimIndent()
        )

        assertEquals(LocalDate(2026, 5, 5), decoded.date)
        assertEquals(LocalTime(10, 15, 30), decoded.time)
        assertEquals(Instant.parse("2026-05-05T10:15:30Z"), decoded.instant)
        assertEquals(90.minutes, decoded.duration)
        assertEquals(Uuid.parse("550e8400-e29b-41d4-a716-446655440000"), decoded.uuid)
        assertContentEquals(byteArrayOf(1, 2, 3), decoded.base64)
    }

    @Test
    fun instantDecodesWithUtcOffset() {
        val decoded = json.decodeFromString<SerialTest>("""{"instant": "2026-05-05T12:15:30+02:00"}""")
        assertEquals(Instant.parse("2026-05-05T10:15:30Z"), decoded.instant)
    }

    @Test
    fun instantDecodesAlternativeOffsetFormats() {
        val expected = Instant.parse("2026-05-05T10:15:30Z")

        val fourDigits = json.decodeFromString<SerialTest>("""{"instant": "2026-05-05T12:15:30+0200"}""")
        assertEquals(expected, fourDigits.instant)

        val hoursOnly = json.decodeFromString<SerialTest>("""{"instant": "2026-05-05T12:15:30+02"}""")
        assertEquals(expected, hoursOnly.instant)

        val negativeOffset = json.decodeFromString<SerialTest>("""{"instant": "2026-05-05T07:45:30-02:30"}""")
        assertEquals(expected, negativeOffset.instant)
    }

    @Test
    fun instantDecodesFractionalSeconds() {
        val decoded = json.decodeFromString<SerialTest>("""{"instant": "2026-05-05T10:15:30.123Z"}""")
        assertEquals(Instant.parse("2026-05-05T10:15:30.123Z"), decoded.instant)
    }

    @Test
    fun allFormatsSurviveRoundTrip() {
        val value = SerialTest(
            date = LocalDate(2026, 5, 5),
            time = LocalTime(10, 15, 30),
            instant = Instant.parse("2026-05-05T10:15:30Z"),
            duration = 90.minutes,
            uuid = Uuid.parse("550e8400-e29b-41d4-a716-446655440000"),
            base64 = byteArrayOf(1, 2, 3),
        )

        val restored = json.decodeFromString<SerialTest>(json.encodeToString(value))

        assertEquals(value.date, restored.date)
        assertEquals(value.time, restored.time)
        assertEquals(value.instant, restored.instant)
        assertEquals(value.duration, restored.duration)
        assertEquals(value.uuid, restored.uuid)
        assertContentEquals(value.base64, restored.base64)
    }

    @Test
    fun epochFormatsUseNumericEncoding() {
        val decoded = json.decodeFromString<SerialTest>(
            """{"epochSeconds": 1746439530, "epochMillis": 1746439530123}"""
        )
        assertEquals(Instant.fromEpochSeconds(1746439530), decoded.epochSeconds)
        assertEquals(Instant.fromEpochMilliseconds(1746439530123), decoded.epochMillis)

        val encoded = json.parseToJsonElement(
            json.encodeToString(
                SerialTest(
                    epochSeconds = Instant.fromEpochSeconds(1746439530),
                    epochMillis = Instant.fromEpochMilliseconds(1746439530123),
                )
            )
        ).jsonObject
        assertEquals("1746439530", encoded.getValue("epochSeconds").jsonPrimitive.content)
        assertEquals("1746439530123", encoded.getValue("epochMillis").jsonPrimitive.content)
    }

    @Test
    fun encodedFieldsAreIsoStrings() {
        val value = SerialTest(
            date = LocalDate(2026, 5, 5),
            duration = 90.minutes,
            base64 = byteArrayOf(1, 2, 3),
        )

        val encoded = json.parseToJsonElement(json.encodeToString(value)).jsonObject

        assertEquals("2026-05-05", encoded.getValue("date").jsonPrimitive.content)
        assertEquals("PT1H30M", encoded.getValue("duration").jsonPrimitive.content)
        assertEquals("AQID", encoded.getValue("base64").jsonPrimitive.content)
    }
}
