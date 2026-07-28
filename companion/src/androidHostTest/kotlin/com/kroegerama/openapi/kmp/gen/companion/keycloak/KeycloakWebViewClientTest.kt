package com.kroegerama.openapi.kmp.gen.companion.keycloak

import android.net.Uri
import android.webkit.WebResourceRequest
import io.ktor.http.Url
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KeycloakWebViewClientTest {

    private val request = AuthorizationRequest(
        url = Url(
            "https://keycloak.example.com/realms/test/protocol/openid-connect/auth" +
                    "?client_id=test-public&redirect_uri=myapp%3A%2F%2Fcallback&response_type=code"
        ),
        state = "test-state",
        pkce = Pkce("a".repeat(43)),
        redirectUri = "myapp://callback"
    )

    private fun webRequest(url: String, mainFrame: Boolean = true): WebResourceRequest = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame(): Boolean = mainFrame
        override fun isRedirect(): Boolean = false
        override fun hasGesture(): Boolean = true
        override fun getMethod(): String = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

    @Test
    fun aMainFrameRedirectIsInterceptedAndDelivered() {
        val redirects = mutableListOf<String>()
        val client = KeycloakWebViewClient(request) { redirects += it }
        val redirectUrl = "myapp://callback?state=test-state&code=the-code"

        assertTrue(client.shouldOverrideUrlLoading(null, webRequest(redirectUrl)))
        assertEquals(listOf(redirectUrl), redirects)
    }

    @Test
    fun otherNavigationsLoadNormally() {
        val redirects = mutableListOf<String>()
        val client = KeycloakWebViewClient(request) { redirects += it }

        assertFalse(client.shouldOverrideUrlLoading(null, webRequest(request.url.toString())))
        assertFalse(client.shouldOverrideUrlLoading(null, webRequest("https://keycloak.example.com/other")))
        assertTrue(redirects.isEmpty())
    }

    @Test
    fun aSubFrameRedirectIsNotIntercepted() {
        val redirects = mutableListOf<String>()
        val client = KeycloakWebViewClient(request) { redirects += it }
        val redirectUrl = "myapp://callback?state=test-state&code=the-code"

        assertFalse(client.shouldOverrideUrlLoading(null, webRequest(redirectUrl, mainFrame = false)))
        assertTrue(redirects.isEmpty())
    }

    @Test
    fun aNullRequestLoadsNormally() {
        val client = KeycloakWebViewClient(request) { }
        assertFalse(client.shouldOverrideUrlLoading(null, null as WebResourceRequest?))
    }

    @Suppress("DEPRECATION")
    @Test
    fun theLegacyStringOverloadBehavesTheSame() {
        val redirects = mutableListOf<String>()
        val client = KeycloakWebViewClient(request) { redirects += it }
        val redirectUrl = "myapp://callback?state=test-state&code=the-code"

        assertTrue(client.shouldOverrideUrlLoading(null, redirectUrl))
        assertFalse(client.shouldOverrideUrlLoading(null, "https://keycloak.example.com/other"))
        assertFalse(client.shouldOverrideUrlLoading(null, null as String?))
        assertEquals(listOf(redirectUrl), redirects)
    }

    @Test
    fun anUnparseableUrlIsNotIntercepted() {
        val redirects = mutableListOf<String>()
        val client = KeycloakWebViewClient(request) { redirects += it }

        assertFalse(client.shouldOverrideUrlLoading(null, webRequest("not a url")))
        assertTrue(redirects.isEmpty())
    }
}
