package com.m4.red_android.ui.scanner

import UnitOfMeasurementOption
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.m4.red_android.data.enums.UnitOfMeasurement
import com.m4.red_android.viewmodels.BarcodeViewModel
import com.m4.red_android.viewmodels.ProductViewModel

@Composable
fun CreateProduct(
    viewModel: ProductViewModel,
//    onNavigate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var checked by remember { mutableStateOf(false) }

    val result = viewModel.saveResult

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProductViewModel.UiEvent.Success-> {
                    Toast
                        .makeText(
                            context,
                            "Novo produto criado com sucesso!",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }

                is ProductViewModel.UiEvent.Error-> {
                    Toast
                        .makeText(
                            context,
                            "Falha ao cadastrar produto!",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Salvar produto",
                    style = MaterialTheme.typography.titleMedium
                )

                CameraPreview(
//              viewModel = viewModel,
                    onBarcodeDetected = { code ->
                        viewModel.onSmartCodeChange(code)
                    },
                    isDetected = true,
                    modifier = Modifier
                        .fillMaxWidth()
//                        .weight(0.35f)
                        .heightIn(min = 180.dp)
                )

                OutlinedTextField(
                    value = viewModel.smartCode.text,
                    onValueChange = viewModel::onSmartCodeChange,
                    label = { Text("Código *") },
                    isError = viewModel.smartCode.error != null,
                    supportingText = {
                        viewModel.smartCode.error?.let {
                            Text(it)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),

                    trailingIcon = {
                        IconButton(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Digite ou aproxime o código de barras",
                                    Toast.LENGTH_LONG
                                ).show()
//                                onNavigate()
                                // abrir camera aqui
                                //onOpenCamera()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Ler código de barras"
                            )
                        }
                    },

                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                    )
                )

                OutlinedTextField(
                    value = viewModel.name.text,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Nome *") },
                    isError = viewModel.name.error != null,
                    supportingText = {
                        viewModel.name.error?.let {
                            Text(it)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                    )
                )

                OutlinedTextField(
                    value = viewModel.manufacturer.text,
                    onValueChange = viewModel::onManufacturerChange,
                    label = { Text("Fabricante") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                    )
                )

                OutlinedTextField(
                    value = viewModel.category.text,
                    onValueChange = viewModel::onCategoryChange,
                    label = { Text("Categoria") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                    )
                )

                OutlinedTextField(
                    value = viewModel.priceForSale.text,
                    onValueChange = viewModel::onPriceForSaleChange,
                    label = { Text("Preço de venda (R$) *") },
                    singleLine = true,
                    isError = viewModel.priceForSale.error != null,
                    supportingText = {
                        viewModel.priceForSale.error?.let {
                            Text(it)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                    )
                )

                OutlinedTextField(
                    value = viewModel.minQuantity.text,
                    onValueChange = viewModel::onMinQuantityChange,
                    label = { Text("Quantidade mínima") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                    )
                )

                OutlinedTextField(
                    value = viewModel.actualQuantity.text,
                    onValueChange = viewModel::onActualQuantityChange,
                    label = { Text("Quantidade atual *") },
                    singleLine = true,
                    isError = viewModel.actualQuantity.error != null,
                    supportingText = {
                        viewModel.actualQuantity.error?.let {
                            Text(it)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                    )
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = "Unidade de medida",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(12.dp))

                UnitOfMeasurement.values().forEach { measure ->
                    UnitOfMeasurementOption(
                        measure = measure,
                        isSelected = viewModel.unitOfMeasurement == measure,
                        onClick = {
                            viewModel.selectUnitOfMeasurement(measure)
                        }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { checked = !checked },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = viewModel.ownManufacture,
                    onCheckedChange = viewModel :: onOwnManufactureChange
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(text = "fabricação própria")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { createFinish(viewModel, context) }
                ) {
                    Text("Salvar")
                }
                Button(
                    onClick = { cleanForm(viewModel, context) }
                ) { Text("Limpar") }
            }

        }

    }
}

fun createFinish(viewModel: ProductViewModel, context: Context) {
    if(viewModel.unitOfMeasurement == null) {
        Toast.makeText(
            context,
            "Selecione a unidade de medida em que o produto será vendido",
            Toast.LENGTH_LONG
        ).show()
    } else {
        viewModel.save()
    }
}

fun cleanForm(viewModel: ProductViewModel, context: Context) {
    viewModel.cleanForm()
}

//fun openCamera(viewModel: ProductViewModel, context: Context) {
//    //navController.navigate("barcode_camera")
//}
