package com.kroegerama.openapi.kmp.gen.companion.keycloak

import androidx.compose.runtime.Immutable
import arrow.core.Either
import com.kroegerama.openapi.kmp.gen.companion.CallException
import com.kroegerama.openapi.kmp.gen.companion.HttpCallException
import com.kroegerama.openapi.kmp.gen.companion.JWT
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * A token set as returned by the Keycloak token endpoint.
 *
 * The class is [Serializable], so the whole token set can be persisted and restored by the
 * application (e.g. via a [KeycloakTokenListener] and [KeycloakTokenLoader] pair).
 * [toString] redacts the token values, so instances can be logged safely.
 *
 * @property accessToken the access token, usually a JWT.
 * @property expiresIn lifespan of the access token in seconds, relative to [obtainedAt].
 * @property refreshToken the refresh token, if the grant issued one.
 * @property refreshExpiresIn lifespan of the refresh token in seconds, relative to [obtainedAt].
 *   Keycloak sends `0` for offline tokens, which is treated as "never expires".
 * @property tokenType the token type, usually `Bearer`.
 * @property idToken the OpenID Connect ID token, if the `openid` scope was requested.
 * @property scope the effective scopes granted by the server.
 * @property sessionState the Keycloak session identifier.
 * @property obtainedAt when this token set was received; the reference point for the
 *   [expiresIn]/[refreshExpiresIn] fallback used when a token is not a parseable JWT.
 *   Not part of the Keycloak response - it defaults to the decoding time and is included
 *   in the serialized form so persisted token sets keep their original reference point.
 */
@Immutable
@Serializable
public data class KeycloakTokens(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("refresh_expires_in")
    val refreshExpiresIn: Long? = null,
    @SerialName("token_type")
    val tokenType: String? = null,
    @SerialName("id_token")
    val idToken: String? = null,
    @SerialName("scope")
    val scope: String? = null,
    @SerialName("session_state")
    val sessionState: String? = null,
    @SerialName("obtained_at")
    val obtainedAt: Instant = Clock.System.now()
) {
    /** [accessToken] parsed as JWT, or `null` if it is opaque. The signature is not verified. */
    public val accessJwt: JWT? by lazy { JWT.parseOrNull(accessToken) }

    /** [refreshToken] parsed as JWT, or `null` if it is absent or opaque. The signature is not verified. */
    public val refreshJwt: JWT? by lazy { refreshToken?.let(JWT::parseOrNull) }

    /**
     * Whether the access token is expired. Prefers the JWT `exp` claim; falls back to
     * [obtainedAt] + [expiresIn] when the token is opaque or has no `exp` claim. A lifespan
     * of `0` counts as immediately expired; without any expiry information the token is
     * considered not expired.
     *
     * A token counts as expired [leeway] *before* its actual expiry, so it can be
     * refreshed before it lapses mid-request. Note that this is the opposite direction of
     * the clock-skew grace period in [JWT.isExpired].
     *
     * @throws IllegalArgumentException when [leeway] is negative.
     */
    public fun isAccessTokenExpired(leeway: Duration = Duration.ZERO): Boolean =
        isExpired(accessJwt, expiresIn, leeway)

    /**
     * Whether the refresh token is expired. Prefers the JWT `exp` claim; falls back to
     * [obtainedAt] + [refreshExpiresIn] when the token is opaque or has no `exp` claim.
     * Unlike the access token, a lifespan of `0` (Keycloak offline tokens) is treated as
     * "never expires", as is absent expiry information and a JWT `exp` claim of `0`
     * (emitted for offline tokens by Keycloak versions before 13).
     *
     * A token counts as expired [leeway] *before* its actual expiry.
     *
     * @throws IllegalArgumentException when [leeway] is negative.
     */
    public fun isRefreshTokenExpired(leeway: Duration = Duration.ZERO): Boolean =
        isExpired(refreshJwt, refreshExpiresIn?.takeIf { it > 0L }, leeway)

    private fun isExpired(jwt: JWT?, lifespanSeconds: Long?, leeway: Duration): Boolean {
        require(leeway >= Duration.ZERO) { "The leeway must not be negative. Got $leeway instead." }
        // Keycloak versions before 13 serialize offline refresh tokens with an `exp` claim of 0,
        // meaning "never expires" - not an expiry at the epoch. Treat such a claim as absent,
        // like a `refresh_expires_in` of 0.
        val expiresAt = jwt?.expiresAt?.takeIf {
            it > Instant.fromEpochSeconds(0)
        } ?: lifespanSeconds?.let {
            obtainedAt + it.seconds
        } ?: return false
        return Clock.System.now() > expiresAt - leeway
    }

    override fun toString(): String = "KeycloakTokens(" +
            "accessToken=<redacted>, " +
            "expiresIn=$expiresIn, " +
            "refreshToken=${redact(refreshToken)}, " +
            "refreshExpiresIn=$refreshExpiresIn, " +
            "tokenType=$tokenType, " +
            "idToken=${redact(idToken)}, " +
            "scope=$scope, " +
            "sessionState=$sessionState, " +
            "obtainedAt=$obtainedAt" +
            ")"

    private fun redact(value: String?): String = if (value == null) "null" else "<redacted>"
}

/**
 * Error body returned by Keycloak endpoints, e.g. `{"error":"invalid_grant","error_description":"..."}`.
 * Extract it from a failed [Keycloak] call via [keycloakErrorOrNull].
 */
@Immutable
@Serializable
public data class KeycloakErrorResponse(
    @SerialName("error")
    val error: String? = null,
    @SerialName("error_description")
    val errorDescription: String? = null
)

/**
 * The [KeycloakErrorResponse] carried by this exception, or `null` if this is not an
 * [HttpCallException] or its body is not a Keycloak error response.
 */
public suspend fun CallException.keycloakErrorOrNull(): KeycloakErrorResponse? {
    val httpException = this as? HttpCallException ?: return null
    return Either.catch {
        httpException.raw.body<KeycloakErrorResponse>()
    }.getOrNull()
}
