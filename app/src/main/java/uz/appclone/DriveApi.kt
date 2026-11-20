package uz.appclone

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Google Drive v3 REST so'rovlari (qo'shimcha kutubxonasiz). Faqat orqa oqimda chaqiring. */
object DriveApi {

    data class User(val name: String, val email: String)

    private const val BASE = "https://www.googleapis.com/drive/v3"

    private fun request(method: String, url: String, token: String, body: String? = null): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("Authorization", "Bearer $token")
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw IOException("Drive xatosi $code: $text")
        return JSONObject(text)
    }

    fun getUser(token: String): User {
        val json = request("GET", "$BASE/about?fields=user", token)
        val u = json.getJSONObject("user")
        return User(u.optString("displayName"), u.optString("emailAddress"))
    }

    /** "name" nomli papka bo'lsa id'sini qaytaradi, bo'lmasa yaratadi. */
    fun ensureFolder(token: String, name: String): String {
        val q = URLEncoder.encode(
            "name='$name' and mimeType='application/vnd.google-apps.folder' and trashed=false",
            "UTF-8"
        )
        val found = request("GET", "$BASE/files?q=$q&fields=files(id,name)&spaces=drive", token)
        val files = found.getJSONArray("files")
        if (files.length() > 0) return files.getJSONObject(0).getString("id")

        val body = JSONObject()
            .put("name", name)
            .put("mimeType", "application/vnd.google-apps.folder")
            .toString()
        return request("POST", "$BASE/files?fields=id", token, body).getString("id")
    }
}
