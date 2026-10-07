package com.example.pricesapp.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** Muestra [message] en el Snackbar y avisa con [onShown] para limpiarlo del ViewModel. */
@Composable
fun ErrorSnackbarEffect(
    message: String?,
    hostState: SnackbarHostState,
    onShown: () -> Unit
) {
    LaunchedEffect(message) {
        if (message != null) {
            onShown()
            hostState.showSnackbar(message)
        }
    }
}
