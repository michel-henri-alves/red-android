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
import com.m4.red_android.data.repository.RetrofitSalesRepository
import com.m4.red_android.data.repository.SalesRepository
import com.m4.red_android.sales.SaleSnapshot
import com.m4.red_android.sales.Money
import com.m4.red_android.sales.Payment
import com.m4.red_android.sales.SaleCalculation
import com.m4.red_android.sales.SaleCalculator
import com.m4.red_android.sales.SaleSubmissionCoordinator
import com.m4.red_android.sales.SaleSubmissionEffect
import com.m4.red_android.sales.SaleSubmissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.DecimalFormat
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID


//@HiltViewModel
class BarcodeViewModel(
    salesRepository: SalesRepository = RetrofitSalesRepository(RetrofitClient.salesApi),
    private val clock: Clock = Clock.system(ZoneId.of("America/Sao_Paulo")),
) : ViewModel() {

    var isBarcodeDetected by mutableStateOf(false)
        private set

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
    var validationError by mutableStateOf<String?>(null)
        private set

    private var _due = mutableStateOf(0.0)
    val due: Double get() = _due.value
    var dueText by mutableStateOf("")
        private set

    private var _showDiscountDialog = mutableStateOf(false)
    val showDiscountDialog: Boolean get() = _showDiscountDialog.value
    private var _showChangeDialog = mutableStateOf(false)
    val showChangeDialog: Boolean get() = _showChangeDialog.value

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> get() = _product

    private var lastScanTime = 0L
    private val scanDelay = 2000L // 1.5s (pode ajustar)
    private val formatter = DecimalFormat("#0.00")

    //sales
    private val _payments = mutableStateListOf<Payment>()

    //mecanismo para retorno para a tela inicial
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val saleSubmissionCoordinator = SaleSubmissionCoordinator(
        repository = salesRepository,
        onConfirmedSuccess = ::resetState,
    )
    val submissionState: StateFlow<SaleSubmissionState> = saleSubmissionCoordinator.state

    sealed class UiEvent {
        object GoBack : UiEvent()
        object SalesFinished : UiEvent()
        data class RemainNotification(val valueReceived: Double, val valueRemain: Double) :
            UiEvent()
//        object RemaingNotification: UiEvent()
    }

    init {
        viewModelScope.launch {
            saleSubmissionCoordinator.effects.collect { effect ->
                when (effect) {
                    is SaleSubmissionEffect.Completed -> {
                        _uiEvent.emit(UiEvent.SalesFinished)
                        _uiEvent.emit(UiEvent.GoBack)
                    }
                }
            }
        }
    }

    val toneGenerator = ToneGenerator(
        AudioManager.STREAM_MUSIC,
        100 // volume (0–100)
    )

    fun addCode(value: String) {
        isBarcodeDetected = true

        val now = System.currentTimeMillis()

        if (now - lastScanTime < scanDelay) return
        lastScanTime = now

        viewModelScope.launch {
            delay(300)
            _codes.add(value)
            fetchProduct(value)
            isBarcodeDetected = false
        }
    }

    private fun fetchProduct(barcode: String) {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        viewModelScope.launch {
            try {
                val result = RetrofitClient.productApi.getProduct(barcode)
                _products.add(0, result)

                _items.add(
                    buildItem(result)
                )

                updateCalculation(currentCalculation())
                _qty.value++
                println(_amount.value)
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    println(_amount.value)
                    _products.add(
                        0, Product(
                            "0", "Produto não encontrado ($barcode)", 0.0, "", "", ""
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
        _due.value = 0.0
        _qty.value = 0
    }

    fun removeProduct(product: Product) {
        _products.remove(product)
        _items.remove(buildItem(product))
        updateCalculation(currentCalculation())
        if (_qty.value > 0 && product.code != "") {
            _qty.value--
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _paymentMethod.value = method
    }

    fun onPaymentAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' || it == ',' }
        paymentAmount = filtered
        dueText = filtered
        validationError = null
    }

    fun paymentAmountAsDouble(): Double {
//        return paymentAmount
//            .replace(",", ".")
//            .toDoubleOrNull() ?: 0.0
        return dueText
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0
    }

    fun applyDiscount(value: Double): Boolean {
        return try {
            val calculation = SaleCalculator.calculate(
                total = cartTotal(),
                discount = Money.fromLegacyDouble(value),
                payments = _payments,
            )
            updateCalculation(calculation)
            dueText = calculation.balance.toLegacyDouble().toString()
            validationError = null
            true
        } catch (_: IllegalArgumentException) {
            validationError = "Informe um desconto válido, de zero até o total da venda."
            false
        }
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
        val payment: Payment
        val calculation: SaleCalculation
        try {
            payment = Payment.fromInput(paymentMethod, dueText)
            calculation = currentCalculation(_payments + payment)
        } catch (_: IllegalArgumentException) {
            validationError = "Selecione a forma de pagamento e informe um valor válido."
            return
        }
        validationError = null
        _payments.add(payment)
        updateCalculation(calculation)

        if (calculation.isComplete && calculation.change == Money.ZERO) {
            saveSale()
        } else if (calculation.isComplete) {
            setShowChangeDialog(true)
        } else {
            dueText = calculation.balance.toLegacyDouble().toString()
            viewModelScope.launch {
                _uiEvent.emit(
                    UiEvent.RemainNotification(
                        valueReceived = calculation.paid.toLegacyDouble(),
                        valueRemain = calculation.balance.toLegacyDouble(),
                    )
                )
            }
        }
    }

    fun saveSale() {
        val snapshot = SaleSnapshot.create(
            submissionId = UUID.randomUUID().toString(),
            code = "1",
            items = _items,
            payments = _payments,
            discount = Money.fromLegacyDouble(discount),
            change = Money.fromLegacyDouble(change),
            vendor = "app",
            realizedAt = OffsetDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )

        viewModelScope.launch {
            saleSubmissionCoordinator.submit(snapshot)
        }
    }

    fun retrySale() {
        viewModelScope.launch {
            saleSubmissionCoordinator.retry()
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
        validationError = null

        _payments.clear()
    }

    fun buildItem(product: Product): Item {
        return Item(
            smartCode = product.smartCode,
            quantity = product.quantity,
            productName = product.name,
            unitOfMeasurement = product.unitOfMeasurement,
            price = product.priceForSale,
            code = product.code
        )
    }

    private fun cartTotal(): Money = _items.fold(Money.ZERO) { total, item ->
        Money.fromCents(
            Math.addExact(total.cents, Money.fromLegacyDouble(item.price).cents),
        )
    }

    private fun currentCalculation(
        payments: List<Payment> = _payments,
    ): SaleCalculation = SaleCalculator.calculate(
        total = cartTotal(),
        discount = Money.fromLegacyDouble(discount),
        payments = payments,
    )

    private fun updateCalculation(calculation: SaleCalculation) {
        _amount.value = calculation.total.toLegacyDouble()
        _discount.value = calculation.discount.toLegacyDouble()
        _paid.value = calculation.paid.toLegacyDouble()
        _due.value = calculation.balance.toLegacyDouble()
        _change.value = calculation.change.toLegacyDouble()
    }
}
