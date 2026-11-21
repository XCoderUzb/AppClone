package uz.appclone

import android.net.Uri

/** Telefonda paydo bo'lgan yangi fayl. */
data class NewItem(
    val id: Long,
    val name: String,
    val mime: String,
    val size: Long,
    val uri: Uri
) {
    val category: String get() = Cat.of(mime)
}

object Cat {
    const val PHOTOS = "Rasmlar"
    const val VIDEOS = "Videolar"
    const val FILES = "Fayllar"
    const val APPS = "Ilovalar"

    fun of(mime: String): String = when {
        mime.startsWith("image/") -> PHOTOS
        mime.startsWith("video/") -> VIDEOS
        else -> FILES
    }

    fun noun(mime: String): String = when {
        mime.startsWith("image/") -> "rasm"
        mime.startsWith("video/") -> "video"
        else -> "fayl"
    }

    fun emoji(cat: String): String = when (cat) {
        PHOTOS -> "🖼️"
        VIDEOS -> "🎬"
        APPS -> "📱"
        else -> "📄"
    }
}

fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / 1048576.0)
    else -> String.format("%.2f GB", bytes / 1073741824.0)
}
