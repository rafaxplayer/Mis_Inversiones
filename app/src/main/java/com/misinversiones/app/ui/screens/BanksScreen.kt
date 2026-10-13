package com.misinversiones.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.misinversiones.app.data.model.Bank
import com.misinversiones.app.ui.theme.DarkCard
import com.misinversiones.app.ui.theme.LossRed
import com.misinversiones.app.ui.theme.PrimaryGreen
import com.misinversiones.app.ui.theme.TextPrimary
import com.misinversiones.app.ui.theme.TextSecondary

@Composable
fun BanksScreen(
    banks: List<Bank>,
    isOffline: Boolean,
    onAddBankClick: () -> Unit,
    onDeleteBankClick: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Cuentas Bancarias",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Directorio de entidades y cuentas para referencias de transferencia.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (banks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay bancos registrados.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(banks) { index, bank ->
                        BankCard(
                            bank = bank,
                            isOffline = isOffline,
                            onDelete = { onDeleteBankClick(index) }
                        )
                    }
                }
            }
        }

        if (!isOffline) {
            FloatingActionButton(
                onClick = onAddBankClick,
                containerColor = PrimaryGreen,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Banco", tint = TextPrimary)
            }
        }
    }
}

@Composable
private fun BankCard(
    bank: Bank,
    isOffline: Boolean,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bank.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (!isOffline) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar Banco", tint = LossRed)
                    }
                }
            }

            if (bank.iban.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "IBAN: ${bank.iban}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (bank.bicSwift.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "BIC/SWIFT: ${bank.bicSwift}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (bank.holder.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Titular: ${bank.holder}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
