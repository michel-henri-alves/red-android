package com.m4.red_android.ui.scanner

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.BarcodeViewModel

@SuppressLint("DefaultLocale")
@Composable
fun BarcodeList(
    viewModel: BarcodeViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 100.dp)
    ) {

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.products) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null, // remove o ripple OU evita o erro
                            interactionSource = remember { MutableInteractionSource() }
                        ) { viewModel.removeProduct(product) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = product.name +" "+ String.format("R$ %.2f", product.priceForSale),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
