package com.m4.red_android.ui.scanner

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.BarcodeViewModel

@Composable
fun PhoneLayout(
    viewModel: BarcodeViewModel,
    onNavigate: () -> Unit,
    context: Context
) {
    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize()) {

            CameraPreview(
//                viewModel = viewModel,
                onBarcodeDetected = { code ->
                    viewModel.addCode(code)
                },
                isDetected = viewModel.isBarcodeDetected,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .heightIn(min = 180.dp)
            )

            BarcodeList(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
