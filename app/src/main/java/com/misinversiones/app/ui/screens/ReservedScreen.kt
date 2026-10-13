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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.misinversiones.app.data.model.Investment
import com.misinversiones.app.ui.theme.DarkCard
import com.misinversiones.app.ui.theme.LossRed
import com.misinversiones.app.ui.theme.PrimaryGreen
import com.misinversiones.app.ui.theme.ProfitGreen
import com.misinversiones.app.ui.theme.TextPrimary
import com.misinversiones.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ReservedScreen(
    reservedInvestments: List<Investment>,
    isOffline: Boolean,
    onMoveToPortfolio: (Int) -> Unit,
    onDeleteReserve: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val indexedList = reservedInvestments.mapIndexed { index, investment -> index to investment }
    val filteredList = indexedList.filter { (_, inv) ->
        searchQuery.isEmpty() ||
                inv.name.contains(searchQuery, ignoreCase = true) ||
                inv.category.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Reservas de Inversión",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Inversiones guardadas temporalmente fuera de la cartera activa.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar en reservas...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay inversiones en la lista de reservas.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(
                    items = filteredList,
                    key = { _, pair -> pair.first }
                ) { _, (originalIndex, item) ->
                    ReservedCard(
                        investment = item,
                        isOffline = isOffline,
                        onMoveToPortfolio = { onMoveToPortfolio(originalIndex) },
                        onDelete = { onDeleteReserve(originalIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReservedCard(
    investment: Investment,
    isOffline: Boolean,
    onMoveToPortfolio: () -> Unit,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = investment.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = investment.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen
                    )
                }

                Row {
                    if (!isOffline) {
                        IconButton(onClick = onMoveToPortfolio) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Mover a Cartera",
                                tint = PrimaryGreen
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = LossRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "Importe: %.2f €", investment.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = String.format(Locale.US, "ROI: %.2f %%", investment.roi),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (investment.roi >= 0) ProfitGreen else LossRed
                )
            }

            if (investment.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nota: ${investment.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
