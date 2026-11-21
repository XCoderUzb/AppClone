package uz.appclone

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Kichik rasmlarni orqa fonda yuklaydi va eslab qoladi. */
object Thumbs {
    private val cache = LruCache<String, Bitmap>(120)
    private val pool = Executors.newFixedThreadPool(3)
    private val placeholders = HashMap<String, Bitmap>()

    fun placeholder(emoji: String): Bitmap = placeholders.getOrPut(emoji) {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(0xFFFFF0E5.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 64f
            textAlign = Paint.Align.CENTER
        }
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(emoji, size / 2f, y, paint)
        bmp
    }

    fun load(url: String, view: ImageView, activity: Activity) {
        view.tag = url
        cache.get(url)?.let {
            view.setImageBitmap(it)
            return
        }
        pool.execute {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it) } ?: return@execute
                cache.put(url, bmp)
                activity.runOnUiThread {
                    if (view.tag == url) view.setImageBitmap(bmp)
                }
            } catch (_: Exception) {
            }
        }
    }
}
