package com.aks.gallerysorter.classify.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

interface TextReader {
    suspend fun read(bmp: Bitmap): String
}

/** ML Kit Text Recognition v2, bundled Latin-script model (spec 6.3, "Text/documents"). */
class MlKitTextReader(
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
) : TextReader, AutoCloseable {
    override suspend fun read(bmp: Bitmap): String =
        recognizer.process(InputImage.fromBitmap(bmp, 0)).await().text

    override fun close() = recognizer.close()
}
