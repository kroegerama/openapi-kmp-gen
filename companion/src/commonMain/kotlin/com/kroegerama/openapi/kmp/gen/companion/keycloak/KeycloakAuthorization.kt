package com.kroegerama.openapi.kmp.gen.companion.keycloak

import androidx.compose.runtime.Immutable
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.kroegerama.openapi.kmp.gen.companion.CallException
import com.kroegerama.openapi.kmp.gen.companion.UnexpectedCallException
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Failure while parsing an authorization redirect. Deliberately not a
 * [com.kroegerama.openapi.kmp.gen.companion.CallException]: no HTTP call is involved.
 */
public sealed class KeycloakAuthorizationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    /**
     * The authorization server returned an error, e.g. `access_denied` when the user
     * cancelled the login.
     */
    public class AuthorizationError(
        public val error: String,
        public val errorDescription: String?
    ) : KeycloakAuthorizationException(
        "Authorization failed: $error" + if (errorDescription != null) " ($errorDescription)" else ""
    )

    /**
     * The `state` parameter is missing or does not match the pending request - a possible
     * CSRF attempt or a stale redirect. The result must be discarded.
     */
    public class StateMismatch(
        public val expectedState: String,
        public val actualState: String?
    ) : KeycloakAuthorizationException("State mismatch: expected '$expectedState', got '$actualState'")

    /** The redirect contains neither a `code` nor an `error` parameter. */
    public class MissingCode : KeycloakAuthorizationException(
        "The redirect contains neither a 'code' nor an 'error' parameter"
    )

    /**
     * The captured redirect is not a parseable URL. The offending string is not repeated in
     * the message, because a redirect may carry sensitive parameters; see [cause] for the
     * parser failure.
     */
    public class InvalidRedirectUrl(
        cause: Throwable
    ) : KeycloakAuthorizationException("The redirect is not a valid URL", cause)
}

/**
 * A pending Authorization Code + PKCE request created by [Keycloak.createAuthorizationRequest].
 *
 * The application presents [url] in a browser (Custom Tabs, `ASWebAuthenticationSession`,
 * system browser + loopback listener, ...) and passes the captured redirect to [parseRedirect]
 * or [Keycloak.handleAuthorizationRedirect].
 *
 * The class is [Serializable] so a pending request can survive process death (e.g. on Android,
 * store `Json.encodeToString(request)` in the `SavedStateHandle` before launching the browser
 * and restore it before parsing the redirect).
 *
 * @property url the complete authorization URL to open in a browser.
 * @property state the CSRF token bound to this request; validated by [parseRedirect].
 * @property pkce the PKCE pair whose verifier is needed for the token exchange.
 * @property redirectUri the redirect URI, needed again for the token exchange. Must be a
 *   valid URL - construction (including deserialization) rejects it otherwise.
 */
@Immutable
@Serializable
public data class AuthorizationRequest(
    val url: Url,
    val state: String,
    val pkce: Pkce,
    val redirectUri: String
) {
    /**
     * [redirectUri] parsed once for [matchesRedirect]. The eager initialization rejects an
     * invalid redirect URI at construction time instead of on the first match attempt.
     */
    @Transient
    private val parsedRedirectUri: Url = try {
        Url(redirectUri)
    } catch (exception: Exception) {
        throw IllegalArgumentException("The redirectUri is not a valid URL: '$redirectUri'", exception)
    }

    /**
     * Extracts the authorization code from a captured redirect.
     *
     * The `state` parameter is validated first, before `error` or `code` are considered, so an
     * injected response cannot bypass the CSRF check. See [KeycloakAuthorizationException] for
     * the possible failures.
     */
    public fun parseRedirect(redirectedUrl: Url): Either<KeycloakAuthorizationException, String> {
        val parameters = redirectedUrl.parameters
        val actualState = parameters["state"]
        if (actualState != state) {
            return KeycloakAuthorizationException.StateMismatch(state, actualState).left()
        }
        val error = parameters["error"]
        if (error != null) {
            return KeycloakAuthorizationException.AuthorizationError(error, parameters["error_description"]).left()
        }
        val code = parameters["code"]
            ?: return KeycloakAuthorizationException.MissingCode().left()
        return code.right()
    }

    /**
     * [parseRedirect] overload for platform APIs that surface the redirect as a string.
     * An unparseable [redirectedUrl] fails with [KeycloakAuthorizationException.InvalidRedirectUrl].
     */
    public fun parseRedirect(redirectedUrl: String): Either<KeycloakAuthorizationException, String> = Either.catch {
        Url(redirectedUrl)
    }.mapLeft<KeycloakAuthorizationException> {
        KeycloakAuthorizationException.InvalidRedirectUrl(it)
    }.flatMap {
        parseRedirect(it)
    }

    /**
     * Whether [url] is a redirect back to [redirectUri]: scheme, host, port, and path must
     * match (query parameters and the fragment are ignored). Use it to decide when a
     * navigation should be intercepted, e.g. in a WebView or a loopback listener.
     *
     * @param normalizePath when `true` (the default), paths are compared with trailing
     *   slashes trimmed, so `/callback` and `/callback/` match - reverse proxies and some
     *   identity providers normalize the path this way. Pass `false` for exact matching.
     */
    public fun matchesRedirect(url: Url, normalizePath: Boolean = true): Boolean {
        fun path(url: Url): String = if (normalizePath) {
            url.encodedPath.trimEnd('/')
        } else {
            url.encodedPath
        }
        return url.protocol.name.equals(parsedRedirectUri.protocol.name, ignoreCase = true) &&
                url.host.equals(parsedRedirectUri.host, ignoreCase = true) &&
                url.port == parsedRedirectUri.port &&
                path(url) == path(parsedRedirectUri)
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

/**
 * The [KeycloakAuthorizationException] carried by this failure, or `null` when it did not
 * originate from parsing an authorization redirect ([Keycloak.handleAuthorizationRedirect]).
 *
 * The most common case worth special-casing is the user cancelling the login:
 *
 * ```kotlin
 * val cancelled = (exception.authorizationExceptionOrNull()
 *     as? KeycloakAuthorizationException.AuthorizationError)?.error == "access_denied"
 * ```
 */
public fun CallException.authorizationExceptionOrNull(): KeycloakAuthorizationException? =
    (this as? UnexpectedCallException)?.cause as? KeycloakAuthorizationException
