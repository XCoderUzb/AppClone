package uz.appclone

import android.animation.Animator
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import kotlin.concurrent.thread

/** Bildirishnoma bosilganda: avval reklama, keyin fayl va [Saqlash] tugmasi. */
class PreviewActivity : Activity() {

    private lateinit var item: NewItem
    private var pulseAnim: Animator? = null
    @Volatile private var alive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        val name = intent.getStringExtra("name")
        val mime = intent.getStringExtra("mime")
        val uriStr = intent.getStringExtra("uri")
        if (name == null || mime == null || uriStr == null) {
            finish()
            return
        }
        item = NewItem(intent.getLongExtra("id", 0L), name, mime, intent.getLongExtra("size", 0L), Uri.parse(uriStr))

        // Ruxsatsiz ishlamaydi
        if (!Perms.hasAllFiles(this)) {
            finish()
            return
        }

        val root = findViewById<View>(R.id.previewRoot)
        root.visibility = View.INVISIBLE
        Ads.showInterstitial(this) {
            if (!isFinishing && alive) {
                root.visibility = View.VISIBLE
                showContent()
            }
        }
    }

    private fun showContent() {
        findViewById<TextView>(R.id.tvKind).text = "Yangi ${Cat.noun(item.mime)}"
        findViewById<TextView>(R.id.tvName).text = item.name
        findViewById<TextView>(R.id.tvStatus).text = formatSize(item.size)

        val img = findViewById<ImageView>(R.id.ivImage)
        val vid = findViewById<VideoView>(R.id.vvVideo)
        val box = findViewById<View>(R.id.fileBox)
        try {
            when {
                item.mime.startsWith("image/") -> {
                    img.visibility = View.VISIBLE
                    img.setImageURI(item.uri)
                }
                item.mime.startsWith("video/") -> {
                    vid.visibility = View.VISIBLE
                    vid.setMediaController(MediaController(this))
                    vid.setVideoURI(item.uri)
                    vid.seekTo(100)
                }
                else -> box.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            img.visibility = View.GONE
            vid.visibility = View.GONE
            box.visibility = View.VISIBLE
        }

        val btn = findViewById<Button>(R.id.btnSave)
        findViewById<View>(R.id.previewRoot).slideUpIn(0)

        if (Prefs.isSaved(this, item.id)) {
            btn.text = "Saqlangan ✓"
            btn.isEnabled = false
            btn.alpha = 0.6f
            return
        }
        pulseAnim = btn.pulse()
        btn.setOnClickListener { save(btn) }
    }

    private fun save(btn: Button) {
        pulseAnim?.cancel()
        btn.scaleX = 1f
        btn.scaleY = 1f
        btn.isEnabled = false
        btn.text = "Saqlanmoqda..."
        findViewById<ProgressBar>(R.id.progress).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvStatus).text = "Bazaga yuklanmoqda"

        Notifier.cancel(this, item.id)
        Uploader.enqueue(this, item, notify = false)

        thread {
            while (alive) {
                if (Prefs.isSaved(applicationContext, item.id)) {
                    runOnUiThread { finishSave(btn, true) }
                    return@thread
                }
                if (Prefs.isFailed(applicationContext, item.id)) {
                    runOnUiThread { finishSave(btn, false) }
                    return@thread
                }
                Thread.sleep(1000)
            }
        }
    }

    private fun finishSave(btn: Button, ok: Boolean) {
        findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
        if (ok) {
            btn.text = "Saqlandi ✓"
            findViewById<TextView>(R.id.tvStatus).text = "Bazaga saqlandi"
            btn.animate().scaleX(1.08f).scaleY(1.08f).setDuration(160)
                .withEndAction { btn.animate().scaleX(1f).scaleY(1f).setDuration(160).start() }.start()
        } else {
            btn.text = "Qayta urinish"
            btn.isEnabled = true
            findViewById<TextView>(R.id.tvStatus).text = "Saqlab bo'lmadi. Internetni tekshiring."
            btn.setOnClickListener { save(btn) }
        }
    }

    override fun onDestroy() {
        alive = false
        pulseAnim?.cancel()
        super.onDestroy()
    }
}
