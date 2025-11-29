package com.m4.red_android

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.camera.view.PreviewView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.m4.red_android.viewmodels.BarcodeViewModel


@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreviewWithBarcodeReader(
    modifier: Modifier = Modifier,
    onBarcodeScanned: (String) -> Unit,
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS
                    )
                    .build()

                val scanner = BarcodeScanning.getClient(options)

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    try {
                        val mediaImage = imageProxy.image ?: return@setAnalyzer

                        val image = InputImage.fromMediaImage(
                            mediaImage, imageProxy.imageInfo.rotationDegrees
                        )

                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
//                                    barcode.rawValue?.let { value ->
//                                        viewModel.addCode(value)
//                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }

                    } catch (e: Exception) {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    (ctx as ComponentActivity),
                    cameraSelector,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}