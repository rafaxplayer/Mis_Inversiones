package com.misinversiones.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.misinversiones.app.data.auth.AuthState
import com.misinversiones.app.data.model.PortfolioDocument
import com.misinversiones.app.ui.MainTab
import com.misinversiones.app.ui.UiUiMessage
import com.misinversiones.app.ui.theme.DarkBackground
import com.misinversiones.app.ui.theme.DarkSurface
import com.misinversiones.app.ui.theme.LossRed
import com.misinversiones.app.ui.theme.PrimaryGreen
import com.misinversiones.app.ui.theme.TextPrimary
import com.misinversiones.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    authState: AuthState,
    currentTab: MainTab,
    isOffline: Boolean,
    isTransactionLoading: Boolean,
    streamError: String?,
    uiMessage: UiUiMessage?,
    document: PortfolioDocument,
    onTabSelected: (MainTab) -> Unit,
    onClearMessage: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit,
    onSignOut: () -> Unit,
    content: @Composable () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiMessage) {
        if (uiMessage != null) {
            snackbarHostState.showSnackbar(uiMessage.message)
            onClearMessage()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Mis Inversiones",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            if (authState is AuthState.Authenticated) {
                                Text(
                                    text = authState.email,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                    actions = {
                        if (isOffline) {
                            Row(
                                modifier = Modifier
                                    .background(LossRed.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = LossRed
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sin conexión",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LossRed
                                )
                            }
                        }

                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menú", tint = TextPrimary)
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Exportar Cartera (JSON/CSV)") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenExport()
                                }
                            )
                            if (!isOffline) {
                                DropdownMenuItem(
                                    text = { Text("Importar Cartera (JSON)") },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenImport()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Cerrar Sesión") },
                                onClick = {
                                    menuExpanded = false
                                    onSignOut()
                                }
                            )
                        }
                    }
                )

                if (isTransactionLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryGreen
                    )
                }

                if (streamError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = streamError,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (isOffline && streamError == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LossRed)
                            .padding(vertical = 4.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Modo Offline: Datos leídos desde la caché. Las modificaciones están deshabilitadas hasta recuperar la conexión.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = DarkSurface) {
                NavigationBarItem(
                    selected = currentTab == MainTab.CARTERA,
                    onClick = { onTabSelected(MainTab.CARTERA) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Cartera") },
                    label = { Text("Cartera") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.RESERVAS,
                    onClick = { onTabSelected(MainTab.RESERVAS) },
                    icon = { Icon(Icons.Default.Savings, contentDescription = "Reservas") },
                    label = { Text("Reservas") }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.BANCOS,
                    onClick = { onTabSelected(MainTab.BANCOS) },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Bancos") },
                    label = { Text("Bancos") }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                Snackbar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    action = {
                        TextButton(onClick = { snackbarData.dismiss() }) {
                            Text("OK", color = PrimaryGreen)
                        }
                    }
                ) {
                    Text(snackbarData.visuals.message)
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
