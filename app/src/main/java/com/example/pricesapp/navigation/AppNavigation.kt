package com.example.pricesapp.navigation

import androidx.compose.runtime.Composable
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
import com.example.pricesapp.ui.viewmodel.AuthViewModel
import com.example.pricesapp.ui.viewmodel.ProductViewModel

@Composable
fun AppNavigation(authViewModel: AuthViewModel, productViewModel: ProductViewModel = viewModel()) {
    val navController = rememberNavController()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    NavHost(navController = navController, startDestination = if (isAuthenticated) "home" else "login") {
        composable("login") {
            LoginScreen(authViewModel)
        }
        composable("home") {
            HomeScreen(navController, authViewModel, productViewModel)
        }
        composable("add_product") {
            AddProductScreen(navController, productViewModel)
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
            ProfileScreen(authViewModel)
        }
    }
}