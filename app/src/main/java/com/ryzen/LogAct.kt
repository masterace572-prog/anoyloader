package com.ryzen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.ryzen.ui.screens.AnnouncementDialogState
import com.ryzen.ui.screens.LoadingDialogState
import com.ryzen.ui.components.PermissionsDialogState
import com.ryzen.ui.screens.LoginScreen
import com.ryzen.ui.screens.MaintenanceDialogState
import com.ryzen.ui.theme.AppTheme
import com.ryzen.utils.AppConfigManager
import com.ryzen.utils.Downtwo
import com.ryzen.utils.PermissionsManager
import com.ryzen.utils.Prefs
import com.ryzen.utils.SupabaseAuthManager
import org.lsposed.lsparanoid.Obfuscate
import java.net.NetworkInterface

@Obfuscate
class LogAct : AppCompatActivity() {

    companion object {
        init {
            try {
                System.loadLibrary("ryzen")
            } catch (ignored: Throwable) {}
        }

        private const val USER = "USER"
        private const val PREF_SAVE_KEY = "SAVE_KEY"
        private const val PREF_PERMISSIONS_COMPLETED = "permissions_completed"

        @JvmStatic
        private external fun Check(mContext: Context, userKey: String): String
    }

    private external fun GetKey(): String

    // Reactive Compose States
    private val keyTextState = mutableStateOf("")
    private val isKeyVisibleState = mutableStateOf(true)
    private val isSaveKeyEnabledState = mutableStateOf(true)
    private val isAuthenticatingState = mutableStateOf(false)
    private val keyErrorState = mutableStateOf<String?>(null)
    private val isSystemOnlineState = mutableStateOf(true)

    private val loadingDialogState = mutableStateOf<LoadingDialogState?>(null)
    private val maintenanceDialogState = mutableStateOf<MaintenanceDialogState?>(null)
    private val announcementDialogState = mutableStateOf<AnnouncementDialogState?>(null)
    private val permissionsDialogState = mutableStateOf(PermissionsDialogState())

    private lateinit var prefs: Prefs
    private var isMaintenanceActive = false
    private var maintenanceNotice = ""
    private var maintenanceEstimatedEnd = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = Prefs(this)

        // VPN Guard
        if (isVpnActive()) {
            Toast.makeText(this, "VPN Detected. Please disable VPN to continue.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize Key State (empty by default, do not pre-fill TEST-VIPER-2026)
        val savedKey = prefs.getSt(USER, "")
        val savePref = prefs.getBool(PREF_SAVE_KEY, true)
        isSaveKeyEnabledState.value = savePref

        if (savePref && savedKey.isNotBlank() && !savedKey.equals("TEST-VIPER-2026", ignoreCase = true)) {
            keyTextState.value = savedKey
        } else {
            keyTextState.value = ""
            if (savedKey.equals("TEST-VIPER-2026", ignoreCase = true)) {
                prefs.setSt(USER, "")
            }
        }

        setContent {
            AppTheme {
                LoginScreen(
                    keyText = keyTextState.value,
                    onKeyChange = {
                        keyTextState.value = it
                        if (keyErrorState.value != null) {
                            keyErrorState.value = null
                        }
                    },
                    isKeyVisible = isKeyVisibleState.value,
                    onToggleKeyVisibility = {
                        isKeyVisibleState.value = !isKeyVisibleState.value
                    },
                    onPasteClick = { handlePasteKey() },
                    isSaveKeyEnabled = isSaveKeyEnabledState.value,
                    onSaveKeyToggle = { enabled ->
                        isSaveKeyEnabledState.value = enabled
                        prefs.setBool(PREF_SAVE_KEY, enabled)
                        if (!enabled) {
                            prefs.setSt(USER, "")
                        }
                    },
                    isAuthenticating = isAuthenticatingState.value,
                    onAuthenticateClick = { handleAuthenticate() },
                    onGetKeyClick = { handleGetKey() },
                    keyError = keyErrorState.value,
                    isSystemOnline = isSystemOnlineState.value,
                    loadingDialogState = loadingDialogState.value,
                    maintenanceDialogState = maintenanceDialogState.value,
                    announcementDialogState = announcementDialogState.value,
                    permissionsDialogState = permissionsDialogState.value,
                    onGrantAllFiles = {
                        PermissionsManager.requestAllFilesAccess(this)
                    },
                    onGrantRuntimeStorage = {
                        PermissionsManager.requestRuntimeStorage(this)
                    },
                    onGrantNotification = {
                        PermissionsManager.requestNotification(this)
                    },
                    onGrantInstall = {
                        PermissionsManager.requestInstallUnknown(this)
                    },
                    onRestartApp = {
                        prefs.setBool(PREF_PERMISSIONS_COMPLETED, true)
                        PermissionsManager.restartApp(this)
                    }
                )
            }
        }

        checkPermissionsFlow()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsFlow()
    }

    private fun updatePermissionsState(): Boolean {
        val hasAllFiles = PermissionsManager.hasAllFilesAccess(this)
        val hasRuntime = PermissionsManager.hasRuntimeStoragePermissions(this)
        val hasNotification = PermissionsManager.hasNotificationPermission(this)
        val hasInstall = PermissionsManager.hasInstallPermission(this)
        val allGranted = hasAllFiles && hasRuntime && hasNotification && hasInstall

        val isCompleted = prefs.getBool(PREF_PERMISSIONS_COMPLETED, false)

        permissionsDialogState.value = PermissionsDialogState(
            isVisible = !allGranted || !isCompleted,
            hasAllFiles = hasAllFiles,
            hasRuntimeStorage = hasRuntime,
            hasNotification = hasNotification,
            hasInstall = hasInstall
        )
        return allGranted && isCompleted
    }

    private fun checkPermissionsFlow() {
        val ready = updatePermissionsState()
        if (!ready) return

        checkServerConfig()
    }

    private fun checkServerConfig() {
        AppConfigManager.fetchConfig(this) { config ->
            if (isFinishing || isDestroyed) return@fetchConfig

            isMaintenanceActive = config.maintenanceMode
            maintenanceNotice = config.maintenanceMessage
            maintenanceEstimatedEnd = config.maintenanceEstimatedEnd
            isSystemOnlineState.value = !config.maintenanceMode

            if (config.maintenanceMode) {
                maintenanceDialogState.value = MaintenanceDialogState(
                    isVisible = true,
                    message = config.maintenanceMessage,
                    estimatedEnd = config.maintenanceEstimatedEnd,
                    onRefresh = {
                        maintenanceDialogState.value = null
                        checkServerConfig()
                    },
                    onExit = {
                        finishAffinity()
                    }
                )
            } else {
                maintenanceDialogState.value = null

                if (config.announcementActive && config.announcementTitle.trim().isNotEmpty()) {
                    val announcementKey = "announcement_seen_" + config.announcementTitle.hashCode()
                    if (!prefs.getBool(announcementKey, false)) {
                        announcementDialogState.value = AnnouncementDialogState(
                            isVisible = true,
                            title = config.announcementTitle,
                            message = config.announcementMessage,
                            link = config.announcementLink?.takeIf { it.isNotBlank() },
                            onContinue = {
                                prefs.setBool(announcementKey, true)
                                announcementDialogState.value = null
                            },
                            onOpenLink = {
                                prefs.setBool(announcementKey, true)
                                announcementDialogState.value = null
                                try {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(config.announcementLink.trim())))
                                } catch (ignored: Throwable) {}
                            }
                        )
                    }
                }
            }
        }
    }

    private fun handlePasteKey() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pasted = clip.getItemAt(0).coerceToText(this)?.toString()?.trim() ?: ""
                if (pasted.length > 5) {
                    keyTextState.value = pasted
                    keyErrorState.value = null
                    Toast.makeText(this, "Key pasted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Clipboard does not contain a valid key", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleGetKey() {
        try {
            val keyUrl = try { GetKey() } catch (_: Throwable) { null }
            if (!keyUrl.isNullOrBlank() && keyUrl.startsWith("http")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(keyUrl))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Key portal URL is not configured", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Throwable) {
            Toast.makeText(this, "Unable to retrieve key portal URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAuthenticate() {
        if (!PermissionsManager.isAllPermissionsGranted(this)) {
            PermissionsManager.requestNextPermission(this)
            return
        }

        if (isMaintenanceActive) {
            checkServerConfig()
            return
        }

        val userKey = keyTextState.value.trim()
        if (userKey.isEmpty()) {
            keyErrorState.value = "Please enter your license key"
            return
        }

        if (isVpnActive()) {
            Toast.makeText(this, "Please disable VPN to continue", Toast.LENGTH_SHORT).show()
            return
        }

        if (isSaveKeyEnabledState.value) {
            prefs.setSt(USER, userKey)
            prefs.setBool(PREF_SAVE_KEY, true)
        } else {
            prefs.setSt(USER, "")
            prefs.setBool(PREF_SAVE_KEY, false)
        }

        isAuthenticatingState.value = true
        loadingDialogState.value = LoadingDialogState(
            isVisible = true,
            title = "Authenticating",
            message = "Verifying license credentials with server...",
            progress = null,
            isError = false
        )

        SupabaseAuthManager.authenticate(this, userKey, object : SupabaseAuthManager.AuthCallback {
            override fun onSuccess(result: SupabaseAuthManager.AuthResult) {
                runOnUiThread {
                    prefs.setSt("user_key", userKey)
                    prefs.setSt("expiry_date", result.expiresAt)
                    prefs.setSt("remaining_seconds", result.remainingSeconds.toString())
                    prefs.setLong("session_expiry_timestamp", System.currentTimeMillis() + (result.remainingSeconds * 1000L))
                    prefs.setBool("is_lifetime", result.isLifetime)

                    startDownloadLibUpdates()
                }
            }

            override fun onFailure(errorMessage: String) {
                runOnUiThread {
                    isAuthenticatingState.value = false
                    loadingDialogState.value = LoadingDialogState(
                        isVisible = true,
                        title = "Authentication Failed",
                        message = errorMessage.ifBlank { "Invalid or expired license key" },
                        progress = null,
                        isError = true,
                        onDismiss = {
                            loadingDialogState.value = null
                        }
                    )
                }
            }
        })
    }

    private fun startDownloadLibUpdates() {
        loadingDialogState.value = LoadingDialogState(
            isVisible = true,
            title = "Downloading Core Libraries",
            message = "Fetching runtime updates...",
            progress = 0,
            isError = false
        )

        val task = Downtwo(this) { success, message ->
            runOnUiThread {
                isAuthenticatingState.value = false
                if (success) {
                    loadingDialogState.value = null
                    val intent = Intent(applicationContext, MAct::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    loadingDialogState.value = LoadingDialogState(
                        isVisible = true,
                        title = "Download Failed",
                        message = message ?: "Unable to download runtime update files. Check internet connection.",
                        progress = null,
                        isError = true,
                        onDismiss = {
                            loadingDialogState.value = null
                        },
                        onRetry = {
                            startDownloadLibUpdates()
                        }
                    )
                }
            }
        }

        task.setProgressListener { progress ->
            runOnUiThread {
                loadingDialogState.value = LoadingDialogState(
                    isVisible = true,
                    title = "Downloading Core Libraries",
                    message = "Updating runtime dependencies ($progress%)...",
                    progress = progress,
                    isError = false
                )
            }
        }

        task.startDownload()
    }

    private fun isVpnActive(): Boolean {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
                return caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            } else {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces != null && interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    val name = intf.name ?: continue
                    if (name.startsWith("tun") || name.startsWith("ppp") || name.startsWith("tap")) {
                        return true
                    }
                }
            }
        } catch (ignored: Throwable) {}
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermissionsFlow()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        checkPermissionsFlow()
    }
}
