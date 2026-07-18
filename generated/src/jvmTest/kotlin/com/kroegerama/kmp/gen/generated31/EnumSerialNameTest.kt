package com.kroegerama.kmp.gen.generated31

import com.kroegerama.kmp.gen.generated31.models.SealedClassType
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlin.test.Test
import kotlin.test.assertEquals

class EnumSerialNameTest {

    private val json = createDefaultJson()

    @Test
    fun enumConstantsEncodeToSpecValues() {
        assertEquals("\"C1\"", json.encodeToString(SealedClassType.C_1))
        assertEquals("\"C2\"", json.encodeToString(SealedClassType.C_2))
    }

    @Test
    fun enumConstantsDecodeFromSpecValues() {
        assertEquals(SealedClassType.C_1, json.decodeFromString<SealedClassType>("\"C1\""))
        assertEquals(SealedClassType.C_2, json.decodeFromString<SealedClassType>("\"C2\""))
    }
}
