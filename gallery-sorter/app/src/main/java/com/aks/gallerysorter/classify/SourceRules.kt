package com.aks.gallerysorter.classify

import com.aks.gallerysorter.core.Category
import com.aks.gallerysorter.core.ExifData
import com.aks.gallerysorter.core.MediaKind

private class SourceRule(val regex: Regex, val category: Category, val videoOnly: Boolean = false)

private fun rule(pattern: String, category: Category, videoOnly: Boolean = false) =
    SourceRule(Regex("(^|/)(?:$pattern)(/|$)"), category, videoOnly)

/** Ordered: the first matching rule wins, so a file gets at most one source category. */
private val RULES = listOf(
    rule("dcim/screenshots|pictures/screenshots|screenshots", Category.SCREENSHOTS),
    rule("movies/screenrecord|dcim/screenrecorder|dcim/screenrecord|screenrecorder|screenrecord",
        Category.SCREEN_RECORDINGS, videoOnly = true),
    rule("com\\.whatsapp(?:\\.w4b)?|whatsapp|whatsapp business", Category.WHATSAPP),
    rule("telegram", Category.TELEGRAM),
    rule("instagram|messenger|signal|snapchat|facebook", Category.OTHER_MESSAGING),
    rule("download|downloads", Category.DOWNLOADS),
    rule("dcim/camera", Category.CAMERA),
)

/** Lowercased directory portion of the path (file name dropped), segments joined by '/'. */
private fun dirOf(path: String): String =
    path.lowercase().replace('\\', '/').substringBeforeLast('/', "").trim('/')

fun sourceCategories(
    path: String,
    kind: MediaKind,
    exif: ExifData?,
    ownCameraModels: Set<String>,
): Set<Category> {
    val dir = dirOf(path)
    val hit = RULES.firstOrNull { (!it.videoOnly || kind == MediaKind.VIDEO) && it.regex.containsMatchIn(dir) }
    return if (hit == null) emptySet() else setOf(hit.category)
}

/** True for a DCIM/Camera file whose EXIF model is one of the device's own camera models. */
fun isOwnCameraOriginal(path: String, exif: ExifData?, ownCameraModels: Set<String>): Boolean {
    if (!RULES.last().regex.containsMatchIn(dirOf(path))) return false
    val model = exif?.model?.trim()?.lowercase() ?: return false
    return ownCameraModels.any { it.trim().lowercase() == model }
}
