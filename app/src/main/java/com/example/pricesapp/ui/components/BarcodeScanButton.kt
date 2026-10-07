package com.example.pricesapp.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Botón que pide el permiso de cámara al tocarlo y abre el escáner a pantalla completa. */
@Composable
fun BarcodeScanButton(
    onScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var requestCamera by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    if (requestCamera) {
        CameraPermission(
            onPermissionGranted = {
                requestCamera = false
                showScanner = true
            },
            onPermissionDenied = {
                requestCamera = false
                Toast.makeText(context, "Se necesita permiso de cámara", Toast.LENGTH_LONG).show()
            }
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
                    onScanned(code)
                },
                onClose = { showScanner = false }
            )
        }
    }

    OutlinedButton(
        onClick = { requestCamera = true },
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Escanear código de barras")
    }
}
