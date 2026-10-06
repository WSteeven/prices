package com.example.pricesapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pricesapp.ui.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(authViewModel: AuthViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val profile by authViewModel.profile.collectAsState()
        Text("Welcome!")
        profile?.let {
            Text(it.email ?: "")
            Text("Rol: ${it.role}")
        }
        Button(onClick = { authViewModel.signOut() }) {
            Text("Sign Out")
        }
    }
}