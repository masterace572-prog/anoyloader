package com.ryzen

import com.ryzen.R

/**
 * Single place for display branding that differs between Anoy Loader and OG Cheats.
 * Resource strings are the source of truth so product flavors/modules can override them.
 */
object BrandConfig {
    @JvmStatic
    fun appName(): String = try {
        // Prefer runtime resources when an Application context is available via BlackBox host.
        top.niunaijun.blackbox.BlackBoxCore.getContext().getString(R.string.app_name)
    } catch (_: Throwable) {
        fallbackName()
    }

    @JvmStatic
    fun brandName(): String = try {
        top.niunaijun.blackbox.BlackBoxCore.getContext().getString(R.string.brand_name)
    } catch (_: Throwable) {
        fallbackName()
    }

    @JvmStatic
    fun brandEnterprise(): String = try {
        top.niunaijun.blackbox.BlackBoxCore.getContext().getString(R.string.brand_enterprise)
    } catch (_: Throwable) {
        fallbackName() + " Enterprise"
    }

    @JvmStatic
    fun telegramUrl(): String = try {
        top.niunaijun.blackbox.BlackBoxCore.getContext().getString(R.string.telegram_url)
    } catch (_: Throwable) {
        "https://t.me/libAkAudioVisiual"
    }

    @JvmStatic
    fun telegramHandle(): String = try {
        top.niunaijun.blackbox.BlackBoxCore.getContext().getString(R.string.telegram_handle)
    } catch (_: Throwable) {
        "@libAkAudioVisiual"
    }

    @JvmStatic
    fun userAgent(): String = try {
        top.niunaijun.blackbox.BlackBoxCore.getContext().getString(R.string.http_user_agent)
    } catch (_: Throwable) {
        "Mozilla/5.0 (Linux; Android) Anoy-Loader"
    }

    @JvmStatic
    fun crashReportHeader(): String = try {
        top.niunaijun.blackbox.BlackBoxCore.getContext().getString(R.string.crash_report_header)
    } catch (_: Throwable) {
        "Anoy Loader Crash Report"
    }

    private fun fallbackName(): String = "Anoy Loader"
}
