package com.m4.red_android.ui.scanner

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.BarcodeViewModel
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext


@Composable
fun PosScreen(
    onNavigate: () -> Unit,
    viewModel: BarcodeViewModel,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val isTablet = screenWidthDp >= 600

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding() // ajusta quando teclado abrir
            .padding(8.dp)
    ) {

        if (isTablet) {
            TabletLayout(viewModel, onNavigate, context)
        } else {
            PhoneLayout(viewModel, onNavigate, context)
        }
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
