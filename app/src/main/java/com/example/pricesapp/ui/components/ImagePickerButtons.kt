package com.example.pricesapp.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/** Abre la galería o la cámara; el resultado llega a la función pasada a [rememberImagePicker]. */
class ImagePicker internal constructor(
    private val openGallery: () -> Unit,
    private val openCamera: () -> Unit
) {
    fun pickFromGallery() = openGallery()
    fun takePhoto() = openCamera()
}

/**
 * Registra los lanzadores de galería y cámara.
 *
 * Debe llamarse al inicio de la pantalla, antes de cualquier `return` condicional:
 * mientras la cámara está abierta Android puede cerrar la app, y al volver el
 * resultado solo se entrega si el lanzador se vuelve a registrar en la misma posición.
 */
@Composable
fun rememberImagePicker(onImagePicked: (Uri) -> Unit): ImagePicker {
    val context = LocalContext.current
    val currentOnImagePicked by rememberUpdatedState(onImagePicked)
    // rememberSaveable: la ruta de la foto sobrevive si Android reinicia la app
    var pendingPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { currentOnImagePicked(it) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingPhotoUri
        pendingPhotoUri = null
        // Algunas cámaras devuelven false aunque guardaron la foto: se valida el archivo
        if (uri != null && (saved || hasContent(context, uri))) currentOnImagePicked(uri)
    }

    fun launchCamera() {
        val uri = newPhotoUri(context)
        pendingPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    // La app declara CAMERA (escáner), así que la cámara del sistema exige el permiso concedido
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(context, "Se necesita permiso de cámara", Toast.LENGTH_LONG).show()
    }

    return remember(galleryLauncher, cameraLauncher, permissionLauncher) {
        ImagePicker(
            openGallery = { galleryLauncher.launch("image/*") },
            openCamera = {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) launchCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }
}

/** Botones Galería / Cámara. */
@Composable
fun ImagePickerButtons(
    picker: ImagePicker,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = picker::pickFromGallery,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Galería")
        }
        OutlinedButton(
            onClick = picker::takePhoto,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cámara")
        }
    }
}

private fun hasContent(context: Context, uri: Uri): Boolean = try {
    context.contentResolver.openInputStream(uri)?.use { it.read() != -1 } ?: false
} catch (e: Exception) {
    false
}

private fun newPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    // Borra fotos de más de un día: ya se subieron comprimidas
    val dayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
    dir.listFiles()?.filter { it.lastModified() < dayAgo }?.forEach { it.delete() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
