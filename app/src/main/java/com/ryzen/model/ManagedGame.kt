package com.ryzen.model

import org.json.JSONArray
import org.json.JSONObject

data class GameVersion(
    val id: String,
    val gameId: String,
    val versionName: String,
    val versionCode: Int,
    val obbName: String,
    val tag: String = "LATEST",
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
     * Resolves the assigned BGMI library file name for this version.
     * e.g. "libbgmi.so", "libbgmi460.so"
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
    val iconType: String = "bgmi",
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

    fun getDisplayTitle(): String = "BGMI"

    companion object {
        const val BGMI_PACKAGE = "com.pubg.imobile"
        const val BGMI_LIB = "libbgmi.so"

        val DEFAULT_BGMI = ManagedGame(
            id = "bgmi",
            title = "BGMI",
            packageName = BGMI_PACKAGE,
            libName = BGMI_LIB,
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
                    libName = BGMI_LIB,
                    libVersion = "1.0",
                    isDefault = true,
                    isActive = true,
                    sortOrder = 0
                )
            )
        )

        /** BGMI-only product — single game list. */
        val DEFAULT_GAMES = listOf(DEFAULT_BGMI)

        fun isBgmiPackage(packageName: String?): Boolean =
            packageName != null && packageName.equals(BGMI_PACKAGE, ignoreCase = true)

        fun fromJson(obj: JSONObject): ManagedGame? {
            val packageName = obj.optString("package_name", "").trim()
            val gameId = obj.optString("id", "").trim()
            // Drop any non-BGMI game from server config (PUBG GL removed)
            if (packageName.isNotEmpty() && !isBgmiPackage(packageName)) {
                return null
            }
            if (gameId.isNotEmpty() &&
                !gameId.equals("bgmi", ignoreCase = true) &&
                packageName.isEmpty()
            ) {
                return null
            }

            val versionsArray = obj.optJSONArray("versions") ?: JSONArray()
            val parsedVersions = mutableListOf<GameVersion>()
            for (i in 0 until versionsArray.length()) {
                val vObj = versionsArray.optJSONObject(i)
                if (vObj != null) {
                    parsedVersions.add(GameVersion.fromJson(vObj, "bgmi"))
                }
            }

            return ManagedGame(
                id = "bgmi",
                title = "BGMI",
                packageName = if (packageName.isNotEmpty()) packageName else BGMI_PACKAGE,
                libName = obj.optString("lib_name", BGMI_LIB).ifBlank { BGMI_LIB },
                iconType = "bgmi",
                isEnabled = obj.optBoolean("is_enabled", true),
                statusText = obj.optString("status_text", "OBB Ready"),
                sortOrder = obj.optInt("sort_order", 0),
                versions = if (parsedVersions.isNotEmpty()) parsedVersions else DEFAULT_BGMI.versions
            )
        }
    }
}
