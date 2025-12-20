package com.m4.red_android.ui.scanner

import PaymentOption
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.m4.red_android.data.enums.PaymentMethod
import com.m4.red_android.viewmodels.BarcodeViewModel

@Composable
fun PaymentMethodCard(
    viewModel: BarcodeViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Selecione forma de pagamento",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            PaymentMethod.values().forEach { method ->
                PaymentOption(
                    method = method,
                    isSelected = viewModel.paymentMethod == method,
                    onClick = {
                        viewModel.selectPaymentMethod(method)
                    }
                )
            }
        }
    }
}
