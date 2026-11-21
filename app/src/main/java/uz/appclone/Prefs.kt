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
}
