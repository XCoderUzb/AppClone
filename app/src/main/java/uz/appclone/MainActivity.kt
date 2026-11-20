package uz.appclone

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var btnGoogle: Button
    private var signedIn = false

    private val reqAuth = 1001
    private val driveScope = Scope("https://www.googleapis.com/auth/drive.file")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        btnGoogle = findViewById(R.id.btnGoogle)

        findViewById<Button>(R.id.btnPermission).setOnClickListener { requestAccess() }
        btnGoogle.setOnClickListener {
            if (!hasAllFilesAccess()) {
                Toast.makeText(this, "Avval ruxsat bering", Toast.LENGTH_SHORT).show()
            } else {
                signInWithGoogle()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (signedIn) return
        val ok = hasAllFilesAccess()
        status.text = if (ok) {
            "Ruxsat berilgan. Endi Google akkaunt bilan kiring."
        } else {
            "Ilova ishlashi uchun barcha fayllarga ruxsat kerak."
        }
        btnGoogle.alpha = if (ok) 1f else 0.5f
    }

    // ---- Google bilan kirish + Drive ruxsati ----

    private fun signInWithGoogle() {
        status.text = "Google bilan bog'lanilmoqda..."
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(driveScope))
            .build()

        Identity.getAuthorizationClient(this)
            .authorize(request)
            .addOnSuccessListener { result ->
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    // Foydalanuvchi akkaunt tanlaydi va Drive ruxsatini beradi
                    startIntentSenderForResult(pending.intentSender, reqAuth, null, 0, 0, 0)
                } else {
                    onAuthorized(result.accessToken)
                }
            }
            .addOnFailureListener { showError("Kirishda xato: ${it.message}") }
    }

    @Deprecated("Oddiy Activity uchun")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != reqAuth) return
        if (resultCode == RESULT_OK && data != null) {
            try {
                val result = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(data)
                onAuthorized(result.accessToken)
            } catch (e: ApiException) {
                showError("Kirishda xato: ${e.message}")
            }
        } else {
            showError("Kirish bekor qilindi")
        }
    }

    private fun onAuthorized(token: String?) {
        if (token == null) {
            showError("Google token olinmadi")
            return
        }
        status.text = "Drive tekshirilmoqda..."
        thread {
            try {
                val user = DriveApi.getUser(token)
                DriveApi.ensureFolder(token, "App Clone")
                runOnUiThread {
                    signedIn = true
                    status.text = "Salom, ${user.name}\n${user.email}\n\nGoogle Drive ulandi ✓"
                    btnGoogle.text = "Ulangan ✓"
                    btnGoogle.alpha = 1f
                }
            } catch (e: Exception) {
                runOnUiThread { showError(e.message ?: "Noma'lum xato") }
            }
        }
    }

    private fun showError(msg: String) {
        status.text = msg
    }

    // ---- Fayllarga ruxsat ----

    private fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun requestAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 1
            )
        }
    }
}
