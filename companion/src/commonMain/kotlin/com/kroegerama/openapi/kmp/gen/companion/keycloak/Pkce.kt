package com.kroegerama.openapi.kmp.gen.companion.keycloak

import androidx.compose.runtime.Immutable
import io.ktor.util.generateNonceSuspend
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

/**
 * A PKCE (RFC 7636) verifier/challenge pair using the `S256` method.
 *
 * The public constructor allows restoring a previously persisted verifier - required on
 * Android, where the process may die between opening the browser and receiving the redirect.
 * Only [codeVerifier] is serialized; [codeChallenge] is recomputed.
 * [toString] redacts the verifier, so instances can be logged safely.
 *
 * @property codeVerifier 43–128 characters from the unreserved set `[A-Za-z0-9-._~]`.
 */
@Immutable
@Serializable
public data class Pkce(
    val codeVerifier: String
) {
    init {
        require(codeVerifier.matches(VERIFIER_REGEX)) {
            "The code verifier must be 43-128 characters from [A-Za-z0-9-._~]."
        }
    }

    /** base64url (no padding) of SHA-256 over the ASCII bytes of [codeVerifier]. */
    public val codeChallenge: String by lazy {
        Base64Pkce.encode(sha256(codeVerifier.encodeToByteArray()))
    }

    override fun toString(): String = "Pkce(codeVerifier=<redacted>)"

    public companion object {
        /** The only supported code challenge method. The `plain` method is deliberately not offered. */
        public const val CHALLENGE_METHOD_S256: String = "S256"

        private val VERIFIER_REGEX = """[A-Za-z0-9\-._~]{43,128}""".toRegex()
        private val Base64Pkce = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

        /**
         * Generates a fresh verifier: 64 hex characters carrying 256 bits of entropy.
         * Suspends while the entropy is gathered from the platform's secure random source.
         */
        public suspend fun generate(): Pkce = Pkce(generateNonceSuspend(64))
    }
}
