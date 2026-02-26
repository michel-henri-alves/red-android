package com.m4.red_android.ui.scanner

import android.annotation.SuppressLint
import androidx.annotation.ColorLong
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.m4.red_android.viewmodels.BarcodeViewModel

@SuppressLint("DefaultLocale")
@Composable
fun TextInfo(
    viewModel: BarcodeViewModel = viewModel(),
    modifier: Modifier = Modifier,

) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = String.format("R$ %.2f", viewModel.amount),
            style = MaterialTheme.typography.titleLarge,
            color = androidx.compose.ui.graphics.Color.Black

        )

        Text(
            text = "Qtde: " + viewModel.qty,
            style = MaterialTheme.typography.titleLarge,
            color = androidx.compose.ui.graphics.Color.Black

        )
    }
}