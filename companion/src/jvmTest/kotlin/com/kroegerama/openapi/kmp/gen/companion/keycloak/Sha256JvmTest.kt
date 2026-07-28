package com.kroegerama.openapi.kmp.gen.companion.keycloak

import java.security.MessageDigest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals

class Sha256JvmTest {

    @Test
    fun matchesMessageDigestForAllLengthsUpTo256() {
        val random = Random(42)
        val reference = MessageDigest.getInstance("SHA-256")
        for (length in 0..256) {
            val input = random.nextBytes(length)
            assertContentEquals(
                reference.digest(input),
                sha256(input),
                "length $length"
            )
        }
    }
}
