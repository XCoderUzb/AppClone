package uz.appclone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.view.View
import android.view.ViewOutlineProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Profil rasmi: Google rasmi bo'lsa shu, bo'lmasa ismning bosh harfi. */
object Avatar {
    private fun file(c: Context) = File(c.filesDir, "avatar.png")

    /** Orqa oqimda chaqiring. */
    fun fetch(c: Context, url: String) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it) } ?: return
            FileOutputStream(file(c)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (_: Exception) {
        }
    }

    fun bitmapFor(c: Context): Bitmap {
        val f = file(c)
        if (f.exists()) BitmapFactory.decodeFile(f.path)?.let { return it }
        val src = Prefs.userName(c).ifBlank { Prefs.userEmail(c) }
        return initial(src.trim().take(1).uppercase().ifEmpty { "?" })
    }

    fun delete(c: Context) {
        file(c).delete()
    }

    private fun initial(letter: String): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(0xFFE85F12.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 130f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(letter, size / 2f, y, paint)
        return bmp
    }
}

/** View'ni doira shaklida kesadi. */
fun View.makeCircle() {
    clipToOutline = true
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setOval(0, 0, view.width, view.height)
        }
    }
}
