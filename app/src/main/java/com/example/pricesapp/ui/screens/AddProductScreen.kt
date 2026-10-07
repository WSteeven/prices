package com.example.pricesapp.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.pricesapp.ui.components.ErrorSnackbarEffect
import com.example.pricesapp.ui.components.UnitDropdown
import com.example.pricesapp.ui.components.BarcodeScanButton
import com.example.pricesapp.ui.components.ImagePickerButtons
import com.example.pricesapp.ui.components.rememberImagePicker
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
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
import com.example.pricesapp.ui.components.BarcodeScannerScreen
import com.example.pricesapp.ui.components.CameraPermission
import com.example.pricesapp.ui.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productViewModel: ProductViewModel = viewModel(),
    initialBarcode: String? = null
) {
    val context = LocalContext.current

    // rememberSaveable: lo escrito sobrevive si Android reinicia la app con la cámara abierta
    var name by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf(initialBarcode.orEmpty()) }

    // Antes de cualquier otro contenido: así recibe la foto aunque la app se haya reiniciado
    val imagePicker = rememberImagePicker { productViewModel.uploadImage(it, context) }

    val imageUri by productViewModel.selectedImageUri.collectAsState()
    val imageUrl by productViewModel.uploadedImageUrl.collectAsState()
    val isUploading by productViewModel.isUploading.collectAsState()
    val isSaving by productViewModel.isSaving.collectAsState()
    val units by productViewModel.units.collectAsState()
    var unitId by rememberSaveable { mutableStateOf<Long?>(null) }
    val errorMessage by productViewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    ErrorSnackbarEffect(errorMessage, snackbarHostState, productViewModel::clearError)

    LaunchedEffect(Unit) { productViewModel.fetchUnits() }
    DisposableEffect(Unit) { onDispose { productViewModel.clearImageState() } }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nuevo producto") },
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
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del producto") },
                        leadingIcon = { Icon(Icons.Default.Create, null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = price,
                        onValueChange = {
                            if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                price = it
                            }
                        },
                        label = { Text("Precio") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    UnitDropdown(
                        units = units,
                        selectedUnitId = unitId,
                        onUnitSelected = { unitId = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ImagePickerButtons(picker = imagePicker, enabled = !isUploading)

                    Spacer(modifier = Modifier.height(8.dp))

                    imageUri?.let {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = it,
                                contentDescription = "Vista previa",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                            if (isUploading) CircularProgressIndicator()
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Código de barras (opcional)") },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    BarcodeScanButton(onScanned = { barcode = it })

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val product = Product(
                                name = name.trim(),
                                price = price.toDouble(),
                                imageUrl = imageUrl,
                                barcode = barcode.trim().ifBlank { null },
                                unitId = unitId
                            )
                            productViewModel.addProduct(product) {
                                productViewModel.clearImageState()
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled =
                            name.isNotBlank() &&
                                    price.isNotBlank() &&
                                    !isUploading &&
                                    !isSaving &&
                                    (imageUri == null || imageUrl != null),
                    ) {
                        Text("Guardar producto")
                    }
                }
            }
        }
    }
}
