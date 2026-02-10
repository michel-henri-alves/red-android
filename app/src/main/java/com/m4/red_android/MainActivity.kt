package com.m4.red_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
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
                checkCameraPermissionAllowed()
//                ScannerScreen()
                AppNavigator()
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED  
    }

    private fun checkCameraPermissionAllowed() {
        if (!hasCameraPermission()) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                showCameraPermissionDeniedMessage()
            }
        }

    private fun showCameraPermissionDeniedMessage() {
        Toast.makeText(
            this,
            "Permissão da câmera é necessária para escanear códigos de barras",
            Toast.LENGTH_LONG
        ).show()
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
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}