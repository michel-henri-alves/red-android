package com.m4.red_android.ui.scanner

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.m4.red_android.viewmodels.BarcodeViewModel
import java.util.concurrent.Executors
import kotlin.collections.forEach

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
//    viewModel: BarcodeViewModel,
    onBarcodeDetected: (String) -> Unit,
    isDetected: Boolean,
    modifier: Modifier = Modifier
) {
    val tablet = isTablet()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 12.dp,
                bottom = 12.dp,
                start = 16.dp,
                end = 16.dp
            )
            .then(
                if (tablet)
                    Modifier.aspectRatio(16f / 9f)
                else
                    Modifier.height(300.dp)
            )
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->

                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER

                    // IMPORTANTE para respeitar bordas arredondadas
                    clipToOutline = true
                }

                val cameraProviderFuture =
                    ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({

                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build()

                    val executor = Executors.newSingleThreadExecutor()
                    val scanner = BarcodeScanning.getClient()

                    analyzer.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.forEach { barcode ->
                                        val boundingBox = barcode.boundingBox ?: return@forEach

                                        val imageWidth = imageProxy.width
                                        val imageHeight = imageProxy.height

                                        // Região central (60% da largura e altura)
                                        val roiLeft = imageWidth * 0.2
                                        val roiRight = imageWidth * 0.8
                                        val roiTop = imageHeight * 0.2
                                        val roiBottom = imageHeight * 0.8

                                        val barcodeCenterX = boundingBox.centerX().toDouble()
                                        val barcodeCenterY = boundingBox.centerY().toDouble()

                                        val isInsideROI =
                                            barcodeCenterX in roiLeft..roiRight &&
                                                    barcodeCenterY in roiTop..roiBottom

                                        if (isInsideROI) {
                                            barcode.rawValue?.let {
//                                                viewModel.addCode(it)
                                                onBarcodeDetected(it)
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analyzer
                        )
                    } catch (_: Exception) {
                    }

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        ScannerOverlay(
//            isDetected = viewModel.isBarcodeDetected
            isDetected
        )
    }
}


@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= 600
}