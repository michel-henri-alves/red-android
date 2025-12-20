package com.m4.red_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.m4.red_android.ui.scanner.Payment
import com.m4.red_android.ui.scanner.ScannerScreen
import com.m4.red_android.viewmodels.BarcodeViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
//                ScannerScreen()
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val barcodeViewModel: BarcodeViewModel = viewModel()


    NavHost(navController = navController, startDestination = "scanner") {

        composable("scanner") {
            ScannerScreen(
                viewModel = barcodeViewModel,
                onNavigate = {
                    navController.navigate("payment")
                }
            )
        }

        composable("payment") {
//            val viewModel: BarcodeViewModel = hilt
            Payment(
                viewModel = barcodeViewModel,
            )
        }
    }
}