package com.example.pricesapp.ui.screens

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
    productViewModel: ProductViewModel = viewModel()
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val imageUri by productViewModel.selectedImageUri.collectAsState()
    val imageUrl by productViewModel.uploadedImageUrl.collectAsState()
    val isUploading by productViewModel.isUploading.collectAsState()


    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            productViewModel.uploadImage(it, context)
        }
    }
    if (!hasCameraPermission) {
        CameraPermission(
            onPermissionGranted = { hasCameraPermission = true },
            onPermissionDenied = {
                Toast.makeText(
                    context,
                    "Permiso de cámara requerido",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Product") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                        label = { Text("Product Name") },
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
                        label = { Text("Price") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar imagen")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    imageUri?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = "Image preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode (Optional)") },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showScanner = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Escanear código de barras")
                    }
                    if (showScanner && hasCameraPermission) {
                        BarcodeScannerScreen(
                            onBarcodeScanned = { scannedCode ->
                                barcode = scannedCode
                                showScanner = false
                            },
                            onClose = { showScanner = false }
                        )
                    }
                    Button(
                        onClick = {
                            val product = Product(
                                name = name,
                                price = price.toDouble(),
                                imageUrl = imageUrl,
                                barcode = barcode.ifBlank { null }
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
                                    (imageUri == null || imageUrl != null),
                    ) {
                        Text("Save Product")
                    }
                }
            }
        }
    }
}
