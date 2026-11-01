package com.focus.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import java.io.File

/** Custom wallpaper photos and imported font files, stored privately in the app's files dir. */
object Media {

    fun wallpaperFile(context: Context) = File(context.filesDir, "wallpaper.img")
    fun fontFile(context: Context) = File(context.filesDir, "custom_font.ttf")

    fun copy(context: Context, uri: Uri, dest: File): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)!!.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        true
    }.getOrDefault(false)

    fun loadWallpaper(context: Context, maxW: Int = 1440, maxH: Int = 3200): Bitmap? = runCatching {
        val file = wallpaperFile(context)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxW && bounds.outHeight / (sample * 2) >= maxH) sample *= 2
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null

        // Respect camera rotation so portrait photos are not sideways.
        val degrees = when (ExifInterface(file.path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) bitmap
        else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    }.getOrNull()

    fun loadFont(context: Context): Typeface? = runCatching {
        val file = fontFile(context)
        if (file.exists() && file.length() > 0) Typeface.createFromFile(file) else null
    }.getOrNull()

    fun fontFamilyFor(context: Context, choice: FontChoice): FontFamily = when (choice) {
        FontChoice.DEFAULT -> FontFamily.SansSerif
        FontChoice.SERIF -> FontFamily.Serif
        FontChoice.MONO -> FontFamily.Monospace
        FontChoice.CURSIVE -> FontFamily.Cursive
        FontChoice.LIGHT -> FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))
        FontChoice.THIN -> FontFamily(Typeface.create("sans-serif-thin", Typeface.NORMAL))
        FontChoice.CONDENSED -> FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
        FontChoice.CASUAL -> FontFamily(Typeface.create("casual", Typeface.NORMAL))
        FontChoice.CUSTOM -> loadFont(context)?.let { FontFamily(it) } ?: FontFamily.SansSerif
    }
}
