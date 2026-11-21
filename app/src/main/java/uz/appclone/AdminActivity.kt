package uz.appclone

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView

/** Admin panel. Server ulanmaguncha faqat Pro sinovi ishlaydi. */
class AdminActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Parol kiritilmagan yoki admin bo'lmasa, ochilmaydi
        if (!Admin.isUnlocked() || !Admin.isAdminEmail(Prefs.userEmail(this))) {
            finish()
            return
        }
        setContentView(R.layout.activity_admin)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvAdmin).text = Prefs.userEmail(this)

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

        listOf(R.id.swCard, R.id.cardAds, R.id.cardNotify, R.id.cardUsers).forEachIndexed { i, id ->
            findViewById<View>(id).slideUpIn(80L * i)
        }
    }

    override fun onResume() {
        super.onResume()
        // Sessiya tugagan bo'lsa, qayta parol so'raladi
        if (!Admin.isUnlocked()) finish()
    }
}
