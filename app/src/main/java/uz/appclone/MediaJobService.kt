package uz.appclone

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.provider.MediaStore
import kotlin.concurrent.thread

/** Telefon xotirasida o'zgarish bo'lganda uyg'onadi (batareyani tejaydi). */
class MediaJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        val app = applicationContext
        thread {
            try {
                Scanner.checkNew(app)
            } catch (_: Exception) {
            }
            schedule(app)
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        private const val JOB_ID = 4201

        fun schedule(ctx: Context) {
            val js = ctx.getSystemService(JobScheduler::class.java)
            val info = JobInfo.Builder(JOB_ID, ComponentName(ctx, MediaJobService::class.java))
                .addTriggerContentUri(
                    JobInfo.TriggerContentUri(
                        MediaStore.Files.getContentUri("external"),
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
                    )
                )
                .setTriggerContentUpdateDelay(3000)
                .setTriggerContentMaxDelay(15000)
                .build()
            js.schedule(info)
        }
    }
}
