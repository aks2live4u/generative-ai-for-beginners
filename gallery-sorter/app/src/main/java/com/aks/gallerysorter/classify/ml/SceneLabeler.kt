package com.aks.gallerysorter.classify.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

interface SceneLabeler {
    /** Label text → confidence in 0..1. */
    suspend fun labels(bmp: Bitmap): Map<String, Float>
}

/** ML Kit Image Labeling with the bundled default model (spec 6.3, "Scenes/content"). */
class MlKitSceneLabeler(
    private val labeler: ImageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS),
) : SceneLabeler, AutoCloseable {
    override suspend fun labels(bmp: Bitmap): Map<String, Float> =
        labeler.process(InputImage.fromBitmap(bmp, 0)).await().associate { it.text to it.confidence }

    override fun close() = labeler.close()
}
