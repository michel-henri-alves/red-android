package com.m4.red_android.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType.Companion.Text
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.BarcodeViewModel

@Composable
fun BarcodeList(
    viewModel: BarcodeViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .fillMaxSize()
//        .padding(20.dp)
    ) {

        Text(
            text = "Valor: " + viewModel.amount,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
//            items(viewModel.codes) { code ->
            items(viewModel.products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
//                        text = code,
                        text = product.name,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
