package com.example.pricesapp.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricesapp.data.ImageCompressor
import com.example.pricesapp.data.Product
import com.example.pricesapp.data.SupabaseClient
import com.example.pricesapp.data.UnitMeasure
import com.example.pricesapp.data.toUserMessage
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "ProductViewModel"
private const val PRODUCTS = "products"
private const val UNITS = "units_measures"
private const val IMAGES_BUCKET = "product-images"

class ProductViewModel : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    private val _units = MutableStateFlow<List<UnitMeasure>>(emptyList())
    val units = _units.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri = _selectedImageUri.asStateFlow()

    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl = _uploadedImageUrl.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    /** Último error para mostrar en un Snackbar; la UI lo limpia con [clearError]. */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    /** Ejecuta [block]; si falla, registra el error y lo publica en [errorMessage]. */
    private suspend fun <T> runCatchingUser(action: String, block: suspend () -> T): T? {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al $action", e)
            _errorMessage.value = e.toUserMessage(action)
            null
        }
    }

    fun uploadImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isUploading.value = true
            _selectedImageUri.value = uri
            val url = runCatchingUser("subir la imagen") {
                val bytes = withContext(Dispatchers.Default) {
                    ImageCompressor.compress(context.applicationContext, uri)
                }

                val fileName = "product_${System.currentTimeMillis()}.jpg"
                val bucket = SupabaseClient.client.storage.from(IMAGES_BUCKET)
                bucket.upload(path = fileName, data = bytes) {
                    contentType = ContentType.Image.JPEG
                }
                bucket.publicUrl(fileName)
            }
            if (url == null) _selectedImageUri.value = null
            _uploadedImageUrl.value = url
            _isUploading.value = false
        }
    }

    fun clearImageState() {
        _selectedImageUri.value = null
        _uploadedImageUrl.value = null
        _isUploading.value = false
    }

    /** [refresh] = true cuando lo pide el usuario deslizando hacia abajo. */
    fun fetchProducts(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) _isRefreshing.value = true else _isLoading.value = true
            runCatchingUser("cargar los productos") {
                _products.value = SupabaseClient.client.postgrest.from(PRODUCTS)
                    .select { order("name", Order.ASCENDING) }
                    .decodeList<Product>()
            }
            _isLoading.value = false
            _isRefreshing.value = false
        }
    }

    fun fetchUnits() {
        if (_units.value.isNotEmpty()) return
        viewModelScope.launch {
            runCatchingUser("cargar las unidades de medida") {
                _units.value = SupabaseClient.client.postgrest.from(UNITS)
                    .select { order("name", Order.ASCENDING) }
                    .decodeList<UnitMeasure>()
            }
        }
    }

    fun unitById(unitId: Long?): UnitMeasure? =
        unitId?.let { id -> _units.value.find { it.id == id } }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun addProduct(product: Product, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val saved = runCatchingUser("guardar el producto") {
                SupabaseClient.client.postgrest.from(PRODUCTS).insert(product)
            }
            _isSaving.value = false
            if (saved != null) {
                fetchProducts()
                onComplete()
            }
        }
    }

    fun updateProduct(product: Product, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            // Se envían todos los campos explícitamente para que los null borren el valor
            val body = buildJsonObject {
                put("name", product.name)
                put("price", product.price)
                put("image_url", product.imageUrl)
                put("barcode", product.barcode)
                put("unit_id", product.unitId)
            }
            val saved = runCatchingUser("actualizar el producto") {
                SupabaseClient.client.postgrest.from(PRODUCTS).update(body) {
                    filter { Product::id eq product.id }
                }
            }
            _isSaving.value = false
            if (saved != null) {
                fetchProducts()
                onComplete()
            }
        }
    }

    /**
     * Desactiva (active = false) o reactiva un producto. No se borra nada:
     * los desactivados se ven en el filtro "Desactivados" y se pueden reactivar.
     */
    fun setActive(product: Product, active: Boolean, onDone: () -> Unit = {}) {
        val previous = _products.value
        // Cambio inmediato en pantalla; si falla se revierte
        _products.value = previous.map { if (it.id == product.id) it.copy(active = active) else it }
        viewModelScope.launch {
            val action = if (active) "reactivar el producto" else "desactivar el producto"
            val saved = runCatchingUser(action) {
                SupabaseClient.client.postgrest.from(PRODUCTS).update(
                    buildJsonObject { put("active", active) }
                ) {
                    filter { Product::id eq product.id }
                }
            }
            if (saved == null) _products.value = previous else onDone()
        }
    }

    /**
     * Busca un producto por código de barras: primero en la lista cargada y,
     * si no está, consulta Supabase (puede haber sido creado por otro usuario).
     */
    fun findByBarcode(barcode: String, onError: () -> Unit, onResult: (Product?) -> Unit) {
        val code = barcode.trim()
        products.value.find { it.barcode == code }?.let {
            onResult(it)
            return
        }
        viewModelScope.launch {
            val result = runCatchingUser("buscar el código") {
                SupabaseClient.client.postgrest.from(PRODUCTS)
                    .select { filter { Product::barcode eq code } }
                    .decodeList<Product>()
            }
            if (result == null) {
                onError()
                return@launch
            }
            val product = result.firstOrNull()
            if (product != null && products.value.none { it.id == product.id }) {
                _products.value = _products.value + product
            }
            onResult(product)
        }
    }

    /**
     * Trae un producto que no está en memoria (p. ej. Android reinició la app
     * mientras la cámara estaba abierta). [onResult] recibe false si no existe
     * o no se pudo cargar.
     */
    fun loadProduct(productId: String, onResult: (Boolean) -> Unit) {
        if (products.value.any { it.id == productId }) {
            onResult(true)
            return
        }
        viewModelScope.launch {
            val product = runCatchingUser("cargar el producto") {
                SupabaseClient.client.postgrest.from(PRODUCTS)
                    .select { filter { Product::id eq productId } }
                    .decodeSingleOrNull<Product>()
            }
            if (product != null && products.value.none { it.id == product.id }) {
                _products.value = _products.value + product
            }
            onResult(product != null)
        }
    }
}
