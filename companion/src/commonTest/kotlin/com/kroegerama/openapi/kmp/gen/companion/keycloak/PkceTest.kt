package com.kroegerama.openapi.kmp.gen.companion.keycloak

import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PkceTest {

    @Test
    fun rfc7636AppendixBVector() {
        val pkce = Pkce("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")

        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", pkce.codeChallenge)
    }

    @Test
    fun generateProducesValidUniqueVerifiers() = runTest {
        val first = Pkce.generate()
        val second = Pkce.generate()

        assertEquals(64, first.codeVerifier.length)
        assertTrue(first.codeVerifier.all { it in "0123456789abcdef" }, first.codeVerifier)
        assertNotEquals(first.codeVerifier, second.codeVerifier)
    }

    @Test
    fun challengeShape() = runTest {
        val challenge = Pkce.generate().codeChallenge

        // base64url of 32 bytes without padding
        assertEquals(43, challenge.length)
        assertFalse(challenge.any { it in "=+/" }, challenge)
    }

    @Test
    fun rejectsInvalidVerifiers() {
        assertFailsWith<IllegalArgumentException> { Pkce("a".repeat(42)) }
        assertFailsWith<IllegalArgumentException> { Pkce("a".repeat(129)) }
        assertFailsWith<IllegalArgumentException> { Pkce("!" + "a".repeat(42)) }
    }

    @Test
    fun toStringRedactsVerifier() = runTest {
        val pkce = Pkce.generate()

        assertFalse(pkce.codeVerifier in pkce.toString(), pkce.toString())
    }

    @Test
    fun serializationRoundTrip() = runTest {
        val json = createDefaultJson()
        val pkce = Pkce.generate()

        val restored = json.decodeFromString<Pkce>(json.encodeToString(pkce))

        assertEquals(pkce, restored)
        assertEquals(pkce.codeChallenge, restored.codeChallenge)
    }
}
