package com.ryzen.model

/**
 * BGMI-only target. PUBG Global and other regional packages removed.
 */
enum class TargetGame(
    val id: String,
    val title: String,
    val packageName: String,
    val libName: String
) {
    BGMI("bgmi", "BGMI", "com.pubg.imobile", "libbgmi.so")
}
