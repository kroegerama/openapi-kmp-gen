package com.kroegerama.openapi.kmp.gen.companion.keycloak

import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class Sha256Test {

    private fun hash(input: String): String = sha256(input.encodeToByteArray()).toHexString()

    @Test
    fun emptyInput() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hash("")
        )
    }

    @Test
    fun abc() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hash("abc")
        )
    }

    @Test
    fun twoBlockMessage() {
        // RFC 6234 test vector, 56 bytes -> two blocks after padding
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hash("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq")
        )
    }

    @Test
    fun fourBlockMessage() {
        // RFC 6234 test vector, 112 bytes
        assertEquals(
            "cf5b16a778af8380036ce59e7b0492370b249b11e8f07a51afac45037afee9d1",
            hash("abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu")
        )
    }

    @Test
    fun oneMillionA() {
        // RFC 6234 test vector
        assertEquals(
            "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
            hash("a".repeat(1_000_000))
        )
    }

    @Test
    fun paddingBoundaries() {
        // Around the 55/56-byte boundary (padding + length no longer fit into one block)
        // and the 64-byte block size itself.
        val expected = mapOf(
            55 to "9f4390f8d30c2dd92ec9f095b65e2b9ae9b0a925a5258e241c9f1e910f734318",
            56 to "b35439a4ac6f0948b6d6f9e3c6af0f5f590ce20f1bde7090ef7970686ec6738a",
            63 to "7d3e74a05d7db15bce4ad9ec0658ea98e3f06eeecf16b4c6fff2da457ddc2f34",
            64 to "ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb",
            65 to "635361c48bb9eab14198e76ea8ab7f1a41685d6ad62aa9146d301d4f17eb0ae0"
        )
        expected.forEach { (length, digest) ->
            assertEquals(digest, hash("a".repeat(length)), "length $length")
        }
    }
}
