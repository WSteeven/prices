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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import android.net.Uri
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
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
    val isAdmin by authViewModel.isAdmin.collectAsState()

    var requestCamera by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var isSearchingBarcode by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<BarcodeScanResult?>(null) }

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
                    productViewModel.findByBarcode(code) { product ->
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
            isAdmin = isAdmin,
            onDismiss = { scanResult = null },
            onEdit = { product ->
                scanResult = null
                navController.navigate("edit_product/${product.id}")
            },
            onCreate = { code ->
                scanResult = null
                navController.navigate("add_product?barcode=${Uri.encode(code)}")
            }
        )
    }

    LaunchedEffect(Unit) {
        productViewModel.fetchProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchText,
                        onValueChange = productViewModel::onSearchTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search products") },
                        singleLine = true
                    )
                },
                actions = {
                    IconButton(onClick = { requestCamera = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Buscar por código de barras")
                    }
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { navController.navigate("add_product") }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Product")
                }
            }
        }
    ) { padding ->
        val filteredProducts = products.filter {
            it.name.contains(searchText, ignoreCase = true) ||
                it.barcode?.contains(searchText.trim()) == true
        }

        if (isLoading || isSearchingBarcode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No products found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductItem(
                        product = product,
                        onEditClick = { navController.navigate("edit_product/${product.id}") },
                        onDeleteClick = if (isAdmin) {
                            { productViewModel.deleteProduct(product) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    onEditClick: () -> Unit,
    onDeleteClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
                Text(text = "$${product.price}")
            }
            Row(horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                onDeleteClick?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onEdit: (Product) -> Unit,
    onCreate: (String) -> Unit
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
                        text = "$${"%.2f".format(product.price)}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Código: ${result.barcode}")
                }
            } else {
                Text("No hay ningún producto con el código ${result.barcode}.")
            }
        },
        confirmButton = {
            when {
                product != null -> TextButton(onClick = { onEdit(product) }) { Text("Editar") }
                isAdmin -> TextButton(onClick = { onCreate(result.barcode) }) { Text("Crear producto") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
