package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock.System
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
        assertTrue(jwt.isExpired(leeway = 0))
        assertTrue(!jwt.isExpired(leeway = 10))
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
    fun testToString() {
        val tokenString = createToken("""{"alg":"HS256"}""", """{"sub":"user"}""")
        val jwt = JWT.parse(tokenString)

        assertEquals(tokenString, jwt.toString())
    }
}
