package com.ryzen

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.ryzen.model.GameVersion
import com.ryzen.model.ManagedGame
import com.ryzen.ui.components.AppBottomNav
import com.ryzen.ui.components.GameNotInstalledDialogState
import com.ryzen.ui.components.MainNavTab
import com.ryzen.ui.screens.MainDashboardScreen
import com.ryzen.ui.screens.SettingsScreen
import com.ryzen.ui.theme.AppTheme
import com.ryzen.utils.AppConfigManager
import com.ryzen.utils.AppManager
import com.ryzen.utils.Downtwo
import com.ryzen.utils.PermissionsManager
import com.ryzen.utils.Prefs
import org.lsposed.lsparanoid.Obfuscate
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.core.env.BEnvironment
import top.niunaijun.blackbox.entity.pm.InstallResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InstalledAppInfo(
    val isInstalled: Boolean,
    val versionName: String = "",
    val versionCode: Long = 0L
)

@Obfuscate
class MAct : AppCompatActivity() {

    companion object {
        init {
            try {
                System.loadLibrary("ryzen")
            } catch (ignored: Throwable) {}
        }

        private const val USER_ID = 0
        private const val PREF_CLONED_VERSION_PREFIX = "cloned_ver_code_"

        @JvmStatic
        external fun apkcrc(): String

        @JvmStatic
        external fun exdate(): String
    }

    external fun ZENINOP(): String

    // Compose Reactive State Holders
    private val daysState = mutableStateOf("00")
    private val hoursState = mutableStateOf("00")
    private val minsState = mutableStateOf("00")
    private val secsState = mutableStateOf("00")
    private val isLifetimeState = mutableStateOf(false)
    private val isLaunchingState = mutableStateOf(false)
    private val isInstallingState = mutableStateOf(false)
    private val isCopyingObbState = mutableStateOf(false)
    private val obbProgressState = mutableFloatStateOf(0f)
    private val progressMessageState = mutableStateOf("")
    private val hasObbState = mutableStateOf(false)
    private val isClonedInContainerState = mutableStateOf(false)
    private val showOptionsDialogState = mutableStateOf(false)
    private val gameNotInstalledDialogState = mutableStateOf(GameNotInstalledDialogState())

    // Dynamic Game & Version Management States
    private val gamesState = mutableStateOf<List<ManagedGame>>(ManagedGame.DEFAULT_GAMES)
    private val selectedGameState = mutableStateOf<ManagedGame>(ManagedGame.DEFAULT_BGMI)
    private val selectedVersionState = mutableStateOf<GameVersion?>(ManagedGame.DEFAULT_BGMI.versions.firstOrNull())
    private val installedAppInfoState = mutableStateOf(InstalledAppInfo(false))
    private val currentNavTabState = mutableStateOf(MainNavTab.GAMES)

    private var appManager: AppManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var expiryRunnable: Runnable? = null
    private var lastLibCheckTimestamp = 0L

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appManager = AppManager(this)
        refreshSelectedGameAndVersion()
        checkServerGameConfig()
        doCountTimerAccount()



        setContent {
            AppTheme {
                val currentNavTab = currentNavTabState.value
                val currentGame = selectedGameState.value
                val currentVersion = selectedVersionState.value
                val appInfo = installedAppInfoState.value
                val isHostInstalled = appInfo.isInstalled

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.background)
                ) {
                    Crossfade(
                        targetState = currentNavTab,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "screen_crossfade",
                        modifier = Modifier.fillMaxSize()
                    ) { tab ->
                        when (tab) {
                            MainNavTab.GAMES -> {
                                MainDashboardScreen(
                                    days = daysState.value,
                                    hours = hoursState.value,
                                    mins = minsState.value,
                                    secs = secsState.value,
                                    isLifetime = isLifetimeState.value,
                                    isLaunching = isLaunchingState.value,
                                    isInstalling = isInstallingState.value,
                                    isCopyingObb = isCopyingObbState.value,
                                    obbProgress = obbProgressState.floatValue,
                                    progressMessage = progressMessageState.value,
                                    hasObb = hasObbState.value,
                                    isClonedInContainer = isClonedInContainerState.value,
                                    games = gamesState.value,
                                    selectedGame = currentGame,
                                    selectedVersion = currentVersion,
                                    isHostGameInstalled = isHostInstalled,
                                    hostInstalledVersionName = appInfo.versionName,
                                    hostInstalledVersionCode = appInfo.versionCode,
                                    onSelectGame = { game ->
                                        refreshSelectedGameAndVersion(newGame = game)
                                    },
                                    isGameEnabled = currentGame.isEnabled,
                                    gameStatusText = currentGame.statusText,
                                    onLaunchClick = { handleLaunchFlow() },
                                    onInstallClick = { handleInstallFlow() },
                                    onLaunchVersionClick = { version ->
                                        selectedVersionState.value = version
                                        handleLaunchFlow()
                                    },
                                    onInstallVersionClick = { version ->
                                        selectedVersionState.value = version
                                        handleInstallFlow()
                                    },
                                    onSyncObbClick = { copyObbFiles(launchAfterCopy = true) },
                                    onClearLoginClick = { handleClearLoginForGame(currentGame) },
                                    showOptionsDialog = showOptionsDialogState.value,
                                    onOptionsDismiss = { showOptionsDialogState.value = false },
                                    onOptionsClick = { showOptionsDialogState.value = true },
                                    gameNotInstalledDialogState = gameNotInstalledDialogState.value,
                                    onInstallFromPlayStore = {
                                        val pkg = gameNotInstalledDialogState.value.packageName
                                        gameNotInstalledDialogState.value = gameNotInstalledDialogState.value.copy(isVisible = false)
                                        openPlayStore(pkg)
                                    },
                                    onDismissGameNotInstalledDialog = {
                                        gameNotInstalledDialogState.value = gameNotInstalledDialogState.value.copy(isVisible = false)
                                    }
                                )
                            }
                            MainNavTab.SETTINGS -> {
                                val prefs = Prefs(this@MAct)
                                val keyText = prefs.getSt("user_key", "")
                                    .ifBlank { prefs.getSt("USER", "") }
                                    .ifBlank { prefs.getSt("license_key", "") }
                                    .ifBlank { prefs.getSt("key", "") }
                                val expiryStr = prefs.getSt("expiry_date", "")
                                val packageInfo = try { packageManager.getPackageInfo(packageName, 0) } catch (_: Throwable) { null }
                                val verName = packageInfo?.versionName ?: "2026.01.01"
                                val verCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                    packageInfo?.longVersionCode ?: 2L
                                } else {
                                    @Suppress("DEPRECATION")
                                    (packageInfo?.versionCode ?: 2).toLong()
                                }

                                SettingsScreen(
                                    keyText = keyText,
                                    isLifetime = isLifetimeState.value,
                                    expiryDateStr = expiryStr,
                                    days = daysState.value,
                                    hours = hoursState.value,
                                    mins = minsState.value,
                                    games = gamesState.value,
                                    selectedGame = currentGame,
                                    onSelectGame = { game ->
                                        refreshSelectedGameAndVersion(newGame = game)
                                    },
                                    onClearLoginClick = { game ->
                                        handleClearLoginForGame(game)
                                    },
                                    onClearGameDataClick = { game ->
                                        handleClearGameDataForGame(game)
                                    },
                                    onContactAdminClick = {
                                        handleContactAdmin()
                                    },
                                    appVersionName = verName,
                                    appVersionCode = verCode
                                )
                            }
                        }
                    }

                    // Floating Dock Bottom Navigation
                    AppBottomNav(
                        selectedTab = currentNavTab,
                        onTabSelected = { tab ->
                            currentNavTabState.value = tab
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkServerGameConfig()
        refreshSelectedGameAndVersion()
    }

    /**
     * Inspect host device package manager to detect whether the game is installed,
     * and retrieve its versionCode and versionName.
     */
    private fun getInstalledAppInfo(pkg: String): InstalledAppInfo {
        return try {
            val packageInfo = packageManager.getPackageInfo(pkg, 0)
            val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            InstalledAppInfo(
                isInstalled = true,
                versionName = packageInfo.versionName ?: "",
                versionCode = vCode
            )
        } catch (e: PackageManager.NameNotFoundException) {
            InstalledAppInfo(isInstalled = false)
        } catch (t: Throwable) {
            InstalledAppInfo(isInstalled = false)
        }
    }

    /**
     * Refreshes the selected game, auto-selects the version matching the installed APK if present,
     * checks container cloning state, and performs automatic uncloning if host game updated from Play Store.
     */
    private fun refreshSelectedGameAndVersion(
        newGame: ManagedGame? = null,
        forceVersion: GameVersion? = null
    ) {
        val game = newGame ?: selectedGameState.value
        selectedGameState.value = game

        val appInfo = getInstalledAppInfo(game.packageName)
        installedAppInfoState.value = appInfo

        val isContainerInstalled = try {
            BlackBoxCore.get()?.isInstalled(game.packageName, USER_ID) == true
        } catch (t: Throwable) {
            false
        }

        val prefs = Prefs(this)
        val prefKey = PREF_CLONED_VERSION_PREFIX + game.packageName
        val savedClonedCode = prefs.getLong(prefKey, 0L)

        if (appInfo.isInstalled) {
            val matched = if (forceVersion != null) {
                forceVersion
            } else {
                game.findMatchingVersion(appInfo.versionCode)
                    ?: game.versions.firstOrNull { it.isDefault }
                    ?: game.versions.firstOrNull()
            }
            selectedVersionState.value = matched

            if (isContainerInstalled) {
                // Smart Game Update Detection:
                // If host game updated to a newer version (e.g. 21455 != 21325),
                // automatically unclone old container APK & clean old container OBB, preserving user data.
                if (savedClonedCode > 0L && savedClonedCode != appInfo.versionCode) {
                    Log.i("MAct", "Host ${game.getDisplayTitle()} updated from code $savedClonedCode to ${appInfo.versionCode}. Cleaning old container...")
                    uncloneOutdatedVersion(game.packageName, savedClonedCode)
                    prefs.setLong(prefKey, 0L)
                    isClonedInContainerState.value = false
                    hasObbState.value = false
                    Toast.makeText(
                        this,
                        "Detected updated ${game.getDisplayTitle()} (v${appInfo.versionName}). Please tap INSTALL to update sandbox.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    if (savedClonedCode == 0L) {
                        prefs.setLong(prefKey, appInfo.versionCode)
                    }
                    isClonedInContainerState.value = true
                    hasObbState.value = hasDestinationObb(game, matched)
                }
            } else {
                isClonedInContainerState.value = false
                hasObbState.value = false
            }
        } else {
            selectedVersionState.value = game.versions.firstOrNull { it.isDefault } ?: game.versions.firstOrNull()
            isClonedInContainerState.value = false
            hasObbState.value = false
        }
    }

    /**
     * Unclones outdated container APK and deletes old container OBBs.
     * Preserves user data directory (/sdcard/Android/data/<pkg>) so settings/controls are never lost.
     */
    private fun uncloneOutdatedVersion(packageName: String, oldVersionCode: Long) {
        try {
            BlackBoxCore.get()?.stopPackage(packageName, USER_ID)
            BlackBoxCore.get()?.uninstallPackageAsUser(packageName, USER_ID)
            Log.i("MAct", "Uninstalled outdated package $packageName from sandbox container")
        } catch (t: Throwable) {
            Log.w("MAct", "Error during container package uninstall: ${t.message}")
        }

        try {
            val candidateDirs = getCandidateDestinationDirs(packageName)
            for (dir in candidateDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles { _, name ->
                        name != null && (name.contains("$oldVersionCode") || name.endsWith(".obb"))
                    }
                    if (files != null) {
                        for (f in files) {
                            val deleted = f.delete()
                            Log.i("MAct", "Deleted outdated container OBB: ${f.name} (success: $deleted)")
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w("MAct", "Error removing outdated container OBB: ${t.message}")
        }
    }

    /**
     * Poll remote server config to receive dynamically added/removed games and versions.
     */
    private fun checkServerGameConfig() {
        AppConfigManager.fetchConfig(this) { config ->
            if (isFinishing || isDestroyed) return@fetchConfig
            // Games are maintained purely client-side; server config handles maintenance/announcements
        }
        // Auto-check and download new native library ZIP package if admin bumped version
        val now = System.currentTimeMillis()
        if (now - lastLibCheckTimestamp > 120_000L) {
            lastLibCheckTimestamp = now
            try {
                Downtwo(this) { success, msg ->
                    if (success && msg == null) {
                        Log.i("MAct", "Downloaded and deployed updated core native library ZIP")
                    }
                }.startDownload()
            } catch (t: Throwable) {
                Log.w("MAct", "Downtwo auto-check error: ${t.message}")
            }
        }
    }

    private fun getHostGameApkPath(pkg: String): String? {
        return try {
            val packageInfo = packageManager.getPackageInfo(pkg, 0)
            packageInfo.applicationInfo?.sourceDir
        } catch (ignored: Throwable) {
            null
        }
    }

    private fun getExpectedObbName(game: ManagedGame, version: GameVersion?): String {
        if (version != null && version.obbName.isNotBlank()) {
            return version.obbName
        }
        val code = version?.versionCode ?: 0
        return if (code > 0) "main.$code.${game.packageName}.obb" else "main.obb"
    }

    private fun getSourceObbFiles(pkg: String, expectedName: String = ""): List<File> {
        val results = mutableListOf<File>()
        try {
            val candidateDirs = listOf(
                File(Environment.getExternalStorageDirectory(), "Android/obb/$pkg"),
                File("/storage/emulated/0/Android/obb/$pkg"),
                File("/sdcard/Android/obb/$pkg")
            )
            for (obbDir in candidateDirs) {
                if (obbDir.exists() && obbDir.isDirectory) {
                    val files = obbDir.listFiles { _, name -> name != null && name.endsWith(".obb") }
                    if (files != null && files.isNotEmpty()) {
                        for (f in files) {
                            if (f != null && f.isFile && f.length() > 0 && results.none { it.name == f.name }) {
                                if (expectedName.isNotBlank() && f.name.equals(expectedName, ignoreCase = true)) {
                                    results.add(0, f) // Place exact match first
                                } else {
                                    results.add(f)
                                }
                            }
                        }
                        if (results.isNotEmpty()) break
                    }
                }
            }
        } catch (ignored: Throwable) {}
        return results
    }

    private fun getSourceObbFile(pkg: String, expectedName: String = ""): File {
        val files = getSourceObbFiles(pkg, expectedName)
        if (files.isNotEmpty()) {
            if (expectedName.isNotBlank()) {
                val exact = files.firstOrNull { it.name.equals(expectedName, ignoreCase = true) }
                if (exact != null) return exact
            }
            val mainObb = files.firstOrNull { it.name.startsWith("main.") }
            if (mainObb != null) return mainObb
            return files.maxByOrNull { it.length() } ?: files[0]
        }
        return File("/storage/emulated/0/Android/obb/$pkg/${expectedName.ifBlank { "main.obb" }}")
    }

    private fun getCandidateDestinationDirs(pkg: String): List<File> {
        val dirs = mutableListOf<File>()
        try {
            // Priority 1: SdCard/0/Android/obb/pkg (Android 10+ standard in Bcore)
            dirs.add(File(Environment.getExternalStorageDirectory(), "SdCard/0/Android/obb/$pkg"))
            // Priority 2: SdCard/Android/obb/pkg
            dirs.add(File(Environment.getExternalStorageDirectory(), "SdCard/Android/obb/$pkg"))
            // Priority 3: BEnvironment.getExternalObbDir
            val bDir = BEnvironment.getExternalObbDir(pkg)
            if (bDir != null) dirs.add(bDir)
        } catch (ignored: Throwable) {}
        dirs.add(File(filesDir, "blackbox/storage/emulated/0/Android/obb/$pkg"))
        return dirs
    }

    private fun getObbDestinationDir(pkg: String): File {
        val candidate = File(Environment.getExternalStorageDirectory(), "SdCard/0/Android/obb/$pkg")
        if (candidate.exists() || File(Environment.getExternalStorageDirectory(), "SdCard/0").exists()) {
            if (!candidate.exists()) candidate.mkdirs()
            return candidate
        }
        try {
            val dir = BEnvironment.getExternalObbDir(pkg)
            if (dir != null) {
                if (!dir.exists()) dir.mkdirs()
                return dir
            }
        } catch (ignored: Throwable) {}
        val fallback = File(filesDir, "blackbox/storage/emulated/0/Android/obb/$pkg")
        if (!fallback.exists()) fallback.mkdirs()
        return fallback
    }

    private fun hasDestinationObb(
        game: ManagedGame = selectedGameState.value,
        version: GameVersion? = selectedVersionState.value
    ): Boolean {
        return try {
            val pkg = game.packageName
            val expectedName = getExpectedObbName(game, version)
            val candidateDirs = getCandidateDestinationDirs(pkg)
            for (destDir in candidateDirs) {
                if (destDir.isDirectory) {
                    val obbFiles = destDir.listFiles { _, name -> name != null && name.endsWith(".obb") }
                    if (obbFiles != null && obbFiles.isNotEmpty()) {
                        if (obbFiles.any { it.name.equals(expectedName, ignoreCase = true) && it.length() > 10 * 1024 * 1024 }) {
                            return true
                        }
                        val vCode = version?.versionCode ?: 0
                        if (vCode > 0 && obbFiles.any { it.name.contains("$vCode") && it.length() > 10 * 1024 * 1024 }) {
                            return true
                        }
                        if (obbFiles.any { it.name.startsWith("main.") && it.length() > 50 * 1024 * 1024 }) {
                            return true
                        }
                    }
                }
            }
            false
        } catch (t: Throwable) {
            false
        }
    }

    private fun copyFileSimple(source: File, destination: File) {
        try {
            if (destination.exists()) destination.delete()
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            destination.setReadable(true, false)
            destination.setExecutable(true, false)
            destination.setWritable(true, false)
        } catch (e: Exception) {
            Log.w("MAct", "copyFileSimple failed from ${source.name} to ${destination.name}: ${e.message}")
        }
    }

    /**
     * Resolves and sets up the active native library matching the running game and version.
     * Enforces strict version isolation so BGMI only loads libbgmi* and PUBG Global only loads libpubgm*.
     * Never cross-wires the two - that is a primary cause of PUBG GL crash-after-launch.
     */
    private fun prepareActiveLibForGame(game: ManagedGame, version: GameVersion?, appInfo: InstalledAppInfo): Boolean {
        try {
            val loaderDir = File(filesDir, "loader")
            if (!loaderDir.exists()) loaderDir.mkdirs()

            val vCode = if (appInfo.versionCode > 0) appInfo.versionCode else (version?.versionCode?.toLong() ?: 0L)
            val vName = if (appInfo.versionName.isNotBlank()) appInfo.versionName else (version?.versionName ?: "")
            val cleanVer = vName.replace(".", "").trim()

            val isBgmi = game.packageName == "com.pubg.imobile"
            val prefix = if (isBgmi) "libbgmi" else "libpubgm"
            val activeName = "${prefix}_active.so"
            val foreignPrefix = if (isBgmi) "libpubgm" else "libbgmi"

            val assignedLib = version?.getAssignedLibFileName(prefix) ?: (prefix + cleanVer + ".so")

            // Priority order: assigned -> versioned -> generic real lib -> active copy
            val candidateNames = mutableListOf<String>()
            if (assignedLib.isNotBlank()
                && assignedLib.startsWith(prefix, ignoreCase = true)
                && !candidateNames.contains(assignedLib)
            ) {
                candidateNames.add(assignedLib)
            }
            if (vCode > 0) candidateNames.add("${prefix}_$vCode.so")
            if (cleanVer.isNotBlank()) {
                candidateNames.add("$prefix$cleanVer.so")
                candidateNames.add("${prefix}_v$cleanVer.so")
            }
            if (vName.isNotBlank()) candidateNames.add("${prefix}_$vName.so")
            candidateNames.add("$prefix.so") // Real library: libbgmi.so or libpubgm.so
            candidateNames.add(activeName)
            if (!isBgmi) {
                // Alternate names sometimes used by Global packs
                candidateNames.add("libpubg.so")
            }

            var foundFile: File? = null
            for (cand in candidateNames) {
                val f = File(loaderDir, cand)
                if (!f.exists() || !f.isFile || f.length() < 100) continue
                val lower = f.name.lowercase()
                // Hard block cross-game libraries
                if (lower.startsWith(foreignPrefix)) continue
                if (isBgmi && lower.startsWith("libpubg")) continue
                if (!isBgmi && lower.startsWith("libbgmi")) continue
                foundFile = f
                break
            }

            // Last resort: any so in loader that matches prefix
            if (foundFile == null) {
                val extras = loaderDir.listFiles { _, name ->
                    name != null && name.lowercase().startsWith(prefix) && name.lowercase().endsWith(".so")
                }
                if (extras != null) {
                    foundFile = extras
                        .filter { it.isFile && it.length() >= 100 }
                        .maxByOrNull { it.lastModified() }
                }
            }

            val activeConfigFile = File(loaderDir, "active_lib_for_${game.packageName}.txt")
            val activeFile = File(loaderDir, activeName)

            if (foundFile != null) {
                Log.i(
                    "MAct",
                    "Selected version-specific library: ${foundFile.name} for ${game.getDisplayTitle()} v$vName (vCode: $vCode)"
                )
                try {
                    activeConfigFile.writeText(foundFile.name)
                } catch (e: Exception) {
                    Log.w("MAct", "Failed writing active config: ${e.message}")
                }
                // Only rewrite active copy when source differs
                if (foundFile.absolutePath != activeFile.absolutePath) {
                    copyFileSimple(foundFile, activeFile)
                }
                try {
                    Runtime.getRuntime().exec(arrayOf("chmod", "777", activeFile.absolutePath)).waitFor()
                    Runtime.getRuntime().exec(arrayOf("chmod", "777", foundFile.absolutePath)).waitFor()
                } catch (_: Exception) {
                }
                return true
            } else {
                // Do NOT delete a previously-working active file just because resolution failed once
                Log.e(
                    "MAct",
                    "STRICT LIB CHECK: No matching native lib found for ${game.packageName} v$vName ($cleanVer). Expected: $assignedLib"
                )
                return activeFile.exists() && activeFile.length() >= 100
            }
        } catch (t: Throwable) {
            Log.w("MAct", "prepareActiveLibForGame error", t)
            return false
        }
    }

    /**
     * PUBG Global (and other international builds) rely on Google Play Services inside the sandbox.
     * BGMI typically does not. Missing GMS is a common cause of launch / early-crash on com.tencent.ig.
     */
    private fun ensureGmsForGame(packageName: String) {
        if (packageName == "com.pubg.imobile") return
        try {
            val core = BlackBoxCore.get() ?: return
            if (!core.isSupportGms) {
                Log.w("MAct", "Host device has no GMS - skipping sandbox GMS install")
                return
            }
            if (!core.isInstallGms(USER_ID)) {
                Log.i("MAct", "Installing Google Play Services into sandbox for $packageName ...")
                val result = core.installGms(USER_ID)
                if (result != null && result.success) {
                    Log.i("MAct", "Sandbox GMS installed successfully")
                } else {
                    Log.w("MAct", "Sandbox GMS install reported: ${result?.msg}")
                }
            }
        } catch (t: Throwable) {
            Log.w("MAct", "ensureGmsForGame failed: ${t.message}")
        }
    }

    /**
     * Ensure ApplicationInfo.splitSourceDirs are usable and native libs were extracted.
     * Reinstalls the sandbox package if the previous install looks incomplete.
     */
    private fun installGameIntoSandbox(core: BlackBoxCore, packageName: String): InstallResult? {
        // Prefer package-name install (FLAG_SYSTEM) so host split APKs stay linked.
        var installRes = try {
            core.installPackageAsUser(packageName, USER_ID)
        } catch (t: Throwable) {
            Log.w("MAct", "installPackageAsUser(pkg) failed: ${t.message}")
            null
        }

        if (installRes == null || !installRes.success) {
            val apkPath = getHostGameApkPath(packageName)
            if (apkPath != null) {
                installRes = try {
                    core.installPackageAsUser(File(apkPath), USER_ID)
                } catch (t: Throwable) {
                    Log.w("MAct", "installPackageAsUser(file) failed: ${t.message}")
                    null
                }
            }
        }

        // If already marked installed but native lib dir is empty, force reinstall (split recovery)
        try {
            if (core.isInstalled(packageName, USER_ID)) {
                val libDir = try {
                    BEnvironment.getAppLibDir(packageName)
                } catch (_: Throwable) {
                    null
                }
                val libCount = libDir?.listFiles()?.count { it.isFile && it.name.endsWith(".so") } ?: 0
                // PUBG Global almost always ships native code in ABI splits - empty dir is a red flag.
                // Also reinstall if host has splitSourceDirs but sandbox lib dir is sparse.
                val hostHasSplits = try {
                    val ai = packageManager.getApplicationInfo(packageName, 0)
                    // Local val required: splitSourceDirs is a mutable Java field (no smart cast)
                    val splits = ai.splitSourceDirs
                    splits != null && splits.isNotEmpty()
                } catch (_: Throwable) {
                    false
                }
                if (libCount == 0 && (packageName != "com.pubg.imobile" || hostHasSplits)) {
                    Log.w("MAct", "Sandbox native lib dir empty for $packageName (splits=$hostHasSplits) - forcing reinstall")
                    installRes = try {
                        core.reinstallPackageAsUser(packageName, USER_ID)
                    } catch (t: Throwable) {
                        Log.w("MAct", "reinstallPackageAsUser failed: ${t.message}")
                        installRes
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w("MAct", "post-install lib check failed: ${t.message}")
        }

        return installRes
    }

    /**
     * INSTALL flow: clones APK into BlackBoxCore sandbox, transfers OBB, sets up native library.
     */
    private fun handleInstallFlow() {
        val currentGame = selectedGameState.value
        val appInfo = installedAppInfoState.value
        val currentVersion = selectedVersionState.value

        if (currentGame.isComingSoon || (currentVersion != null && currentVersion.isComingSoon)) {
            Toast.makeText(this, "${currentGame.getDisplayTitle()} is coming soon! Stay tuned for release.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!currentGame.isEnabled) {
            val reason = currentGame.statusText.ifBlank { "Game is currently disabled" }
            Toast.makeText(this, "${currentGame.getDisplayTitle()} is currently unavailable ($reason)", Toast.LENGTH_SHORT).show()
            return
        }

        if (isInstallingState.value || isCopyingObbState.value || isLaunchingState.value) return

        if (!PermissionsManager.hasStoragePermission(this)) {
            Toast.makeText(this, "Storage access is required to set up sandbox", Toast.LENGTH_SHORT).show()
            PermissionsManager.requestNextPermission(this)
            return
        }

        if (!appInfo.isInstalled) {
            gameNotInstalledDialogState.value = GameNotInstalledDialogState(
                isVisible = true,
                gameTitle = currentGame.getDisplayTitle(),
                packageName = currentGame.packageName,
                iconType = currentGame.iconType,
                isBgmi = currentGame.packageName == "com.pubg.imobile"
            )
            return
        }

        isInstallingState.value = true
        progressMessageState.value = "Cloning ${currentGame.getDisplayTitle()} into sandbox..."
        obbProgressState.floatValue = 0.05f

        Thread {
            try {
                val core = BlackBoxCore.get()
                if (core == null) {
                    runOnUiThread {
                        isInstallingState.value = false
                        Toast.makeText(this, "Virtual engine is initializing. Please retry in a moment.", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                // 0. PUBG Global / international builds need GMS inside the sandbox
                runOnUiThread {
                    progressMessageState.value = "Preparing sandbox services..."
                }
                ensureGmsForGame(currentGame.packageName)

                // 1. Clone APK (+ splits / native libs) into virtual container
                runOnUiThread {
                    progressMessageState.value = "Cloning ${currentGame.getDisplayTitle()} into sandbox..."
                }
                val installRes = installGameIntoSandbox(core, currentGame.packageName)

                if (installRes == null || !installRes.success) {
                    // If already installed from a previous session, treat as success
                    val already = try { core.isInstalled(currentGame.packageName, USER_ID) } catch (_: Throwable) { false }
                    if (!already) {
                        val err = installRes?.msg ?: "Virtual installation failed"
                        runOnUiThread {
                            isInstallingState.value = false
                            Toast.makeText(this, err, Toast.LENGTH_LONG).show()
                        }
                        return@Thread
                    }
                    Log.i("MAct", "Install reported failure but package is present in sandbox - continuing")
                }

                // 2. Prepare OBB
                val expectedObb = getExpectedObbName(currentGame, currentVersion)
                val destinationDir = getObbDestinationDir(currentGame.packageName)
                val sourceFiles = getSourceObbFiles(currentGame.packageName, expectedObb)
                if (sourceFiles.isNotEmpty() && !hasDestinationObb(currentGame, currentVersion)) {
                    runOnUiThread {
                        progressMessageState.value = "Transferring game OBB..."
                    }
                    copyObbInternal(sourceFiles, destinationDir, expectedObb)
                } else if (sourceFiles.isEmpty()) {
                    Log.w("MAct", "No host OBB found for ${currentGame.packageName} (expected $expectedObb)")
                }

                // 3. Prepare version-specific library (strict per-game, no cross-wiring)
                runOnUiThread {
                    progressMessageState.value = "Binding native library..."
                }
                val libOk = prepareActiveLibForGame(currentGame, currentVersion, appInfo)
                if (!libOk) {
                    Log.w("MAct", "Native lib not ready for ${currentGame.packageName} - launch may be unstable until lib update finishes")
                }

                // 4. Record cloned version code in Prefs
                val prefs = Prefs(this)
                prefs.setLong(PREF_CLONED_VERSION_PREFIX + currentGame.packageName, appInfo.versionCode)

                runOnUiThread {
                    isInstallingState.value = false
                    isClonedInContainerState.value = true
                    hasObbState.value = hasDestinationObb(currentGame, currentVersion) || sourceFiles.isEmpty()
                    obbProgressState.floatValue = 1.0f
                    progressMessageState.value = "Installation Complete"
                    Toast.makeText(this, "${currentGame.getDisplayTitle()} is ready to launch!", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                Log.e("MAct", "Sandbox installation error", t)
                runOnUiThread {
                    isInstallingState.value = false
                    Toast.makeText(this, "Installation error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /**
     * LAUNCH flow: verifies permissions, license session, Bcore activation, and executes launch.
     */
    private fun handleLaunchFlow() {
        val currentGame = selectedGameState.value
        val currentVersion = selectedVersionState.value
        val appInfo = installedAppInfoState.value

        if (currentGame.isComingSoon || (currentVersion != null && currentVersion.isComingSoon)) {
            Toast.makeText(this, "${currentGame.getDisplayTitle()} is coming soon! Stay tuned for release.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!currentGame.isEnabled) {
            val reason = currentGame.statusText.ifBlank { "Game is currently disabled" }
            Toast.makeText(this, "${currentGame.getDisplayTitle()} is currently unavailable ($reason)", Toast.LENGTH_SHORT).show()
            return
        }

        if (isInstallingState.value || isCopyingObbState.value || isLaunchingState.value) return

        if (!PermissionsManager.hasStoragePermission(this)) {
            Toast.makeText(this, "Storage access is required to run the game", Toast.LENGTH_SHORT).show()
            PermissionsManager.requestNextPermission(this)
            return
        }

        if (!appInfo.isInstalled) {
            gameNotInstalledDialogState.value = GameNotInstalledDialogState(
                isVisible = true,
                gameTitle = currentGame.getDisplayTitle(),
                packageName = currentGame.packageName,
                iconType = currentGame.iconType,
                isBgmi = currentGame.packageName == "com.pubg.imobile"
            )
            return
        }

        if (isSessionExpired()) {
            Toast.makeText(this, "License expired. Please renew your key.", Toast.LENGTH_LONG).show()
            return
        }

        // If not cloned into sandbox yet, transition to INSTALL flow
        if (!isClonedInContainerState.value || BlackBoxCore.get()?.isInstalled(currentGame.packageName, USER_ID) != true) {
            handleInstallFlow()
            return
        }

        // If OBB missing, copy it before launch
        if (!hasObbState.value) {
            copyObbFiles(launchAfterCopy = true)
            return
        }

        // Ensure GMS for international builds (safe no-op if already installed)
        Thread {
            ensureGmsForGame(currentGame.packageName)
        }.start()

        // Prepare active library before launching (strict version isolation, no cross-game)
        val libReady = prepareActiveLibForGame(currentGame, currentVersion, appInfo)
        if (!libReady) {
            val assigned = currentVersion?.getAssignedLibFileName(
                if (currentGame.packageName == "com.pubg.imobile") "libbgmi" else "libpubgm"
            ) ?: if (currentGame.packageName == "com.pubg.imobile") "libbgmi.so" else "libpubgm.so"
            Log.w("MAct", "Launch notice: Assigned native lib ($assigned) not yet downloaded in loader storage")
            // Still allow launch - some configs run without injected payload - but warn user
            Toast.makeText(
                this,
                "Native library for ${currentGame.getDisplayTitle()} is missing. Game may be unstable.",
                Toast.LENGTH_LONG
            ).show()
        }

        isLaunchingState.value = true
        launchGame()
    }

    /**
     * Clears login session:
     * 1. Closes virtual game package if running.
     * 2. Deletes /storage/emulated/0/SdCard/0/Android/data/<pkg>/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames/loginInfoFile.json
     * 3. Clears all files and folders inside /data/user/0/com.ryzen/SdCard/data/user/0/<pkg>/
     */
    private fun handleClearLoginForGame(game: ManagedGame) {
        val pkg = game.packageName
        try {
            BlackBoxCore.get()?.stopPackage(pkg, USER_ID)
            Log.i("MAct", "Closed package $pkg prior to clearing login credentials")
        } catch (t: Throwable) {
            Log.w("MAct", "Could not stop $pkg: ${t.message}")
        }

        var deletedLoginFiles = 0

        // 1. Delete loginInfoFile.json in virtual SD card storage
        val candidateLoginFiles = listOf(
            File("/storage/emulated/0/SdCard/0/Android/data/$pkg/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames/loginInfoFile.json"),
            File("/storage/emulated/0/SdCard/0/Android/data/$pkg/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/savegames/loginInfoFile.json"),
            File("/storage/emulated/0/SdCard/Android/data/$pkg/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames/loginInfoFile.json"),
            File(Environment.getExternalStorageDirectory(), "SdCard/0/Android/data/$pkg/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames/loginInfoFile.json")
        )
        for (f in candidateLoginFiles) {
            if (f.exists() && f.delete()) {
                deletedLoginFiles++
                Log.i("MAct", "Deleted loginInfoFile: ${f.absolutePath}")
            }
        }

        // Clean any other login json files in SaveGames directory
        val candidateSaveDirs = listOf(
            File("/storage/emulated/0/SdCard/0/Android/data/$pkg/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames"),
            File("/storage/emulated/0/SdCard/0/Android/data/$pkg/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/savegames")
        )
        for (dir in candidateSaveDirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { _, name -> name.contains("login", ignoreCase = true) }?.forEach {
                    if (it.delete()) deletedLoginFiles++
                }
            }
        }

        // 2. Clear all folders and files inside phone root app data for package:
        // /data/user/0/com.ryzen/SdCard/data/user/0/<pkg>/
        val candidateRootDirs = listOf(
            File("/data/user/0/com.ryzen/SdCard/data/user/0/$pkg"),
            File("/data/data/com.ryzen/SdCard/data/user/0/$pkg"),
            File(dataDir, "SdCard/data/user/0/$pkg"),
            File(filesDir.parentFile, "SdCard/data/user/0/$pkg")
        )
        for (rootDir in candidateRootDirs) {
            if (rootDir.exists() && rootDir.isDirectory) {
                rootDir.listFiles()?.forEach { child ->
                    deleteDirectoryRecursively(child)
                }
                Log.i("MAct", "Cleared root user data directory: ${rootDir.absolutePath}")
            }
        }

        Toast.makeText(this, "${game.getDisplayTitle()} login & guest session cleared.", Toast.LENGTH_SHORT).show()
    }

    /**
     * Clears full game sandbox data:
     * 1. Closes virtual game package if running.
     * 2. Clears login and root directories.
     * 3. Wipes /storage/emulated/0/SdCard/0/Android/data/<pkg>/ entirely.
     * (Preserves OBB in /storage/emulated/0/SdCard/0/Android/obb/<pkg>/)
     */
    private fun handleClearGameDataForGame(game: ManagedGame) {
        val pkg = game.packageName
        try {
            BlackBoxCore.get()?.stopPackage(pkg, USER_ID)
            Log.i("MAct", "Closed package $pkg prior to clearing full sandbox data")
        } catch (t: Throwable) {
            Log.w("MAct", "Could not stop $pkg: ${t.message}")
        }

        // 1. Clear login & root data first
        handleClearLoginForGame(game)

        // 2. Wipe /storage/emulated/0/SdCard/0/Android/data/<pkg>/
        val candidateDataDirs = listOf(
            File("/storage/emulated/0/SdCard/0/Android/data/$pkg"),
            File("/storage/emulated/0/SdCard/Android/data/$pkg"),
            File(Environment.getExternalStorageDirectory(), "SdCard/0/Android/data/$pkg")
        )
        for (dDir in candidateDataDirs) {
            if (dDir.exists() && dDir.isDirectory) {
                dDir.listFiles()?.forEach { child ->
                    deleteDirectoryRecursively(child)
                }
                Log.i("MAct", "Cleared sandbox data directory: ${dDir.absolutePath}")
            }
        }

        hasObbState.value = hasDestinationObb(game, selectedVersionState.value)
        Toast.makeText(this, "${game.getDisplayTitle()} sandbox data wiped. Fresh download ready.", Toast.LENGTH_SHORT).show()
    }

    private fun handleContactAdmin() {
        try {
            val telegramUrl = "https://t.me/libAkAudioVisiual"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
            startActivity(intent)
        } catch (t: Throwable) {
            Toast.makeText(this, "Could not open Telegram: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteDirectoryRecursively(file: File): Boolean {
        if (!file.exists()) return true
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteDirectoryRecursively(child)
                }
            }
        }
        return file.delete()
    }

    private fun parseExpiryDate(raw: String): Date? {
        if (raw.isBlank() || raw.contains("LIFETIME", ignoreCase = true)) return null
        val cleaned = raw.replace("T", " ")
            .let { if (it.contains(".")) it.substringBefore(".") else it }
            .let { if (it.contains("+")) it.substringBefore("+") else it }
            .let { if (it.endsWith("Z", ignoreCase = true)) it.dropLast(1) else it }
            .trim()

        val formats = arrayOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val d = sdf.parse(cleaned)
                if (d != null) return d
            } catch (ignored: Exception) {}
        }
        return null
    }

    private fun isSessionExpired(): Boolean {
        val prefs = Prefs(this)
        if (prefs.getBool("is_lifetime", false)) return false
        val savedTimestamp = prefs.getLong("session_expiry_timestamp", 0L)
        if (savedTimestamp > 0L) {
            return System.currentTimeMillis() >= savedTimestamp
        }
        val expiryStr = prefs.getSt("expiry_date", "")
        if (expiryStr.isBlank() || expiryStr.contains("LIFETIME", ignoreCase = true)) return false
        val exp = parseExpiryDate(expiryStr) ?: return false
        return System.currentTimeMillis() >= exp.time
    }

    private fun doCountTimerAccount() {
        expiryRunnable = object : Runnable {
            override fun run() {
                try {
                    mainHandler.postDelayed(this, 1000)
                    val prefs = Prefs(this@MAct)
                    val isLifetime = prefs.getBool("is_lifetime", false)
                    var expiryStr = prefs.getSt("expiry_date", "")
                    if (expiryStr.isEmpty()) {
                        expiryStr = exdate()
                    }

                    if (isLifetime || expiryStr.contains("LIFETIME", ignoreCase = true)) {
                        isLifetimeState.value = true
                        daysState.value = "99"
                        hoursState.value = "99"
                        minsState.value = "99"
                        secsState.value = "99"
                        return
                    }

                    val now = System.currentTimeMillis()
                    val savedTimestamp = prefs.getLong("session_expiry_timestamp", 0L)
                    val distance = if (savedTimestamp > 0L) {
                        savedTimestamp - now
                    } else {
                        val expiryDate = parseExpiryDate(expiryStr)
                        if (expiryDate == null) {
                            return
                        }
                        expiryDate.time - now
                    }

                    val d = distance / (24 * 60 * 60 * 1000)
                    val h = (distance / (60 * 60 * 1000)) % 24
                    val m = (distance / (60 * 1000)) % 60
                    val s = (distance / 1000) % 60

                    if (distance <= 0) {
                        daysState.value = "00"
                        hoursState.value = "00"
                        minsState.value = "00"
                        secsState.value = "00"
                    } else {
                        daysState.value = String.format(Locale.US, "%02d", Math.max(0, d))
                        hoursState.value = String.format(Locale.US, "%02d", Math.max(0, h))
                        minsState.value = String.format(Locale.US, "%02d", Math.max(0, m))
                        secsState.value = String.format(Locale.US, "%02d", Math.max(0, s))
                    }
                } catch (ignored: Exception) {}
            }
        }
        mainHandler.post(expiryRunnable as Runnable)
    }

    private fun copyObbInternal(sourceFiles: List<File>, destinationDir: File, expectedObbName: String) {
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            throw IOException("Sandbox OBB folder could not be created")
        }

        val totalBytes = sourceFiles.sumOf { it.length() }
        if (totalBytes <= 0) throw IOException("Source OBB files are empty")
        var totalCopied = 0L
        val buffer = ByteArray(1024 * 128)

        for (source in sourceFiles) {
            val destFileName = if (sourceFiles.size == 1 && expectedObbName.isNotBlank()) expectedObbName else source.name
            val destination = File(destinationDir, destFileName)

            if (destination.exists() && destination.length() == source.length()) {
                totalCopied += source.length()
                continue
            }

            val temporary = File(destinationDir, "$destFileName.part")
            if (temporary.exists()) temporary.delete()

            FileInputStream(source).use { input ->
                FileOutputStream(temporary).use { output ->
                    var length: Int
                    while (input.read(buffer).also { length = it } != -1) {
                        output.write(buffer, 0, length)
                        totalCopied += length
                        val progress = Math.min(99f, (totalCopied * 100f) / totalBytes)
                        runOnUiThread {
                            obbProgressState.floatValue = progress / 100f
                            progressMessageState.value = "Copying $destFileName: ${progress.toInt()}%"
                        }
                    }
                    output.flush()
                }
            }

            if (!temporary.isFile || temporary.length() != source.length()) {
                if (temporary.exists()) temporary.delete()
                throw IOException("Transfer verification failed for $destFileName")
            }

            if (destination.exists()) destination.delete()
            if (!temporary.renameTo(destination)) {
                if (temporary.exists()) temporary.delete()
                throw IOException("Failed finalizing $destFileName")
            }
        }
    }

    private fun copyObbFiles(launchAfterCopy: Boolean) {
        val currentGame = selectedGameState.value
        val currentVersion = selectedVersionState.value
        if (isCopyingObbState.value) return
        isCopyingObbState.value = true
        progressMessageState.value = "Preparing OBB transfer..."
        obbProgressState.floatValue = 0f

        val expectedObbName = getExpectedObbName(currentGame, currentVersion)

        Thread {
            try {
                val sourceFiles = getSourceObbFiles(currentGame.packageName, expectedObbName)
                if (sourceFiles.isEmpty()) {
                    throw IOException("Source OBB file not found in Android/obb/${currentGame.packageName}")
                }

                val destinationDir = getObbDestinationDir(currentGame.packageName)
                copyObbInternal(sourceFiles, destinationDir, expectedObbName)

                runOnUiThread {
                    hasObbState.value = true
                    obbProgressState.floatValue = 1.0f
                    progressMessageState.value = "OBB ready"
                    isCopyingObbState.value = false
                    if (launchAfterCopy) {
                        launchGame()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    isCopyingObbState.value = false
                    Toast.makeText(this, "OBB copy failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /** Launch selected game and smoothly minimize loader to avoid activity focus clashing */
    private fun launchGame() {
        val currentGame = selectedGameState.value
        try {
            val core = BlackBoxCore.get()
            if (core == null) {
                isLaunchingState.value = false
                Toast.makeText(this, "Virtual engine is not ready. Please retry.", Toast.LENGTH_LONG).show()
                return
            }
            if (!core.isInstalled(currentGame.packageName, USER_ID)) {
                isLaunchingState.value = false
                Toast.makeText(this, "${currentGame.title} is not installed in the virtual container.", Toast.LENGTH_LONG).show()
                return
            }

            // Stop any previous zombie instance of this package before relaunch
            // (common after PUBG Global crash-loop leaves a half-dead process)
            try {
                core.stopPackage(currentGame.packageName, USER_ID)
                Thread.sleep(250)
            } catch (_: Throwable) {
            }

            val launched = core.launchApk(currentGame.packageName, USER_ID)
            if (!launched) {
                isLaunchingState.value = false
                Toast.makeText(this, "Game launch was rejected. Please retry INSTALL then LAUNCH.", Toast.LENGTH_LONG).show()
                return
            }
            Toast.makeText(this, "Launching ${currentGame.title}...", Toast.LENGTH_SHORT).show()
            // Keep launching flag a bit longer so double-taps don't re-enter while game boots
            mainHandler.postDelayed({
                isLaunchingState.value = false
            }, 2500)
        } catch (t: Throwable) {
            isLaunchingState.value = false
            Log.e("MAct", "Game launch failed", t)
            Toast.makeText(this, "Launch failed safely. Please retry.", Toast.LENGTH_LONG).show()
        }
    }

    private fun openPlayStore(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(webIntent)
            } catch (t: Throwable) {
                Toast.makeText(this, "Unable to open Play Store", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshSelectedGameAndVersion()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        refreshSelectedGameAndVersion()
    }

    override fun onDestroy() {
        expiryRunnable?.let { mainHandler.removeCallbacks(it) }
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
