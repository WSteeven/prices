package com.example.pricesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pricesapp.ui.screens.AddProductScreen
import com.example.pricesapp.ui.screens.EditProductScreen
import com.example.pricesapp.ui.screens.HomeScreen
import com.example.pricesapp.ui.theme.PricesAppTheme
import com.example.pricesapp.ui.viewmodel.ProductViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PricesAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val productViewModel: ProductViewModel = viewModel()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(navController = navController, productViewModel = productViewModel)
                        }
                        composable("add_product") {
                            AddProductScreen(navController = navController, productViewModel = productViewModel)
                        }
                        composable(
                            route = "edit_product/{productId}",
                            arguments = listOf(navArgument("productId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getInt("productId")
                            if (productId != null) {
                                EditProductScreen(
                                    navController = navController,
                                    productId = productId,
                                    productViewModel = productViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
