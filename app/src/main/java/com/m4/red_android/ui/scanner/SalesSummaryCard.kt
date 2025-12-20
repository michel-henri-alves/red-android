package com.m4.red_android.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SaleSummaryCard(
    total: Double,
    paid: Double,
    due: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Resumo da Venda",
                style = MaterialTheme.typography.titleMedium
            )

            SummaryRow(
                label = "Total da compra",
                value = formatCurrency(total)
            )

            SummaryRow(
                label = "Valor pago",
                value = formatCurrency(paid)
            )

            Divider()

            SummaryRow(
                label = "Valor devido",
                value = formatCurrency(due),
                highlight = true
            )
        }
    }
}

fun formatCurrency(value: Double): String {
    return "R$ %.2f".format(value)
}
