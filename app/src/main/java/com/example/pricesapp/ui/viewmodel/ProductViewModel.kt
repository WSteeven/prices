package com.example.pricesapp.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricesapp.data.Product
import com.example.pricesapp.data.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri = _selectedImageUri.asStateFlow()

    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl = _uploadedImageUrl.asStateFlow()


    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    fun uploadImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                println("➡️ uploadImage llamado con uri=$uri")

                _isUploading.value = true
                _selectedImageUri.value = uri

                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    println("❌ No se pudo abrir InputStream")
                    return@launch
                }

                val bytes = inputStream.readBytes()
                println("✅ Bytes leídos: ${bytes.size}")

                val fileName = "product_${System.currentTimeMillis()}.jpg"
                println("📁 Subiendo archivo: $fileName")

                SupabaseClient.client.storage
                    .from("product-images")
                    .upload(
                        path = fileName,
                        data = bytes
                    )

                val publicUrl = SupabaseClient.client.storage
                    .from("product-images")
                    .publicUrl(fileName)

                println("🌍 URL pública: $publicUrl")

                _uploadedImageUrl.value = publicUrl

            } catch (e: Exception) {
                println("❌ ERROR uploadImage")
                e.printStackTrace()
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun clearImageState() {
        _selectedImageUri.value = null
        _uploadedImageUrl.value = null
        _isUploading.value = false
    }

    fun fetchProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = SupabaseClient.client.postgrest.from("products").select()
                _products.value = result.decodeList<Product>()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun addProduct(product: Product, onComplete: () -> Unit) {
        viewModelScope.launch {
            SupabaseClient.client.postgrest.from("products").insert(product)
            fetchProducts()
            onComplete()
        }
    }

    fun updateProduct(product: Product, onComplete: () -> Unit) {
        viewModelScope.launch {
            SupabaseClient.client.postgrest.from("products").update(product) { filter {
                Product::id eq product.id
            } }
            fetchProducts()
            onComplete()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            SupabaseClient.client.postgrest.from("products").delete { filter {
                Product::id eq product.id
            } }
            fetchProducts()
        }
    }

    fun getProductById(productId: String): Product? {
        return products.value.find { it.id == productId }
    }
}