package uz.appclone

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class HomeActivity : Activity() {

    private val counters = mutableMapOf<String, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        Ads.init(this)
        Notifier.ensureChannel(this)
        MediaJobService.schedule(this)
        if (Build.VERSION.SDK_INT >= 33 && !Perms.canNotify(this)) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2)
        }

        findViewById<TextView>(R.id.tvVersion).text = "Versiya ${versionName()}"

        buildCategories()
        findViewById<Button>(R.id.btnPro).setOnClickListener { showProInfo() }
        findViewById<Button>(R.id.btnPro).pressEffect()

        // Faqat sinov (debug) APK'da: PRO yozuvini uzoq bosib Pro'ni yoqish/o'chirish
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) {
            findViewById<View>(R.id.proBadge).setOnLongClickListener { toggleProForTesting(); true }
            findViewById<View>(R.id.homeLogo).setOnLongClickListener { toggleProForTesting(); true }
        }

        // Kirish animatsiyalari
        findViewById<View>(R.id.header).slideUpIn(0)
        findViewById<View>(R.id.userCard).slideUpIn(100)
        findViewById<View>(R.id.tvCats).slideUpIn(180)
        findViewById<View>(R.id.row1).slideUpIn(240)
        findViewById<View>(R.id.row2).slideUpIn(340)
        findViewById<View>(R.id.proCard).slideUpIn(440)
    }

    override fun onResume() {
        super.onResume()
        if (!Perms.hasAllFiles(this)) {            // ruxsatsiz hech narsa ishlamaydi
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        refresh()
    }

    private fun refresh() {
        val pro = Prefs.isPro(this)
        findViewById<TextView>(R.id.tvUser).text =
            "${Prefs.userName(this)}\n${Prefs.userEmail(this)}"
        findViewById<TextView>(R.id.tvMode).text =
            if (pro) "Pro: yangi fayllar avtomatik saqlanadi"
            else "Yangi fayl chiqsa, bildirishnoma orqali o'zingiz saqlaysiz"
        findViewById<View>(R.id.proBadge).visibility = if (pro) View.VISIBLE else View.GONE
        findViewById<View>(R.id.proCard).visibility = if (pro) View.GONE else View.VISIBLE

        counters.forEach { (cat, tv) ->
            tv.text = if (cat == Cat.APPS) "Tez orada" else "${Prefs.count(this, cat)} ta saqlangan"
        }
    }

    private fun buildCategories() {
        val row1 = findViewById<LinearLayout>(R.id.row1)
        val row2 = findViewById<LinearLayout>(R.id.row2)
        val gap = (6 * resources.displayMetrics.density).toInt()
        val inflater = LayoutInflater.from(this)

        listOf(
            Cat.PHOTOS to row1, Cat.VIDEOS to row1,
            Cat.FILES to row2, Cat.APPS to row2
        ).forEach { (cat, row) ->
            val card = inflater.inflate(R.layout.item_category, row, false)
            (card.layoutParams as LinearLayout.LayoutParams).setMargins(gap, 0, gap, 0)
            card.findViewById<TextView>(R.id.catIcon).text = Cat.emoji(cat)
            card.findViewById<TextView>(R.id.catTitle).text = cat
            counters[cat] = card.findViewById(R.id.catCount)
            card.pressEffect()
            card.setOnClickListener {
                Toast.makeText(this, "$cat ro'yxati keyingi qadamda qo'shiladi", Toast.LENGTH_SHORT).show()
            }
            row.addView(card)
        }
    }

    private fun showProInfo() {
        AlertDialog.Builder(this)
            .setTitle("⭐ App Clone Pro")
            .setMessage("• Yangi rasm, video va fayllar avtomatik Bazaga saqlanadi\n• Reklama ko'rsatilmaydi\n\nTo'lov tizimi keyingi bosqichda ulanadi.")
            .setPositiveButton("Yaxshi", null)
            .show()
    }

    private fun toggleProForTesting() {
        val now = !Prefs.isPro(this)
        Prefs.setPro(this, now)
        Toast.makeText(this, if (now) "Pro yoqildi (sinov)" else "Pro o'chirildi (sinov)", Toast.LENGTH_SHORT).show()
        refresh()
    }

    private fun versionName(): String =
        try { packageManager.getPackageInfo(packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
}
