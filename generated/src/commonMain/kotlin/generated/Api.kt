package generated

import com.kroegerama.openapi.kmp.gen.companion.ApiHolder
import io.ktor.http.*

object Api : ApiHolder() {
    const val TITLE: String = "My API"
    const val DESCRIPTION: String = "Description"
    const val VERSION: String = "1.2.3"
    const val CREATED_AT: String = "2025-09-19T20:15:00Z"

    val SERVERS: List<Url> = listOf(
        Url("https://jsonplaceholder.typicode.com")
    )

    override var baseUrl = SERVERS.first()

    fun setAuthProvider(auth: Auth) {
        setAuthProvider(auth.key, auth::provideAuthItem)
    }

    fun clearAuthProvider(auth: Auth) {
        clearAuthProvider(auth.key)
    }
}
