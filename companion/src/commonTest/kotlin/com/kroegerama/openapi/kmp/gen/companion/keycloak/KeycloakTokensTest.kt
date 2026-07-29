package com.kroegerama.openapi.kmp.gen.companion.keycloak

import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class KeycloakTokensTest {

    private val json = createDefaultJson()

    @Test
    fun decodeRealisticResponseDefaultsObtainedAt() {
        val accessToken = unsignedJwt(exp = nowEpochSeconds() + 300)
        val decoded = json.decodeFromString<KeycloakTokens>(
            """
            {
                "access_token": "$accessToken",
                "expires_in": 300,
                "refresh_expires_in": 1800,
                "refresh_token": "opaque-refresh",
                "token_type": "Bearer",
                "not-before-policy": 0,
                "session_state": "abc-123",
                "scope": "openid profile"
            }
            """.trimIndent()
        )

        assertEquals(accessToken, decoded.accessToken)
        assertEquals(300, decoded.expiresIn)
        assertEquals("opaque-refresh", decoded.refreshToken)
        assertEquals(1800, decoded.refreshExpiresIn)
        assertEquals("Bearer", decoded.tokenType)
        assertEquals("abc-123", decoded.sessionState)
        assertEquals("openid profile", decoded.scope)
        assertNotNull(decoded.accessJwt)
        assertNull(decoded.refreshJwt)

        val age = Clock.System.now() - decoded.obtainedAt
        assertTrue(age < 10.seconds, "obtainedAt should default to decoding time, but is $age old")
    }

    @Test
    fun roundTripPreservesObtainedAt() {
        val tokens = KeycloakTokens(
            accessToken = "opaque-access",
            expiresIn = 60,
            refreshToken = "opaque-refresh",
            refreshExpiresIn = 1800,
            obtainedAt = Instant.fromEpochSeconds(1_700_000_000)
        )

        val restored = json.decodeFromString<KeycloakTokens>(json.encodeToString(tokens))

        assertEquals(tokens, restored)
        assertEquals(Instant.fromEpochSeconds(1_700_000_000), restored.obtainedAt)
    }

    @Test
    fun accessTokenExpiryUsesJwtExp() {
        val expired = KeycloakTokens(accessToken = unsignedJwt(exp = nowEpochSeconds() - 100))
        val valid = KeycloakTokens(accessToken = unsignedJwt(exp = nowEpochSeconds() + 100))

        assertTrue(expired.isAccessTokenExpired())
        assertFalse(valid.isAccessTokenExpired())
    }

    @Test
    fun accessTokenExpiryRespectsLeeway() {
        val tokens = KeycloakTokens(accessToken = unsignedJwt(exp = nowEpochSeconds() + 5))

        assertFalse(tokens.isAccessTokenExpired())
        assertTrue(tokens.isAccessTokenExpired(leeway = 30.seconds))
    }

    @Test
    fun opaqueAccessTokenFallsBackToExpiresIn() {
        val expired = KeycloakTokens(
            accessToken = "opaque",
            expiresIn = 60,
            obtainedAt = Clock.System.now() - 120.seconds
        )
        val valid = KeycloakTokens(
            accessToken = "opaque",
            expiresIn = 60,
            obtainedAt = Clock.System.now()
        )

        assertTrue(expired.isAccessTokenExpired())
        assertFalse(valid.isAccessTokenExpired())
    }

    @Test
    fun jwtWithoutExpFallsBackToExpiresIn() {
        val tokens = KeycloakTokens(
            accessToken = unsignedJwt(iat = nowEpochSeconds() - 120),
            expiresIn = 60,
            obtainedAt = Clock.System.now() - 120.seconds
        )

        assertTrue(tokens.isAccessTokenExpired())
    }

    @Test
    fun missingExpiryInformationMeansNotExpired() {
        val tokens = KeycloakTokens(accessToken = "opaque")

        assertFalse(tokens.isAccessTokenExpired())
        assertFalse(tokens.isRefreshTokenExpired())
    }

    @Test
    fun refreshTokenExpiryUsesJwtExp() {
        val expired = KeycloakTokens(
            accessToken = "opaque",
            refreshToken = unsignedJwt(exp = nowEpochSeconds() - 100)
        )
        val valid = KeycloakTokens(
            accessToken = "opaque",
            refreshToken = unsignedJwt(exp = nowEpochSeconds() + 100)
        )

        assertTrue(expired.isRefreshTokenExpired())
        assertFalse(valid.isRefreshTokenExpired())
    }

    @Test
    fun accessTokenWithZeroLifespanCountsAsExpired() {
        val tokens = KeycloakTokens(
            accessToken = "opaque",
            expiresIn = 0,
            obtainedAt = Clock.System.now() - 1.seconds
        )

        assertTrue(tokens.isAccessTokenExpired())
    }

    @Test
    fun offlineTokenWithZeroLifespanNeverExpires() {
        val tokens = KeycloakTokens(
            accessToken = "opaque",
            refreshToken = "opaque-offline",
            refreshExpiresIn = 0,
            obtainedAt = Instant.fromEpochSeconds(0)
        )

        assertFalse(tokens.isRefreshTokenExpired())
    }

    @Test
    fun offlineTokenWithZeroExpClaimNeverExpires() {
        // Keycloak < 13 serializes offline refresh tokens with "exp": 0; that must not count
        // as an expiry at the epoch.
        val tokens = KeycloakTokens(
            accessToken = "opaque",
            refreshToken = unsignedJwt(exp = 0),
            refreshExpiresIn = 0,
            obtainedAt = Instant.fromEpochSeconds(0)
        )

        assertFalse(tokens.isRefreshTokenExpired())
    }

    @Test
    fun zeroExpClaimFallsBackToLifespan() {
        val expired = KeycloakTokens(
            accessToken = "opaque",
            refreshToken = unsignedJwt(exp = 0),
            refreshExpiresIn = 60,
            obtainedAt = Clock.System.now() - 120.seconds
        )
        val valid = KeycloakTokens(
            accessToken = "opaque",
            refreshToken = unsignedJwt(exp = 0),
            refreshExpiresIn = 1800,
            obtainedAt = Clock.System.now()
        )

        assertTrue(expired.isRefreshTokenExpired())
        assertFalse(valid.isRefreshTokenExpired())
    }

    @Test
    fun opaqueRefreshTokenFallsBackToRefreshExpiresIn() {
        val expired = KeycloakTokens(
            accessToken = "opaque",
            refreshToken = "opaque-refresh",
            refreshExpiresIn = 60,
            obtainedAt = Clock.System.now() - 120.seconds
        )

        assertTrue(expired.isRefreshTokenExpired())
    }

    @Test
    fun idTokenParsesAsJwt() {
        val subject = "user-123"
        val withIdToken = KeycloakTokens(
            accessToken = "opaque",
            idToken = unsignedJwt(sub = subject)
        )
        val withoutIdToken = KeycloakTokens(accessToken = "opaque")

        assertEquals(subject, assertNotNull(withIdToken.idJwt).subject)
        assertNull(withoutIdToken.idJwt)
    }

    @Test
    fun toStringRedactsTokens() {
        val tokens = KeycloakTokens(
            accessToken = "secret-access",
            refreshToken = "secret-refresh",
            idToken = "secret-id",
            scope = "openid",
            sessionState = "abc-123"
        )

        val string = tokens.toString()

        assertFalse("secret-access" in string, string)
        assertFalse("secret-refresh" in string, string)
        assertFalse("secret-id" in string, string)
        assertTrue("openid" in string, string)
        assertTrue("abc-123" in string, string)
    }

    @Test
    fun errorResponseDecodes() {
        val error = json.decodeFromString<KeycloakErrorResponse>(
            """{"error":"invalid_grant","error_description":"Invalid user credentials"}"""
        )

        assertEquals("invalid_grant", error.error)
        assertEquals("Invalid user credentials", error.errorDescription)
    }
}
