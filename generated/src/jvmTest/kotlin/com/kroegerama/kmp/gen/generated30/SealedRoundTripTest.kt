package com.kroegerama.kmp.gen.generated30

import com.kroegerama.kmp.gen.generated30.models.ActionResponse
import com.kroegerama.kmp.gen.generated30.models.CreateActionResponse
import com.kroegerama.kmp.gen.generated30.models.CreateEventNotification
import com.kroegerama.kmp.gen.generated30.models.DeleteActionResponse
import com.kroegerama.kmp.gen.generated30.models.DeleteEventNotification
import com.kroegerama.kmp.gen.generated30.models.EventNotification
import com.kroegerama.kmp.gen.generated30.models.SealedClass1
import com.kroegerama.kmp.gen.generated30.models.SealedClass1Child1
import com.kroegerama.kmp.gen.generated30.models.SealedClass1Child2
import com.kroegerama.kmp.gen.generated30.models.SealedClass2
import com.kroegerama.kmp.gen.generated30.models.SealedClass2Child1
import com.kroegerama.kmp.gen.generated30.models.SealedClass2Child2
import com.kroegerama.kmp.gen.generated30.models.UpdateActionResponse
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class SealedRoundTripTest {

    private val json = createDefaultJson()

    private inline fun <reified T> roundTrip(value: T, discriminatorProperty: String, expectedDiscriminator: String) {
        val encoded = json.encodeToString(value)
        val discriminator = json.parseToJsonElement(encoded).jsonObject.getValue(discriminatorProperty).jsonPrimitive.content
        assertEquals(expectedDiscriminator, discriminator)
        assertEquals(value, json.decodeFromString<T>(encoded))
    }

    @Test
    fun actionResponseVariantsRoundTrip() {
        roundTrip<ActionResponse>(
            CreateActionResponse(itemId = "item-1"),
            discriminatorProperty = "kind",
            expectedDiscriminator = "create",
        )
        roundTrip<ActionResponse>(
            UpdateActionResponse(oldItemId = "old", newItemId = "new"),
            discriminatorProperty = "kind",
            expectedDiscriminator = "update",
        )
        roundTrip<ActionResponse>(
            DeleteActionResponse(itemId = "item-2"),
            discriminatorProperty = "kind",
            expectedDiscriminator = "delete",
        )
    }

    @Test
    fun eventNotificationVariantsRoundTrip() {
        roundTrip<EventNotification>(
            CreateEventNotification(eventId = "event-1"),
            discriminatorProperty = "kind",
            expectedDiscriminator = "create",
        )
        roundTrip<EventNotification>(
            DeleteEventNotification(eventId = "event-2"),
            discriminatorProperty = "kind",
            expectedDiscriminator = "delete",
        )
    }

    @Test
    fun sealedClass1VariantsRoundTrip() {
        roundTrip<SealedClass1>(
            SealedClass1Child1(commonAttr = "common", child1Only = 1L),
            discriminatorProperty = "#discriminator",
            expectedDiscriminator = "C1",
        )
        roundTrip<SealedClass1>(
            SealedClass1Child2(commonAttr = "common", child2Only = "two"),
            discriminatorProperty = "#discriminator",
            expectedDiscriminator = "C2",
        )
    }

    @Test
    fun sealedClass2VariantsRoundTrip() {
        roundTrip<SealedClass2>(
            SealedClass2Child1(commonAttr = "common", child1Only = 1L),
            discriminatorProperty = "#discriminator",
            expectedDiscriminator = "C1",
        )
        roundTrip<SealedClass2>(
            SealedClass2Child2(commonAttr = "common", child2Only = "two"),
            discriminatorProperty = "#discriminator",
            expectedDiscriminator = "C2",
        )
    }
}
