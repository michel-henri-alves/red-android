package com.m4.red_android.ui.scanner

import ActionButtons
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.BarcodeViewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext


@Composable
fun ScannerScreen(
    onNavigate: () -> Unit,
    viewModel: BarcodeViewModel,
    modifier: Modifier = Modifier
) {

    val product = viewModel.product.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {

        CameraPreview(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 10.dp)
                .weight(0.5f)
        )

        Spacer(modifier = Modifier.weight(1f))   // empurra para baixo

        BarcodeList(viewModel, modifier = Modifier.weight(3f))

        Spacer(modifier = Modifier.weight(1f))   // empurra para baixo

        TextInfo(
            viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        ActionButtons(
            { goToPayment(onNavigate, viewModel, context) },
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
    viewModel.resetState()
}

fun goToPayment(onNavigate: () -> Unit, viewModel: BarcodeViewModel, context: Context) {
    if (viewModel.qty != 0)
        onNavigate()
    else
        Toast.makeText(
            context,
            "O carrinho está vazio",
            Toast.LENGTH_LONG
        ).show()

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