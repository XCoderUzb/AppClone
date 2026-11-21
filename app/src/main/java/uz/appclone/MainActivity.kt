package uz.appclone

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var btnGoogle: Button

    private val reqAuth = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allaqachon kirilgan bo'lsa, to'g'ridan-to'g'ri bosh ekranga
        if (Prefs.isSignedIn(this) && Perms.hasAllFiles(this)) {
            goHome()
            return
        }

        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        btnGoogle = findViewById(R.id.btnGoogle)

        val btnPerm = findViewById<Button>(R.id.btnPermission)
        btnPerm.setOnClickListener { requestAccess() }
        btnPerm.pressEffect()
        btnGoogle.pressEffect()
        btnGoogle.setOnClickListener {
            if (!Perms.hasAllFiles(this)) {
                Toast.makeText(this, "Avval ruxsat bering", Toast.LENGTH_SHORT).show()
            } else {
                signInWithGoogle()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::status.isInitialized) return
        if (Prefs.isSignedIn(this) && Perms.hasAllFiles(this)) {
            goHome()
            return
        }
        val ok = Perms.hasAllFiles(this)
        status.text = if (ok) {
            "Ruxsat berilgan. Endi Google akkaunt bilan kiring."
        } else {
            "Ilova ishlashi uchun barcha fayllarga ruxsat kerak."
        }
        btnGoogle.alpha = if (ok) 1f else 0.5f
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    // ---- Google bilan kirish + Baza ruxsati ----

    private fun signInWithGoogle() {
        status.text = "Google bilan bog'lanilmoqda..."
        Identity.getAuthorizationClient(this)
            .authorize(Auth.request())
            .addOnSuccessListener { result ->
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    startIntentSenderForResult(pending.intentSender, reqAuth, null, 0, 0, 0)
                } else {
                    onAuthorized(result.accessToken)
                }
            }
            .addOnFailureListener { status.text = "Kirishda xato: ${it.message}" }
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
                status.text = "Kirishda xato: ${e.message}"
            }
        } else {
            status.text = "Kirish bekor qilindi"
        }
    }

    private fun onAuthorized(token: String?) {
        if (token == null) {
            status.text = "Google token olinmadi"
            return
        }
        status.text = "Baza tekshirilmoqda..."
        thread {
            try {
                val user = DriveApi.getUser(token)
                val root = DriveApi.ensureFolder(token, "App Clone")
                Prefs.setFolderId(this, "root", root)
                Prefs.setSignedIn(this, user.name, user.email)
                Prefs.setAdminAutoOpen(this, Admin.isAdminEmail(user.email))
                if (user.photo.isNotEmpty()) {
                    Prefs.setPhotoUrl(this, user.photo)
                    Avatar.fetch(this, user.photo)
                }
                if (Prefs.lastScan(this) < 0) Prefs.setLastScan(this, System.currentTimeMillis() / 1000)
                runOnUiThread { goHome() }
            } catch (e: Exception) {
                runOnUiThread { status.text = e.message ?: "Noma'lum xato" }
            }
        }
    }

    // ---- Fayllarga ruxsat ----

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
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 1
            )
        }
    }
}
