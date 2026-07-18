package com.kroegerama.kmp.gen.generated31

import com.kroegerama.kmp.gen.generated31.models.ActionResponse
import com.kroegerama.kmp.gen.generated31.models.MergedRequiredTest
import com.kroegerama.kmp.gen.generated31.models.SealedClass1
import com.kroegerama.kmp.gen.generated31.models.SerialTest
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class DecodingFailureTest {

    private val json = createDefaultJson()

    @Test
    fun unknownDiscriminatorValueFails() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<ActionResponse>("""{"kind":"unknown","itemId":"item-1"}""")
        }
        assertFailsWith<SerializationException> {
            json.decodeFromString<SealedClass1>("""{"#discriminator":"C3","commonAttr":"common"}""")
        }
    }

    @Test
    fun missingDiscriminatorFails() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<ActionResponse>("""{"itemId":"item-1"}""")
        }
    }

    @Test
    fun missingRequiredPropertyFails() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<ActionResponse>("""{"kind":"update","oldItemId":"old"}""")
        }
        assertFailsWith<SerializationException> {
            json.decodeFromString<MergedRequiredTest>("""{"outerAttr":"outer"}""")
        }
    }

    @Test
    fun nullForRequiredPropertyWithoutDefaultFails() {
        assertFailsWith<SerializationException> {
            json.decodeFromString<ActionResponse>("""{"kind":"create","itemId":null}""")
        }
    }

    @Test
    fun malformedFormatValuesFail() {
        assertFails { json.decodeFromString<SerialTest>("""{"date":"not-a-date"}""") }
        assertFails { json.decodeFromString<SerialTest>("""{"instant":"2026-05-05"}""") }
        assertFails { json.decodeFromString<SerialTest>("""{"duration":"90 minutes"}""") }
        assertFails { json.decodeFromString<SerialTest>("""{"uuid":"not-a-uuid"}""") }
        assertFails { json.decodeFromString<SerialTest>("""{"base64":"!!!"}""") }
    }
}
