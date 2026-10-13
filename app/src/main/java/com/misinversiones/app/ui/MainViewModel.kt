package com.misinversiones.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.misinversiones.app.data.auth.AuthManager
import com.misinversiones.app.data.auth.AuthState
import com.misinversiones.app.data.export.ImportExportManager
import com.misinversiones.app.data.model.Bank
import com.misinversiones.app.data.model.Investment
import com.misinversiones.app.data.model.PortfolioDocument
import com.misinversiones.app.data.model.PortfolioPayload
import com.misinversiones.app.data.repository.DuplicateNameException
import com.misinversiones.app.data.repository.OfflineException
import com.misinversiones.app.data.repository.PortfolioRepository
import com.misinversiones.app.data.repository.PortfolioStreamState
import com.misinversiones.app.data.repository.RevisionConflictException
import com.misinversiones.app.data.util.NameUtils
import com.misinversiones.app.data.util.SectionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    CARTERA,
    RESERVAS,
    BANCOS
}

enum class InvestmentSortOption(val label: String) {
    NAME("Nombre"),
    CATEGORY("Categoría"),
    INVESTED_DESC("Invertido (mayor a menor)"),
    ROI_DESC("ROI (mayor a menor)")
}

enum class DistributionMode {
    BY_INVESTMENT,
    BY_CATEGORY
}

enum class BenefitSortOption(val label: String) {
    INVESTED("Invertido"),
    ROI("ROI"),
    PROFIT("Beneficio"),
    ALLOCATION("Asignación (%)")
}

data class UiUiMessage(
    val message: String,
    val isError: Boolean = false,
    val isConflict: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val authManager = AuthManager(application)
    val portfolioRepository = PortfolioRepository(application)

    val authState: StateFlow<AuthState> = authManager.authState

    @OptIn(ExperimentalCoroutinesApi::class)
    val portfolioStream: StateFlow<PortfolioStreamState> = authState
        .flatMapLatest { state ->
            if (state is AuthState.Authenticated) {
                portfolioRepository.getPortfolioStream()
            } else {
                flowOf(PortfolioStreamState())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PortfolioStreamState()
        )

    private val _selectedTab = MutableStateFlow(MainTab.CARTERA)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _investmentSort = MutableStateFlow(InvestmentSortOption.NAME)
    val investmentSort: StateFlow<InvestmentSortOption> = _investmentSort.asStateFlow()

    private val _distributionMode = MutableStateFlow(DistributionMode.BY_CATEGORY)
    val distributionMode: StateFlow<DistributionMode> = _distributionMode.asStateFlow()

    private val _benefitSort = MutableStateFlow(BenefitSortOption.INVESTED)
    val benefitSort: StateFlow<BenefitSortOption> = _benefitSort.asStateFlow()

    private val _uiMessage = MutableStateFlow<UiUiMessage?>(null)
    val uiMessage: StateFlow<UiUiMessage?> = _uiMessage.asStateFlow()

    private val _isTransactionLoading = MutableStateFlow(false)
    val isTransactionLoading: StateFlow<Boolean> = _isTransactionLoading.asStateFlow()

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setInvestmentSort(sort: InvestmentSortOption) {
        _investmentSort.value = sort
    }

    fun setDistributionMode(mode: DistributionMode) {
        _distributionMode.value = mode
    }

    fun setBenefitSort(sort: BenefitSortOption) {
        _benefitSort.value = sort
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun signInWithGoogleToken(idToken: String) {
        authManager.signInWithGoogleIdToken(idToken) { result ->
            if (result.isFailure) {
                _uiMessage.value = UiUiMessage(
                    message = "Error en el inicio de sesión: ${result.exceptionOrNull()?.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    fun signOut() {
        _uiMessage.value = null
        authManager.signOut()
    }

    // --- MUTATION OPERATIONS WITH FIRESTORE TRANSACTIONS ---

    fun addInvestment(investment: Investment, onSuccess: () -> Unit) {
        val currentDoc = portfolioStream.value.document ?: PortfolioDocument()
        val payload = currentDoc.payload

        if (NameUtils.isDuplicateName(investment.name, payload.investments, payload.reservedInvestments)) {
            _uiMessage.value = UiUiMessage(
                message = "Ya existe una inversión o reserva con el mismo nombre (normalizado).",
                isError = true
            )
            return
        }

        executeMutation(currentDoc.revision) { currentPayload ->
            val updatedList = currentPayload.investments + investment
            currentPayload.copy(investments = updatedList)
        } success {
            _uiMessage.value = UiUiMessage("Inversión añadida correctamente.")
            onSuccess()
        }
    }

    fun editInvestment(originalIndex: Int, updatedInvestment: Investment, onSuccess: () -> Unit) {
        val currentDoc = portfolioStream.value.document ?: PortfolioDocument()
        val payload = currentDoc.payload

        if (originalIndex !in payload.investments.indices) return

        if (NameUtils.isDuplicateName(
                name = updatedInvestment.name,
                investments = payload.investments,
                reservedInvestments = payload.reservedInvestments,
                excludeSection = SectionType.PORTFOLIO,
                excludeIndex = originalIndex
            )
        ) {
            _uiMessage.value = UiUiMessage(
                message = "Ya existe otra inversión o reserva con el mismo nombre (normalizado).",
                isError = true
            )
            return
        }

        executeMutation(currentDoc.revision) { currentPayload ->
            val list = currentPayload.investments.toMutableList()
            if (originalIndex in list.indices) {
                list[originalIndex] = updatedInvestment
            }
            currentPayload.copy(investments = list)
        } success {
            _uiMessage.value = UiUiMessage("Inversión actualizada correctamente.")
            onSuccess()
        }
    }

    fun deleteInvestment(originalIndex: Int, moveToReserved: Boolean) {
        val currentDoc = portfolioStream.value.document ?: PortfolioDocument()
        val payload = currentDoc.payload

        if (originalIndex !in payload.investments.indices) return
        val targetItem = payload.investments[originalIndex]

        if (moveToReserved) {
            if (NameUtils.isDuplicateName(
                    name = targetItem.name,
                    investments = payload.investments.filterIndexed { idx, _ -> idx != originalIndex },
                    reservedInvestments = payload.reservedInvestments
                )
            ) {
                _uiMessage.value = UiUiMessage(
                    message = "No se puede mover a reservas: ya existe un elemento con el mismo nombre en reservas.",
                    isError = true
                )
                return
            }
        }

        executeMutation(currentDoc.revision) { currentPayload ->
            val list = currentPayload.investments.toMutableList()
            val itemToMove = list.removeAt(originalIndex)
            val reservedList = if (moveToReserved) {
                currentPayload.reservedInvestments + itemToMove
            } else {
                currentPayload.reservedInvestments
            }
            currentPayload.copy(investments = list, reservedInvestments = reservedList)
        } success {
            val actionText = if (moveToReserved) "movida a reservas" else "eliminada definitivamente"
            _uiMessage.value = UiUiMessage("Inversión $actionText.")
        }
    }

    fun moveReserveToPortfolio(originalIndex: Int) {
        val currentDoc = portfolioStream.value.document ?: PortfolioDocument()
        val payload = currentDoc.payload

        if (originalIndex !in payload.reservedInvestments.indices) return
        val targetItem = payload.reservedInvestments[originalIndex]

        if (NameUtils.isDuplicateName(
                name = targetItem.name,
                investments = payload.investments,
                reservedInvestments = payload.reservedInvestments.filterIndexed { idx, _ -> idx != originalIndex }
            )
        ) {
            _uiMessage.value = UiUiMessage(
                message = "No se puede mover a la cartera: ya existe una inversión con el mismo nombre.",
                isError = true
            )
            return
        }

        executeMutation(currentDoc.revision) { currentPayload ->
            val resList = currentPayload.reservedInvestments.toMutableList()
            val itemToMove = resList.removeAt(originalIndex)
            val invList = currentPayload.investments + itemToMove
            currentPayload.copy(investments = invList, reservedInvestments = resList)
        } success {
            _uiMessage.value = UiUiMessage("Reserva movida a la cartera correctamente.")
        }
    }

    fun deleteReserve(originalIndex: Int) {
        val currentDoc = portfolioStream.value.document ?: PortfolioDocument()
        val payload = currentDoc.payload

        if (originalIndex !in payload.reservedInvestments.indices) return

        executeMutation(currentDoc.revision) { currentPayload ->
            val resList = currentPayload.reservedInvestments.toMutableList()
            resList.removeAt(originalIndex)
            currentPayload.copy(reservedInvestments = resList)
        } success {
            _uiMessage.value = UiUiMessage("Reserva eliminada.")
        }
    }

    fun addBank(bank: Bank, onSuccess: () -> Unit) {
        val currentDoc = portfolioStream.value.document ?: PortfolioDocument()

        if (bank.name.trim().isEmpty()) {
            _uiMessage.value = UiUiMessage("El nombre del banco no puede estar vacío.", isError = true)
            return
        }

        executeMutation(currentDoc.revision) { currentPayload ->
            val updatedBanks = currentPayload.banks + bank
            currentPayload.copy(banks = updatedBanks)
        } success {
            _uiMessage.value = UiUiMessage("Banco añadido correctamente.")
            onSuccess()
        }
    }

    fun deleteBank(originalIndex: Int) {
        val currentDoc = portfolioStream.value.document ?: PortfolioDocument()
        val payload = currentDoc.payload

        if (originalIndex !in payload.banks.indices) return

        executeMutation(currentDoc.revision) { currentPayload ->
            val bankList = currentPayload.banks.toMutableList()
            bankList.removeAt(originalIndex)
            currentPayload.copy(banks = bankList)
        } success {
            _uiMessage.value = UiUiMessage("Banco eliminado.")
        }
    }

    fun importJson(jsonContent: String, onSuccess: () -> Unit) {
        val currentDoc = portfolioStream.value.document ?: PortfolioDocument()

        val parseResult = ImportExportManager.validateAndParseImportJson(jsonContent)
        if (parseResult.isFailure) {
            _uiMessage.value = UiUiMessage(
                message = "Importación rechazada: ${parseResult.exceptionOrNull()?.message}",
                isError = true
            )
            return
        }

        val newPayload = parseResult.getOrThrow()

        executeMutation(currentDoc.revision) {
            newPayload
        } success {
            _uiMessage.value = UiUiMessage("Cartera importada y guardada correctamente en Firestore.")
            onSuccess()
        }
    }

    private fun executeMutation(
        expectedRevision: Long,
        transform: (PortfolioPayload) -> PortfolioPayload
    ): MutationRunner {
        return MutationRunner(expectedRevision, transform)
    }

    inner class MutationRunner(
        private val expectedRevision: Long,
        private val transform: (PortfolioPayload) -> PortfolioPayload
    ) {
        infix fun success(onSuccessAction: () -> Unit) {
            viewModelScope.launch {
                _isTransactionLoading.value = true
                val result = portfolioRepository.updatePortfolioTransaction(expectedRevision, transform)
                _isTransactionLoading.value = false

                if (result.isSuccess) {
                    onSuccessAction()
                } else {
                    val ex = result.exceptionOrNull()
                    when (ex) {
                        is RevisionConflictException -> {
                            _uiMessage.value = UiUiMessage(
                                message = ex.message ?: "Conflicto de revisión. Recarga e reintenta.",
                                isError = true,
                                isConflict = true
                            )
                        }
                        is DuplicateNameException -> {
                            _uiMessage.value = UiUiMessage(
                                message = ex.message ?: "Validación de duplicados fallida.",
                                isError = true
                            )
                        }
                        is OfflineException -> {
                            _uiMessage.value = UiUiMessage(
                                message = ex.message ?: "Modo sin conexión activo.",
                                isError = true
                            )
                        }
                        else -> {
                            _uiMessage.value = UiUiMessage(
                                message = "Error en transacción: ${ex?.localizedMessage ?: "Error desconocido"}",
                                isError = true
                            )
                        }
                    }
                }
            }
        }
    }
}
