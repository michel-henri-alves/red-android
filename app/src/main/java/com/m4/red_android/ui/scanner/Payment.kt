package com.m4.red_android.ui.scanner


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.m4.red_android.viewmodels.BarcodeViewModel

@Composable
fun Payment(
    viewModel: BarcodeViewModel,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SaleSummaryCard(
            total = viewModel.amount,
            paid = 0.0,
            due = viewModel.amount,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        PaymentMethodCard(
            viewModel = viewModel,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        PaymentInputCard(viewModel)
    }

//    Text(
//        text = String.format("R$ %.2f", viewModel.amount),
//        style = MaterialTheme.typography.titleLarge,
//        color = androidx.compose.ui.graphics.Color.White
//
//    )
}

