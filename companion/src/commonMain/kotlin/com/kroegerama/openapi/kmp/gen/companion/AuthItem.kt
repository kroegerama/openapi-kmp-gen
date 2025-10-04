package com.kroegerama.openapi.kmp.gen.companion

public sealed interface AuthItem {
    public data class Basic(
        val username: String,
        val password: String
    ) : AuthItem

    public data class Bearer(
        val token: String
    ) : AuthItem

    public data class ApiKey(
        val position: Position,
        val name: String,
        val value: String
    ) : AuthItem

    public enum class Position {
        Header, Query, Cookie
    }
}
