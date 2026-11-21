package uz.appclone

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object Uploader {
    fun enqueue(ctx: Context, item: NewItem, notify: Boolean) {
        Prefs.setFailed(ctx, item.id, false)
        val data = workDataOf(
            "id" to item.id,
            "uri" to item.uri.toString(),
            "name" to item.name,
            "mime" to item.mime,
            "notify" to notify
        )
        val req = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("save-${item.id}", ExistingWorkPolicy.KEEP, req)
    }

    fun uniqueName(id: Long) = "save-$id"
}

class UploadWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        val id = inputData.getLong("id", 0L)
        val uri = Uri.parse(inputData.getString("uri") ?: return Result.failure())
        val name = inputData.getString("name") ?: "fayl"
        val mime = inputData.getString("mime") ?: "application/octet-stream"
        val notify = inputData.getBoolean("notify", false)
        val cat = Cat.of(mime)

        return try {
            val token = Auth.tokenBlocking(ctx)
            if (token == null) {
                Prefs.setFailed(ctx, id, true)
                if (notify) Notifier.result(ctx, id, name, false)
                return Result.failure()
            }
            val root = Prefs.folderId(ctx, "root")
                ?: DriveApi.ensureFolder(token, "App Clone").also { Prefs.setFolderId(ctx, "root", it) }
            val sub = Prefs.folderId(ctx, cat)
                ?: DriveApi.ensureFolder(token, cat, root).also { Prefs.setFolderId(ctx, cat, it) }

            DriveApi.upload(ctx, token, uri, name, mime, sub)
            Prefs.addSaved(ctx, cat, id)
            if (notify) Notifier.result(ctx, id, name, true)
            Result.success()
        } catch (e: Exception) {
            // Papka Bazadan o'chirilgan bo'lishi mumkin: keshni tozalab qayta urinamiz
            if (e.message?.contains("404") == true) Prefs.clearFolders(ctx)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Prefs.setFailed(ctx, id, true)
                if (notify) Notifier.result(ctx, id, name, false)
                Result.failure()
            }
        }
    }
}
