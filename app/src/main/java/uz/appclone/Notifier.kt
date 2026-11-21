package uz.appclone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon

object Notifier {
    private const val CHANNEL = "new_files"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL, "Yangi fayllar", NotificationManager.IMPORTANCE_HIGH)
        ch.description = "Yangi rasm, video va fayllarni saqlash taklifi"
        nm.createNotificationChannel(ch)
    }

    private fun nid(id: Long) = (id % Int.MAX_VALUE).toInt()

    private fun putItem(i: Intent, item: NewItem): Intent =
        i.putExtra("id", item.id).putExtra("uri", item.uri.toString())
            .putExtra("name", item.name).putExtra("mime", item.mime).putExtra("size", item.size)

    /** "Ushbu rasmni saqlab qo'yishni xohlaysizmi?" + [Saqlash] */
    fun promptSave(ctx: Context, item: NewItem) {
        ensureChannel(ctx)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val open = PendingIntent.getActivity(
            ctx, nid(item.id),
            putItem(Intent(ctx, PreviewActivity::class.java), item)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags
        )
        val save = PendingIntent.getBroadcast(
            ctx, nid(item.id),
            putItem(Intent(ctx, SaveReceiver::class.java), item),
            flags
        )
        val action = Notification.Action.Builder(
            Icon.createWithResource(ctx, R.drawable.ic_stat), "Saqlash", save
        ).build()

        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFFF47B20.toInt())
            .setContentTitle("Yangi ${Cat.noun(item.mime)}: ${item.name}")
            .setContentText("Ushbu ${Cat.noun(item.mime)}ni saqlab qo'yishni xohlaysizmi?")
            .setContentIntent(open)
            .addAction(action)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(nid(item.id), n)
    }

    /** Ko'p fayl bir vaqtda paydo bo'lsa, bitta umumiy bildirishnoma. */
    fun summary(ctx: Context, count: Int) {
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 7, Intent(ctx, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFFF47B20.toInt())
            .setContentTitle("$count ta yangi fayl")
            .setContentText("Saqlash uchun ilovani oching")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(7, n)
    }

    fun progress(ctx: Context, id: Long, name: String) {
        ensureChannel(ctx)
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFFF47B20.toInt())
            .setContentTitle("Bazaga saqlanmoqda...")
            .setContentText(name)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(nid(id), n)
    }

    fun result(ctx: Context, id: Long, name: String, ok: Boolean, reason: String? = null) {
        ensureChannel(ctx)
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFFF47B20.toInt())
            .setContentTitle(if (ok) "Bazaga saqlandi ✓" else "Saqlab bo'lmadi")
            .setContentText(if (ok) name else (reason ?: "Internetni tekshiring yoki ilovaga qayta kiring"))
            .setTimeoutAfter(6000)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(nid(id), n)
    }

    fun cancel(ctx: Context, id: Long) {
        ctx.getSystemService(NotificationManager::class.java).cancel(nid(id))
    }
}
