package uz.appclone

import android.content.Context
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Admin kirishi: Google akkaunt ro'yxati + parol (parol faqat xesh ko'rinishida saqlanadi). */
object Admin {
    private val ADMIN_EMAILS = setOf("animanxwa.uz@gmail.com", "animelarx@gmail.com")

    private const val SALT_HEX = "81d144d20a71fa4b15c95f01bbca9a48"
    private const val HASH_HEX = "a7628228e1b7a35d6968a1cee6d1cf786926ffb729ddf8f8d12bb31793f08a12"
    private const val ITERATIONS = 100_000

    private const val MAX_FAILS = 5
    private const val LOCK_MS = 15 * 60 * 1000L
    private const val SESSION_MS = 10 * 60 * 1000L

    @Volatile private var unlockedUntil = 0L

    fun isAdminEmail(email: String) = email.trim().lowercase() in ADMIN_EMAILS

    /** Ko'rsatish uchun admin akkauntlar ro'yxati. */
    fun listAdmins(): List<String> = listOf("AniManxwa.uz@gmail.com", "AnimelarX@gmail.com")

    /** Admin akkaunt bilan kirilgan: parol kerak emas, panel doim ochiq. */
    fun isAdminAccount(ctx: Context) = isAdminEmail(Prefs.userEmail(ctx))

    /** Panelni ochish mumkinmi: admin akkaunt yoki parol bilan (tiklash rejimi) kirilgan. */
    fun canOpenPanel(ctx: Context) = isAdminAccount(ctx) || isUnlocked()

    fun isUnlocked() = System.currentTimeMillis() < unlockedUntil

    fun lockedOutMs(ctx: Context): Long =
        maxOf(0L, Prefs.adminLockUntil(ctx) - System.currentTimeMillis())

    fun tryUnlock(ctx: Context, password: String): Boolean {
        if (lockedOutMs(ctx) > 0) return false
        return if (check(password)) {
            Prefs.setAdminFails(ctx, 0)
            unlockedUntil = System.currentTimeMillis() + SESSION_MS
            true
        } else {
            val fails = Prefs.adminFails(ctx) + 1
            if (fails >= MAX_FAILS) {
                Prefs.setAdminLockUntil(ctx, System.currentTimeMillis() + LOCK_MS)
                Prefs.setAdminFails(ctx, 0)
            } else {
                Prefs.setAdminFails(ctx, fails)
            }
            false
        }
    }

    private fun check(password: String): Boolean {
        val spec = PBEKeySpec(password.toCharArray(), hex(SALT_HEX), ITERATIONS, 256)
        val h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return MessageDigest.isEqual(h, hex(HASH_HEX))
    }

    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(2 * it, 2 * it + 2).toInt(16).toByte() }
}
