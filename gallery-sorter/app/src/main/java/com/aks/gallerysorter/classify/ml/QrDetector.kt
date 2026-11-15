package com.aks.gallerysorter.classify.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

interface QrDetector {
    suspend fun hasCode(bmp: Bitmap): Boolean
}

/** ML Kit barcode scanning (bundled), restricted to QR codes (spec 6.3, "Scenes/content"). */
class MlKitQrDetector(
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
    ),
) : QrDetector, AutoCloseable {
    override suspend fun hasCode(bmp: Bitmap): Boolean =
        scanner.process(InputImage.fromBitmap(bmp, 0)).await().isNotEmpty()

    override fun close() = scanner.close()
}
