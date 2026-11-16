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

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var btnGoogle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        btnGoogle = findViewById(R.id.btnGoogle)

        findViewById<Button>(R.id.btnPermission).setOnClickListener { requestAccess() }
        btnGoogle.setOnClickListener {
            if (hasAllFilesAccess()) {
                Toast.makeText(this, "Google bilan kirish keyingi qadamda ulanadi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Avval ruxsat bering", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val ok = hasAllFilesAccess()
        status.text = if (ok) {
            "Ruxsat berilgan. Endi Google akkaunt bilan kiring."
        } else {
            "Ilova ishlashi uchun barcha fayllarga ruxsat kerak."
        }
        btnGoogle.alpha = if (ok) 1f else 0.5f
    }

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
