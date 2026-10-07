package com.example.pricesapp.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BarcodeScanner"

/**
 * Vista de cámara que lee un código de barras y llama a [onBarcodeScanned] una sola vez.
 * Al salir de la composición libera la cámara, el hilo de análisis y el lector de ML Kit.
 */
@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanned by rememberUpdatedState(onBarcodeScanned)
    val currentOnClose by rememberUpdatedState(onClose)

    // Formatos de productos de tienda + QR: menos formatos = lectura más rápida
    val scanner: BarcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_QR_CODE
                )
                .build()
        )
    }
    val analysisExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val alreadyScanned = remember { AtomicBoolean(false) }
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(lifecycleOwner) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var cameraProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            analyze(imageProxy, scanner, alreadyScanned) { code ->
                                // Volver al hilo principal antes de tocar la UI
                                mainExecutor.execute {
                                    provider.unbindAll()
                                    currentOnScanned(code)
                                }
                            }
                        }
                    }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo iniciar la cámara", e)
                Toast.makeText(context, "No se pudo abrir la cámara", Toast.LENGTH_LONG).show()
                currentOnClose()
            }
        }, mainExecutor)

        onDispose {
            cameraProvider?.unbindAll()
            analysisExecutor.shutdown()
            scanner.close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Text(
            text = "Apunta al código de barras",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        )

        IconButton(
            onClick = onClose,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar")
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun analyze(
    imageProxy: ImageProxy,
    scanner: BarcodeScanner,
    alreadyScanned: AtomicBoolean,
    onCode: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null || alreadyScanned.get()) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    try {
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val code = barcodes.firstNotNullOfOrNull { it.rawValue }
                if (code != null && alreadyScanned.compareAndSet(false, true)) onCode(code)
            }
            .addOnFailureListener { Log.w(TAG, "Fallo al leer el código", it) }
            .addOnCompleteListener { imageProxy.close() }
    } catch (e: Exception) {
        // El lector ya se cerró (el escáner se está cerrando)
        imageProxy.close()
    }
}

@Composable
fun CameraPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onPermissionGranted()
        else onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                onPermissionGranted()
            }
            else -> {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
