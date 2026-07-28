package com.kroegerama.openapi.kmp.gen.companion.keycloak

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * A [WebViewClient] that intercepts the redirect of an Authorization Code + PKCE [request]
 * (matched via [AuthorizationRequest.matchesRedirect]) and passes the captured redirect URL
 * to [onRedirect]; every other navigation loads normally. The class is `open`, so additional
 * [WebViewClient] behavior can be layered on top.
 *
 * Prefer the system browser (Custom Tabs), as recommended by RFC 8252: embedded WebViews are
 * blocked by some brokered identity providers (e.g. Google), do not support passkeys, and do
 * not share SSO cookies. Use this client only for first-party logins into your own realm.
 *
 * ```kotlin
 * val request = keycloak.createAuthorizationRequest(redirectUri = "myapp://callback")
 * webView.settings.javaScriptEnabled = true // the Keycloak login page requires JavaScript
 * webView.settings.domStorageEnabled = true
 * webView.webViewClient = KeycloakWebViewClient(request) { redirectedUrl ->
 *     scope.launch { keycloak.handleAuthorizationRedirect(request, redirectedUrl) }
 * }
 * webView.loadUrl(request.url.toString())
 * ```
 *
 * @param request the pending authorization request; only its redirect URI is used for matching.
 * @param onRedirect called on the main thread with the full redirect URL.
 */
public open class KeycloakWebViewClient(
    private val request: AuthorizationRequest,
    private val onRedirect: (redirectedUrl: String) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        webRequest: WebResourceRequest?
    ): Boolean = webRequest != null && webRequest.isForMainFrame && interceptRedirect(webRequest.url.toString())

    @Deprecated("Called only on API < 24; newer API levels use the WebResourceRequest overload.")
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        url: String?
    ): Boolean = url != null && interceptRedirect(url)

    private fun interceptRedirect(url: String): Boolean {
        if (!request.matchesRedirect(url)) return false
        onRedirect(url)
        return true
    }
}
