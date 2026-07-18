package com.kroegerama.kmp.gen.generated31

import com.kroegerama.kmp.gen.generated31.models.IntegerTest
import com.kroegerama.kmp.gen.generated31.models.NumberTest
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlin.test.Test
import kotlin.test.assertEquals

class NumericFormatsRoundTripTest {

    private val json = createDefaultJson()

    @Test
    fun integerFormatsDecodeToExpectedTypes() {
        val decoded = json.decodeFromString<IntegerTest>(
            """
            {
              "unknown": 9223372036854775807,
              "int32": 2147483647,
              "int64": 9223372036854775807,
              "float": 1.5,
              "double": 1.5
            }
            """.trimIndent()
        )

        assertEquals(Long.MAX_VALUE, decoded.unknown)
        assertEquals(Int.MAX_VALUE, decoded.int32)
        assertEquals(Long.MAX_VALUE, decoded.int64)
        assertEquals(1.5f, decoded.float)
        assertEquals(1.5, decoded.double)
    }

    @Test
    fun integerBoundaryValuesSurviveRoundTrip() {
        val value = IntegerTest(
            unknown = Long.MIN_VALUE,
            int32 = Int.MIN_VALUE,
            int64 = Long.MAX_VALUE,
            float = Float.MAX_VALUE,
            double = Double.MAX_VALUE,
        )
        assertEquals(value, json.decodeFromString<IntegerTest>(json.encodeToString(value)))
    }

    @Test
    fun longValuesBeyondDoublePrecisionAreExact() {
        val value = IntegerTest(int64 = 9007199254740993L)
        val decoded = json.decodeFromString<IntegerTest>(json.encodeToString(value))
        assertEquals(9007199254740993L, decoded.int64)
    }

    @Test
    fun numberFormatsDecodeToExpectedTypes() {
        val decoded = json.decodeFromString<NumberTest>(
            """
            {
              "unknown": 1.25,
              "float": 2.5,
              "double": 3.75,
              "int32": 4,
              "int64": 9223372036854775807
            }
            """.trimIndent()
        )

        assertEquals(1.25, decoded.unknown)
        assertEquals(2.5f, decoded.float)
        assertEquals(3.75, decoded.double)
        assertEquals(4, decoded.int32)
        assertEquals(Long.MAX_VALUE, decoded.int64)
    }

    @Test
    fun numberBoundaryValuesSurviveRoundTrip() {
        val value = NumberTest(
            unknown = Double.MIN_VALUE,
            float = -Float.MAX_VALUE,
            double = -Double.MAX_VALUE,
            int32 = Int.MAX_VALUE,
            int64 = Long.MIN_VALUE,
        )
        assertEquals(value, json.decodeFromString<NumberTest>(json.encodeToString(value)))
    }
}
