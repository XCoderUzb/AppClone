package uz.appclone

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

object Auth {
    private val driveScope = Scope("https://www.googleapis.com/auth/drive.file")

    fun request(): AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(listOf(driveScope)).build()

    /** Orqa fonda jimgina token oladi. Ruxsat qayta kerak bo'lsa null qaytaradi. */
    fun tokenBlocking(ctx: Context): String? {
        val result = Tasks.await(
            Identity.getAuthorizationClient(ctx).authorize(request()),
            30, TimeUnit.SECONDS
        )
        return if (result.hasResolution()) null else result.accessToken
    }
}
