package uz.appclone

import android.content.Context
import android.net.Uri
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore

/** Telefonda yangi paydo bo'lgan fayllarni topadi. */
object Scanner {

    fun checkNew(ctx: Context) {
        if (!Prefs.isSignedIn(ctx) || !Perms.hasAllFiles(ctx)) return

        val nowSec = System.currentTimeMillis() / 1000
        val last = Prefs.lastScan(ctx)
        if (last < 0) {          // birinchi marta: eski fayllarni emas, faqat keyingilarni kuzatamiz
            Prefs.setLastScan(ctx, nowSec)
            return
        }

        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATA
        )
        var selection = "${MediaStore.Files.FileColumns.DATE_ADDED} > ? AND " +
            "${MediaStore.Files.FileColumns.SIZE} > 0 AND " +
            "${MediaStore.Files.FileColumns.MIME_TYPE} IS NOT NULL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection += " AND ${MediaStore.MediaColumns.IS_PENDING} = 0"
        }
        // Yarim yuklangan fayllar o'tib ketmasligi uchun 10 daqiqa orqaga qaraymiz
        val args = arrayOf((last - 600).toString())

        val found = mutableListOf<NewItem>()
        ctx.contentResolver.query(
            collection, projection, selection, args,
            "${MediaStore.Files.FileColumns.DATE_ADDED} ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val name = c.getString(1) ?: continue
                val mime = c.getString(2) ?: continue
                val size = c.getLong(3)
                val path = c.getString(4) ?: ""

                if (path.contains("/Android/") || path.contains("/.") || name.startsWith(".")) continue
                if (Prefs.wasNotified(ctx, id) || Prefs.isSaved(ctx, id)) continue

                Prefs.markNotified(ctx, id)
                found.add(NewItem(id, name, mime, size, ContentUris.withAppendedId(collection, id)))
            }
        }
        Prefs.setLastScan(ctx, nowSec)
        if (found.isEmpty()) return

        if (Prefs.isPro(ctx)) {
            // Pro: avtomatik saqlash, bildirishnomasiz
            found.forEach { Uploader.enqueue(ctx, it, notify = false) }
        } else if (Perms.canNotify(ctx)) {
            if (found.size <= 5) found.forEach { Notifier.promptSave(ctx, it) }
            else Notifier.summary(ctx, found.size)
        }
    }
}
