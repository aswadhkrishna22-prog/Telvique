package com.telvique.phone.ui

import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.telvique.phone.TelviqueApp
import com.telvique.phone.data.CallLogItem
import com.telvique.phone.data.CallLogType
import com.telvique.phone.data.ContactItem
import com.telvique.phone.data.PhoneRepository
import com.telvique.phone.data.RecentsFilter
import com.telvique.phone.data.SimAccountInfo
import com.telvique.phone.data.T9Matcher
import com.telvique.phone.data.TelviqueSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val dialerInput: String = "",
    val contacts: List<ContactItem> = emptyList(),
    val t9Matches: List<ContactItem> = emptyList(),
    val callLogs: List<CallLogItem> = emptyList(),
    val recentsFilter: RecentsFilter = RecentsFilter.ALL,
    val recentsQuery: String = "",
    val contactsQuery: String = "",
    val simAccounts: List<SimAccountInfo> = emptyList(),
    val settings: TelviqueSettings = TelviqueSettings(),
    val isDefaultDialer: Boolean = false,
    val missingPermissions: List<String> = emptyList(),
    val pendingSimChoiceNumber: String? = null
) {
    val filteredCallLogs: List<CallLogItem>
        get() = callLogs.filter { item ->
            val matchesFilter = when (recentsFilter) {
                RecentsFilter.ALL -> true
                RecentsFilter.MISSED -> item.type == CallLogType.MISSED || item.type == CallLogType.REJECTED
                RecentsFilter.INCOMING -> item.type == CallLogType.INCOMING
                RecentsFilter.OUTGOING -> item.type == CallLogType.OUTGOING
            }
            val q = recentsQuery.trim()
            val matchesQuery = q.isEmpty() ||
                item.displayTitle.contains(q, ignoreCase = true) ||
                item.number.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }

    val filteredContacts: List<ContactItem>
        get() {
            val q = contactsQuery.trim()
            return if (q.isEmpty()) contacts else contacts.filter { T9Matcher.matches(it, q) }
        }

    val favoriteContacts: List<ContactItem>
        get() = contacts.filter { it.isStarred }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = PhoneRepository(application)
    private val settingsRepo = (application as TelviqueApp).settingsRepository

    private val dialerInput = MutableStateFlow("")
    private val contactsFlow = MutableStateFlow<List<ContactItem>>(emptyList())
    private val callLogsFlow = MutableStateFlow<List<CallLogItem>>(emptyList())
    private val recentsFilter = MutableStateFlow(RecentsFilter.ALL)
    private val recentsQuery = MutableStateFlow("")
    private val contactsQuery = MutableStateFlow("")
    private val simAccountsFlow = MutableStateFlow<List<SimAccountInfo>>(emptyList())
    private val isDefaultDialerFlow = MutableStateFlow(false)
    private val missingPermissionsFlow = MutableStateFlow<List<String>>(emptyList())
    private val pendingSimChoiceNumber = MutableStateFlow<String?>(null)

    private data class CoreListsState(
        val dialerInput: String,
        val contacts: List<ContactItem>,
        val callLogs: List<CallLogItem>,
        val recentsFilter: RecentsFilter,
        val recentsQuery: String
    )

    private val coreListsFlow = combine(
        dialerInput,
        contactsFlow,
        callLogsFlow,
        recentsFilter,
        recentsQuery
    ) { input, contacts, logs, filter, rQuery ->
        CoreListsState(input, contacts, logs, filter, rQuery)
    }

    private data class AuxiliaryState(
        val contactsQuery: String,
        val simAccounts: List<SimAccountInfo>,
        val settings: TelviqueSettings,
        val isDefaultDialer: Boolean,
        val missingPermissions: List<String>
    )

    private val auxiliaryFlow = combine(
        contactsQuery,
        simAccountsFlow,
        settingsRepo.settingsFlow,
        isDefaultDialerFlow,
        missingPermissionsFlow
    ) { cQuery, sims, settings, isDefault, missing ->
        AuxiliaryState(cQuery, sims, settings, isDefault, missing)
    }

    val uiState: StateFlow<MainUiState> = combine(
        coreListsFlow,
        auxiliaryFlow,
        pendingSimChoiceNumber
    ) { core, aux, simChoiceNumber ->
        val t9 = if (core.dialerInput.isBlank()) {
            emptyList()
        } else {
            core.contacts.filter { T9Matcher.matches(it, core.dialerInput) }.take(6)
        }
        MainUiState(
            dialerInput = core.dialerInput,
            contacts = core.contacts,
            t9Matches = t9,
            callLogs = core.callLogs,
            recentsFilter = core.recentsFilter,
            recentsQuery = core.recentsQuery,
            contactsQuery = aux.contactsQuery,
            simAccounts = aux.simAccounts,
            settings = aux.settings,
            isDefaultDialer = aux.isDefaultDialer,
            missingPermissions = aux.missingPermissions,
            pendingSimChoiceNumber = simChoiceNumber
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        refreshSystemStatus()
        observeSystemData()
    }

    fun refreshSystemStatus() {
        val ctx = getApplication<Application>()
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ctx.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage == ctx.packageName
        }
        isDefaultDialerFlow.value = isDefault

        val required = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            required.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        missingPermissionsFlow.value = required.filterNot { repo.hasPermission(it) }
        simAccountsFlow.value = repo.getSimAccounts()

        viewModelScope.launch {
            contactsFlow.value = repo.loadContacts()
            callLogsFlow.value = repo.loadCallLogs()
        }
    }

    private fun observeSystemData() {
        viewModelScope.launch {
            repo.observeContactsChanges().collect {
                contactsFlow.value = repo.loadContacts()
            }
        }
        viewModelScope.launch {
            repo.observeCallLogChanges().collect {
                callLogsFlow.value = repo.loadCallLogs()
            }
        }
    }

    fun appendDialDigit(digit: String) {
        dialerInput.value += digit
    }

    fun setDialerNumber(number: String) {
        dialerInput.value = number
    }

    fun backspaceDialDigit() {
        val current = dialerInput.value
        if (current.isNotEmpty()) {
            dialerInput.value = current.dropLast(1)
        }
    }

    fun clearDialInput() {
        dialerInput.value = ""
    }

    fun setRecentsFilter(filter: RecentsFilter) {
        recentsFilter.value = filter
    }

    fun setRecentsQuery(query: String) {
        recentsQuery.value = query
    }

    fun setContactsQuery(query: String) {
        contactsQuery.value = query
    }

    fun callNumber(number: String, forceSimChooser: Boolean = false) {
        val cleaned = number.trim()
        if (cleaned.isEmpty()) return

        val sims = simAccountsFlow.value
        val preferredId = uiState.value.settings.preferredSimId
        val preferredAccount = preferredId?.let { id -> sims.firstOrNull { it.id == id } }

        when {
            forceSimChooser && sims.size > 1 -> pendingSimChoiceNumber.value = cleaned
            preferredAccount != null -> repo.placeCall(cleaned, preferredAccount.handle)
            sims.size > 1 -> pendingSimChoiceNumber.value = cleaned
            else -> repo.placeCall(cleaned, sims.firstOrNull()?.handle)
        }
    }

    fun callWithSim(number: String, handle: PhoneAccountHandle) {
        pendingSimChoiceNumber.value = null
        repo.placeCall(number, handle)
    }

    fun dismissSimChooser() {
        pendingSimChoiceNumber.value = null
    }

    fun deleteCallLog(id: Long) {
        viewModelScope.launch {
            if (repo.deleteCallLogEntry(id)) {
                callLogsFlow.value = repo.loadCallLogs()
            }
        }
    }

    fun clearCallLogs() {
        viewModelScope.launch {
            if (repo.clearAllCallLogs()) {
                callLogsFlow.value = emptyList()
            }
        }
    }

    fun toggleFavorite(contact: ContactItem) {
        viewModelScope.launch {
            if (repo.setContactStarred(contact.id, !contact.isStarred)) {
                contactsFlow.value = repo.loadContacts()
            }
        }
    }

    fun setDialpadTones(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDialpadTones(enabled) }
    }

    fun setDialpadVibrate(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDialpadVibrate(enabled) }
    }

    fun setVibrateOnConnect(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setVibrateOnConnect(enabled) }
    }

    fun setPreferredSimId(simId: String?) {
        viewModelScope.launch { settingsRepo.setPreferredSimId(simId) }
    }

    fun blockNumber(number: String) {
        viewModelScope.launch { settingsRepo.blockNumber(number) }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch { settingsRepo.unblockNumber(number) }
    }

    fun updateQuickResponse(index: Int, text: String) {
        viewModelScope.launch { settingsRepo.updateQuickResponse(index, text) }
    }
}
