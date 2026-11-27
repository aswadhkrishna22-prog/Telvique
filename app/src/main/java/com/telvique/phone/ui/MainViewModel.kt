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
import com.telvique.phone.data.DialerContactMatch
import com.telvique.phone.data.PhoneFormatUtils
import com.telvique.phone.data.PhoneLayoutMode
import com.telvique.phone.data.PhoneRepository
import com.telvique.phone.data.RecentsFilter
import com.telvique.phone.data.SimAccountInfo
import com.telvique.phone.data.T9Matcher
import com.telvique.phone.data.TelviqueSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val dialerInput: String = "",
    val contacts: List<ContactItem> = emptyList(),
    val t9Matches: List<DialerContactMatch> = emptyList(),
    val callLogs: List<CallLogItem> = emptyList(),
    val recentsFilter: RecentsFilter = RecentsFilter.ALL,
    val recentsQuery: String = "",
    val contactsQuery: String = "",
    val simAccounts: List<SimAccountInfo> = emptyList(),
    val settings: TelviqueSettings = TelviqueSettings(),
    val isDefaultDialer: Boolean = false,
    val isCallScreeningAvailable: Boolean = false,
    val isCallScreeningHeld: Boolean = false,
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
    private data class RoleStatus(
        val isDefaultDialer: Boolean = false,
        val isCallScreeningAvailable: Boolean = false,
        val isCallScreeningHeld: Boolean = false
    )

    private val simAccountsFlow = MutableStateFlow<List<SimAccountInfo>>(emptyList())
    private val roleStatusFlow = MutableStateFlow(RoleStatus())
    private val missingPermissionsFlow = MutableStateFlow<List<String>>(emptyList())
    private val pendingSimChoiceNumber = MutableStateFlow<String?>(null)
    private val systemBlockedNumbersFlow = MutableStateFlow<Set<String>>(emptySet())
    private val _navigateToKeypadTrigger = MutableStateFlow(0)
    val navigateToKeypadTrigger: StateFlow<Int> = _navigateToKeypadTrigger.asStateFlow()
    private val _navigateToRecentsTrigger = MutableStateFlow(0)
    val navigateToRecentsTrigger: StateFlow<Int> = _navigateToRecentsTrigger.asStateFlow()

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
        val contactByTail = buildMap {
            for (contact in contacts) {
                for (numEntry in contact.numbers) {
                    val tail = PhoneFormatUtils.comparableTail(numEntry.number)
                    if (tail.isNotEmpty()) {
                        put(tail, contact)
                    }
                }
            }
        }
        val enrichedLogs = logs.map { log ->
            val logTail = PhoneFormatUtils.comparableTail(log.number)
            val matchedContact = if (logTail.isNotEmpty()) contactByTail[logTail] else null
            if (matchedContact != null) {
                log.copy(
                    cachedName = matchedContact.name,
                    photoUri = matchedContact.photoUri ?: log.photoUri,
                    photoFullUri = matchedContact.photoFullUri ?: log.photoFullUri
                )
            } else {
                log
            }
        }
        CoreListsState(input, contacts, enrichedLogs, filter, rQuery)
    }.flowOn(Dispatchers.Default)

    @OptIn(FlowPreview::class)
    private val t9MatchesFlow: Flow<List<DialerContactMatch>> = combine(
        dialerInput
            .debounce { query -> if (query.isBlank()) 0L else 65L }
            .distinctUntilChanged(),
        contactsFlow
    ) { query, contacts ->
        if (query.isBlank()) {
            emptyList()
        } else {
            T9Matcher.rankMatches(contacts, query)
        }
    }.flowOn(Dispatchers.Default)

    private data class AuxiliaryState(
        val contactsQuery: String,
        val simAccounts: List<SimAccountInfo>,
        val settings: TelviqueSettings,
        val roleStatus: RoleStatus,
        val missingPermissions: List<String>
    )

    private val auxiliaryFlow = combine(
        contactsQuery,
        simAccountsFlow,
        settingsRepo.settingsFlow,
        roleStatusFlow,
        missingPermissionsFlow
    ) { cQuery, sims, settings, roleStatus, missing ->
        AuxiliaryState(cQuery, sims, settings, roleStatus, missing)
    }

    val uiState: StateFlow<MainUiState> = combine(
        coreListsFlow,
        t9MatchesFlow,
        auxiliaryFlow,
        pendingSimChoiceNumber,
        systemBlockedNumbersFlow,
    ) { core, t9, aux, simChoiceNumber, systemBlocked ->
        val mergedBlocked = aux.settings.blockedNumbers + systemBlocked
        MainUiState(
            dialerInput = core.dialerInput,
            contacts = core.contacts,
            t9Matches = t9,
            callLogs = core.callLogs,
            recentsFilter = core.recentsFilter,
            recentsQuery = core.recentsQuery,
            contactsQuery = aux.contactsQuery,
            simAccounts = aux.simAccounts,
            settings = aux.settings.copy(blockedNumbers = mergedBlocked),
            isDefaultDialer = aux.roleStatus.isDefaultDialer,
            isCallScreeningAvailable = aux.roleStatus.isCallScreeningAvailable,
            isCallScreeningHeld = aux.roleStatus.isCallScreeningHeld,
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
        val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ctx.getSystemService(RoleManager::class.java)
        } else {
            null
        }
        val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage == ctx.packageName
        }
        val screeningAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }
        val screeningHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }
        roleStatusFlow.value = RoleStatus(
            isDefaultDialer = isDefault,
            isCallScreeningAvailable = screeningAvailable,
            isCallScreeningHeld = screeningHeld
        )

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
            systemBlockedNumbersFlow.value = repo.loadSystemBlockedNumbers()
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

    fun onDialIntent(number: String) {
        dialerInput.value = number
        _navigateToKeypadTrigger.value = _navigateToKeypadTrigger.value + 1
    }

    fun onOpenRecentsIntent() {
        _navigateToRecentsTrigger.value = _navigateToRecentsTrigger.value + 1
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

        // Never delay or intercept emergency calls with SIM chooser or preferred SIM overrides.
        if (repo.isEmergencyNumber(cleaned)) {
            repo.placeCall(cleaned, null)
            return
        }

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

    fun deleteCallLogs(ids: List<Long>) {
        viewModelScope.launch {
            var deletedAny = false
            ids.forEach { id ->
                if (repo.deleteCallLogEntry(id)) deletedAny = true
            }
            if (deletedAny) {
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

    fun setLayoutMode(mode: PhoneLayoutMode) {
        viewModelScope.launch { settingsRepo.setLayoutMode(mode) }
    }

    fun setSpeedDial(digit: String, number: String?) {
        viewModelScope.launch { settingsRepo.setSpeedDial(digit, number) }
    }

    fun setVoicemailNumber(number: String) {
        viewModelScope.launch { settingsRepo.setVoicemailNumber(number) }
    }

    fun blockNumber(number: String) {
        viewModelScope.launch {
            settingsRepo.blockNumber(number)
            repo.addSystemBlockedNumber(number)
            systemBlockedNumbersFlow.value = repo.loadSystemBlockedNumbers()
        }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch {
            settingsRepo.unblockNumber(number)
            repo.removeSystemBlockedNumber(number)
            systemBlockedNumbersFlow.value = repo.loadSystemBlockedNumbers()
        }
    }

    fun updateQuickResponse(index: Int, text: String) {
        viewModelScope.launch { settingsRepo.updateQuickResponse(index, text) }
    }
}
