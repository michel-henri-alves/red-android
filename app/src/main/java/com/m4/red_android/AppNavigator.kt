package com.m4.red_android

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.m4.red_android.ui.scanner.CreateProduct
import com.m4.red_android.ui.scanner.Payment
import com.m4.red_android.ui.scanner.PosScreen
import com.m4.red_android.ui.scanner.ScannerScreen
import com.m4.red_android.viewmodels.BarcodeViewModel
import com.m4.red_android.viewmodels.ProductViewModel

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val barcodeViewModel: BarcodeViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()


    val bottomItems = listOf(
        Screen.Pos,
        Screen.Create
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.Blue,
                contentColor = Color.White
            ) {

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                bottomItems.forEach { screen ->

                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0, 0, 140),
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                        ),
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.Pos -> Icons.Default.PointOfSale
                                    Screen.Create -> Icons.Default.Add
                                    else -> Icons.Default.Home
                                },
                                contentDescription = null,
                                tint = Color.White
                            )
                        },
                        label = {
                            Text(
                                when (screen) {
                                    Screen.Pos -> "Vendas"
                                    Screen.Create -> "Novo produto"
                                    else -> ""
                                },
                                color = Color.White
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Pos.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(Screen.Pos.route) {
                PosScreen(
                    viewModel = barcodeViewModel,
                    onNavigate = {
                        navController.navigate(Screen.Payment.route)
                    }
                )
            }

            composable(Screen.Create.route) {
                CreateProduct(
                    viewModel = productViewModel,
//                    onNavigate = {
//                        navController.navigate(Screen.Scanner.route)
//                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Payment.route) {
                Payment(
                    viewModel = barcodeViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Scanner.route) {
                ScannerScreen(
                    viewModel = productViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

        }
    }
}