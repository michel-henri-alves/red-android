package com.m4.red_android.ui.scanner


import ChangeDialog
import DiscountDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.BarcodeViewModel

@Composable
fun Payment(
    viewModel: BarcodeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
    ) {
        SaleSummaryCard(
            total = viewModel.amount,
            paid = viewModel.paid,
            discount = viewModel.discount,
            due = viewModel.due,
            change = viewModel.change,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        PaymentMethodCard(
            viewModel = viewModel,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        PaymentInputCard(viewModel, onBack)

    }

    if (viewModel.showDiscountDialog) {
        DiscountDialog(
            onDismiss = { viewModel.setShowDiscountDialog(false) },
            onConfirm = { discount ->
                viewModel.applyDiscount(discount)
                viewModel.setShowDiscountDialog(false)
            }
        )
    }

    if (viewModel.showChangeDialog) {
        ChangeDialog(
            viewModel = viewModel,
            onDismiss = {
                viewModel.saveSale()
            },
        )
    }
}

