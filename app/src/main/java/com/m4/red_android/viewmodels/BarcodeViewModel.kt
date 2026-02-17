package com.m4.red_android.viewmodels

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4.red_android.data.api.RetrofitClient
import com.m4.red_android.data.enums.PaymentMethod
import com.m4.red_android.data.models.Item
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
    private val _items = mutableStateListOf<Item>()
    val items: List<Item> get() = _items
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
    var dueText by mutableStateOf("")
        private set

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
        object SalesFinished : UiEvent()
        data class RemainNotification(val valueReceived: Double, val valueRemain: Double) :
            UiEvent()
//        object RemaingNotification: UiEvent()
    }

    val toneGenerator = ToneGenerator(
        AudioManager.STREAM_MUSIC,
        100 // volume (0–100)
    )

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
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        viewModelScope.launch {
            try {
                val result = RetrofitClient.productApi.getProduct(barcode)
                _products.add(0, result)

                _items.add(
                    buildItem(result.smartCode,1, result.name)
                )

                result.priceForSale
                _amount.value += result.priceForSale
                _due.value = _amount.value
                _qty.value++
                println(_amount.value)
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    println(_amount.value)
                    _products.add(
                        0, Product(
                            "0", "Produto não encontrado ($barcode)", 0.0
                        )
                    )
                } else {
                    e.printStackTrace()
                    _product.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _product.value = null
            }
        }
    }

    fun clearProducts() {
        _products.clear()
        _items.clear()
        _codes.clear()
        _amount.value = 0.0
        _qty.value = 0
    }

    fun removeProduct(product: Product) {
        _products.remove(product)
        _items.remove(buildItem(product.smartCode,1, product.name))
        _amount.value -= product.priceForSale
        _due.value -= product.priceForSale
        _qty.value--
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _paymentMethod.value = method
    }

    fun onPaymentAmountChange(value: String) {
        paymentAmount = value.filter { it.isDigit() || it == '.' }
        dueText = value.filter { it.isDigit() || it == '.' }
    }

    fun paymentAmountAsDouble(): Double {
//        return paymentAmount
//            .replace(",", ".")
//            .toDoubleOrNull() ?: 0.0
        return dueText
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0
    }

    fun applyDiscount(value: Double) {
        _discount.value = value
        _due.value -= value
        dueText = _due.value.toString()

    }

    fun setShowDiscountDialog(value: Boolean) {
        _showDiscountDialog.value = value
    }

    fun setShowChangeDialog(value: Boolean) {
        _showChangeDialog.value = value
    }

    fun setPaymentAmount() {
        paymentAmount = formatter.format(_amount.value);
        dueText = formatter.format(_amount.value);
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
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.SalesFinished)
            }

        } else if (totalWithDiscount < BigDecimal.ZERO) {
            _due.value = 0.0
            _change.value = totalWithDiscount.abs().toDouble()
            setShowChangeDialog(true)

        } else {
            "faltam $totalWithDiscount"
            dueText = _due.value.toString()
            viewModelScope.launch {
                _uiEvent.emit(
                    UiEvent.RemainNotification(
                        valueReceived = _paid.value,
                        valueRemain = _due.value
                    )
                )
            }
        }
    }

    fun LocalDateTime.toApiString(): String =
        this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    fun saveSale() {
        val sale = Sales(
//            paymentMethod = listOf(paymentMethod!!),
//            amountPaid = listOf(paymentAmountAsDouble()),
            code = "1",
            items = _items as List<Item>,
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
        _items.clear()

        _amount.value = 0.0
        _paid.value = 0.0
        _qty.value = 0
        _paymentMethod.value = null

        paymentAmount = ""
        dueText = ""

        _showChangeDialog.value = false
        _discount.value = 0.0
        _change.value = 0.0
        _due.value = 0.0

        _paymentMethodList?.clear()
        _amountPayedList?.clear()
    }

    fun buildItem(smartCode: String, quantity: Int, name: String): Item {
        return Item(
            smartCode = smartCode,
            quantity = quantity,
            name
        )
    }

    private fun Double.toMoney(): BigDecimal =
        BigDecimal(this).setScale(2, RoundingMode.HALF_EVEN)
}