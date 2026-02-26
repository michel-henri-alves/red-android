import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.BarcodeViewModel

@Composable
fun ActionButtons(
    onClick1: () -> Unit = {},
//    onClick2: () -> Unit = {},
    onClick2: () -> Unit = {},
//    onClick4: () -> Unit = {},
    viewModel: BarcodeViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
//            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onClick1,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(228, 88, 88),
                contentColor = Color.White
            )
        ) { Text("Vender") }
//        Button(onClick = onClick2) { Text("Buscar") }
        Button(
            onClick = onClick2,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(228, 88, 88),
                contentColor = Color.White
            )
        ) { Text("Limpar") }
    }
}