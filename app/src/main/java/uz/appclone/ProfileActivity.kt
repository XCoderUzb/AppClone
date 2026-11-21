package uz.appclone

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlin.concurrent.thread

class ProfileActivity : Activity() {

    private var taps = 0
    private var lastTap = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val avatar = findViewById<ImageView>(R.id.ivAvatar)
        avatar.makeCircle()
        avatar.setOnClickListener { onAvatarTap(it) }

        findViewById<View>(R.id.statusCard).setOnClickListener { showStatus() }
        findViewById<View>(R.id.statusCard).pressEffect()

        findViewById<ProgressBar>(R.id.pbStorage).progressTintList =
            ColorStateList.valueOf(0xFFF47B20.toInt())

        findViewById<Button>(R.id.btnSignOut).setOnClickListener { confirmSignOut() }

        val ver = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
        findViewById<TextView>(R.id.tvVersionP).text = "Versiya $ver"
        findViewById<Button>(R.id.btnUpdate).apply {
            pressEffect()
            setOnClickListener { Updater.check(this@ProfileActivity, true) }
        }

        avatar.slideUpIn(0)
        findViewById<View>(R.id.tvName).slideUpIn(80)
        findViewById<View>(R.id.tvEmail).slideUpIn(120)
        findViewById<View>(R.id.statusCard).slideUpIn(200)
        findViewById<View>(R.id.storageCard).slideUpIn(300)
        findViewById<View>(R.id.btnSignOut).slideUpIn(400)
    }

    override fun onResume() {
        super.onResume()
        bind()
        refreshFromServer()
    }

    private fun bind() {
        findViewById<ImageView>(R.id.ivAvatar).setImageBitmap(Avatar.bitmapFor(this))
        findViewById<TextView>(R.id.tvName).text = Prefs.userName(this)
        findViewById<TextView>(R.id.tvEmail).text = Prefs.userEmail(this)

        val pro = Prefs.isPro(this)
        findViewById<TextView>(R.id.tvStar).text = if (pro) "⭐" else "☆"
        findViewById<TextView>(R.id.tvStatus).text = if (pro) "Pro foydalanuvchi" else "Oddiy foydalanuvchi"

        val used = Prefs.usedBytes(this)
        val limit = Limits.APP_BYTES
        val left = maxOf(0L, limit - used)
        val permille = (used * 1000 / limit).toInt().coerceIn(0, 1000)
        findViewById<ProgressBar>(R.id.pbStorage).progress = permille
        findViewById<TextView>(R.id.tvStorage).text = "${formatSize(used)} / ${formatSize(limit)}"
        findViewById<TextView>(R.id.tvStorageSub).text =
            "Band: ${permille / 10.0}%  •  Bo'sh: ${formatSize(left)}"
    }

    /** Baza hajmi va profil rasmini yangilaydi. */
    private fun refreshFromServer() {
        thread {
            try {
                val token = Auth.tokenBlocking(this) ?: return@thread
                val user = DriveApi.getUser(token)
                if (user.photo.isNotEmpty() && user.photo != Prefs.photoUrl(this)) {
                    Avatar.fetch(this, user.photo)
                    Prefs.setPhotoUrl(this, user.photo)
                }
                Prefs.setUsedBytes(this, DriveApi.appUsedBytes(token))
                runOnUiThread { if (!isFinishing) bind() }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing) {
                        findViewById<TextView>(R.id.tvStorageSub).append("\n(yangilab bo'lmadi)")
                    }
                }
            }
        }
    }

    private fun showStatus() {
        val pro = Prefs.isPro(this)
        AlertDialog.Builder(this)
            .setTitle(if (pro) "⭐ Pro foydalanuvchi" else "Oddiy foydalanuvchi")
            .setMessage(
                if (pro) "Yangi fayllar avtomatik Bazaga saqlanadi va reklama ko'rsatilmaydi."
                else "Yangi fayllarni bildirishnoma orqali o'zingiz saqlaysiz. Pro bilan hammasi avtomatik bo'ladi."
            )
            .setPositiveButton("Yaxshi", null)
            .show()
    }

    // ---- Yashirin admin kirishi: profil rasmini 4 marta bosish ----

    private fun onAvatarTap(v: View) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(90).start() }.start()

        val now = SystemClock.elapsedRealtime()
        taps = if (now - lastTap < 1500) taps + 1 else 1
        lastTap = now
        if (taps >= 4) {
            taps = 0
            openAdminGate()
        }
    }

    private fun openAdminGate() {
        // Admin akkaunt: parolsiz ochiladi
        if (Admin.isAdminAccount(this)) {
            startActivity(Intent(this, AdminActivity::class.java))
            return
        }
        // Boshqa akkaunt: faqat parol bilan (tiklash rejimi)

        val lock = Admin.lockedOutMs(this)
        if (lock > 0) {
            Toast.makeText(this, "Ko'p xato urinish. ${lock / 60000 + 1} daqiqadan keyin urinib ko'ring", Toast.LENGTH_LONG).show()
            return
        }
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Parol"
        }
        AlertDialog.Builder(this)
            .setTitle("Admin")
            .setView(input)
            .setPositiveButton("Kirish") { _, _ ->
                if (Admin.tryUnlock(this, input.text.toString())) {
                    startActivity(Intent(this, AdminActivity::class.java))
                } else {
                    Toast.makeText(this, "Noto'g'ri parol", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Bekor", null)
            .show()
    }

    private fun confirmSignOut() {
        AlertDialog.Builder(this)
            .setTitle("Akkauntdan chiqish")
            .setMessage("Chiqsangiz, yangi fayllar kuzatilmaydi. Bazadagi fayllaringiz joyida qoladi.")
            .setPositiveButton("Chiqish") { _, _ ->
                MediaJobService.cancel(this)
                Prefs.signOut(this)
                Avatar.delete(this)
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
            .setNegativeButton("Bekor", null)
            .show()
    }
}
