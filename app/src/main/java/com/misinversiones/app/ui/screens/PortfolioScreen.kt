package com.misinversiones.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.misinversiones.app.data.model.Investment
import com.misinversiones.app.data.model.PortfolioPayload
import com.misinversiones.app.ui.DistributionMode
import com.misinversiones.app.ui.InvestmentSortOption
import com.misinversiones.app.ui.theme.DarkCard
import com.misinversiones.app.ui.theme.LossRed
import com.misinversiones.app.ui.theme.PrimaryGreen
import com.misinversiones.app.ui.theme.ProfitGreen
import com.misinversiones.app.ui.theme.TextPrimary
import com.misinversiones.app.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    payload: PortfolioPayload,
    isOffline: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    investmentSort: InvestmentSortOption,
    onInvestmentSortChange: (InvestmentSortOption) -> Unit,
    distributionMode: DistributionMode,
    onDistributionModeChange: (DistributionMode) -> Unit,
    onAddInvestmentClick: () -> Unit,
    onEditInvestmentClick: (Int, Investment) -> Unit,
    onDeleteInvestmentClick: (Int, Boolean) -> Unit
) {
    val context = LocalContext.current

    val totalInvested = payload.investments.sumOf { it.amount }
    val totalProfit = payload.investments.sumOf { it.profit }
    val globalRoi = if (totalInvested > 0) (totalProfit / totalInvested) * 100.0 else 0.0

    // Filter & Sort preserving original index
    val indexedList = payload.investments.mapIndexed { index, investment -> index to investment }
    val filteredList = indexedList.filter { (_, inv) ->
        searchQuery.isEmpty() ||
                inv.name.contains(searchQuery, ignoreCase = true) ||
                inv.category.contains(searchQuery, ignoreCase = true)
    }

    val sortedList = when (investmentSort) {
        InvestmentSortOption.NAME -> filteredList.sortedBy { it.second.name.lowercase() }
        InvestmentSortOption.CATEGORY -> filteredList.sortedBy { it.second.category.lowercase() }
        InvestmentSortOption.INVESTED_DESC -> filteredList.sortedByDescending { it.second.amount }
        InvestmentSortOption.ROI_DESC -> filteredList.sortedByDescending { it.second.roi }
    }

    var deleteDialogTarget by remember { mutableStateOf<Pair<Int, Investment>?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- SUMMARY CARDS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        title = "Total Invertido",
                        value = String.format(Locale.US, "%.2f €", totalInvested),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Beneficio Total",
                        value = String.format(Locale.US, "%.2f €", totalProfit),
                        valueColor = if (totalProfit >= 0) ProfitGreen else LossRed,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Rentabilidad Global",
                        value = String.format(Locale.US, "%.2f %%", globalRoi),
                        valueColor = if (globalRoi >= 0) ProfitGreen else LossRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- DISTRIBUTION SELECTOR & CHART ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Distribución de Cartera",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = distributionMode == DistributionMode.BY_CATEGORY,
                                onClick = { onDistributionModeChange(DistributionMode.BY_CATEGORY) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Por Categoría")
                            }
                            SegmentedButton(
                                selected = distributionMode == DistributionMode.BY_INVESTMENT,
                                onClick = { onDistributionModeChange(DistributionMode.BY_INVESTMENT) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("Por Inversión")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (distributionMode == DistributionMode.BY_CATEGORY) {
                            val categoryGroups = payload.investments
                                .groupBy { it.category }
                                .mapValues { entry -> entry.value.sumOf { it.amount } }

                            categoryGroups.forEach { (category, amount) ->
                                val pct = if (totalInvested > 0) (amount / totalInvested) else 0.0
                                DistributionRow(
                                    label = category,
                                    amount = amount,
                                    percentage = pct
                                )
                            }
                        } else {
                            payload.investments.forEach { inv ->
                                val pct = if (totalInvested > 0) (inv.amount / totalInvested) else 0.0
                                DistributionRow(
                                    label = inv.name,
                                    amount = inv.amount,
                                    percentage = pct
                                )
                            }
                        }
                    }
                }
            }

            // --- SEARCH & SORT CONTROLS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Buscar en la cartera...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        TextButton(onClick = { sortMenuExpanded = true }) {
                            Text("Orden", fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            InvestmentSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        onInvestmentSortChange(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- INVESTMENT LIST ITEMS ---
            if (sortedList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay inversiones registradas o coincidentes.", color = TextSecondary)
                    }
                }
            } else {
                itemsIndexed(
                    items = sortedList,
                    key = { _, pair -> pair.first }
                ) { _, (originalIndex, investment) ->
                    InvestmentCard(
                        investment = investment,
                        isOffline = isOffline,
                        onEdit = { onEditInvestmentClick(originalIndex, investment) },
                        onDelete = { deleteDialogTarget = originalIndex to investment },
                        onOpenLink = { url ->
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    )
                }
            }
        }

        // --- FAB ADD INVESTMENT ---
        if (!isOffline) {
            FloatingActionButton(
                onClick = onAddInvestmentClick,
                containerColor = PrimaryGreen,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Inversión", tint = TextPrimary)
            }
        }
    }

    // --- DELETE / MOVE DIALOG ---
    if (deleteDialogTarget != null) {
        val (origIdx, targetInv) = deleteDialogTarget!!
        AlertDialog(
            onDismissRequest = { deleteDialogTarget = null },
            title = { Text("Eliminar / Mover a reservas") },
            text = { Text("¿Qué deseas hacer con la inversión '${targetInv.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteInvestmentClick(origIdx, true) // move to reserve
                        deleteDialogTarget = null
                    }
                ) {
                    Text("Mover a Reservas")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onDeleteInvestmentClick(origIdx, false) // delete permanently
                            deleteDialogTarget = null
                        }
                    ) {
                        Text("Eliminar Definitivamente", color = LossRed)
                    }
                    TextButton(onClick = { deleteDialogTarget = null }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = DarkCard),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun DistributionRow(
    label: String,
    amount: Double,
    percentage: Double
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(
                text = String.format(Locale.US, "%.2f € (%.1f%%)", amount, percentage * 100.0),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { percentage.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = PrimaryGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun InvestmentCard(
    investment: Investment,
    isOffline: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenLink: (String) -> Unit
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
                    if (investment.link.isNotBlank()) {
                        IconButton(onClick = { onOpenLink(investment.link) }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Abrir enlace", tint = TextSecondary)
                        }
                    }
                    if (!isOffline) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = TextSecondary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = LossRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Invertido", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = String.format(Locale.US, "%.2f €", investment.amount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column {
                    Text("ROI", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = String.format(Locale.US, "%.2f %%", investment.roi),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (investment.roi >= 0) ProfitGreen else LossRed
                    )
                }

                Column {
                    Text("Beneficio", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = String.format(Locale.US, "%.2f €", investment.profit),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (investment.profit >= 0) ProfitGreen else LossRed
                    )
                }
            }

            if (investment.bankName.isNotBlank() || investment.accountNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Transferencia: ${investment.bankName} ${investment.accountNumber}".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
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
