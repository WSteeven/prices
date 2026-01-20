package com.example.pricesapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricesapp.data.Product
import com.example.pricesapp.data.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Returning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    fun fetchProducts() {
        viewModelScope.launch {
            val result = SupabaseClient.client.postgrest.from("products").select()
            _products.value = result.decodeList<Product>()
        }
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun addProduct(product: Product, onComplete: () -> Unit) {
        viewModelScope.launch {
            SupabaseClient.client.postgrest.from("products").insert(product)
            fetchProducts() // Refresh products after adding
            onComplete()
        }
    }

    fun updateProduct(product: Product, onComplete: () -> Unit) {
        viewModelScope.launch {
            SupabaseClient.client.postgrest.from("products").update(product) { filter {
                Product::id eq product.id
            } }
            fetchProducts() // Refresh products after updating
            onComplete()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            SupabaseClient.client.postgrest.from("products").delete { filter {
                Product::id eq product.id
            } }
            fetchProducts() // Refresh products after deleting
        }
    }

    fun getProductById(productId: Int): Product? {
        return products.value.find { it.id == productId }
    }
}
