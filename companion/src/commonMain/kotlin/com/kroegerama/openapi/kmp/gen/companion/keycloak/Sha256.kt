package com.kroegerama.openapi.kmp.gen.companion.keycloak

/**
 * Pure-Kotlin SHA-256 (FIPS 180-4).
 *
 * Internal because it exists only for the PKCE `S256` code challenge: Ktor's `Digest` is not
 * available on native targets and the input (the code verifier) is not secret-key material,
 * so a constant-time platform implementation is not required.
 */
internal fun sha256(input: ByteArray): ByteArray {
    val bitLength = input.size.toLong() * 8
    val paddedLength = ((input.size + 8) / 64 + 1) * 64
    val padded = input.copyOf(paddedLength)
    padded[input.size] = 0x80.toByte()
    for (i in 0 until 8) {
        padded[paddedLength - 1 - i] = (bitLength ushr (8 * i)).toByte()
    }

    val h = H.copyOf()
    val w = IntArray(64)
    var offset = 0
    while (offset < paddedLength) {
        for (t in 0 until 16) {
            val i = offset + 4 * t
            w[t] = ((padded[i].toInt() and 0xff) shl 24) or
                ((padded[i + 1].toInt() and 0xff) shl 16) or
                ((padded[i + 2].toInt() and 0xff) shl 8) or
                (padded[i + 3].toInt() and 0xff)
        }
        for (t in 16 until 64) {
            val s0 = w[t - 15].rotateRight(7) xor w[t - 15].rotateRight(18) xor (w[t - 15] ushr 3)
            val s1 = w[t - 2].rotateRight(17) xor w[t - 2].rotateRight(19) xor (w[t - 2] ushr 10)
            w[t] = w[t - 16] + s0 + w[t - 7] + s1
        }

        var a = h[0]
        var b = h[1]
        var c = h[2]
        var d = h[3]
        var e = h[4]
        var f = h[5]
        var g = h[6]
        var hh = h[7]
        for (t in 0 until 64) {
            val bigS1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = hh + bigS1 + ch + K[t] + w[t]
            val bigS0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = bigS0 + maj
            hh = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }
        h[0] += a
        h[1] += b
        h[2] += c
        h[3] += d
        h[4] += e
        h[5] += f
        h[6] += g
        h[7] += hh

        offset += 64
    }

    val out = ByteArray(32)
    for (i in 0 until 8) {
        out[4 * i] = (h[i] ushr 24).toByte()
        out[4 * i + 1] = (h[i] ushr 16).toByte()
        out[4 * i + 2] = (h[i] ushr 8).toByte()
        out[4 * i + 3] = h[i].toByte()
    }
    return out
}

// FIPS 180-4 constants, written as unsigned Long literals so they can be
// copied verbatim from the specification.
private val H = longArrayOf(
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
).toIntArray()

private val K = longArrayOf(
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
).toIntArray()

private fun LongArray.toIntArray(): IntArray = IntArray(size) { this[it].toInt() }
