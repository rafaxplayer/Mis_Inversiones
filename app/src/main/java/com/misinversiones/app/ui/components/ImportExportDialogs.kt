package com.misinversiones.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.misinversiones.app.data.export.ImportExportManager
import com.misinversiones.app.data.model.PortfolioDocument
import com.misinversiones.app.data.model.PortfolioPayload

@Composable
fun ExportDialog(
    document: PortfolioDocument,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var exportType by remember { mutableStateOf<String?>(null) } // "JSON" or "CSV"
    var exportedText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exportar Cartera") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (exportType == null) {
                    Text("Selecciona el formato de exportación:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            exportType = "JSON"
                            exportedText = ImportExportManager.exportToJson(document.payload, document.revision)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Exportar Copia de Seguridad JSON")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            exportType = "CSV"
                            exportedText = ImportExportManager.exportToCsv(document.payload)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Exportar a CSV (Excel/Tablas)")
                    }
                } else {
                    Text("Resultado de exportación ($exportType):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Export $exportType", exportedText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copiar al Portapapeles")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onConfirmImport: (String) -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var previewPayload by remember { mutableStateOf<PortfolioPayload?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar Cartera (JSON)") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (previewPayload == null) {
                    Text("Pega el contenido JSON de la copia de seguridad. Se validará completamente todo el contenido antes de guardarlo.")
                    Spacer(modifier = Modifier.height(8.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it; errorMessage = null },
                        placeholder = { Text("Coloca tu JSON aquí...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                } else {
                    Text(
                        text = "Confirmar importación",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Resumen del archivo a importar:")
                    Text("• Inversiones en cartera: ${previewPayload!!.investments.size}")
                    Text("• Inversiones en reservas: ${previewPayload!!.reservedInvestments.size}")
                    Text("• Bancos: ${previewPayload!!.banks.size}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ Se reemplazará el contenido actual con una nueva transacción en Firestore.",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            if (previewPayload == null) {
                Button(
                    onClick = {
                        val validation = ImportExportManager.validateAndParseImportJson(jsonInput)
                        if (validation.isSuccess) {
                            previewPayload = validation.getOrThrow()
                            errorMessage = null
                        } else {
                            errorMessage = validation.exceptionOrNull()?.message ?: "Error al validar JSON."
                        }
                    }
                ) {
                    Text("Validar JSON")
                }
            } else {
                Button(
                    onClick = { onConfirmImport(jsonInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirmar y Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
