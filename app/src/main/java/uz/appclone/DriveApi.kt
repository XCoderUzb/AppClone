package uz.appclone

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Baza (Google Drive v3) bilan ishlash. Faqat orqa oqimda chaqiring. */
object DriveApi {

    data class DriveFile(
        val id: String,
        val name: String,
        val mime: String,
        val size: Long,
        val created: String,
        val thumb: String?
    )

    data class User(val name: String, val email: String, val photo: String = "")

    private const val BASE = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"

    private fun readBody(c: HttpURLConnection): String {
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw IOException("Baza xatosi $code: $text")
        return text
    }

    private fun request(method: String, url: String, token: String, body: String? = null): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        if (method == "PATCH") {
            c.requestMethod = "POST"
            c.setRequestProperty("X-HTTP-Method-Override", "PATCH")
        } else {
            c.requestMethod = method
        }
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("Authorization", "Bearer $token")
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        return JSONObject(readBody(c))
    }

    fun getUser(token: String): User {
        val json = request("GET", "$BASE/about?fields=user(displayName,emailAddress,photoLink)", token)
        val u = json.getJSONObject("user")
        return User(u.optString("displayName"), u.optString("emailAddress"), u.optString("photoLink"))
    }

    /** parentId ichida "name" papkasini topadi yoki yaratadi. */
    fun ensureFolder(token: String, name: String, parentId: String? = null): String {
        var query = "name='$name' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        if (parentId != null) query += " and '$parentId' in parents"
        val q = URLEncoder.encode(query, "UTF-8")
        val found = request("GET", "$BASE/files?q=$q&fields=files(id,name)&spaces=drive", token)
        val files = found.getJSONArray("files")
        if (files.length() > 0) return files.getJSONObject(0).getString("id")

        val body = JSONObject()
            .put("name", name)
            .put("mimeType", "application/vnd.google-apps.folder")
        if (parentId != null) body.put("parents", JSONArray().put(parentId))
        return request("POST", "$BASE/files?fields=id", token, body.toString()).getString("id")
    }

    /** Kategoriya papkasining id'si (yo'q bo'lsa yaratadi). */
    fun categoryFolder(ctx: Context, token: String, cat: String): String {
        val root = Prefs.folderId(ctx, "root")
            ?: ensureFolder(token, "App Clone").also { Prefs.setFolderId(ctx, "root", it) }
        return Prefs.folderId(ctx, cat)
            ?: ensureFolder(token, cat, root).also { Prefs.setFolderId(ctx, cat, it) }
    }

    /** Papkadagi fayllar, yangilari birinchi. */
    fun listFiles(token: String, folderId: String): List<DriveFile> {
        val out = mutableListOf<DriveFile>()
        var page: String? = null
        val q = URLEncoder.encode(
            "'$folderId' in parents and trashed=false and mimeType != 'application/vnd.google-apps.folder'",
            "UTF-8"
        )
        do {
            var url = "$BASE/files?q=$q&fields=nextPageToken,files(id,name,mimeType,size,createdTime,thumbnailLink)" +
                "&orderBy=createdTime%20desc&pageSize=200"
            if (page != null) url += "&pageToken=" + URLEncoder.encode(page, "UTF-8")
            val json = request("GET", url, token)
            val arr = json.getJSONArray("files")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    DriveFile(
                        o.getString("id"),
                        o.optString("name"),
                        o.optString("mimeType"),
                        o.optString("size", "0").toLongOrNull() ?: 0L,
                        o.optString("createdTime"),
                        o.optString("thumbnailLink").takeIf { it.isNotEmpty() }
                    )
                )
            }
            page = if (json.has("nextPageToken")) json.getString("nextPageToken") else null
        } while (page != null && out.size < 1000)
        return out
    }

    /** Faylni Bazaning axlat qutisiga tashlaydi (30 kun ichida tiklash mumkin). */
    fun trash(token: String, fileId: String) {
        request("PATCH", "$BASE/files/$fileId?fields=id", token, "{\"trashed\":true}")
    }

    fun sizeOf(ctx: Context, uri: Uri): Long =
        ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L

    /** Ilova Bazada egallagan jami hajm (papkalarsiz, faqat fayllar). */
    fun appUsedBytes(token: String): Long {
        var total = 0L
        var page: String? = null
        val q = URLEncoder.encode("mimeType != 'application/vnd.google-apps.folder' and trashed = false", "UTF-8")
        do {
            var url = "$BASE/files?q=$q&fields=nextPageToken,files(size)&pageSize=1000&spaces=drive"
            if (page != null) url += "&pageToken=" + URLEncoder.encode(page, "UTF-8")
            val json = request("GET", url, token)
            val arr = json.getJSONArray("files")
            for (i in 0 until arr.length()) {
                total += arr.getJSONObject(i).optString("size", "0").toLongOrNull() ?: 0L
            }
            page = if (json.has("nextPageToken")) json.getString("nextPageToken") else null
        } while (page != null)
        return total
    }

    /** Faylni parentId papkasiga yuklaydi (oqim bilan, xotirani to'ldirmaydi). */
    fun upload(ctx: Context, token: String, uri: Uri, name: String, mime: String, parentId: String): String {
        val size = ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        if (size < 0) throw IOException("Fayl hajmi aniqlanmadi")

        val meta = JSONObject().put("name", name).put("parents", JSONArray().put(parentId)).toString()
        val start = URL("$UPLOAD?uploadType=resumable&fields=id").openConnection() as HttpURLConnection
        start.requestMethod = "POST"
        start.connectTimeout = 15000
        start.readTimeout = 30000
        start.doOutput = true
        start.setRequestProperty("Authorization", "Bearer $token")
        start.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        start.setRequestProperty("X-Upload-Content-Type", mime)
        start.setRequestProperty("X-Upload-Content-Length", size.toString())
        start.outputStream.use { it.write(meta.toByteArray(Charsets.UTF_8)) }
        val location = start.getHeaderField("Location")
        if (start.responseCode !in 200..299 || location == null) {
            readBody(start)
            throw IOException("Yuklash sessiyasi ochilmadi")
        }

        val put = URL(location).openConnection() as HttpURLConnection
        put.requestMethod = "PUT"
        put.connectTimeout = 15000
        put.readTimeout = 120000
        put.doOutput = true
        put.setFixedLengthStreamingMode(size)
        put.setRequestProperty("Content-Type", mime)
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            put.outputStream.use { out -> input.copyTo(out, 256 * 1024) }
        } ?: throw IOException("Fayl ochilmadi")
        return JSONObject(readBody(put)).getString("id")
    }
}
