package com.m4.red_android.ui.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.m4.red_android.viewmodels.BarcodeViewModel
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
        viewModel: BarcodeViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val executor = Executors.newSingleThreadExecutor()

                val analyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val scanner = BarcodeScanning.getClient()

                analyzer.setAnalyzer(executor) { imageProxy ->
                    try {
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.forEach { barcode ->
                                        barcode.rawValue?.let { raw ->
                                            viewModel.addCode(raw)
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    Log.e("CameraPreview", "Erro no scanner", it)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Erro inesperado", e)
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        analyzer
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Erro ao iniciar câmera", exc)
                }

            }, ContextCompat.getMainExecutor(context))

            previewView
        }
    )
}