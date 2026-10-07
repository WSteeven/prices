package com.example.pricesapp.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.pricesapp.ui.components.ErrorSnackbarEffect
import com.example.pricesapp.ui.components.UnitDropdown
import com.example.pricesapp.ui.components.BarcodeScanButton
import com.example.pricesapp.ui.components.ImagePickerButtons
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pricesapp.ui.viewmodel.ProductViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    navController: NavController,
    productId: String,
    isAdmin: Boolean,
    productViewModel: ProductViewModel = viewModel()
) {
    val context = LocalContext.current
    val product = productViewModel.getProductById(productId)

    val uploadedImageUrl by productViewModel.uploadedImageUrl.collectAsState()
    val isUploading by productViewModel.isUploading.collectAsState()
    val isSaving by productViewModel.isSaving.collectAsState()
    val units by productViewModel.units.collectAsState()
    val errorMessage by productViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ErrorSnackbarEffect(errorMessage, snackbarHostState, productViewModel::clearError)

    LaunchedEffect(Unit) { productViewModel.fetchUnits() }
    DisposableEffect(Unit) { onDispose { productViewModel.clearImageState() } }

    if (product == null) {
        navController.popBackStack()
        return
    }

    var name by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var imageUrl by remember { mutableStateOf(product.imageUrl) }
    var barcode by remember { mutableStateOf(product.barcode ?: "") }
    var unitId by remember { mutableStateOf(product.unitId) }

    // Cuando termina la subida, actualiza la URL
    LaunchedEffect(uploadedImageUrl) {
        uploadedImageUrl?.let {
            imageUrl = it
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Editar producto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // 🔹 Preview imagen
                    imageUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = "Imagen del producto",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    ImagePickerButtons(
                        onImagePicked = { productViewModel.uploadImage(it, context) },
                        enabled = !isUploading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del producto") },
                        enabled = isAdmin,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = price,
                        onValueChange = {
                            if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) price = it
                        },
                        label = { Text("Precio") },
                        enabled = isAdmin,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Código de barras (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BarcodeScanButton(onScanned = { barcode = it })

                    Spacer(modifier = Modifier.height(8.dp))

                    UnitDropdown(
                        units = units,
                        selectedUnitId = unitId,
                        onUnitSelected = { unitId = it },
                        enabled = isAdmin,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val updatedProduct = product.copy(
                                name = name,
                                price = price.toDouble(),
                                imageUrl = imageUrl,
                                barcode = barcode.trim().ifEmpty { null },
                                unitId = unitId
                            )
                            productViewModel.updateProduct(updatedProduct) {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotBlank() && price.isNotBlank() && !isUploading && !isSaving
                    ) {
                        Text("Guardar cambios")
                    }
                }
            }
        }
    }
}
