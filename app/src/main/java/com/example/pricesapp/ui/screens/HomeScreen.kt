package com.example.pricesapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import android.net.Uri
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material.icons.filled.Restore
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pricesapp.ui.components.BarcodeScannerScreen
import com.example.pricesapp.ui.components.CameraPermission
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.pricesapp.ui.components.ErrorSnackbarEffect
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pricesapp.data.Product
import com.example.pricesapp.ui.viewmodel.AuthViewModel
import com.example.pricesapp.ui.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, authViewModel: AuthViewModel, productViewModel: ProductViewModel = viewModel()) {
    val products by productViewModel.products.collectAsState()
    val searchText by productViewModel.searchText.collectAsState()
    val isLoading by productViewModel.isLoading.collectAsState()
    val units by productViewModel.units.collectAsState()
    val errorMessage by productViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ErrorSnackbarEffect(errorMessage, snackbarHostState, productViewModel::clearError)

    var requestCamera by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var isSearchingBarcode by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<BarcodeScanResult?>(null) }
    var showInactive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun deactivate(product: Product) {
        productViewModel.setActive(product, active = false) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = "\"${product.name}\" se movió a desactivados",
                    actionLabel = "Deshacer",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    productViewModel.setActive(product, active = true)
                }
            }
        }
    }
    val isRefreshing by productViewModel.isRefreshing.collectAsState()

    if (requestCamera) {
        CameraPermission(
            onPermissionGranted = {
                requestCamera = false
                showScanner = true
            },
            onPermissionDenied = { requestCamera = false }
        )
    }

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BarcodeScannerScreen(
                onBarcodeScanned = { code ->
                    showScanner = false
                    isSearchingBarcode = true
                    productViewModel.findByBarcode(
                        code,
                        onError = { isSearchingBarcode = false }
                    ) { product ->
                        isSearchingBarcode = false
                        scanResult = BarcodeScanResult(code, product)
                    }
                },
                onClose = { showScanner = false }
            )
        }
    }

    scanResult?.let { result ->
        BarcodeResultDialog(
            result = result,
            onDismiss = { scanResult = null },
            onEdit = { product ->
                scanResult = null
                navController.navigate("edit_product/${product.id}")
            },
            onCreate = { code ->
                scanResult = null
                navController.navigate("add_product?barcode=${Uri.encode(code)}")
            },
            onReactivate = { product ->
                scanResult = null
                productViewModel.setActive(product, active = true)
            }
        )
    }

    LaunchedEffect(Unit) {
        productViewModel.fetchUnits()
        productViewModel.fetchProducts()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchText,
                        onValueChange = productViewModel::onSearchTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar por nombre o código") },
                        singleLine = true
                    )
                },
                actions = {
                    IconButton(onClick = { requestCamera = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Buscar por código de barras")
                    }
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Mi perfil")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showInactive) {
                FloatingActionButton(onClick = { navController.navigate("add_product") }) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar producto")
                }
            }
        }
    ) { padding ->
        val inactiveCount = products.count { !it.active }
        val filteredProducts = products.filter {
            it.active != showInactive && (
                it.name.contains(searchText, ignoreCase = true) ||
                    it.barcode?.contains(searchText.trim()) == true
                )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { productViewModel.fetchProducts(refresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if ((isLoading && products.isEmpty()) || isSearchingBarcode) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Siempre un LazyColumn para que el gesto de refrescar funcione aunque esté vacío
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            FilterChip(
                                selected = !showInactive,
                                onClick = { showInactive = false },
                                label = { Text("Activos (${products.size - inactiveCount})") }
                            )
                            FilterChip(
                                selected = showInactive,
                                onClick = { showInactive = true },
                                label = { Text("Desactivados ($inactiveCount)") }
                            )
                        }
                    }
                    if (filteredProducts.isEmpty()) {
                        item {
                            Text(
                                text = when {
                                    searchText.isNotBlank() -> "Ningún producto coincide con \"$searchText\"."
                                    showInactive -> "No hay productos desactivados."
                                    else -> "No hay productos."
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItem(
                            product = product,
                            unitLabel = units.find { it.id == product.unitId }?.abbreviation,
                            onEditClick = { navController.navigate("edit_product/${product.id}") },
                            onDeleteClick = { deactivate(product) },
                            onReactivateClick = { productViewModel.setActive(product, active = true) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    unitLabel: String?,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReactivateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .alpha(if (product.active) 1f else 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            product.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = product.name,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name)
                Text(text = formatPrice(product.price) + (unitLabel?.let { " / $it" } ?: ""))
            }
            Row(horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                if (product.active) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Desactivar")
                    }
                } else {
                    IconButton(onClick = onReactivateClick) {
                        Icon(Icons.Default.Restore, contentDescription = "Reactivar")
                    }
                }
            }
        }
    }
}

data class BarcodeScanResult(val barcode: String, val product: Product?)

@Composable
fun BarcodeResultDialog(
    result: BarcodeScanResult,
    onDismiss: () -> Unit,
    onEdit: (Product) -> Unit,
    onCreate: (String) -> Unit,
    onReactivate: (Product) -> Unit
) {
    val product = result.product
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product?.name ?: "Producto no encontrado") },
        text = {
            if (product != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    product.imageUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = product.name,
                            modifier = Modifier.size(120.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = formatPrice(product.price),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Código: ${result.barcode}")
                    if (!product.active) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Este producto está desactivado.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text("No hay ningún producto con el código ${result.barcode}.")
            }
        },
        confirmButton = {
            if (product != null && !product.active) {
                TextButton(onClick = { onReactivate(product) }) { Text("Reactivar") }
            } else if (product != null) {
                TextButton(onClick = { onEdit(product) }) { Text("Editar") }
            } else {
                TextButton(onClick = { onCreate(result.barcode) }) { Text("Crear producto") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

fun formatPrice(price: Double): String = "$" + "%.2f".format(java.util.Locale.US, price)
