package com.ryzen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.ryzen.ui.screens.CrashScreen
import com.ryzen.ui.theme.AppTheme

class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val errorMessage = intent.getStringExtra("error_message")
        val stackTrace = intent.getStringExtra("stack_trace")

        setContent {
            AppTheme {
                CrashScreen(
                    errorMessage = errorMessage,
                    stackTrace = stackTrace,
                    onCopyClick = {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        if (cm != null && !stackTrace.isNullOrBlank()) {
                            cm.setPrimaryClip(ClipData.newPlainText("Crash Report", stackTrace))
                            Toast.makeText(this, "Crash details copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRestartClick = {
                        val intent = Intent(this, LogAct::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}
