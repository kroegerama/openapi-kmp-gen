package com.kroegerama.openapi.kmp.gen.companion.keycloak

import androidx.compose.runtime.Immutable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * The claims returned by the OpenID Connect userinfo endpoint, see [Keycloak.userInfo].
 *
 * The standard profile claims are exposed as typed properties. Keycloak protocol mappers can
 * add arbitrary further claims, available via [claims] and [getClaim]. A typed property is
 * `null` when its claim is absent or not a primitive.
 *
 * @property claims the full, unmodified response document.
 */
@Immutable
public data class KeycloakUserInfo(
    val claims: JsonObject
) {
    /** The `sub` claim - the end-user's stable identifier. */
    public val subject: String? get() = stringClaim("sub")

    /** The `preferred_username` claim - the Keycloak username by default. */
    public val preferredUsername: String? get() = stringClaim("preferred_username")

    /** The `name` claim - the full display name. */
    public val name: String? get() = stringClaim("name")

    /** The `given_name` claim. */
    public val givenName: String? get() = stringClaim("given_name")

    /** The `family_name` claim. */
    public val familyName: String? get() = stringClaim("family_name")

    /** The `email` claim. Verified only when [emailVerified] is `true`. */
    public val email: String? get() = stringClaim("email")

    /** The `email_verified` claim. */
    public val emailVerified: Boolean? get() = (claims["email_verified"] as? JsonPrimitive)?.booleanOrNull

    /** The `picture` claim - a URL to the end-user's profile picture. */
    public val picture: String? get() = stringClaim("picture")

    /** The raw claim named [name], or `null` if absent. */
    public fun getClaim(name: String): JsonElement? = claims[name]

    private fun stringClaim(name: String): String? = (claims[name] as? JsonPrimitive)?.contentOrNull
}
