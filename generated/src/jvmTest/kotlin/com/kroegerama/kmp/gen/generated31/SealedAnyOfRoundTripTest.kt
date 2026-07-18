package com.kroegerama.kmp.gen.generated31

import com.kroegerama.kmp.gen.generated31.models.ActionResponse
import com.kroegerama.kmp.gen.generated31.models.CreateActionResponse
import com.kroegerama.kmp.gen.generated31.models.CreateEventNotification
import com.kroegerama.kmp.gen.generated31.models.DeleteActionResponse
import com.kroegerama.kmp.gen.generated31.models.DeleteEventNotification
import com.kroegerama.kmp.gen.generated31.models.EventNotification
import com.kroegerama.kmp.gen.generated31.models.NullableEventNotification
import com.kroegerama.kmp.gen.generated31.models.UpdateActionResponse
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class SealedAnyOfRoundTripTest {

    private val json = createDefaultJson()

    private inline fun <reified T> roundTrip(value: T, expectedKind: String) {
        val encoded = json.encodeToString(value)
        val kind = json.parseToJsonElement(encoded).jsonObject.getValue("kind").jsonPrimitive.content
        assertEquals(expectedKind, kind)
        assertEquals(value, json.decodeFromString<T>(encoded))
    }

    @Test
    fun actionResponseVariantsRoundTrip() {
        roundTrip<ActionResponse>(CreateActionResponse(itemId = "item-1"), expectedKind = "create")
        roundTrip<ActionResponse>(UpdateActionResponse(oldItemId = "old", newItemId = "new"), expectedKind = "update")
        roundTrip<ActionResponse>(DeleteActionResponse(itemId = "item-2"), expectedKind = "delete")
    }

    @Test
    fun eventNotificationVariantsRoundTrip() {
        roundTrip<EventNotification>(CreateEventNotification(eventId = "event-1"), expectedKind = "create")
        roundTrip<EventNotification>(DeleteEventNotification(eventId = "event-2"), expectedKind = "delete")
    }

    @Test
    fun nullableEventNotificationVariantsRoundTrip() {
        roundTrip<NullableEventNotification>(CreateEventNotification(eventId = "event-1"), expectedKind = "create")
        roundTrip<NullableEventNotification>(DeleteEventNotification(eventId = "event-2"), expectedKind = "delete")
    }
}
