package com.m4.red_android.viewmodels

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4.red_android.data.api.RetrofitClient
import com.m4.red_android.data.enums.PaymentMethod
import com.m4.red_android.data.models.Product
import com.m4.red_android.data.models.Sales
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


//@HiltViewModel
class BarcodeViewModel : ViewModel() {

    private val _codes = mutableStateListOf<String>()
    val codes: List<String> get() = _codes
    private val _products = mutableStateListOf<Product>()
    val products: List<Product> get() = _products
    private var _amount = mutableStateOf(0.0)
    val amount: Double get() = _amount.value
    private var _qty = mutableStateOf(0)
    val qty: Int get() = _qty.value
    private val _paymentMethod = mutableStateOf<PaymentMethod?>(null)
    val paymentMethod: PaymentMethod? get() = _paymentMethod.value
    var paymentAmount by mutableStateOf("")
        private set
    private var _paid = mutableStateOf(0.0)
    val paid: Double get() = _paid.value
    private var _discount = mutableStateOf(0.0)
    val discount: Double get() = _discount.value
    private var _change = mutableStateOf(0.0)
    val change: Double get() = _change.value

    private var _due = mutableStateOf(0.0)
    val due: Double get() = _due.value
    val dueText: String
        get() = String.format("%.2f", _due.value)

    fun updateDue(value: Double) {
        _due.value = value
    }

    private var _showDiscountDialog = mutableStateOf(false)
    val showDiscountDialog: Boolean get() = _showDiscountDialog.value
    private var _showChangeDialog = mutableStateOf(false)
    val showChangeDialog: Boolean get() = _showChangeDialog.value

    private val _sales = mutableStateOf<Sales?>(null)
    val sales: Sales? get() = _sales.value

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> get() = _product

    private var lastScanTime = 0L
    private val scanDelay = 2000L // 1.5s (pode ajustar)
    private val formatter = DecimalFormat("#0.00")

    //sales
    private var _paymentMethodList: MutableList<PaymentMethod?>? = mutableStateListOf()
    private var _amountPayedList: MutableList<Double?>? = mutableStateListOf()
//    private var _amountPayedList: MutableList<String?>? = mutableStateListOf()

    //mecanismo para retorno para a tela inicial
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        object GoBack : UiEvent()
    }

    fun onPaymentFinishedSuccessfully() {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.GoBack)
        }
    }

    fun addCode(value: String) {
        val now = System.currentTimeMillis()

        if (now - lastScanTime < scanDelay) return
        lastScanTime = now

        viewModelScope.launch {
            delay(300)
            _codes.add(value)
            fetchProduct(value)
        }
    }

    private fun fetchProduct(barcode: String) {
        viewModelScope.launch {
            try {
                val result = RetrofitClient.productApi.getProduct(barcode)
                _products.add(result)
                result.priceForSale
                _amount.value += result.priceForSale
                _due.value = _amount.value
                _qty.value++
                println(_amount.value)
            } catch (e: Exception) {
                e.printStackTrace()
                _product.value = null
            }
        }
    }

    fun clearProducts() {
        _products.clear()
        _codes.clear()
        _amount.value = 0.0
        _qty.value = 0
    }

    fun removeProduct(product: Product) {
        _products.remove(product)
        _amount.value -= product.priceForSale
        _qty.value--
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _paymentMethod.value = method
    }

    fun onPaymentAmountChange(value: String) {

        paymentAmount = value.filter { it.isDigit() || it == '.' }
    }

    fun paymentAmountAsDouble(): Double {
        return paymentAmount
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0
    }

    fun applyDiscount(value: Double) {
        _discount.value = value
        _due.value -= value

    }

    fun setShowDiscountDialog(value: Boolean) {
        _showDiscountDialog.value = value
    }

    fun setShowChangeDialog(value: Boolean) {
        _showChangeDialog.value = value
    }

    fun setPaymentAmount() {
        paymentAmount = formatter.format(_amount.value);
    }

    fun finalizePayment() {
        _paymentMethodList?.add(paymentMethod)
        _amountPayedList?.add(paymentAmountAsDouble())

        val totalWithDiscount =
            due.toMoney()
                .subtract(paymentAmountAsDouble().toMoney())

        _due.value = totalWithDiscount.toDouble()
        _paid.value += paymentAmountAsDouble()


        if (totalWithDiscount.compareTo(BigDecimal.ZERO) == 0) {
           saveSale()

        } else if (totalWithDiscount < BigDecimal.ZERO) {
            _due.value = 0.0
            _change.value = totalWithDiscount.abs().toDouble()
            setShowChangeDialog(true)

        }
//        else {
//            "faltam $totalWithDiscount"
//        }
    }

    fun LocalDateTime.toApiString(): String =
        this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    fun saveSale() {
        val sale = Sales(
//            paymentMethod = listOf(paymentMethod!!),
//            amountPaid = listOf(paymentAmountAsDouble()),
            paymentMethod = _paymentMethodList as List<PaymentMethod>,
            amountPaid = _amountPayedList,
            discount = discount,
            change = change,
            vendor = "app",
            realizedAt = LocalDateTime.now().toApiString()
        )

        postSale(sale)

        resetState()
        onPaymentFinishedSuccessfully()
    }

    fun postSale(sales: Sales) {
        println(sales)
        viewModelScope.launch {
            try {
                RetrofitClient.salesApi.postSales(sales)
                clearProducts()
                println("Venda registrada com sucesso")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetState() {
        _codes.clear()
        _products.clear()

        _amount.value = 0.0
        _paid.value = 0.0
        _qty.value = 0
        _paymentMethod.value = null

        paymentAmount = ""

        _showChangeDialog.value = false
        _discount.value = 0.0
        _change.value = 0.0
        _due.value = 0.0

        _paymentMethodList?.clear()
        _amountPayedList?.clear()
    }

    private fun Double.toMoney(): BigDecimal =
        BigDecimal(this).setScale(2, RoundingMode.HALF_EVEN)
}