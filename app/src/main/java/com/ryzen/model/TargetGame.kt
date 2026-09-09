package com.ryzen.model

enum class TargetGame(
    val id: String,
    val title: String,
    val packageName: String,
    val libName: String
) {
    BGMI("bgmi", "BGMI (BATTLEGROUNDS)", "com.pubg.imobile", "libbgmi.so"),
    PUBG_GLOBAL("pubg_global", "PUBG MOBILE (GLOBAL)", "com.tencent.ig", "libpubgm.so")
}
