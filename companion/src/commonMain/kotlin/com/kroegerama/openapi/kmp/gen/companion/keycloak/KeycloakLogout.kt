package com.kroegerama.openapi.kmp.gen.companion.keycloak

import androidx.compose.runtime.Immutable
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A pending RP-initiated logout request created by [Keycloak.createLogoutRequest].
 *
 * The application presents [url] in a browser - the same presentation used for the
 * [AuthorizationRequest] - which ends the Keycloak SSO session held in browser cookies,
 * something the backchannel [Keycloak.logout] cannot do. With a [postLogoutRedirectUri],
 * the server redirects back after the logout; pass the captured redirect to
 * [Keycloak.handleLogoutRedirect] (or [parseRedirect]). Without one, the browser stays on
 * a Keycloak page - clear the local state via `updateTokens(null)` once the application
 * considers the logout done.
 *
 * The class is [Serializable] so a pending request can survive process death,
 * like [AuthorizationRequest].
 *
 * @property url the complete end-session URL to open in a browser.
 * @property state the CSRF token bound to this request; validated by [parseRedirect].
 *   Sent - and echoed back by the server - only when [postLogoutRedirectUri] is set.
 * @property postLogoutRedirectUri the redirect URI the browser returns to after the logout,
 *   or `null` when none was requested. Must be a valid URL - construction (including
 *   deserialization) rejects it otherwise.
 */
@Immutable
@Serializable
public data class LogoutRequest(
    val url: Url,
    val state: String,
    val postLogoutRedirectUri: String?
) {
    /**
     * [postLogoutRedirectUri] parsed once for [matchesRedirect]. The eager initialization
     * rejects an invalid redirect URI at construction time instead of on the first match attempt.
     */
    @Transient
    private val parsedRedirectUri: Url? = postLogoutRedirectUri?.let { uri ->
        try {
            Url(uri)
        } catch (exception: Exception) {
            throw IllegalArgumentException("The postLogoutRedirectUri is not a valid URL: '$uri'", exception)
        }
    }

    /**
     * Validates a captured post-logout redirect: its `state` parameter must match this
     * request's, otherwise the redirect is stale or injected and fails with
     * [KeycloakAuthorizationException.StateMismatch].
     */
    public fun parseRedirect(redirectedUrl: Url): Either<KeycloakAuthorizationException, Unit> {
        val actualState = redirectedUrl.parameters["state"]
        if (actualState != state) {
            return KeycloakAuthorizationException.StateMismatch(state, actualState).left()
        }
        return Unit.right()
    }

    /**
     * [parseRedirect] overload for platform APIs that surface the redirect as a string.
     * An unparseable [redirectedUrl] fails with [KeycloakAuthorizationException.InvalidRedirectUrl].
     */
    public fun parseRedirect(redirectedUrl: String): Either<KeycloakAuthorizationException, Unit> = Either.catch {
        Url(redirectedUrl)
    }.mapLeft<KeycloakAuthorizationException> {
        KeycloakAuthorizationException.InvalidRedirectUrl(it)
    }.flatMap {
        parseRedirect(it)
    }

    /**
     * Whether [url] is a redirect back to [postLogoutRedirectUri]: scheme, host, port, and
     * path must match (query parameters and the fragment are ignored). Use it to decide when
     * a navigation should be intercepted, e.g. in a WebView or a loopback listener. Always
     * `false` when no redirect URI was requested.
     *
     * @param normalizePath when `true` (the default), paths are compared with trailing
     *   slashes trimmed, so `/callback` and `/callback/` match. Pass `false` for exact matching.
     */
    public fun matchesRedirect(url: Url, normalizePath: Boolean = true): Boolean {
        val redirectUri = parsedRedirectUri ?: return false
        return redirectTargetMatches(url, redirectUri, normalizePath)
    }

    /**
     * [matchesRedirect] overload for platform APIs that surface the navigation target as a
     * string. An unparseable [url] does not match.
     */
    public fun matchesRedirect(url: String, normalizePath: Boolean = true): Boolean {
        val parsed = Either.catch { Url(url) } getOrElse { return false }
        return matchesRedirect(parsed, normalizePath)
    }
}
