package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock.System
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class JWTTest {

    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    private fun encode(json: String): String = base64.encode(json.encodeToByteArray())

    private fun createToken(header: String, payload: String, signature: String = "sig"): String {
        return "${encode(header)}.${encode(payload)}.${encode(signature)}"
    }

    @Test
    fun testParseValidToken() {
        val token = createToken(
            """{"alg":"HS256","typ":"JWT"}""",
            """{"sub":"1234567890","name":"John Doe","iat":1516239022}"""
        )

        val jwt = JWT.parse(token)

        assertEquals("1234567890", jwt.subject)
        assertEquals(Instant.fromEpochSeconds(1516239022), jwt.issuedAt)
        assertNotNull(jwt.getClaim("name"))
        assertEquals("John Doe", (jwt.getClaim("name") as JsonPrimitive).content)
    }

    @Test
    fun testParseAudienceAsString() {
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"aud":"api.example.com"}"""
        )

        val jwt = JWT.parse(token)

        assertEquals(listOf("api.example.com"), jwt.audience)
    }

    @Test
    fun testParseAudienceAsArray() {
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"aud":["api.example.com","admin.example.com"]}"""
        )

        val jwt = JWT.parse(token)

        assertEquals(listOf("api.example.com", "admin.example.com"), jwt.audience)
    }

    @Test
    fun testParseAllStandardClaims() {
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"iss":"issuer","sub":"subject","aud":"audience","exp":1735689600,"nbf":1704067200,"iat":1704067200,"jti":"id123"}"""
        )

        val jwt = JWT.parse(token)

        assertEquals("issuer", jwt.issuer)
        assertEquals("subject", jwt.subject)
        assertEquals(listOf("audience"), jwt.audience)
        assertEquals(Instant.fromEpochSeconds(1735689600), jwt.expiresAt)
        assertEquals(Instant.fromEpochSeconds(1704067200), jwt.notBefore)
        assertEquals(Instant.fromEpochSeconds(1704067200), jwt.issuedAt)
        assertEquals("id123", jwt.id)
    }

    @Test
    fun testParseFractionalNumericDate() {
        // RFC 7519 NumericDate may be non-integer; fractional seconds must be preserved.
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"exp":1516239022.5,"iat":1516239022}"""
        )

        val jwt = JWT.parse(token)

        assertEquals(Instant.fromEpochSeconds(1516239022, 500_000_000), jwt.expiresAt)
        assertEquals(Instant.fromEpochSeconds(1516239022), jwt.issuedAt)
    }

    @Test
    fun testParseCustomClaims() {
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"sub":"user","role":"admin","permissions":["read","write"]}"""
        )

        val jwt = JWT.parse(token)

        assertNotNull(jwt.getClaim("role"))
        assertNotNull(jwt.getClaim("permissions"))
        assertNull(jwt.getClaim("nonexistent"))
    }

    @Test
    fun testParseHeaderWithNonStringValues() {
        val token = createToken(
            """{"alg":"HS256","kid":12345,"custom":true}""",
            """{"sub":"user"}"""
        )

        val jwt = JWT.parse(token)

        assertEquals("user", jwt.subject)
        assertEquals(3, jwt.header.size)
    }

    @Test
    fun testParseInvalidTokenTooFewParts() {
        assertFailsWith<IllegalArgumentException> {
            JWT.parse("invalid.token")
        }
    }

    @Test
    fun testParseInvalidTokenTooManyParts() {
        assertFailsWith<IllegalArgumentException> {
            JWT.parse("too.many.parts.here")
        }
    }

    @Test
    fun testParseInvalidBase64() {
        assertFailsWith<Exception> {
            JWT.parse("invalid!!!.base64!!!.sig")
        }
    }

    @Test
    fun testParseInvalidJson() {
        val invalidToken = "${encode("{invalid json}")}.${encode("{}")}.sig"
        assertFailsWith<Exception> {
            JWT.parse(invalidToken)
        }
    }

    @Test
    fun testParseOrNullValidToken() {
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"sub":"user"}"""
        )

        val jwt = JWT.parseOrNull(token)

        assertNotNull(jwt)
        assertEquals("user", jwt.subject)
    }

    @Test
    fun testParseOrNullInvalidToken() {
        val jwt = JWT.parseOrNull("invalid.token")
        assertNull(jwt)
    }

    @Test
    fun testIsExpiredWithExpiredToken() {
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"exp":1000000000}"""
        )

        val jwt = JWT.parse(token)
        assertTrue(jwt.isExpired())
    }

    @Test
    fun testIsExpiredWithValidToken() {
        val futureExp = Instant.fromEpochSeconds(9999999999)
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"exp":${futureExp.epochSeconds}}"""
        )

        val jwt = JWT.parse(token)
        assertTrue(!jwt.isExpired())
    }

    @Test
    fun testIsExpiredWithLeeway() {
        val recentlyExpired = System.now().epochSeconds - 5
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"exp":$recentlyExpired}"""
        )

        val jwt = JWT.parse(token)
        assertTrue(jwt.isExpired(leeway = Duration.ZERO))
        assertTrue(!jwt.isExpired(leeway = 10.seconds))
    }

    @Test
    fun testIsTimeValidAllValid() {
        val now = System.now().epochSeconds
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"iat":${now - 100},"nbf":${now - 50},"exp":${now + 1000}}"""
        )

        val jwt = JWT.parse(token)
        assertTrue(jwt.isTimeValid())
    }

    @Test
    fun testIsTimeValidExpired() {
        val now = System.now().epochSeconds
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"exp":${now - 100}}"""
        )

        val jwt = JWT.parse(token)
        assertTrue(!jwt.isTimeValid())
    }

    @Test
    fun testIsTimeValidNotYetValid() {
        val now = System.now().epochSeconds
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"nbf":${now + 100}}"""
        )

        val jwt = JWT.parse(token)
        assertTrue(!jwt.isTimeValid())
    }

    @Test
    fun testIsTimeValidIssuedInFuture() {
        val now = System.now().epochSeconds
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"iat":${now + 100}}"""
        )

        val jwt = JWT.parse(token)
        assertTrue(!jwt.isTimeValid())
    }

    @Test
    fun testParsePreEpochFractionalNumericDate() {
        // floor-based conversion must round pre-epoch fractional values towards negative infinity
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"exp":-0.5}"""
        )

        val jwt = JWT.parse(token)

        assertEquals(Instant.fromEpochSeconds(-1, 500_000_000), jwt.expiresAt)
    }

    @Test
    fun testSignatureIsPreserved() {
        val token = createToken("""{"alg":"HS256"}""", """{"sub":"user"}""", signature = "my-signature")
        val jwt = JWT.parse(token)

        // the third token part is kept verbatim (still base64), not decoded
        assertEquals(encode("my-signature"), jwt.signature)
    }

    @Test
    fun testNegativeLeewayThrows() {
        val token = createToken("""{"alg":"HS256"}""", """{"exp":1000000000}""")
        val jwt = JWT.parse(token)

        assertFailsWith<IllegalArgumentException> { jwt.isExpired(leeway = (-1).seconds) }
        assertFailsWith<IllegalArgumentException> { jwt.isTimeValid(leeway = (-1).seconds) }
    }

    @Test
    fun testTimeChecksWithoutTimeClaims() {
        val token = createToken("""{"alg":"HS256"}""", """{"sub":"user"}""")
        val jwt = JWT.parse(token)

        // a token without exp/nbf/iat is never expired and always time-valid
        assertFalse(jwt.isExpired())
        assertTrue(jwt.isTimeValid())
    }

    @Test
    fun testParseAudienceInvalidTypes() {
        val invalidAudiences = listOf(
            "123",
            "true",
            """{"a":1}""",
            "[1,2]",
            """["ok",false]"""
        )

        for (aud in invalidAudiences) {
            val token = createToken("""{"alg":"HS256"}""", """{"aud":$aud}""")
            assertFailsWith<SerializationException>("Expected failure for aud: $aud") {
                JWT.parse(token)
            }
            assertNull(JWT.parseOrNull(token), "Expected null for aud: $aud")
        }
    }

    @Test
    fun testToHumanReadableString() {
        val token = createToken(
            """{"alg":"HS256"}""",
            """{"iss":"issuer","sub":"subject","role":"admin"}"""
        )
        val jwt = JWT.parse(token)
        val readable = jwt.toHumanReadableString()

        assertTrue("issuer=issuer" in readable, readable)
        assertTrue("subject=subject" in readable, readable)
        assertTrue("role" in readable, readable)
    }

    @Test
    fun testAudienceSerializeSingle() {
        val payload = JWTPayload(aud = listOf("api.example.com"))
        val json = Json.encodeToString(payload)

        // A single audience is written as a bare string, and round-trips.
        assertTrue(""""aud":"api.example.com"""" in json, "Actual: $json")
        assertEquals(payload, Json.decodeFromString<JWTPayload>(json))
    }

    @Test
    fun testAudienceSerializeMultiple() {
        val payload = JWTPayload(aud = listOf("api.example.com", "admin.example.com"))
        val json = Json.encodeToString(payload)

        // Multiple audiences are written as an array, and round-trip.
        assertTrue(""""aud":["api.example.com","admin.example.com"]""" in json, "Actual: $json")
        assertEquals(payload, Json.decodeFromString<JWTPayload>(json))
    }

    @Test
    fun testToString() {
        val tokenString = createToken("""{"alg":"HS256"}""", """{"sub":"user"}""")
        val jwt = JWT.parse(tokenString)

        assertEquals(tokenString, jwt.toString())
    }
}
