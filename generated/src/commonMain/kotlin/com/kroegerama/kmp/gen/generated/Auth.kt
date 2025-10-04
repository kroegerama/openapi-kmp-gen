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

import com.kroegerama.openapi.kmp.gen.`companion`.AuthItem
import kotlin.String

public sealed interface Auth {
  public val key: String

  public suspend fun provideAuthItem(): AuthItem?

  public data class TokenAuth(
    public val getHeaderValue: suspend () -> String?,
  ) : Auth {
    override val key: String = ID

    override suspend fun provideAuthItem(): AuthItem? = getHeaderValue()?.let {
      AuthItem.ApiKey(
        position = AuthItem.Position.Header,
        name = "token",
        value = it
      )
    }

    public companion object {
      public const val ID: String = "TokenAuth"
    }
  }

  public data class APIKeyAuth(
    public val getQueryValue: suspend () -> String?,
  ) : Auth {
    override val key: String = ID

    override suspend fun provideAuthItem(): AuthItem? = getQueryValue()?.let {
      AuthItem.ApiKey(
        position = AuthItem.Position.Query,
        name = "token",
        value = it
      )
    }

    public companion object {
      public const val ID: String = "APIKeyAuth"
    }
  }

  public data class BasicAuth(
    public val getBasic: suspend () -> AuthItem.Basic?,
  ) : Auth {
    override val key: String = ID

    override suspend fun provideAuthItem(): AuthItem? = getBasic()

    public companion object {
      public const val ID: String = "BasicAuth"
    }
  }

  public data class BearerAuth(
    public val getBearer: suspend () -> AuthItem.Bearer?,
  ) : Auth {
    override val key: String = ID

    override suspend fun provideAuthItem(): AuthItem? = getBearer()

    public companion object {
      public const val ID: String = "BearerAuth"
    }
  }
}
