package com.m4.red_android.ui.scanner

import ActionButtons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.m4.red_android.viewmodels.BarcodeViewModel

@Composable
fun ScannerScreen(
    viewModel: BarcodeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {

    val product = viewModel.product.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {

        CameraPreview(viewModel = viewModel, modifier = Modifier.fillMaxWidth().padding(10.dp, 10.dp).weight(0.5f))


        BarcodeList(viewModel, modifier = Modifier.weight(3f))

        Spacer(modifier = Modifier.weight(1f))   // empurra para baixo

        ActionButtons(
            { println("Clicou no 1") },
            { println("clicou no 2") },
            { cleanList(viewModel) },
            { println("clicou no 4") },
            viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}


fun cleanList(viewModel: BarcodeViewModel) {
    viewModel.clearProducts()
}


//@Composable
//fun ScannerScreen(viewModel1: BarcodeViewModel = viewModel()) {
//
//    Column(modifier = Modifier.fillMaxSize()) {
//
//        // preview da camera
//        Box(n
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(300.dp)
//        ) {
//            CameraPreview(
//                viewModel = viewModel1
//            ) // onde fica o seu preview
//        }
//
//        Divider()
//
//        // lista dos códigos
//        BarcodeList(viewModel1)
//    }
//}