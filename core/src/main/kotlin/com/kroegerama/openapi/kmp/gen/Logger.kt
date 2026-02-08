package com.kroegerama.openapi.kmp.gen

interface Logger {
    fun info(message: String)
    fun lifecycle(message: String)
    fun error(message: String)
}
