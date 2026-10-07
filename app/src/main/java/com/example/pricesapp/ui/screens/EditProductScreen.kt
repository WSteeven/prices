package com.example.pricesapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.pricesapp.data.Product
import com.example.pricesapp.ui.components.BarcodeScanButton
import com.example.pricesapp.ui.components.ErrorSnackbarEffect
import com.example.pricesapp.ui.components.ImagePicker
import com.example.pricesapp.ui.components.ImagePickerButtons
import com.example.pricesapp.ui.components.UnitDropdown
import com.example.pricesapp.ui.components.rememberImagePicker
import com.example.pricesapp.ui.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    navController: NavController,
    productId: String,
    productViewModel: ProductViewModel = viewModel()
) {
    val context = LocalContext.current
    val products by productViewModel.products.collectAsState()
    val product = products.find { it.id == productId }
    val errorMessage by productViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Antes de cualquier return: así recibe la foto aunque Android haya reiniciado la app
    val imagePicker = rememberImagePicker { productViewModel.uploadImage(it, context) }

    ErrorSnackbarEffect(errorMessage, snackbarHostState, productViewModel::clearError)

    LaunchedEffect(Unit) { productViewModel.fetchUnits() }
    DisposableEffect(Unit) { onDispose { productViewModel.clearImageState() } }

    // Si la app se reinició, la lista en memoria está vacía: se carga el producto
    LaunchedEffect(productId) {
        productViewModel.loadProduct(productId) { found ->
            if (!found) navController.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Editar producto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            EditProductForm(
                product = product,
                imagePicker = imagePicker,
                productViewModel = productViewModel,
                onSaved = { navController.popBackStack() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun EditProductForm(
    product: Product,
    imagePicker: ImagePicker,
    productViewModel: ProductViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedImageUri by productViewModel.selectedImageUri.collectAsState()
    val uploadedImageUrl by productViewModel.uploadedImageUrl.collectAsState()
    val isUploading by productViewModel.isUploading.collectAsState()
    val isSaving by productViewModel.isSaving.collectAsState()
    val units by productViewModel.units.collectAsState()

    // rememberSaveable: los cambios sin guardar sobreviven a la cámara y a girar la pantalla
    var name by rememberSaveable(product.id) { mutableStateOf(product.name) }
    var price by rememberSaveable(product.id) { mutableStateOf(product.price.toString()) }
    var imageUrl by rememberSaveable(product.id) { mutableStateOf(product.imageUrl) }
    var barcode by rememberSaveable(product.id) { mutableStateOf(product.barcode ?: "") }
    var unitId by rememberSaveable(product.id) { mutableStateOf(product.unitId) }

    // Cuando termina la subida, la nueva URL reemplaza a la anterior
    LaunchedEffect(uploadedImageUrl) {
        uploadedImageUrl?.let { imageUrl = it }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
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

                // Mientras sube se muestra la foto local; después, la URL guardada
                val preview: Any? = if (isUploading) selectedImageUri else imageUrl
                preview?.let {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = it,
                            contentDescription = "Imagen del producto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        if (isUploading) CircularProgressIndicator()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                ImagePickerButtons(picker = imagePicker, enabled = !isUploading)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del producto") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) price = it
                    },
                    label = { Text("Precio") },
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val updatedProduct = product.copy(
                            name = name.trim(),
                            price = price.toDouble(),
                            imageUrl = imageUrl,
                            barcode = barcode.trim().ifEmpty { null },
                            unitId = unitId
                        )
                        productViewModel.updateProduct(updatedProduct, onSaved)
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
