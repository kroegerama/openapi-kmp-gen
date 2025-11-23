/* 
 * NOTE: This file is auto generated. Do not edit the file manually!
 * 
 * Test API
 * Test API Description
 * Version 1.0.0-SNAPSHOT
 * 
 * Generated Sun, 23 Nov 2025 14:04:33 +0100
 * OpenAPI KMP Gen (version 1.0.0-rc02) by kroegerama
 */
@file:Suppress("ArrayInDataClass", "RedundantVisibilityModifier", "unused", "ConstPropertyName")

package com.kroegerama.kmp.gen.generated

import com.kroegerama.openapi.kmp.gen.`companion`.ApiHolder
import io.ktor.http.Url
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.listOf

public object Api : ApiHolder() {
  public const val title: String = "Test API"

  public const val description: String = "Test API Description"

  public const val version: String = "1.0.0-SNAPSHOT"

  public const val createdAt: String = "2025-11-23T14:04:33+01:00"

  public val servers: List<Url> = listOf(
    Url("https://jsonplaceholder.typicode.com/"),
    Url("https://example.com/"),
    Url("https://mock.httpstatus.io/"),
  )

  override var baseUrl: Url = servers.first()

  public fun setAuthProvider(auth: Auth) {
    setAuthProvider(auth.key, auth::provideAuthItem)
  }

  public fun clearAuthProvider(auth: Auth) {
    clearAuthProvider(auth.key)
  }
}
