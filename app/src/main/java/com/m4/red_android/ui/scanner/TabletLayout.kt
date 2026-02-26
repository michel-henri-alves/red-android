package com.m4.red_android.ui.scanner

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.m4.red_android.viewmodels.BarcodeViewModel

@Composable
fun TabletLayout(
    viewModel: BarcodeViewModel,
    onNavigate: () -> Unit,
    context: Context
) {

    Row(modifier = Modifier.fillMaxSize()) {

        // 📷 Câmera ocupa lado esquerdo
        CameraPreview(
//            viewModel = viewModel,
            onBarcodeDetected = { code ->
                viewModel.addCode(code)
            },
            isDetected = viewModel.isBarcodeDetected,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {

            BarcodeList(
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )

            BottomPanel(
                viewModel = viewModel,
                onNavigate = onNavigate,
                context = context,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
