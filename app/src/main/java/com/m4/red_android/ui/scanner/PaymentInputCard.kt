package com.m4.red_android.ui.scanner

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.BarcodeViewModel

@Composable
fun PaymentInputCard(
    viewModel: BarcodeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {


    var input by remember { mutableStateOf("") }
    viewModel.setPaymentAmount()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BarcodeViewModel.UiEvent.GoBack -> {
                    onBack()
                }

                is BarcodeViewModel.UiEvent.SalesFinished -> {
                    Toast
                        .makeText(
                            context,
                            "Venda finalizada",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }

                is BarcodeViewModel.UiEvent.RemainNotification -> {
                    Toast
                        .makeText(
                            context,
                            "Recebido R$${event.valueReceived}. Restam R$${event.valueRemain}",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(

            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Pagamento",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = viewModel.dueText,
                onValueChange = viewModel::onPaymentAmountChange,
                label = { Text("Valor recebido") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { discountButton(viewModel) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(228, 88, 88),
                        contentColor = Color.White
                    ),
                    enabled = viewModel.discount == 0.0 && viewModel.due > 0.0
                ) {
                    Text("Desconto")
                }
                Button(
                    onClick = { paymentFinishing(viewModel, context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(228, 88, 88),
                        contentColor = Color.White
                    ),
                    enabled = viewModel.paymentAmountAsDouble() > 0.0 && viewModel.due > 0.0
                ) {
                    Text("Receber")
                }
            }
        }
    }
}

fun discountButton(viewModel: BarcodeViewModel) {
    viewModel.setShowDiscountDialog(true)
}

fun paymentFinishing(viewModel: BarcodeViewModel, context: Context) {
    if (viewModel.paymentMethod != null) {
        viewModel.finalizePayment()
    } else {
        Toast.makeText(
            context,
            "Selecione a forma de pagamento",
            Toast.LENGTH_LONG
        ).show()
    }
}

//fun setTotalValue(viewModel: BarcodeViewModel) {
//    viewModel.setPaymentAmount()
//}
