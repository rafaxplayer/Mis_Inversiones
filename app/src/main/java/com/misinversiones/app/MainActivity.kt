package com.misinversiones.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.misinversiones.app.data.auth.AuthState
import com.misinversiones.app.data.model.Investment
import com.misinversiones.app.data.model.PortfolioDocument
import com.misinversiones.app.ui.MainTab
import com.misinversiones.app.ui.MainViewModel
import com.misinversiones.app.ui.components.AddBankDialog
import com.misinversiones.app.ui.components.AddEditInvestmentDialog
import com.misinversiones.app.ui.components.ExportDialog
import com.misinversiones.app.ui.components.ImportDialog
import com.misinversiones.app.ui.components.MainScaffold
import com.misinversiones.app.ui.screens.BanksScreen
import com.misinversiones.app.ui.screens.LoginScreen
import com.misinversiones.app.ui.screens.PortfolioScreen
import com.misinversiones.app.ui.screens.ReservedScreen
import com.misinversiones.app.ui.theme.MisInversionesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MisInversionesTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MainViewModel) {
    val authState by viewModel.authState.collectAsState()
    val streamState by viewModel.portfolioStream.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val investmentSort by viewModel.investmentSort.collectAsState()
    val distributionMode by viewModel.distributionMode.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val isTransactionLoading by viewModel.isTransactionLoading.collectAsState()

    var showAddInvestmentDialog by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<Pair<Int, Investment>?>(null) }
    var showAddBankDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val document = streamState.document ?: PortfolioDocument()
    val hasNetwork = viewModel.portfolioRepository.isNetworkAvailable()
    val isOffline = !hasNetwork

    if (authState !is AuthState.Authenticated) {
        LoginScreen(
            authState = authState,
            onGoogleTokenReceived = { token -> viewModel.signInWithGoogleToken(token) }
        )
    } else {
        MainScaffold(
            authState = authState,
            currentTab = selectedTab,
            isOffline = isOffline,
            isTransactionLoading = isTransactionLoading,
            streamError = streamState.error,
            uiMessage = uiMessage,
            document = document,
            onTabSelected = { tab -> viewModel.selectTab(tab) },
            onClearMessage = { viewModel.clearMessage() },
            onOpenExport = { showExportDialog = true },
            onOpenImport = { showImportDialog = true },
            onSignOut = { viewModel.signOut() }
        ) {
            when (selectedTab) {
                MainTab.CARTERA -> {
                    PortfolioScreen(
                        payload = document.payload,
                        isOffline = isOffline,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                        investmentSort = investmentSort,
                        onInvestmentSortChange = { s -> viewModel.setInvestmentSort(s) },
                        distributionMode = distributionMode,
                        onDistributionModeChange = { d -> viewModel.setDistributionMode(d) },
                        onAddInvestmentClick = { showAddInvestmentDialog = true },
                        onEditInvestmentClick = { idx, inv -> editingInvestment = idx to inv },
                        onDeleteInvestmentClick = { idx, moveToReserved ->
                            viewModel.deleteInvestment(idx, moveToReserved)
                        }
                    )
                }

                MainTab.RESERVAS -> {
                    ReservedScreen(
                        reservedInvestments = document.payload.reservedInvestments,
                        isOffline = isOffline,
                        onMoveToPortfolio = { idx -> viewModel.moveReserveToPortfolio(idx) },
                        onDeleteReserve = { idx -> viewModel.deleteReserve(idx) }
                    )
                }

                MainTab.BANCOS -> {
                    BanksScreen(
                        banks = document.payload.banks,
                        isOffline = isOffline,
                        onAddBankClick = { showAddBankDialog = true },
                        onDeleteBankClick = { idx -> viewModel.deleteBank(idx) }
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showAddInvestmentDialog) {
        AddEditInvestmentDialog(
            initialInvestment = null,
            onDismiss = { showAddInvestmentDialog = false },
            onConfirm = { newInvestment ->
                viewModel.addInvestment(newInvestment) {
                    showAddInvestmentDialog = false
                }
            }
        )
    }

    if (editingInvestment != null) {
        val (index, inv) = editingInvestment!!
        AddEditInvestmentDialog(
            initialInvestment = inv,
            onDismiss = { editingInvestment = null },
            onConfirm = { updatedInvestment ->
                viewModel.editInvestment(index, updatedInvestment) {
                    editingInvestment = null
                }
            }
        )
    }

    if (showAddBankDialog) {
        AddBankDialog(
            onDismiss = { showAddBankDialog = false },
            onConfirm = { newBank ->
                viewModel.addBank(newBank) {
                    showAddBankDialog = false
                }
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            document = document,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onConfirmImport = { jsonString ->
                viewModel.importJson(jsonString) {
                    showImportDialog = false
                }
            }
        )
    }
}
