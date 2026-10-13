package com.misinversiones.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.misinversiones.app.data.model.Categories
import com.misinversiones.app.data.model.Investment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInvestmentDialog(
    initialInvestment: Investment? = null,
    onDismiss: () -> Unit,
    onConfirm: (Investment) -> Unit
) {
    var name by remember { mutableStateOf(initialInvestment?.name ?: "") }
    var category by remember { mutableStateOf(initialInvestment?.category ?: "Otros") }
    var link by remember { mutableStateOf(initialInvestment?.link ?: "") }
    var amountText by remember { mutableStateOf(initialInvestment?.amount?.toString() ?: "") }
    var roiText by remember { mutableStateOf(initialInvestment?.roi?.toString() ?: "") }
    var note by remember { mutableStateOf(initialInvestment?.note ?: "") }
    var beneficiary by remember { mutableStateOf(initialInvestment?.beneficiary ?: "") }
    var bankName by remember { mutableStateOf(initialInvestment?.bankName ?: "") }
    var accountNumber by remember { mutableStateOf(initialInvestment?.accountNumber ?: "") }
    var bicSwift by remember { mutableStateOf(initialInvestment?.bicSwift ?: "") }
    var paymentReference by remember { mutableStateOf(initialInvestment?.paymentReference ?: "") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isEditing = initialInvestment != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar Inversión" else "Alta de Inversión") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Inversión *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        Categories.ALL.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Invertido (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = roiText,
                    onValueChange = { roiText = it },
                    label = { Text("Rentabilidad / ROI (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Enlace / Link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Datos de transferencia",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = beneficiary,
                    onValueChange = { beneficiary = it },
                    label = { Text("Beneficiario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Nombre del Banco") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Número de Cuenta / IBAN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bicSwift,
                    onValueChange = { bicSwift = it },
                    label = { Text("BIC / SWIFT") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = paymentReference,
                    onValueChange = { paymentReference = it },
                    label = { Text("Concepto / Referencia de pago") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorMessage = "El nombre no puede estar vacío."
                        return@TextButton
                    }
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val roi = roiText.toDoubleOrNull() ?: 0.0

                    if (amount < 0) {
                        errorMessage = "El importe invertido no puede ser negativo."
                        return@TextButton
                    }

                    val updated = Investment(
                        name = name,
                        category = category,
                        link = link,
                        amount = amount,
                        roi = roi,
                        note = note,
                        beneficiary = beneficiary,
                        bankName = bankName,
                        accountNumber = accountNumber,
                        bicSwift = bicSwift,
                        paymentReference = paymentReference
                    )
                    onConfirm(updated)
                }
            ) {
                Text(if (isEditing) "Guardar" else "Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
