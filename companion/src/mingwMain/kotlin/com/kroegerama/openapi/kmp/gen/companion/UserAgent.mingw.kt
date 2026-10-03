package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import platform.windows.GetModuleHandleA
import platform.windows.GetProcAddress
import platform.windows.OSVERSIONINFOEXW

@OptIn(ExperimentalForeignApi::class)
public actual val platformUserAgent: String = memScoped {
    val osvi = alloc<OSVERSIONINFOEXW>()
    osvi.dwOSVersionInfoSize = sizeOf<OSVERSIONINFOEXW>().toUInt()

    val module = GetModuleHandleA("ntdll.dll") ?: return@memScoped "WinHTTP"
    val address = GetProcAddress(module, "RtlGetVersion") ?: return@memScoped "WinHTTP"

    val rtlGetVersion = address.reinterpret<CFunction<(CPointer<OSVERSIONINFOEXW>?) -> Int>>()

    val status = rtlGetVersion(osvi.ptr)
    if (status == 0) {
        "WinHTTP Windows/${osvi.dwMajorVersion}.${osvi.dwMinorVersion}.${osvi.dwBuildNumber}"
    } else {
        "WinHTTP"
    }
}
