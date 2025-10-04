/* 
 * NOTE: This file is auto generated. Do not edit the file manually!
 * 
 * Test API
 * Test API Description
 * Version 1.0.0-SNAPSHOT
 * 
 * Generated Sat, 4 Oct 2025 22:37:04 +0200
 * OpenAPI KMP Gen (version 1.0.0-alpha01) by kroegerama
 */
package com.kroegerama.kmp.gen.generated

import com.kroegerama.openapi.kmp.gen.`companion`.ApiHolder
import io.ktor.http.Url
import kotlin.String
import kotlin.collections.List
import kotlin.collections.listOf

public object Api : ApiHolder() {
  public const val title: String = "Test API"

  public const val description: String = "Test API Description"

  public const val version: String = "1.0.0-SNAPSHOT"

  public const val createdAt: String = "2025-10-04T22:37:04+02:00"

  public val servers: List<Url> = listOf(
    Url("https://jsonplaceholder.typicode.com/"),
    Url("https://example.com/"),
  )

  override var baseUrl: Url = servers.first()

  public fun setAuthProvider(auth: Auth) {
    setAuthProvider(auth.key, auth::provideAuthItem)
  }

  public fun clearAuthProvider(auth: Auth) {
    clearAuthProvider(auth.key)
  }
}
