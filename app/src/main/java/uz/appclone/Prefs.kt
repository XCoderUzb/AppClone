package uz.appclone

import android.content.Context

/** Ilovaning mahalliy sozlamalari va holati. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("appclone", Context.MODE_PRIVATE)

    // --- Kirish ---
    fun isSignedIn(c: Context) = sp(c).getBoolean("signed_in", false)
    fun setSignedIn(c: Context, name: String, email: String) {
        sp(c).edit().putBoolean("signed_in", true)
            .putString("user_name", name).putString("user_email", email).apply()
    }
    fun userName(c: Context) = sp(c).getString("user_name", "") ?: ""
    fun userEmail(c: Context) = sp(c).getString("user_email", "") ?: ""

    // --- Pro ---
    fun isPro(c: Context) = sp(c).getBoolean("pro", false)
    fun setPro(c: Context, v: Boolean) = sp(c).edit().putBoolean("pro", v).apply()

    // --- Yangi fayllarni skanerlash ---
    fun lastScan(c: Context) = sp(c).getLong("last_scan", -1L)
    fun setLastScan(c: Context, v: Long) = sp(c).edit().putLong("last_scan", v).apply()

    private fun addToSet(c: Context, key: String, value: String, cap: Int = 3000) {
        val s = sp(c).getStringSet(key, emptySet())!!.toMutableSet()
        if (s.size >= cap) s.clear()
        s.add(value)
        sp(c).edit().putStringSet(key, s).apply()
    }
    private fun inSet(c: Context, key: String, value: String) =
        sp(c).getStringSet(key, emptySet())!!.contains(value)

    fun wasNotified(c: Context, id: Long) = inSet(c, "notified_ids", id.toString())
    fun markNotified(c: Context, id: Long) = addToSet(c, "notified_ids", id.toString())

    // --- Saqlanganlar ---
    fun isSaved(c: Context, id: Long) = inSet(c, "saved_ids", id.toString())
    fun addSaved(c: Context, category: String, id: Long) {
        if (isSaved(c, id)) return
        addToSet(c, "saved_ids", id.toString(), cap = 100000)
        sp(c).edit().putInt("count_$category", count(c, category) + 1).apply()
    }
    fun setFailed(c: Context, id: Long, v: Boolean) = sp(c).edit().putBoolean("failed_$id", v).apply()
    fun isFailed(c: Context, id: Long) = sp(c).getBoolean("failed_$id", false)

    fun count(c: Context, category: String) = sp(c).getInt("count_$category", 0)

    // --- Baza papkalari id'lari ---
    fun folderId(c: Context, key: String): String? = sp(c).getString("folder_$key", null)
    fun setFolderId(c: Context, key: String, id: String?) =
        sp(c).edit().putString("folder_$key", id).apply()
    fun clearFolders(c: Context) {
        val e = sp(c).edit()
        sp(c).all.keys.filter { it.startsWith("folder_") }.forEach { e.remove(it) }
        e.apply()
    }

    // --- Profil / Baza hajmi ---
    fun usedBytes(c: Context) = sp(c).getLong("used_bytes", 0L)
    fun setUsedBytes(c: Context, v: Long) = sp(c).edit().putLong("used_bytes", v).apply()
    fun addUsedBytes(c: Context, v: Long) = setUsedBytes(c, usedBytes(c) + v)
    fun photoUrl(c: Context) = sp(c).getString("photo_url", "") ?: ""
    fun setPhotoUrl(c: Context, v: String) = sp(c).edit().putString("photo_url", v).apply()

    fun failReason(c: Context, id: Long): String? = sp(c).getString("fail_reason_$id", null)
    fun setFailReason(c: Context, id: Long, v: String?) =
        sp(c).edit().putString("fail_reason_$id", v).apply()

    // --- Admin ---
    fun adminFails(c: Context) = sp(c).getInt("admin_fails", 0)
    fun setAdminFails(c: Context, v: Int) = sp(c).edit().putInt("admin_fails", v).apply()
    fun adminLockUntil(c: Context) = sp(c).getLong("admin_lock_until", 0L)
    fun setAdminLockUntil(c: Context, v: Long) = sp(c).edit().putLong("admin_lock_until", v).apply()

    /** Akkauntdan chiqish: kirish ma'lumotlarini tozalaydi. */
    fun signOut(c: Context) {
        val e = sp(c).edit()
        listOf("signed_in", "user_name", "user_email", "photo_url", "last_scan", "used_bytes")
            .forEach { e.remove(it) }
        sp(c).all.keys.filter { it.startsWith("folder_") }.forEach { e.remove(it) }
        e.apply()
    }
}
