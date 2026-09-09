package com.ryzen.model

import org.json.JSONArray
import org.json.JSONObject

data class GameVersion(
    val id: String,
    val gameId: String,
    val versionName: String,
    val versionCode: Int,
    val obbName: String,
    val tag: String = "LATEST", // "LATEST", "BETA", "TEST", "STABLE"
    val statusText: String = "Ready",
    val libName: String = "",
    val libVersion: String = "1.0",
    val libDownloadUrl: String = "",
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
) {
    val isComingSoon: Boolean
        get() = tag.equals("COMING SOON", ignoreCase = true) ||
                statusText.contains("coming soon", ignoreCase = true) ||
                statusText.contains("soon", ignoreCase = true)

    /**
     * Resolves the strictly assigned library file name for this game version.
     * e.g. "libbgmi460.so", "libbgmi450.so", "libpubgm450.so"
     */
    fun getAssignedLibFileName(defaultPrefix: String = "libbgmi"): String {
        val direct = libName.trim()
        if (direct.endsWith(".so", ignoreCase = true)) {
            return direct
        }
        val trimmed = libVersion.trim()
        if (trimmed.endsWith(".so", ignoreCase = true)) {
            return trimmed
        }
        val clean = versionName.replace(".", "").trim()
        if (clean.isNotBlank()) {
            return "$defaultPrefix$clean.so"
        }
        return "$defaultPrefix.so"
    }

    companion object {
        fun fromJson(obj: JSONObject, defaultGameId: String = ""): GameVersion {
            val directLib = obj.optString("lib_name", "").trim()
            val rawLibVer = obj.optString("lib_version", "1.0").trim()
            val resolvedLib = if (directLib.isNotEmpty()) directLib else (if (rawLibVer.endsWith(".so")) rawLibVer else "")
            return GameVersion(
                id = obj.optString("id", "${defaultGameId}_${obj.optInt("version_code")}"),
                gameId = obj.optString("game_id", defaultGameId),
                versionName = obj.optString("version_name", "1.0"),
                versionCode = obj.optInt("version_code", 0),
                obbName = obj.optString("obb_name", ""),
                tag = obj.optString("tag", "LATEST").uppercase(),
                statusText = obj.optString("status_text", "Ready"),
                libName = resolvedLib,
                libVersion = rawLibVer,
                libDownloadUrl = obj.optString("lib_download_url", ""),
                isDefault = obj.optBoolean("is_default", false),
                isActive = obj.optBoolean("is_active", true),
                sortOrder = obj.optInt("sort_order", 0)
            )
        }
    }
}

data class ManagedGame(
    val id: String,
    val title: String,
    val packageName: String,
    val libName: String,
    val iconType: String = "bgmi", // "bgmi", "pubg_global"
    val isEnabled: Boolean = true,
    val statusText: String = "OBB Ready",
    val sortOrder: Int = 0,
    val versions: List<GameVersion> = emptyList()
) {
    val isComingSoon: Boolean
        get() = statusText.contains("coming soon", ignoreCase = true) ||
                statusText.contains("soon", ignoreCase = true) ||
                (!isEnabled && statusText.contains("unreleased", ignoreCase = true))

    fun findMatchingVersion(versionCode: Long): GameVersion? {
        if (versionCode <= 0L) return versions.firstOrNull { it.isDefault } ?: versions.firstOrNull()
        return versions.firstOrNull { it.versionCode.toLong() == versionCode }
    }

    fun getDisplayTitle(): String {
        return when {
            title.contains("bgmi", ignoreCase = true) -> "BGMI"
            title.contains("pubg", ignoreCase = true) && title.contains("global", ignoreCase = true) -> "PUBG GL"
            title.contains("pubg", ignoreCase = true) -> "PUBG GL"
            else -> title.substringBefore(" (").ifBlank { title }
        }
    }

    companion object {
        val DEFAULT_BGMI = ManagedGame(
            id = "bgmi",
            title = "BGMI",
            packageName = "com.pubg.imobile",
            libName = "libbgmi.so",
            iconType = "bgmi",
            isEnabled = true,
            statusText = "OBB Ready",
            sortOrder = 0,
            versions = listOf(
                GameVersion(
                    id = "bgmi_4_5_0",
                    gameId = "bgmi",
                    versionName = "4.5.0",
                    versionCode = 21325,
                    obbName = "main.21325.com.pubg.imobile.obb",
                    tag = "LATEST",
                    statusText = "Ready",
                    libName = "libbgmi.so",
                    libVersion = "1.0",
                    isDefault = true,
                    isActive = true,
                    sortOrder = 0
                )
            )
        )

        val DEFAULT_PUBG = ManagedGame(
            id = "pubg_global",
            title = "PUBG GL",
            packageName = "com.tencent.ig",
            libName = "libpubgm.so",
            iconType = "pubg_global",
            isEnabled = true,
            statusText = "OBB Ready",
            sortOrder = 1,
            versions = listOf(
                GameVersion(
                    id = "pubg_4_6_0",
                    gameId = "pubg_global",
                    versionName = "4.6.0",
                    versionCode = 21525,
                    obbName = "main.21525.com.tencent.ig.obb",
                    tag = "LATEST",
                    statusText = "Ready",
                    libName = "libpubgm.so",
                    libVersion = "1.0",
                    isDefault = true,
                    isActive = true,
                    sortOrder = 0
                )
            )
        )

        val DEFAULT_GAMES = listOf(DEFAULT_BGMI, DEFAULT_PUBG)

        fun fromJson(obj: JSONObject): ManagedGame {
            val gameId = obj.optString("id", "")
            val rawTitle = obj.optString("title", gameId)
            val cleanTitle = when {
                rawTitle.contains("bgmi", ignoreCase = true) -> "BGMI"
                rawTitle.contains("pubg", ignoreCase = true) && rawTitle.contains("global", ignoreCase = true) -> "PUBG GL"
                rawTitle.contains("pubg", ignoreCase = true) -> "PUBG GL"
                else -> rawTitle.substringBefore(" (").ifBlank { rawTitle }
            }
            val versionsArray = obj.optJSONArray("versions") ?: JSONArray()
            val parsedVersions = mutableListOf<GameVersion>()
            for (i in 0 until versionsArray.length()) {
                val vObj = versionsArray.optJSONObject(i)
                if (vObj != null) {
                    parsedVersions.add(GameVersion.fromJson(vObj, gameId))
                }
            }

            return ManagedGame(
                id = gameId,
                title = cleanTitle,
                packageName = obj.optString("package_name", ""),
                libName = obj.optString("lib_name", "libbgmi.so"),
                iconType = obj.optString("icon_type", gameId),
                isEnabled = obj.optBoolean("is_enabled", true),
                statusText = obj.optString("status_text", "OBB Ready"),
                sortOrder = obj.optInt("sort_order", 0),
                versions = if (parsedVersions.isNotEmpty()) parsedVersions else when (gameId) {
                    "bgmi" -> DEFAULT_BGMI.versions
                    "pubg_global" -> DEFAULT_PUBG.versions
                    else -> emptyList()
                }
            )
        }
    }
}
