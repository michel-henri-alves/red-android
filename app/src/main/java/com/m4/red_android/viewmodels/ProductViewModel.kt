package com.m4.red_android.viewmodels

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4.red_android.data.api.RetrofitClient
import com.m4.red_android.data.enums.UnitOfMeasurement
import com.m4.red_android.data.models.FormField
import com.m4.red_android.data.models.Product1
import com.m4.red_android.data.validation.ProductFormValidation
import com.m4.red_android.data.validation.validateField
import com.m4.red_android.viewmodels.BarcodeViewModel.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class ProductViewModel : ViewModel() {

    var smartCode by mutableStateOf(FormField<String>())
        private set

    var name by mutableStateOf(FormField<String>())
        private set

    var manufacturer by mutableStateOf(FormField<String>())
        private set

    var category by mutableStateOf(FormField<String>())
        private set

    var priceForSale by mutableStateOf(FormField<Double>())
        private set

    var minQuantity by mutableStateOf(FormField<Int>())
        private set

    var actualQuantity by mutableStateOf(FormField<Int>())
        private set

    var saveResult by mutableStateOf<String?>(null)
        private set

    private val _unitOfMeasurement = mutableStateOf<UnitOfMeasurement?>(null)
    val unitOfMeasurement: UnitOfMeasurement? get() = _unitOfMeasurement.value

    var ownManufacture by mutableStateOf(false)
        private set

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        object Success : UiEvent()
        object Error : UiEvent()
    }


    fun onSaveResultChange(input: String) {
        saveResult = input
    }


    fun onSmartCodeChange(input: String) {
        if (input != smartCode.text) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }
        smartCode = smartCode.copy(
            text = input,
            value = input
        )
    }

    private fun validateSmartCode() {
        smartCode = validateField(smartCode) { field ->
            if (field.text.isBlank())
                "Código é obrigatório"
            else
                null
        }
    }


    fun onNameChange(input: String) {
        name = name.copy(
            text = input,
            value = input
        )
    }

    private fun validateName() {
        name = validateField(name) { field ->
            if (field.text.isBlank())
                "Nome é obrigatório"
            else
                null
        }
    }

    fun onManufacturerChange(input: String) {
        manufacturer = manufacturer.copy(
            text = input,
            value = input
        )
    }

    fun onCategoryChange(input: String) {
        category = category.copy(
            text = input,
            value = input
        )
    }

    fun onPriceForSaleChange(value: String) {
        val normalized = value
            .replace(",", ".")
            .filter { it.isDigit() || it == '.' }

        val doubleValue = normalized.toDoubleOrNull()

        priceForSale = priceForSale.copy(
            text = normalized,
            value = doubleValue,
            error = null
        )
    }

    fun validatePriceForSale(): Boolean {
        val errorMessage =
            when {
                priceForSale.text.isBlank() -> "Preço é obrigatório"
                priceForSale.value == null -> "Preço inválido"
//                priceForSale.value!! <= 0.0 -> "Preço deve ser maior que zero"
                else -> null
            }

        priceForSale = priceForSale.copy(error = errorMessage)

        return errorMessage == null
    }

    fun onMinQuantityChange(value: String) {
        val normalized = value
            .filter { it.isDigit() || it == '.' || it == ',' }

        val intValue = normalized.toIntOrNull()

        minQuantity = minQuantity.copy(
            text = normalized,
            value = intValue,
            error = null
        )
    }

    fun onActualQuantityChange(value: String) {
        val normalized = value
            .filter { it.isDigit() || it == '.' || it == ',' }

        val intValue = normalized.toIntOrNull()

        actualQuantity = actualQuantity.copy(
            text = normalized,
            value = intValue,
            error = null
        )
    }

    fun validateActualQuantity(): Boolean {

        val errorMessage =
            when {
                actualQuantity.text.isBlank() -> "Quantidade atual é obrigatório"
                actualQuantity.value == null -> "Quantidade atual inválido"
                actualQuantity.value!! <= 0 -> "Quantidade atual deve ser maior que zero"
                else -> null
            }

        actualQuantity = actualQuantity.copy(error = errorMessage)

        return errorMessage == null
    }

    fun selectUnitOfMeasurement(measure: UnitOfMeasurement) {
        _unitOfMeasurement.value = measure
    }

    fun onOwnManufactureChange(input: Boolean) {
        ownManufacture = input
    }


    fun validateForm(): Boolean {
        validateSmartCode()
        validateName()
        validatePriceForSale()
        validateActualQuantity()

        return smartCode.error == null &&
                name.error == null &&
                priceForSale.error == null &&
                actualQuantity.error == null
    }


    var errors by mutableStateOf(ProductFormValidation())
        private set


    fun save() {
        if (validateForm()) {
            var product = buildProduct()
            postProduct(product)
        }
    }

    fun postProduct(product1: Product1) {
        var message: String? = null;
        viewModelScope.launch {
            try {
                val response = RetrofitClient.productApi.postProduct(product1)

                when {
                    response.isSuccessful -> {
                        when (response.code()) {
                            200, 201 -> {
                                cleanForm()
                                _uiEvent.emit(UiEvent.Success)
                            }
                        }
                    }

                    else -> {
                        when (response.code()) {
                            400 -> _uiEvent.emit(UiEvent.Error)
                            404 -> _uiEvent.emit(UiEvent.Error)
                            500 -> _uiEvent.emit(UiEvent.Error)
                            else -> _uiEvent.emit(UiEvent.Error)
                        }
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
//                message = "Erro ao comunicar com servidor"
            }
        }

//        return message
    }

    fun buildProduct(): Product1 {
        return Product1(
            smartCode.value,
            name.value,
            manufacturer.value,
            category.value,
            ownManufacture,
            priceForSale.value,
            minQuantity.value,
            actualQuantity.value,
            unitOfMeasurement,
            code = "1"
        )
    }

    fun cleanForm() {
        smartCode = FormField()
        name = FormField()
        manufacturer = FormField()
        category = FormField()
        ownManufacture = false
        priceForSale = FormField()
        minQuantity = FormField()
        actualQuantity = FormField()
    }


    val toneGenerator = ToneGenerator(
        AudioManager.STREAM_MUSIC,
        100 // volume (0–100)
    )
}




