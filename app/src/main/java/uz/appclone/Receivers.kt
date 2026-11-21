package uz.appclone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Bildirishnomadagi "Saqlash" tugmasi. */
class SaveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", 0L)
        val name = intent.getStringExtra("name") ?: return
        val mime = intent.getStringExtra("mime") ?: return
        val uri = Uri.parse(intent.getStringExtra("uri") ?: return)
        val size = intent.getLongExtra("size", 0L)

        Notifier.progress(context, id, name)
        Uploader.enqueue(context, NewItem(id, name, mime, size, uri), notify = true)
    }
}

/** Telefon qayta yoqilganda kuzatuvni tiklaydi. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.isSignedIn(context)) MediaJobService.schedule(context)
    }
}
