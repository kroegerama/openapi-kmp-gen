package generated

import com.kroegerama.openapi.kmp.gen.companion.AuthItem

sealed interface Auth {
    val key: String
    suspend fun provideAuthItem(): AuthItem?

    data class TokenAuth(
        val getHeaderValue: suspend () -> String?
    ) : Auth {
        override val key: String = ID

        override suspend fun provideAuthItem(): AuthItem? =
            getHeaderValue()?.let {
                AuthItem.ApiKey(
                    position = AuthItem.Position.Header,
                    name = "token",
                    value = it
                )
            }

        companion object {
            const val ID = "TokenAuth"
        }
    }

    data class APIKeyAuth(
        val getQueryValue: suspend () -> String?
    ) : Auth {
        override val key: String = ID
        override suspend fun provideAuthItem(): AuthItem? =
            getQueryValue()?.let {
                AuthItem.ApiKey(
                    position = AuthItem.Position.Query,
                    name = "token",
                    value = it
                )
            }

        companion object {
            const val ID: String = "APIKeyAuth"
        }
    }

    data class BasicAuth(
        val getAuthItem: suspend () -> AuthItem.Basic?
    ) : Auth {
        override val key: String = ID
        override suspend fun provideAuthItem(): AuthItem? = getAuthItem()

        companion object {
            const val ID: String = "BasicAuth"
        }
    }
}
