# Keycloak login with Custom Tabs (Android)

A complete Authorization Code + PKCE integration using
[Chrome Custom Tabs](https://developer.chrome.com/docs/android/custom-tabs), the browser presentation recommended by
[RFC 8252](https://datatracker.ietf.org/doc/html/rfc8252): SSO cookies are shared with the browser, passkeys/WebAuthn work, and brokered identity
providers (e.g. Google) that block embedded WebViews are supported.

The flow:

1. `Keycloak.createAuthorizationRequest` builds the authorization URL; the pending `AuthorizationRequest` is persisted in the `SavedStateHandle` so
   the flow survives process death while the browser is in the foreground
2. the URL is opened in a Custom Tab
3. Keycloak redirects to the app's redirect URI; a small receiver activity catches it and re-delivers it to the main activity, which forwards it to
   the view model
4. `Keycloak.handleAuthorizationRedirect` validates `state`, exchanges the code, and stores the tokens - from then on
   `keycloak.asBearerProvider()` serves and refreshes them as usual

## Prerequisites

- In the Keycloak admin console, add the redirect URI (here `myapp://callback`) to the client's **Valid redirect URIs**; the client is public (no
  client secret ships in the app), so PKCE is the code-interception protection
- Dependencies (beyond the usual Compose/lifecycle setup):

```kotlin
implementation("androidx.browser:browser:1.10.0")
// SavedStateHandle.saved delegate (kotlinx-serialization based), 2.9.0 or newer:
implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.11.0")
```

## Manifest

The main activity keeps its usual `singleTop` launch mode. The redirect is caught by a dedicated receiver activity instead: the Custom Tab sits on top
of the task, so an intent-filter on a `singleTop` main activity would create a second instance (with an empty `SavedStateHandle`) instead of reaching
the existing one. `singleTask` on the main activity would avoid that, but changes how every deep link and notification tap behaves - the receiver
keeps the navigation semantics untouched.

```xml
<activity android:name=".MainActivity" android:launchMode="singleTop" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<activity android:name=".AuthRedirectActivity" android:exported="true" android:theme="@android:style/Theme.Translucent.NoTitleBar">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="myapp" android:host="callback" />
    </intent-filter>
</activity>
```

An `https` App Link works the same way - `AuthorizationRequest.matchesRedirect` compares scheme, host, port, and path, so only the `<data>` element
and the redirect URI string change. Add `android:autoVerify="true"` to the intent-filter and host an
[`assetlinks.json`](https://developer.android.com/training/app-links/verify-android-applinks), otherwise an unverified `https` filter falls back to
the browser/app disambiguation chooser instead of opening the app directly.

## Redirect receiver

```kotlin
class AuthRedirectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Re-deliver the redirect to the existing MainActivity instance. CLEAR_TOP finishes
        // the Custom Tab sitting between this activity and MainActivity; SINGLE_TOP makes the
        // redirect arrive via onNewIntent instead of recreating the activity.
        startActivity(
            Intent(this, MainActivity::class.java)
                .setData(intent.data)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}
```

## View model

The pending request is stored via the `SavedStateHandle.saved` delegate (`androidx.lifecycle.serialization.saved`), which uses kotlinx-serialization -
`AuthorizationRequest` is
`@Serializable`, so no converter is needed. The view model must be **activity-scoped**, because the redirect enters through the activity while the
login screen renders the state.

Activity-scoped means both sides resolve the view model from the activity's `ViewModelStore`; `ViewModelProvider` caches by key within one store, so
whoever asks first creates the instance and everyone else gets the same one back. The sample below guarantees this by construction: `MainActivity`
obtains the view model via `by viewModels()` and passes the instance down to the screen. When resolving inside a composable instead, pass the activity
as the owner explicitly - inside a `NavHost` destination, a plain `viewModel()` / `hiltViewModel()` call scopes to the `NavBackStackEntry` and
silently creates a *second* instance with an empty `SavedStateHandle`, so the redirect handled by the activity's instance never reaches the UI:

```kotlin
// LocalActivity requires androidx.activity:activity-compose 1.10.0 or newer;
// on older versions, unwrap LocalContext.current to the ComponentActivity.
val activity = LocalActivity.current as ComponentActivity
val viewModel: LoginViewModel = viewModel(viewModelStoreOwner = activity, factory = /* same factory as the activity */)
// Hilt: hiltViewModel(viewModelStoreOwner = activity)
// Koin: koinViewModel(viewModelStoreOwner = activity)
```

An activity-scoped view model still restores across process death: its `SavedStateHandle` is backed by the activity's saved instance state.

```kotlin
class LoginViewModel(
    private val keycloak: Keycloak,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data object Exchanging : UiState
        data class Failed(val cancelled: Boolean) : UiState
    }

    /**
     * The in-flight authorization request. Non-null exactly while a login is awaiting its
     * redirect; saved state keeps it across process death while the browser is open.
     */
    private var pendingRequest: AuthorizationRequest? by savedStateHandle.saved { null }

    private val mutableUiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = mutableUiState.asStateFlow()

    /** Creates and remembers the authorization request; open the returned URL in a Custom Tab. */
    suspend fun beginLogin(): Url {
        val request = keycloak.createAuthorizationRequest(redirectUri = REDIRECT_URI)
        pendingRequest = request
        mutableUiState.value = UiState.Idle
        return request.url
    }

    /** Handles a captured redirect (`intent.data` from `onCreate` or `onNewIntent`). */
    fun onRedirect(redirectedUrl: String) {
        // matchesRedirect keeps unrelated deep links out of the token exchange; a redirect with
        // no pending request (stale or replayed) is dropped - handleAuthorizationRedirect would
        // reject it via the state check anyway.
        val request = pendingRequest?.takeIf { it.matchesRedirect(redirectedUrl) } ?: return
        pendingRequest = null
        mutableUiState.value = UiState.Exchanging
        viewModelScope.launch {
            keycloak.handleAuthorizationRedirect(request, redirectedUrl)
                .onRight {
                    // Tokens are stored; navigation reacts to keycloak.isLoggedIn.
                    mutableUiState.value = UiState.Idle
                }
                .onLeft { exception ->
                    val cancelled = (exception.authorizationExceptionOrNull()
                            as? KeycloakAuthorizationException.AuthorizationError)
                        ?.error == "access_denied"
                    mutableUiState.value = UiState.Failed(cancelled)
                }
        }
    }

    /**
     * Called from the activity's `onResume`. A still-pending request at that point means the
     * user came back without a redirect, i.e. closed the tab: `onNewIntent` (and a process-death
     * `onCreate`) run before `onResume`, so a delivered redirect has already cleared it.
     */
    fun onReturnedWithoutRedirect() {
        if (pendingRequest == null) return
        pendingRequest = null
        mutableUiState.value = UiState.Failed(cancelled = true)
    }

    private companion object {
        const val REDIRECT_URI = "myapp://callback"
    }
}
```

## Main activity

```kotlin
class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels { /* factory / DI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Redirect arriving after process death: the activity is recreated with the redirect
        // as its intent; the SavedStateHandle has already restored the pending request.
        intent.data?.let { loginViewModel.onRedirect(it.toString()) }
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Redirect arriving while the activity instance is alive.
        intent.data?.let { loginViewModel.onRedirect(it.toString()) }
    }

    override fun onResume() {
        super.onResume()
        loginViewModel.onReturnedWithoutRedirect()
    }
}
```

## Login screen

```kotlin
@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Button(
        enabled = uiState !is LoginViewModel.UiState.Exchanging,
        onClick = {
            scope.launch {
                val url = viewModel.beginLogin()
                CustomTabsIntent.Builder().build()
                    .launchUrl(context, url.toString().toUri())
            }
        }
    ) {
        Text("Log in")
    }
    if (uiState is LoginViewModel.UiState.Failed) {
        Text(
            if ((uiState as LoginViewModel.UiState.Failed).cancelled) "Login cancelled"
            else "Login failed"
        )
    }
}
```

Successful login needs no dedicated event: `Keycloak` exposes the session as `tokens` / `isLoggedIn` flows, so navigation away from the login screen
keys off `keycloak.isLoggedIn`, and the generated API authenticates via
`Api.setAuthProvider(Auth.MyScheme(keycloak.asBearerProvider()))`. Optionally add
`Api.setUnauthorizedHandler(keycloak.asUnauthorizedHandler())`: an API request answered with 401 despite a locally valid access token (e.g. after a
server-side session termination) then triggers a refresh and is retried once with the fresh token.

## Forcing a fresh login

The Custom Tab shares the browser's cookies, so an existing Keycloak SSO session skips the login form: the authorization request redirects back
immediately with a fresh code. To force the credentials form, append the standard OIDC `prompt=login` parameter via the `decorator`:

```kotlin
val request = keycloak.createAuthorizationRequest(redirectUri = REDIRECT_URI) {
    append("prompt", "login")
}
```

Note that `prompt=login` re-authenticates the user but leaves the SSO session itself intact. To switch accounts, end the session first by opening
Keycloak's `end_session` endpoint in the browser, so the SSO cookie is cleared for all clients of the realm.

## Covered edge cases

| Scenario                                   | Behavior                                                                                                                                       |
|--------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| User cancels on the Keycloak page          | Redirect carries `error=access_denied`; surfaced via `authorizationExceptionOrNull()` as `Failed(cancelled = true)`                            |
| User closes the tab                        | No redirect arrives; `onResume` sees the still-pending request and reports `Failed(cancelled = true)`                                          |
| Process death while the tab is open        | The redirect recreates `MainActivity`; the `saved` delegate restores the request and `onCreate` handles `intent.data`                          |
| Configuration change while the tab is open | The view model (and the in-memory request) survives; the recreated activity receives the redirect via `onNewIntent`                            |
| Stale, replayed, or injected redirect      | Dropped when nothing is pending; otherwise rejected by the `state` validation inside `handleAuthorizationRedirect` (`StateMismatch`)           |
| Unrelated deep link during login           | `matchesRedirect` keeps it out of the token exchange; if it brings the activity to the foreground, the login is cancelled like a closed tab    |

## When not to use Custom Tabs

For first-party logins into your own realm where a browser round-trip is undesirable (enterprise/kiosk apps),
`androidMain` provides `KeycloakWebViewClient` as an embedded-WebView fallback - see the
[companion README](../README.md#authorization-code--pkce) for its caveats.
