package com.listeik.familyapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listeik.familyapp.data.model.ActivityEvent
import com.listeik.familyapp.data.model.FamilyItem
import com.listeik.familyapp.data.model.FamilyMember
import com.listeik.familyapp.data.model.FamilyMessage
import com.listeik.familyapp.data.model.FamilySecurityState
import com.listeik.familyapp.data.model.FamilySession
import com.listeik.familyapp.data.model.ItemCategory
import com.listeik.familyapp.data.repository.FamilyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FamilyUiState(
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val session: FamilySession? = null,
    val securityState: FamilySecurityState? = null,
    val secureInvite: String? = null,
    val members: List<FamilyMember> = emptyList(),
    val items: List<FamilyItem> = emptyList(),
    val events: List<ActivityEvent> = emptyList(),
    val messages: List<FamilyMessage> = emptyList(),
    val errorMessage: String? = null,
)

class FamilyViewModel(
    private val repository: FamilyRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    private val observationJobs = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            runCatching {
                val userId = repository.ensureSignedIn()
                val session = repository.loadSavedSession(userId)
                val securityState = session?.let { repository.getSecurityState(it) }
                session to securityState
            }.onSuccess { (session, securityState) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        securityState = securityState,
                        secureInvite = session
                            ?.takeIf { securityState?.isReady == true }
                            ?.let(repository::getSecureInvite),
                    )
                }
                if (session != null && securityState?.isReady == true) {
                    observeFamily(session)
                }
            }.onFailure(::showFailure)
        }
    }

    fun createFamily(familyName: String, userName: String) {
        if (familyName.isBlank() || userName.isBlank()) {
            showMessage("Введите ваше имя и название семьи")
            return
        }
        runAction {
            val session = repository.createFamily(familyName, userName)
            activateSession(session)
        }
    }

    fun joinFamily(inviteCode: String, userName: String) {
        if (inviteCode.isBlank() || userName.isBlank()) {
            showMessage("Введите ваше имя и код приглашения")
            return
        }
        runAction {
            val session = repository.joinFamily(inviteCode, userName)
            activateSession(session)
        }
    }

    fun enableEncryption() {
        val session = _uiState.value.session ?: return
        runAction {
            repository.enableEncryption(session)
            activateSession(session)
        }
    }

    fun importSecurityKey(secureInvite: String) {
        val session = _uiState.value.session ?: return
        if (secureInvite.isBlank()) {
            showMessage("Вставьте полное защищенное приглашение")
            return
        }
        runAction {
            repository.importSecurityKey(session, secureInvite)
            activateSession(session)
        }
    }

    fun leaveFamily() {
        val session = _uiState.value.session ?: return
        runAction {
            repository.leaveFamily(session)
            observationJobs.forEach(Job::cancel)
            observationJobs.clear()
            _uiState.update {
                FamilyUiState(isLoading = false, isWorking = true)
            }
        }
    }

    fun createItem(title: String, category: ItemCategory, portions: Int?) {
        val session = _uiState.value.session ?: return
        if (title.isBlank()) {
            showMessage("Введите название")
            return
        }
        if (category == ItemCategory.FOOD && (portions == null || portions <= 0)) {
            showMessage("Укажите количество порций")
            return
        }
        runAction { repository.createItem(session, title, category, portions) }
    }

    fun moveItemForward(item: FamilyItem) {
        val session = _uiState.value.session ?: return
        runAction { repository.moveItemForward(session, item) }
    }

    fun adjustFoodPortions(item: FamilyItem, delta: Int) {
        val session = _uiState.value.session ?: return
        if (delta !in setOf(-1, 1)) return
        runAction { repository.adjustFoodPortions(session, item, delta) }
    }

    fun setItemCompleted(item: FamilyItem, completed: Boolean) {
        val session = _uiState.value.session ?: return
        runAction { repository.setItemCompleted(session, item, completed) }
    }

    fun deleteItem(item: FamilyItem) {
        val session = _uiState.value.session ?: return
        runAction { repository.deleteItem(session, item) }
    }

    fun sendMessage(text: String) {
        val session = _uiState.value.session ?: return
        if (text.isBlank()) return
        runAction { repository.sendMessage(session, text) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun activateSession(session: FamilySession) {
        _uiState.update {
            it.copy(
                session = session,
                securityState = FamilySecurityState(
                    isEnabled = true,
                    hasLocalKey = true,
                    canEnable = false,
                ),
                secureInvite = repository.getSecureInvite(session),
            )
        }
        observeFamily(session)
    }

    private fun observeFamily(session: FamilySession) {
        observationJobs.forEach(Job::cancel)
        observationJobs.clear()

        observationJobs += repository.observeItems(session.familyId)
            .onEach { items -> _uiState.update { it.copy(items = items) } }
            .catch { showFailure(it) }
            .launchIn(viewModelScope)
        observationJobs += repository.observeMembers(session.familyId)
            .onEach { members -> _uiState.update { it.copy(members = members) } }
            .catch { showFailure(it) }
            .launchIn(viewModelScope)
        observationJobs += repository.observeEvents(session.familyId)
            .onEach { events -> _uiState.update { it.copy(events = events) } }
            .catch { showFailure(it) }
            .launchIn(viewModelScope)
        observationJobs += repository.observeMessages(session.familyId)
            .onEach { messages -> _uiState.update { it.copy(messages = messages) } }
            .catch { showFailure(it) }
            .launchIn(viewModelScope)
    }

    private fun runAction(action: suspend () -> Unit) {
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            runCatching { action() }
                .onFailure(::showFailure)
            _uiState.update { it.copy(isWorking = false) }
        }
    }

    private fun showFailure(error: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isWorking = false,
                errorMessage = error.localizedMessage ?: "Не удалось выполнить действие",
            )
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    companion object {
        fun factory(repository: FamilyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FamilyViewModel(repository) as T
            }
    }
}
