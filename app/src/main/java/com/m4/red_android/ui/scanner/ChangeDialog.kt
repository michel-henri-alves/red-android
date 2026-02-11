import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.m4.red_android.viewmodels.BarcodeViewModel

@Composable
fun ChangeDialog(
    onDismiss: () -> Unit,
    viewModel: BarcodeViewModel,
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Devolução de valores")
        },
        text = {
            Column {
                Text("Devolva R$%.2f de troco para o cliente".format(viewModel.change))
                Spacer(Modifier.height(8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveSale()
                }
            ) {
                Text("Devolvido")
            }
        },
    )
}
