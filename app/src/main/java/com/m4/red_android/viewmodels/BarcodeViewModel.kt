package com.m4.red_android.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4.red_android.data.api.RetrofitClient
import com.m4.red_android.data.models.Product
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BarcodeViewModel : ViewModel() {

    private val _codes = mutableStateListOf<String>()
    val codes: List<String> get() = _codes
    private val _products = mutableStateListOf<Product>()
    val products: List<Product> get() = _products
    private var _amount = mutableStateOf(0.0)
    val amount: Double get() = _amount.value

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> get() = _product

    private var lastScanTime = 0L
    private val scanDelay = 2000L // 1.5s (pode ajustar)

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

//    suspend fun addProductByCode(barcode: String) {
//        try {
//            val product = RetrofitClient.api.getProduct(barcode) // seu método real
//            _products.add(product)
//        } catch (e: Exception) {
//            Log.e("API", "Erro ao buscar produto", e)
//        }
//    }

    private fun fetchProduct(barcode: String) {
//        println(barcode)
        viewModelScope.launch {
            try {
                val result = RetrofitClient.api.getProduct(barcode)
//                _product.value = result
                _products.add(result)
                result.priceForSale
                _amount.value += result.priceForSale
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
    }
}