package com.example.pricesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.example.pricesapp.data.AuthRepository
import com.example.pricesapp.data.SupabaseClient
import com.example.pricesapp.navigation.AppNavigation
import com.example.pricesapp.ui.theme.PricesAppTheme
import com.example.pricesapp.ui.viewmodel.AuthViewModel
import com.example.pricesapp.ui.viewmodel.AuthViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authRepository = AuthRepository(SupabaseClient.auth)
        val authViewModelFactory = AuthViewModelFactory(authRepository)
        val authViewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]

        setContent {
            PricesAppTheme {
                AppNavigation(authViewModel = authViewModel)
            }
        }
    }
}