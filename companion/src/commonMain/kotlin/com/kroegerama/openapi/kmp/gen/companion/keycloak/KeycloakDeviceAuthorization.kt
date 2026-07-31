package com.kroegerama.openapi.kmp.gen.companion.keycloak

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * A pending Device Authorization Grant (RFC 8628) as returned by the device authorization
 * endpoint, created via [Keycloak.startDeviceAuthorization].
 *
 * Display [userCode] and [verificationUri] to the user (or encode [verificationUriComplete]
 * in a QR code), then pass this object to [Keycloak.awaitDeviceAuthorization] to poll for
 * the tokens.
 *
 * The class is [Serializable], so a pending authorization can be persisted and resumed after
 * process death. [toString] redacts the credential values, so instances can be logged safely.
 *
 * @property deviceCode the device verification code, sent with each poll by
 *   [Keycloak.awaitDeviceAuthorization]. A credential - never shown to the user.
 * @property userCode the short code the user enters at [verificationUri].
 * @property verificationUri the URL the user opens on a second device.
 * @property verificationUriComplete [verificationUri] with [userCode] already embedded,
 *   if the server provides it - suited for a QR code.
 * @property expiresIn lifespan of the codes in seconds, relative to [obtainedAt].
 * @property interval minimum number of seconds between token endpoint polls. RFC 8628
 *   defines a default of `5` when the server omits it.
 * @property obtainedAt when this authorization was received; the reference point for
 *   [expiresIn]. Not part of the server response - it defaults to the decoding time and is
 *   included in the serialized form so persisted authorizations keep their original
 *   reference point.
 * @property pkce the PKCE pair passed to [Keycloak.startDeviceAuthorization], if any. Not
 *   part of the server response; [Keycloak.awaitDeviceAuthorization] sends its verifier
 *   with every poll.
 */
@Immutable
@Serializable
public data class DeviceAuthorization(
    @SerialName("device_code")
    val deviceCode: String,
    @SerialName("user_code")
    val userCode: String,
    @SerialName("verification_uri")
    val verificationUri: String,
    @SerialName("verification_uri_complete")
    val verificationUriComplete: String? = null,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("interval")
    val interval: Long = 5,
    @SerialName("obtained_at")
    val obtainedAt: Instant = Clock.System.now(),
    @SerialName("pkce")
    val pkce: Pkce? = null
) {
    /** When the codes expire: [obtainedAt] + [expiresIn]. */
    public val expiresAt: Instant
        get() = obtainedAt + expiresIn.seconds

    /**
     * Whether the codes are expired by the local clock. The server's `expired_token` answer
     * stays authoritative - [Keycloak.awaitDeviceAuthorization] keeps polling until the
     * server reports the expiry; use this e.g. to decide whether a persisted authorization
     * is still worth resuming.
     */
    public fun isExpired(): Boolean = Clock.System.now() > expiresAt

    override fun toString(): String = "DeviceAuthorization(" +
            "deviceCode=<redacted>, " +
            "userCode=$userCode, " +
            "verificationUri=$verificationUri, " +
            "verificationUriComplete=${if (verificationUriComplete == null) "null" else "<redacted>"}, " +
            "expiresIn=$expiresIn, " +
            "interval=$interval, " +
            "obtainedAt=$obtainedAt, " +
            // Pkce redacts its own verifier.
            "pkce=$pkce" +
            ")"
}
