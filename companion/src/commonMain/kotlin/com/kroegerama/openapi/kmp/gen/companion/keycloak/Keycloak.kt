package com.kroegerama.openapi.kmp.gen.companion.keycloak

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.kroegerama.openapi.kmp.gen.companion.AuthItem
import com.kroegerama.openapi.kmp.gen.companion.CallException
import com.kroegerama.openapi.kmp.gen.companion.HttpCallException
import com.kroegerama.openapi.kmp.gen.companion.PlatformHttpClientEngineConfig
import com.kroegerama.openapi.kmp.gen.companion.UnauthorizedHandler
import com.kroegerama.openapi.kmp.gen.companion.UnexpectedCallException
import com.kroegerama.openapi.kmp.gen.companion.createDefaultJson
import com.kroegerama.openapi.kmp.gen.companion.createPlatformHttpClient
import com.kroegerama.openapi.kmp.gen.companion.defaultConfig
import com.kroegerama.openapi.kmp.gen.companion.eitherRequest
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.parameters
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.generateNonceSuspend
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Loads a previously persisted token set, e.g. from DataStore or the Keychain.
 * Invoked lazily on the first token access; once it completes, it is not invoked again. A thrown
 * exception is swallowed - the access proceeds as if no tokens were stored - and the load
 * is retried on the next token access. The retry is skipped once an explicit state change
 * (a login, [Keycloak.updateTokens], or [Keycloak.logout]) has made the in-memory state
 * authoritative.
 *
 * Runs while the [Keycloak] instance's internal (non-reentrant) lock is held: it must not
 * call back into the instance, or the call deadlocks.
 */
public typealias KeycloakTokenLoader = suspend () -> KeycloakTokens?

/**
 * Notified whenever the token set changes: after login, refresh, [Keycloak.updateTokens],
 * and with `null` after logout or when the refresh token was rejected. Use it to persist tokens.
 * Exceptions thrown by the listener are ignored - handle persistence failures inside.
 *
 * Runs while the [Keycloak] instance's internal (non-reentrant) lock is held: it must not
 * call back into the instance, or the call deadlocks.
 */
public typealias KeycloakTokenListener = suspend (KeycloakTokens?) -> Unit

/** Why a [Keycloak] session ended; emitted via [Keycloak.sessionEnded]. */
public enum class KeycloakSessionEndReason {
    /** [Keycloak.logout] was called, or [Keycloak.updateTokens] cleared the state with `null`. */
    Logout,

    /**
     * The refresh token was rejected by the server or expired locally, and the session could
     * not be renewed. A new login is required - use this to show a "you were logged out"
     * notification and return to the login screen.
     */
    SessionExpired
}

/**
 * Creates the lightweight [HttpClient] used by [Keycloak] by default: the library's
 * [defaultConfig] plus JSON content negotiation and conservative [HttpTimeout] limits.
 * The timeouts matter because token requests run while [Keycloak]'s internal lock is
 * held - a hung request would otherwise stall every call waiting for a bearer token.
 * Intentionally separate from any [com.kroegerama.openapi.kmp.gen.companion.ApiHolder]
 * client, so token requests never pass through the API's auth plugin or default request
 * configuration.
 *
 * @param decorator additional configuration, applied last - it may override the defaults,
 *   e.g. re-`install(HttpTimeout)` with different limits.
 */
public fun createKeycloakHttpClient(
    decorator: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit = {}
): HttpClient = createPlatformHttpClient {
    defaultConfig()
    install(ContentNegotiation) {
        json(createDefaultJson())
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
    decorator()
}

/**
 * A Keycloak client that obtains, stores, and refreshes OAuth 2.0 tokens.
 *
 * Supported grants: [login] (Direct Access Grants / password), [loginClientCredentials], and
 * the Authorization Code + PKCE flow via [createAuthorizationRequest] /
 * [handleAuthorizationRedirect] (browser presentation is the application's responsibility).
 * Obtained tokens are refreshed automatically by [bearerOrNull] when the access token is
 * expired, which makes [asBearerProvider] a drop-in provider for a generated bearer `Auth`
 * variant:
 *
 * ```kotlin
 * val keycloak = Keycloak(Url("https://auth.example.com"), realm = "my-realm", clientId = "my-app")
 * Api.setAuthProvider(Auth.OIDCAuth(keycloak.asBearerProvider()))
 * Api.setUnauthorizedHandler(keycloak.asUnauthorizedHandler())
 * ```
 *
 * [bearerOrNull] refreshes proactively, based on the token's own expiry information. A server
 * may reject an access token earlier - e.g. after an administrator terminated the session -
 * which surfaces as a 401 on an API request; [handleUnauthorized] / [asUnauthorizedHandler]
 * cover that reactive case.
 *
 * Token persistence is optional and fully delegated to the application via [KeycloakTokenLoader]
 * and [KeycloakTokenListener]; the serialized form is the [KeycloakTokens] class itself.
 * The current state is observable via [tokens]; the end of a session - an explicit [logout]
 * or an expired/rejected refresh token - is additionally signaled via [sessionEnded].
 *
 * All token state transitions are serialized through an internal [Mutex], so concurrent
 * [bearerOrNull] calls trigger at most one refresh request (single flight).
 *
 * An instance is intended to live for the application's lifetime: [httpClient] is never
 * closed, whether it was provided by the caller or created by default.
 *
 * @param clientId the Keycloak client id.
 * @param endpoints the endpoints to use; see [KeycloakEndpoints.fromRealm] and [discover].
 * @param clientSecret the client secret for confidential clients, sent in the request body.
 * @param accessTokenLeeway an access token expiring within this duration is
 *   treated as already expired, so a request never leaves with a token about to lapse.
 *   Defaults to [DEFAULT_ACCESS_TOKEN_LEEWAY].
 * @param tokenLoader see [KeycloakTokenLoader].
 * @param tokenListener see [KeycloakTokenListener].
 * @param httpClient the client used for token requests. Defaults to [createKeycloakHttpClient].
 */
public class Keycloak(
    public val clientId: String,
    public val endpoints: KeycloakEndpoints,
    private val clientSecret: String? = null,
    private val accessTokenLeeway: Duration = DEFAULT_ACCESS_TOKEN_LEEWAY,
    private val tokenLoader: KeycloakTokenLoader? = null,
    private val tokenListener: KeycloakTokenListener? = null,
    private val httpClient: HttpClient = createKeycloakHttpClient()
) {
    /**
     * Convenience constructor using Keycloak's standard realm URL layout,
     * see [KeycloakEndpoints.fromRealm].
     */
    public constructor(
        baseUrl: Url,
        realm: String,
        clientId: String,
        clientSecret: String? = null,
        accessTokenLeeway: Duration = DEFAULT_ACCESS_TOKEN_LEEWAY,
        tokenLoader: KeycloakTokenLoader? = null,
        tokenListener: KeycloakTokenListener? = null,
        httpClient: HttpClient = createKeycloakHttpClient()
    ) : this(
        clientId = clientId,
        endpoints = KeycloakEndpoints.fromRealm(baseUrl, realm),
        clientSecret = clientSecret,
        accessTokenLeeway = accessTokenLeeway,
        tokenLoader = tokenLoader,
        tokenListener = tokenListener,
        httpClient = httpClient
    )

    init {
        require(accessTokenLeeway >= Duration.ZERO) {
            "The access token leeway must not be negative. Got $accessTokenLeeway instead."
        }
    }

    private val mutex = Mutex()
    private var loaded = tokenLoader == null
    private val mutableTokens = MutableStateFlow<KeycloakTokens?>(null)

    /**
     * Set while the current token set came from [loginClientCredentials]; repeats that request.
     * A client-credentials session can always be renewed this way instead of being cleared:
     * [bearerOrNull] uses it directly when the grant issued no refresh token (the usual case),
     * and [refreshLocked] falls back to it when a refresh token was issued but is rejected.
     * Guarded by [mutex].
     */
    private var clientCredentialsRelogin: (suspend () -> Either<CallException, KeycloakTokens>)? = null

    /**
     * The current token set. `null` while logged out - but also before the [KeycloakTokenLoader]
     * has run, which happens lazily on the first suspending token access. Collect [isLoggedIn]
     * or call [currentTokens] first when that distinction matters, e.g. to decide the start
     * screen of an app.
     */
    public val tokens: StateFlow<KeycloakTokens?> = mutableTokens.asStateFlow()

    /**
     * Whether a session exists, i.e. whether [tokens] is non-`null`. Unlike [tokens], collecting
     * this flow first runs the [KeycloakTokenLoader] if it has not run yet, so the first emission
     * already reflects persisted tokens instead of reporting a logged-out state at startup.
     */
    public val isLoggedIn: Flow<Boolean> = flow {
        currentTokens()
        emitAll(mutableTokens.map { it != null }.distinctUntilChanged())
    }

    private val mutableSessionEnded = MutableSharedFlow<KeycloakSessionEndReason>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Emits after the token state transitioned from an existing session to logged-out, tagged
     * with the reason. [KeycloakSessionEndReason.SessionExpired] fires only for involuntary
     * session loss, so it can directly drive a "you were logged out" notification. When it
     * emits, [tokens] is already `null`. Events are not replayed: only collectors active at
     * emission time receive them.
     */
    public val sessionEnded: SharedFlow<KeycloakSessionEndReason> = mutableSessionEnded.asSharedFlow()

    /**
     * Direct Access Grants (`grant_type=password`) login. Stores the tokens on success.
     *
     * @param scopes optional scopes, joined with a space.
     * @param decorator appends additional form parameters, e.g. `append("totp", otp)`.
     */
    public suspend fun login(
        username: String,
        password: String,
        scopes: List<String> = emptyList(),
        decorator: ParametersBuilder.() -> Unit = {}
    ): Either<CallException, KeycloakTokens> = mutex.withLock {
        ensureLoaded()
        tokenRequest {
            append("grant_type", "password")
            append("username", username)
            append("password", password)
            appendScopes(scopes)
            decorator()
        }.onRight {
            clientCredentialsRelogin = null
            store(it)
        }
    }

    /**
     * Service-account (`grant_type=client_credentials`) login. Requires a confidential
     * client, i.e. a `clientSecret`. Stores the tokens on success.
     *
     * Client-credentials grants usually issue no refresh token; [bearerOrNull] therefore
     * renews the session by repeating this request (with the same [scopes] and [decorator])
     * once the access token expires.
     *
     * @param scopes optional scopes, joined with a space.
     * @param decorator appends additional form parameters.
     */
    public suspend fun loginClientCredentials(
        scopes: List<String> = emptyList(),
        decorator: ParametersBuilder.() -> Unit = {}
    ): Either<CallException, KeycloakTokens> = mutex.withLock {
        ensureLoaded()
        val request: suspend () -> Either<CallException, KeycloakTokens> = {
            tokenRequest {
                append("grant_type", "client_credentials")
                appendScopes(scopes)
                decorator()
            }
        }
        request().onRight {
            clientCredentialsRelogin = request
            store(it)
        }
    }

    /**
     * Forces a refresh using the current refresh token, regardless of the access token's expiry.
     *
     * When the server rejects the refresh token - an [HttpCallException] with status 400 or 401
     * whose Keycloak error body says `invalid_grant` - the token is expired, revoked, or the
     * session ended, and the local token state is cleared. A session obtained via
     * [loginClientCredentials] is renewed by repeating that request instead, also when the
     * grant issued no refresh token in the first place. Any other failure - transient errors,
     * a 400/401 without a readable Keycloak error body (e.g. from a reverse proxy, gateway, or
     * VPN portal), or e.g. `invalid_client` from a misconfigured client secret - keeps the
     * current tokens, since the refresh token was not consumed.
     */
    public suspend fun refresh(): Either<CallException, KeycloakTokens> = mutex.withLock {
        ensureLoaded()
        refreshLocked()
    }

    /**
     * Clears the local token state and notifies the Keycloak end-session endpoint (best effort).
     * The local state is cleared even when the server call fails. Without a refresh token or
     * a configured logout endpoint, no request is sent.
     */
    public suspend fun logout(): Either<CallException, Unit> = mutex.withLock {
        ensureLoaded()
        clientCredentialsRelogin = null
        val refreshToken = mutableTokens.value?.refreshToken
        clear(KeycloakSessionEndReason.Logout)
        val logoutEndpoint = endpoints.logoutEndpoint
        if (refreshToken == null || logoutEndpoint == null) return Unit.right()
        httpClient.eitherRequest<Unit> {
            method = HttpMethod.Post
            url.takeFrom(logoutEndpoint)
            setBody(FormDataContent(parameters {
                appendClient()
                append("refresh_token", refreshToken)
            }))
        }.map { }
    }

    /** The current token set, running the [KeycloakTokenLoader] first if it has not run yet. */
    public suspend fun currentTokens(): KeycloakTokens? = mutex.withLock {
        ensureLoaded()
        mutableTokens.value
    }

    /**
     * Replaces the current token set with externally obtained tokens (e.g. from a platform
     * OAuth library, or the built-in Authorization Code flow when the exchange is done
     * elsewhere - see [createAuthorizationRequest]) and notifies the [KeycloakTokenListener].
     * Refreshing is handled by this instance from then on. Pass `null` to clear the state locally.
     */
    public suspend fun updateTokens(tokens: KeycloakTokens?): Unit = mutex.withLock {
        loaded = true
        clientCredentialsRelogin = null
        if (tokens == null) {
            clear(KeycloakSessionEndReason.Logout)
        } else {
            store(tokens)
        }
    }

    /**
     * Builds an Authorization Code + PKCE (`S256`) request. Presenting
     * [AuthorizationRequest.url] in a browser and capturing the redirect is the application's
     * responsibility; pass the captured redirect to [handleAuthorizationRedirect] (or
     * [AuthorizationRequest.parseRedirect] + [exchangeAuthorizationCode]).
     *
     * An OIDC `nonce` is not added automatically, because this library does not validate ID
     * tokens. Applications that verify the `nonce` claim themselves can add one via
     * [decorator]: `append("nonce", myNonce)`.
     *
     * For [pkce] and [state], `null` (the default) generates fresh secure values; pass
     * persisted values to rebuild an identical request after process death.
     *
     * @param decorator appends additional query parameters. Use it for the optional OIDC and
     *   Keycloak parameters, e.g. `append("prompt", "login")` to force re-authentication or
     *   `append("login_hint", email)` to prefill the username.
     * @throws IllegalStateException when [endpoints] contains no
     *   [KeycloakEndpoints.authorizationEndpoint].
     * @throws IllegalArgumentException when [redirectUri] is not a valid URL.
     */
    public suspend fun createAuthorizationRequest(
        redirectUri: String,
        scopes: List<String> = listOf("openid"),
        pkce: Pkce? = null,
        state: String? = null,
        decorator: ParametersBuilder.() -> Unit = {}
    ): AuthorizationRequest {
        val authorizationEndpoint = checkNotNull(endpoints.authorizationEndpoint) {
            "endpoints.authorizationEndpoint is null - the discovery document did not contain " +
                "an authorization_endpoint, or the endpoints were constructed without one."
        }
        val actualPkce = pkce ?: Pkce.generate()
        val actualState = state ?: generateNonceSuspend(32)
        val url = URLBuilder(authorizationEndpoint).apply {
            with(parameters) {
                append("response_type", "code")
                append("client_id", clientId)
                append("redirect_uri", redirectUri)
                appendScopes(scopes)
                append("state", actualState)
                append("code_challenge", actualPkce.codeChallenge)
                append("code_challenge_method", Pkce.CHALLENGE_METHOD_S256)
                decorator()
            }
        }.build()
        return AuthorizationRequest(
            url = url,
            state = actualState,
            pkce = actualPkce,
            redirectUri = redirectUri
        )
    }

    /**
     * Redeems an authorization code (`grant_type=authorization_code`) obtained via the
     * Authorization Code + PKCE flow. Stores the tokens on success, like [login].
     *
     * @param decorator appends additional form parameters.
     */
    public suspend fun exchangeAuthorizationCode(
        code: String,
        codeVerifier: String,
        redirectUri: String,
        decorator: ParametersBuilder.() -> Unit = {}
    ): Either<CallException, KeycloakTokens> = mutex.withLock {
        ensureLoaded()
        tokenRequest {
            append("grant_type", "authorization_code")
            append("code", code)
            append("redirect_uri", redirectUri)
            append("code_verifier", codeVerifier)
            decorator()
        }.onRight {
            clientCredentialsRelogin = null
            store(it)
        }
    }

    /**
     * Convenience for the Authorization Code + PKCE flow: parses the captured redirect against
     * [request] and exchanges the code in one call. Parse failures surface as
     * [UnexpectedCallException] with a [KeycloakAuthorizationException] cause; use
     * [authorizationExceptionOrNull] to inspect them, e.g. to detect a cancelled login.
     */
    public suspend fun handleAuthorizationRedirect(
        request: AuthorizationRequest,
        redirectedUrl: Url
    ): Either<CallException, KeycloakTokens> =
        exchangeParsedRedirect(request, request.parseRedirect(redirectedUrl))

    /**
     * [handleAuthorizationRedirect] overload for platform APIs that surface the redirect as a
     * string. An unparseable [redirectedUrl] surfaces like the other parse failures, with a
     * [KeycloakAuthorizationException.InvalidRedirectUrl] cause.
     */
    public suspend fun handleAuthorizationRedirect(
        request: AuthorizationRequest,
        redirectedUrl: String
    ): Either<CallException, KeycloakTokens> =
        exchangeParsedRedirect(request, request.parseRedirect(redirectedUrl))

    private suspend fun exchangeParsedRedirect(
        request: AuthorizationRequest,
        parsedCode: Either<KeycloakAuthorizationException, String>
    ): Either<CallException, KeycloakTokens> = parsedCode
        .mapLeft<CallException> { UnexpectedCallException(it.message, it) }
        .flatMap { code ->
            exchangeAuthorizationCode(
                code = code,
                codeVerifier = request.pkce.codeVerifier,
                redirectUri = request.redirectUri
            )
        }

    /**
     * Returns a bearer item for the current access token, refreshing it first when it is
     * expired (with [accessTokenLeeway] leeway). A session obtained via
     * [loginClientCredentials] is renewed by repeating that request instead, since it
     * usually has no refresh token.
     *
     * Returns `null` when logged out, when the refresh token is expired or rejected (state
     * is cleared, a new [login] is required), or when the renewal fails otherwise (tokens
     * are kept; the API request then proceeds without authentication and surfaces its own
     * error).
     */
    public suspend fun bearerOrNull(): AuthItem.Bearer? = mutex.withLock {
        ensureLoaded()
        val current = mutableTokens.value ?: return null
        if (!current.isAccessTokenExpired(accessTokenLeeway)) {
            return AuthItem.Bearer(current.accessToken)
        }
        if (current.refreshToken == null || current.isRefreshTokenExpired()) {
            val relogin = clientCredentialsRelogin ?: run {
                clear(KeycloakSessionEndReason.SessionExpired)
                return null
            }
            return relogin()
                .onRight { store(it) }
                .getOrNull()?.let { AuthItem.Bearer(it.accessToken) }
        }
        refreshLocked().getOrNull()?.let { AuthItem.Bearer(it.accessToken) }
    }

    /**
     * This instance as an auto-refreshing token provider for a generated bearer `Auth` variant:
     * `Api.setAuthProvider(Auth.MyScheme(keycloak.asBearerProvider()))`.
     */
    public fun asBearerProvider(): suspend () -> AuthItem.Bearer? = ::bearerOrNull

    /**
     * Handles a 401 Unauthorized received by an API request that carried [rejectedAccessToken],
     * returning whether a retry is worthwhile. This complements [bearerOrNull], which refreshes
     * only tokens that are expired by their local expiry information: a server may reject an
     * access token earlier, e.g. after an administrator terminated the session or the realm
     * keys rotated.
     *
     * When the current access token already differs from [rejectedAccessToken], the session was
     * renewed in the meantime (e.g. by a concurrent request) and `true` is returned without a
     * server round trip. Otherwise a refresh is forced with the semantics of [refresh]: a
     * rejected refresh token ends the session - [sessionEnded] emits
     * [KeycloakSessionEndReason.SessionExpired] - and `false` is returned (a session obtained
     * via [loginClientCredentials] is renewed by repeating that request instead), while
     * transient failures keep the current tokens. Returns `false` while logged out.
     */
    public suspend fun handleUnauthorized(rejectedAccessToken: String): Boolean = mutex.withLock {
        ensureLoaded()
        val current = mutableTokens.value ?: return false
        if (current.accessToken != rejectedAccessToken) return true
        refreshLocked().isRight()
    }

    /**
     * This instance as an [UnauthorizedHandler] for
     * [com.kroegerama.openapi.kmp.gen.companion.ApiHolder.setUnauthorizedHandler]: a 401 on a
     * request that carried a bearer token triggers [handleUnauthorized], so the request is
     * retried once with a fresh token when the session could be renewed. Intended for APIs
     * whose bearer tokens all come from this instance; requests without a bearer item are
     * never retried:
     *
     * ```kotlin
     * Api.setAuthProvider(Auth.MyScheme(keycloak.asBearerProvider()))
     * Api.setUnauthorizedHandler(keycloak.asUnauthorizedHandler())
     * ```
     */
    public fun asUnauthorizedHandler(): UnauthorizedHandler = { appliedItems ->
        val bearer = appliedItems.values.filterIsInstance<AuthItem.Bearer>().firstOrNull()
        bearer != null && handleUnauthorized(bearer.token)
    }

    /**
     * Fetches the end-user's claims from the OpenID Connect userinfo endpoint, authenticated
     * with the current access token (refreshed first when expired, like [bearerOrNull]).
     * Unlike the locally parsed [KeycloakTokens.idJwt], the returned claims come from a direct
     * response of the server, so they are trustworthy without local signature validation.
     *
     * The endpoint requires a token issued with the `openid` scope - [createAuthorizationRequest]
     * requests it by default, [login] and [loginClientCredentials] do not. As required by the
     * OIDC specification, the response's `sub` claim is compared to the ID token's when one is
     * present; a mismatch fails with an [UnexpectedCallException]. It also fails while logged
     * out and when [endpoints] contains no [KeycloakEndpoints.userInfoEndpoint].
     *
     * A userinfo endpoint configured to respond with a signed or encrypted JWT instead of
     * plain JSON is not supported, in line with this library not validating token signatures.
     */
    public suspend fun userInfo(): Either<CallException, KeycloakUserInfo> {
        val userInfoEndpoint = endpoints.userInfoEndpoint
            ?: return UnexpectedCallException(
                "endpoints.userInfoEndpoint is null - the discovery document did not contain " +
                    "a userinfo_endpoint, or the endpoints were constructed without one.",
                null
            ).left()
        val bearer = bearerOrNull()
            ?: return UnexpectedCallException(
                "No access token available - the userinfo endpoint requires a session.",
                null
            ).left()
        // bearerOrNull just stored any refreshed token set, so this reads the ID token
        // matching the bearer without taking the mutex again.
        val idTokenSubject = mutableTokens.value?.idJwt?.subject
        return httpClient.eitherRequest<JsonObject> {
            method = HttpMethod.Get
            url.takeFrom(userInfoEndpoint)
            header(HttpHeaders.Authorization, "Bearer ${bearer.token}")
        }.map {
            KeycloakUserInfo(it.data)
        }.flatMap { userInfo ->
            if (idTokenSubject != null && userInfo.subject != idTokenSubject) {
                UnexpectedCallException(
                    "The userinfo sub claim '${userInfo.subject}' does not match " +
                        "the ID token sub claim '$idTokenSubject'.",
                    null
                ).left()
            } else {
                userInfo.right()
            }
        }
    }

    /** Must be called with [mutex] held. */
    private suspend fun ensureLoaded() {
        if (loaded) return
        // A throwing loader (e.g. a transient storage failure) must neither leak out of the
        // token accessors nor count as a completed load: [loaded] stays false, so the next
        // access retries. Either.catch rethrows fatal exceptions including CancellationException.
        val loadedTokens = Either.catch { tokenLoader?.invoke() }.getOrElse { return }
        loaded = true
        // Tokens came from storage - set them without notifying the listener,
        // which would only write the same value back.
        loadedTokens?.let { mutableTokens.value = it }
    }

    /**
     * Clears the token state and emits [sessionEnded] when there actually was a session to
     * end. Must be called with [mutex] held.
     */
    private suspend fun clear(reason: KeycloakSessionEndReason) {
        val hadSession = mutableTokens.value != null
        store(null)
        if (hadSession) {
            // tryEmit never suspends and cannot fail with DROP_OLDEST, so a slow collector
            // cannot block token operations while the mutex is held.
            mutableSessionEnded.tryEmit(reason)
        }
    }

    /** Must be called with [mutex] held. */
    private suspend fun store(tokens: KeycloakTokens?) {
        // An explicit state change makes the in-memory state authoritative: a pending
        // (previously failed) token load must not overwrite it on a later access.
        loaded = true
        mutableTokens.value = tokens
        val listener = tokenListener ?: return
        // The listener only persists an already-applied state change, so its failures must not
        // turn a successful token request into an error. Either.catch rethrows fatal exceptions
        // including CancellationException.
        Either.catch { listener(tokens) }
    }

    /** Must be called with [mutex] held. */
    private suspend fun refreshLocked(): Either<CallException, KeycloakTokens> {
        val refreshToken = mutableTokens.value?.refreshToken
        if (refreshToken == null) {
            // A client-credentials session usually has no refresh token; renew it by
            // repeating the original request, consistent with the rejected-token fallback below.
            val relogin = clientCredentialsRelogin
                ?: return UnexpectedCallException("No refresh token available", null).left()
            return relogin().onRight { store(it) }
        }
        val result = tokenRequest {
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
        }.onRight {
            store(it)
        }
        val exception = result.leftOrNull() ?: return result
        if (!exception.isRefreshTokenRejected()) return result
        // A rejected refresh token ends the session - except for a client-credentials session,
        // which is renewed by repeating the original request. Like in [bearerOrNull], a failed
        // renewal keeps the current tokens, so a later call can retry.
        val relogin = clientCredentialsRelogin ?: run {
            clear(KeycloakSessionEndReason.SessionExpired)
            return result
        }
        return relogin().onRight { store(it) }
    }

    /**
     * Whether this failure means the refresh token itself was rejected, as opposed to a
     * transient or client-configuration failure (e.g. `invalid_client`). Keycloak reports a
     * rejected refresh token as 400/401 with a JSON error body of `invalid_grant` (per
     * RFC 6749). A body on those statuses that cannot be read or has no `error` field does
     * not count as rejected: it did not come from the token endpoint (e.g. a reverse proxy,
     * gateway, or VPN portal answering with an HTML page) and must not end the session.
     */
    private suspend fun CallException.isRefreshTokenRejected(): Boolean {
        if (this !is HttpCallException || code !in REFRESH_REJECTED_CODES) return false
        return keycloakErrorOrNull()?.error == "invalid_grant"
    }

    private suspend fun tokenRequest(
        parameters: ParametersBuilder.() -> Unit
    ): Either<CallException, KeycloakTokens> = httpClient.eitherRequest<KeycloakTokens> {
        method = HttpMethod.Post
        url.takeFrom(endpoints.tokenEndpoint)
        setBody(FormDataContent(parameters {
            appendClient()
            parameters()
        }))
    }.map { it.data }

    private fun ParametersBuilder.appendClient() {
        append("client_id", clientId)
        clientSecret?.let { append("client_secret", it) }
    }

    private fun ParametersBuilder.appendScopes(scopes: List<String>) {
        if (scopes.isNotEmpty()) {
            append("scope", scopes.joinToString(" "))
        }
    }

    public companion object {
        /** Default [accessTokenLeeway]: an access token expiring within this duration is refreshed up front. */
        public val DEFAULT_ACCESS_TOKEN_LEEWAY: Duration = 10.seconds

        private val REFRESH_REJECTED_CODES = setOf(
            HttpStatusCode.BadRequest.value,
            HttpStatusCode.Unauthorized.value
        )
    }
}

/**
 * Creates a [Keycloak] instance by resolving the endpoints from the realm's OpenID Connect
 * discovery document (`.well-known/openid-configuration`).
 *
 * The client used for discovery is reused by the resulting instance:
 * - A caller-provided [httpClient] is never closed, even on failure - its lifecycle stays
 *   with the caller, so it can be reused across discovery retries. Its `Json` configuration
 *   must ignore unknown keys (like [createDefaultJson]), because discovery documents contain
 *   many more fields than the decoded [OpenIdConfiguration] subset.
 * - When [httpClient] is `null`, a [createKeycloakHttpClient] is created for this call: on
 *   success the resulting instance takes it over; on failure - including cancellation - it
 *   is closed, so retried discoveries do not accumulate abandoned clients. A Keycloak error
 *   body carried by the returned [CallException] stays readable via [keycloakErrorOrNull]
 *   after that close, because it is buffered in memory.
 *
 * Fails with an [UnexpectedCallException] when the document's `issuer` is not
 * `{baseUrl}/realms/{realm}`, as required by the OIDC discovery specification.
 */
public suspend fun Keycloak.Companion.discover(
    baseUrl: Url,
    realm: String,
    clientId: String,
    clientSecret: String? = null,
    accessTokenLeeway: Duration = Keycloak.DEFAULT_ACCESS_TOKEN_LEEWAY,
    tokenLoader: KeycloakTokenLoader? = null,
    tokenListener: KeycloakTokenListener? = null,
    httpClient: HttpClient? = null
): Either<CallException, Keycloak> {
    val client = httpClient ?: createKeycloakHttpClient()
    val ownsClient = httpClient == null
    try {
        return client.eitherRequest<OpenIdConfiguration> {
            method = HttpMethod.Get
            url.takeFrom(KeycloakEndpoints.wellKnownUrl(baseUrl, realm))
        }.flatMap { response ->
            val configuration = response.data
            val expectedIssuer = KeycloakEndpoints.issuerUrl(baseUrl, realm)
            val actualIssuer = Either.catch { Url(configuration.issuer) }.getOrElse { throwable ->
                return@flatMap UnexpectedCallException(
                    "Discovery document issuer '${configuration.issuer}' is not a valid URL",
                    throwable
                ).left()
            }
            if (actualIssuer != expectedIssuer) {
                return@flatMap UnexpectedCallException(
                    "Discovery document issuer '${configuration.issuer}' does not match the expected issuer '$expectedIssuer'",
                    null
                ).left()
            }
            val endpoints = Either.catch { configuration.toEndpoints() }.getOrElse { throwable ->
                return@flatMap UnexpectedCallException(
                    "The discovery document contains a malformed endpoint URL",
                    throwable
                ).left()
            }
            Keycloak(
                clientId = clientId,
                endpoints = endpoints,
                clientSecret = clientSecret,
                accessTokenLeeway = accessTokenLeeway,
                tokenLoader = tokenLoader,
                tokenListener = tokenListener,
                httpClient = client
            ).right()
        }.onLeft {
            if (ownsClient) client.close()
        }
    } catch (throwable: Throwable) {
        // Fatal exceptions (including CancellationException) are rethrown by the request
        // instead of surfacing as a Left, so the self-created client must be closed here too.
        if (ownsClient) client.close()
        throw throwable
    }
}
