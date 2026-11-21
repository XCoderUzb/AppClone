package uz.appclone

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView

/**
 * Admin panel.
 * - Admin akkaunt bilan kirilsa: parolsiz, to'liq panel.
 * - Boshqa akkaunt parol bilan kirsa: "tiklash rejimi" (faqat admin akkauntlar ro'yxati).
 */
class AdminActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Admin.canOpenPanel(this)) {
            finish()
            return
        }
        setContentView(R.layout.activity_admin)

        val isAdmin = Admin.isAdminAccount(this)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvAdmin).text =
            if (isAdmin) Prefs.userEmail(this) else "Tiklash rejimi (parol bilan kirildi)"
        findViewById<TextView>(R.id.tvAdminList).text =
            Admin.listAdmins().joinToString("\n") { "• $it" }

        findViewById<View>(R.id.cardAdmins).apply {
            pressEffect()
            setOnClickListener {
                AlertDialog.Builder(this@AdminActivity)
                    .setTitle("Admin akkauntni olib tashlash")
                    .setMessage("Bu amal admin ro'yxati serverga ko'chirilgandan keyin ishlaydi.")
                    .setPositiveButton("Tushunarli", null)
                    .show()
            }
        }

        val adminOnly = listOf(R.id.swCard, R.id.cardAds, R.id.cardNotify, R.id.cardUsers)
        if (!isAdmin) {
            adminOnly.forEach { findViewById<View>(it).visibility = View.GONE }
        } else {
            val sw = findViewById<Switch>(R.id.swPro)
            sw.isChecked = Prefs.isPro(this)
            sw.setOnCheckedChangeListener { _, checked -> Prefs.setPro(this, checked) }

            mapOf(
                R.id.cardAds to "Reklama joylash",
                R.id.cardNotify to "Bildirishnoma yuborish",
                R.id.cardUsers to "Foydalanuvchilar"
            ).forEach { (id, title) ->
                findViewById<View>(id).apply {
                    pressEffect()
                    setOnClickListener {
                        AlertDialog.Builder(this@AdminActivity)
                            .setTitle(title)
                            .setMessage("Bu funksiya server ulangandan keyin ishlaydi.")
                            .setPositiveButton("Tushunarli", null)
                            .show()
                    }
                }
            }
        }

        val shown = listOf(R.id.cardAdmins) + if (isAdmin) adminOnly else emptyList()
        shown.forEachIndexed { i, id -> findViewById<View>(id).slideUpIn(80L * i) }
    }

    override fun onResume() {
        super.onResume()
        // Admin akkaunt bo'lmasa va parol sessiyasi tugagan bo'lsa, yopiladi
        if (!Admin.canOpenPanel(this)) finish()
    }
}
