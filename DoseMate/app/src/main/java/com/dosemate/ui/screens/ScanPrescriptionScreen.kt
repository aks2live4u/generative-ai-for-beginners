package com.dosemate.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dosemate.data.Frequency
import com.dosemate.data.Medicine
import com.dosemate.data.ParsedMedicine
import com.dosemate.ui.components.GlassCard
import com.dosemate.ui.theme.GlassBackdrop
import com.dosemate.ui.theme.LocalDoseMateColors
import com.dosemate.viewmodel.ScanPrescriptionViewModel
import com.dosemate.viewmodel.ScanState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.nio.ByteBuffer

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanPrescriptionScreen(onDone: () -> Unit) {
    val viewModel: ScanPrescriptionViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    when (val s = state) {
        is ScanState.Camera -> {
            if (cameraPermission.status.isGranted) {
                CameraCapture(
                    onCapture = { bmp -> viewModel.processImage(bmp) },
                    onCancel = onDone
                )
            } else {
                CameraPermissionRationale(
                    onRequest = { cameraPermission.launchPermissionRequest() },
                    onCancel = onDone
                )
            }
        }
        is ScanState.Processing -> ProcessingView()
        is ScanState.Review -> ReviewScreen(
            parsed = s.parsed,
            existingActive = s.existingActive,
            onApply = { proposed, archive -> viewModel.applyPrescription(proposed, archive) },
            onRetry = viewModel::retryCamera,
            onCancel = onDone
        )
        is ScanState.Error -> ErrorView(
            message = s.message,
            onRetry = viewModel::retryCamera,
            onCancel = onDone
        )
        is ScanState.Applied -> {
            LaunchedEffect(Unit) { onDone() }
        }
    }
}

@Composable
private fun CameraCapture(onCapture: (Bitmap) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { uriToBitmap(context, it)?.let(onCapture) }
    }

    LaunchedEffect(Unit) {
        bindCamera(context, lifecycleOwner, previewView, imageCapture)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Cancel", tint = Color.White)
            }
            Text(
                "Scan Prescription",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Bottom controls: gallery | capture | spacer (symmetric)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery picker
            IconButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = "Pick from gallery",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Shutter
            IconButton(
                onClick = { captureImage(context, imageCapture, onCapture) },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "Capture",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Symmetry spacer
            Spacer(Modifier.size(56.dp))
        }
    }
}

private fun bindCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
    }, ContextCompat.getMainExecutor(context))
}

private fun captureImage(context: Context, imageCapture: ImageCapture, onCapture: (Bitmap) -> Unit) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                if (bitmap != null) onCapture(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                // silently ignored — ViewModel stays in Camera state
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val rotation = image.imageInfo.rotationDegrees
    if (rotation == 0) return bitmap
    val matrix = Matrix().also { it.postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? = try {
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
} catch (_: Exception) { null }

@Composable
private fun CameraPermissionRationale(onRequest: () -> Unit, onCancel: () -> Unit) {
    GlassBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📷", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Camera needed",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LocalDoseMateColors.current.headerText
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "DoseMate needs camera access to scan your prescription and auto-create your medicine reminders.",
                fontSize = 14.sp,
                color = LocalDoseMateColors.current.textSecondary
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = LocalDoseMateColors.current.accent),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Allow Camera") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun ProcessingView() {
    GlassBackdrop {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = LocalDoseMateColors.current.accent, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                "Reading prescription…",
                fontSize = 16.sp,
                color = LocalDoseMateColors.current.textSecondary
            )
        }
    }
}

@Composable
private fun ReviewScreen(
    parsed: List<ParsedMedicine>,
    existingActive: List<Medicine>,
    onApply: (List<ParsedMedicine>, Boolean) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = LocalDoseMateColors.current
    var archiveOld by remember { mutableStateOf(true) }

    val unmatched = existingActive.filter { existing ->
        parsed.none { p ->
            existing.name.lowercase().contains(p.name.lowercase()) ||
                p.name.lowercase().contains(existing.name.lowercase())
        }
    }

    GlassBackdrop {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Retake", tint = colors.headerText)
                }
                Text(
                    "Prescription Found",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.headerText,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        "${parsed.size} medicine${if (parsed.size == 1) "" else "s"} detected",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                }

                items(parsed) { medicine ->
                    val isUpdate = existingActive.any { existing ->
                        existing.name.lowercase().contains(medicine.name.lowercase()) ||
                            medicine.name.lowercase().contains(existing.name.lowercase())
                    }
                    PrescriptionMedicineCard(medicine = medicine, isUpdate = isUpdate)
                }

                if (unmatched.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = archiveOld,
                                    onCheckedChange = { archiveOld = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha = 0.4f))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Archive ${unmatched.size} old medicine${if (unmatched.size == 1) "" else "s"}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        unmatched.joinToString(", ") { it.name },
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom buttons
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Button(
                    onClick = { onApply(parsed, archiveOld) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) { Text("Apply Prescription", fontSize = 16.sp) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun PrescriptionMedicineCard(medicine: ParsedMedicine, isUpdate: Boolean) {
    val colors = LocalDoseMateColors.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(medicine.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (isUpdate) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.accent.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("update", fontSize = 10.sp, color = colors.accent)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("new", fontSize = 10.sp, color = Color(0xFF388E3C))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(medicine.dosage, fontSize = 13.sp, color = colors.textSecondary)
                Spacer(Modifier.height(2.dp))
                Text(frequencyLabel(medicine), fontSize = 12.sp, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    val colors = LocalDoseMateColors.current
    GlassBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔍", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Nothing detected",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.headerText
            )
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 14.sp, color = colors.textSecondary)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Try Again") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

private fun frequencyLabel(medicine: ParsedMedicine): String = when (medicine.frequency) {
    Frequency.DAILY -> when (medicine.timesOfDay.size) {
        1 -> "Once daily · ${formatTime(medicine.timesOfDay[0])}"
        2 -> "Twice daily · ${medicine.timesOfDay.joinToString(", ", transform = ::formatTime)}"
        3 -> "3× daily · ${medicine.timesOfDay.joinToString(", ", transform = ::formatTime)}"
        else -> "${medicine.timesOfDay.size}× daily"
    }
    Frequency.EVERY_X_HOURS -> "Every ${medicine.everyXHours}h"
    Frequency.ALTERNATE_DAYS -> "Every other day"
    Frequency.SPECIFIC_DAYS -> "Specific days"
    Frequency.SOS -> "As needed (SOS)"
}

private fun formatTime(time: Pair<Int, Int>): String {
    val (h, m) = time
    val ampm = if (h < 12) "AM" else "PM"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "$h12:${String.format("%02d", m)} $ampm"
}
