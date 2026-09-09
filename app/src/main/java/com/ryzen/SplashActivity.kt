package com.ryzen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.ryzen.ui.screens.AppUpdateDialogState
import com.ryzen.ui.screens.MaintenanceDialogState
import com.ryzen.ui.screens.SplashScreen
import com.ryzen.ui.theme.AppTheme
import com.ryzen.utils.ApkUpdateManager
import com.ryzen.utils.AppConfigManager
import com.ryzen.utils.SecurityCheckManager
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@Obfuscate
class SplashActivity : AppCompatActivity() {

    private val statusTextState = mutableStateOf("Initializing security core...")
    private val updateDialogState = mutableStateOf<AppUpdateDialogState?>(null)
    private val maintenanceDialogState = mutableStateOf<MaintenanceDialogState?>(null)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isMaintenanceBlocked = false
    private var isUpdateDialogShowing = false
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val localVersion = ApkUpdateManager.getLocalVersionName(this)

        setContent {
            AppTheme {
                SplashScreen(
                    versionName = localVersion,
                    statusText = statusTextState.value,
                    updateDialogState = updateDialogState.value,
                    maintenanceDialogState = maintenanceDialogState.value
                )
            }
        }

        runRealSystemChecks()

        // Fail-safe timeout: Ensure user is never permanently stuck on splash screen
        mainHandler.postDelayed({
            if (!isMaintenanceBlocked && !isUpdateDialogShowing) {
                navigateToLogin()
            }
        }, 2500)
    }

    private fun runRealSystemChecks() {
        updateStatus("Initializing security core...")

        SecurityCheckManager.runChecks(this, object : SecurityCheckManager.SecurityCallback {
            override fun onProgress(status: String) {
                updateStatus(status)
            }

            override fun onResult(isSecure: Boolean, warningMessage: String) {
                updateStatus("Connecting to backend...")
                checkBackendStatus()
            }
        })
    }

    private fun checkBackendStatus() {
        AppConfigManager.fetchConfig(this) { config ->
            if (isFinishing || isDestroyed) return@fetchConfig

            if (config.maintenanceMode) {
                isMaintenanceBlocked = true
                updateStatus("Server maintenance active")
                maintenanceDialogState.value = MaintenanceDialogState(
                    isVisible = true,
                    message = config.maintenanceMessage,
                    estimatedEnd = config.maintenanceEstimatedEnd,
                    onRefresh = {
                        maintenanceDialogState.value = null
                        isMaintenanceBlocked = false
                        updateStatus("Checking server status...")
                        checkBackendStatus()
                    },
                    onExit = {
                        finishAffinity()
                    }
                )
                return@fetchConfig
            }

            updateStatus("Checking for updates...")
            checkForApkUpdates()
        }
    }

    private fun checkForApkUpdates() {
        ApkUpdateManager.checkForUpdate(this) { updateInfo ->
            if (isFinishing || isDestroyed) return@checkForUpdate

            if (updateInfo.updateAvailable && updateInfo.downloadUrl.isNotEmpty()) {
                isUpdateDialogShowing = true
                updateStatus("Update available: v" + updateInfo.serverVersionName)

                updateDialogState.value = AppUpdateDialogState(
                    isVisible = true,
                    serverVersionName = updateInfo.serverVersionName,
                    changelog = updateInfo.changelog,
                    isMandatory = updateInfo.isMandatory,
                    isDownloading = false,
                    onUpdateClick = {
                        startInAppDownload(updateInfo)
                    },
                    onLaterClick = {
                        updateDialogState.value = null
                        isUpdateDialogShowing = false
                        proceedToLogin()
                    }
                )
            } else {
                updateStatus("Entering app...")
                proceedToLogin()
            }
        }
    }

    private fun startInAppDownload(updateInfo: ApkUpdateManager.AppUpdateInfo) {
        updateDialogState.value = updateDialogState.value?.copy(
            isDownloading = true,
            downloadProgress = 0,
            downloadStatusText = "Connecting..."
        )

        ApkUpdateManager.downloadApk(this, updateInfo.downloadUrl, object : ApkUpdateManager.DownloadProgressCallback {
            override fun onProgress(percent: Int, currentBytes: Long, totalBytes: Long) {
                if (isFinishing || isDestroyed) return
                val currentMb = currentBytes / (1024 * 1024)
                val totalMb = totalBytes / (1024 * 1024)
                runOnUiThread {
                    updateDialogState.value = updateDialogState.value?.copy(
                        downloadProgress = percent,
                        downloadStatusText = "Downloading APK: $percent% ($currentMb MB / $totalMb MB)"
                    )
                }
            }

            override fun onCompleted(apkFile: File) {
                if (isFinishing || isDestroyed) return
                runOnUiThread {
                    updateDialogState.value = updateDialogState.value?.copy(
                        downloadProgress = 100,
                        downloadStatusText = "Download complete. Installing..."
                    )
                    Toast.makeText(this@SplashActivity, "Update downloaded! Installing...", Toast.LENGTH_SHORT).show()
                    ApkUpdateManager.installApk(this@SplashActivity, apkFile)
                }
            }

            override fun onError(error: String) {
                if (isFinishing || isDestroyed) return
                runOnUiThread {
                    Toast.makeText(this@SplashActivity, "Download failed: $error", Toast.LENGTH_LONG).show()
                    if (!updateInfo.isMandatory) {
                        updateDialogState.value = null
                        isUpdateDialogShowing = false
                        proceedToLogin()
                    } else {
                        finishAffinity()
                    }
                }
            }
        })
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            statusTextState.value = status
        }
    }

    private fun proceedToLogin() {
        mainHandler.postDelayed({
            if (!isMaintenanceBlocked && !isUpdateDialogShowing) {
                navigateToLogin()
            }
        }, 400)
    }

    @Synchronized
    private fun navigateToLogin() {
        if (hasNavigated) return
        hasNavigated = true
        mainHandler.post {
            if (!isFinishing && !isDestroyed) {
                val intent = Intent(this@SplashActivity, LogAct::class.java)
                startActivity(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
                } else {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
                finish()
            }
        }
    }
}
