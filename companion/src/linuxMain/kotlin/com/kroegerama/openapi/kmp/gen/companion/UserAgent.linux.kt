package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.uname
import platform.posix.utsname

@OptIn(ExperimentalForeignApi::class)
public actual val platformUserAgent: String = memScoped {
    val uts = alloc<utsname>()
    uname(uts.ptr)
    val sysName = uts.sysname.toKString()
    val release = uts.release.toKString()
    "curl $sysName/$release"
}
