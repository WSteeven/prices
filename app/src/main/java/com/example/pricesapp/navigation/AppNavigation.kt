package com.example.pricesapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pricesapp.ui.screens.AddProductScreen
import com.example.pricesapp.ui.screens.EditProductScreen
import com.example.pricesapp.ui.screens.HomeScreen
import com.example.pricesapp.ui.screens.LoginScreen
import com.example.pricesapp.ui.screens.ProfileScreen
import com.example.pricesapp.ui.viewmodel.AuthState
import com.example.pricesapp.ui.viewmodel.AuthViewModel
import com.example.pricesapp.ui.viewmodel.ProductViewModel

@Composable
fun AppNavigation(authViewModel: AuthViewModel, productViewModel: ProductViewModel = viewModel()) {
    val authState by authViewModel.authState.collectAsState()
    val user by authViewModel.user.collectAsState()

    when (authState) {
        AuthState.LOADING -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        AuthState.NOT_AUTHENTICATED -> {
            LoginScreen(authViewModel)
            return
        }
        AuthState.AUTHENTICATED -> Unit
    }

    // Un NavController nuevo por usuario: al cambiar de cuenta no queda historial previo
    val navController = key(user?.id) { rememberNavController() }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController, authViewModel, productViewModel)
        }
        composable(
            "add_product?barcode={barcode}",
            arguments = listOf(navArgument("barcode") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            AddProductScreen(
                navController,
                productViewModel,
                initialBarcode = backStackEntry.arguments?.getString("barcode")
            )
        }
        composable(
            "edit_product/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments
                ?.getString("productId")
                ?: return@composable
            EditProductScreen(navController, productId, productViewModel)
        }
        composable("profile") {
            ProfileScreen(navController, authViewModel)
        }
    }
}