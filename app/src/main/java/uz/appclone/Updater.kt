package uz.appclone

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.concurrent.thread

/**
 * Ilova ichida yangilash. GitHub Releases'dagi latest.json ni o'qiydi.
 * Faqat REPO ommaviy (public) bo'lsa ishlaydi.
 */
object Updater {
    /** Masalan: "foydalanuvchi/AppClone". O'zingizning repo nomingizga almashtiring. */
    const val REPO = "OWNER/REPO"

    private const val CHECK_GAP_MS = 6 * 60 * 60 * 1000L

    data class Info(val code: Long, val name: String, val apkUrl: String, val sha256: String)

    fun currentCode(a: Activity): Long {
        val pi = a.packageManager.getPackageInfo(a.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
    }

    private fun fetch(): Info {
        val url = "https://github.com/$REPO/releases/latest/download/latest.json"
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 15000
        if (c.responseCode !in 200..299) throw Exception("HTTP ${c.responseCode}")
        val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        return Info(
            j.getLong("versionCode"),
            j.optString("versionName"),
            j.getString("apkUrl"),
            j.optString("sha256")
        )
    }

    /** manual = foydalanuvchi o'zi bosgan (natijani har doim ko'rsatadi). */
    fun check(activity: Activity, manual: Boolean) {
        if (REPO.startsWith("OWNER/")) {
            if (manual) toast(activity, "Yangilanish manzili hali sozlanmagan")
            return
        }
        if (!manual && System.currentTimeMillis() - Prefs.lastUpdateCheck(activity) < CHECK_GAP_MS) return

        thread {
            val info = try { fetch() } catch (e: Exception) { null }
            if (!manual) Prefs.setLastUpdateCheck(activity, System.currentTimeMillis())
            activity.runOnUiThread {
                if (activity.isFinishing) return@runOnUiThread
                when {
                    info == null -> if (manual) toast(activity, "Yangilanishni tekshirib bo'lmadi")
                    info.code > currentCode(activity) -> showDialog(activity, info)
                    manual -> toast(activity, "Sizda eng so'nggi versiya")
                }
            }
        }
    }

    private fun showDialog(activity: Activity, info: Info) {
        AlertDialog.Builder(activity)
            .setTitle("Yangi versiya bor")
            .setMessage("App Clone ${info.name} chiqdi. Fayllaringiz joyida qoladi, ilovani o'chirish shart emas.")
            .setPositiveButton("Yangilash") { _, _ -> download(activity, info) }
            .setNegativeButton("Keyinroq", null)
            .show()
    }

    private fun download(activity: Activity, info: Info) {
        if (!activity.packageManager.canRequestPackageInstalls()) {
            toast(activity, "Ilovaga o'rnatishga ruxsat bering, so'ng yana \"Yangilash\"ni bosing")
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
            )
            return
        }

        val text = TextView(activity).apply {
            this.text = "Yuklanmoqda... 0%"
            setPadding(48, 32, 48, 16)
        }
        val bar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            setPadding(48, 0, 48, 24)
        }
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(text)
            addView(bar)
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Yangilanmoqda")
            .setView(box)
            .setCancelable(false)
            .create()
        dialog.show()

        thread {
            try {
                val dir = activity.getExternalFilesDir(null) ?: activity.filesDir
                val file = File(dir, "update.apk")
                val c = URL(info.apkUrl).openConnection() as HttpURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 30000
                val total = c.contentLengthLong
                var done = 0L
                var lastPct = -1
                c.inputStream.use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                if (pct != lastPct) {
                                    lastPct = pct
                                    activity.runOnUiThread {
                                        bar.progress = pct
                                        text.text = "Yuklanmoqda... $pct%"
                                    }
                                }
                            }
                        }
                    }
                }
                if (info.sha256.isNotEmpty() && sha256(file) != info.sha256.lowercase()) {
                    file.delete()
                    throw Exception("Fayl butunligi tekshiruvidan o'tmadi")
                }
                activity.runOnUiThread {
                    dialog.dismiss()
                    install(activity, file)
                }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    dialog.dismiss()
                    toast(activity, "Yuklab bo'lmadi: ${e.message}")
                }
            }
        }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun install(activity: Activity, file: File) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        activity.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun toast(a: Activity, m: String) = Toast.makeText(a, m, Toast.LENGTH_LONG).show()
}
