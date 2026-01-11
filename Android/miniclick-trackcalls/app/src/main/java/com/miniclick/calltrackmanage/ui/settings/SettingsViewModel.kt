package com.miniclick.calltrackmanage.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.widget.Toast
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import android.net.Uri
import java.io.OutputStreamWriter
import java.io.InputStreamReader
import com.miniclick.calltrackmanage.ui.settings.viewmodel.*
import com.miniclick.calltrackmanage.util.system.WhatsAppUtils
import com.miniclick.calltrackmanage.util.system.LogExporter

data class PermissionState(
    val name: String,
    val isGranted: Boolean,
    val permission: String
)

data class SimInfo(
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val subscriptionId: Int
)

data class AppInfo(
    val label: String,
    val packageName: String
)

data class SettingsUiState(
    val simSelection: String = "Off", // "Both", "Sim1", "Sim2", "Off"
    val trackStartDate: Long = 0L,
    val isSyncing: Boolean = false,
    val recordingPath: String = "",
    val pairingCode: String = "", // Format: ORGID-USERID
    val callerPhoneSim1: String = "",
    val callerPhoneSim2: String = "",
    val permissions: List<PermissionState> = emptyList(),
    val availableSims: List<SimInfo> = emptyList(),
    val excludedPersons: List<com.miniclick.calltrackmanage.data.db.PersonDataEntity> = emptyList(),
    val lastSyncStats: String? = null,
    val lastSyncTime: Long = 0L,
    val whatsappPreference: String = "Always Ask",
    val availableWhatsappApps: List<AppInfo> = emptyList(),
    val isVerifying: Boolean = false,
    val verificationStatus: String? = null, // null, "verified", "failed"
    val verifiedOrgName: String? = null,
    val verifiedEmployeeName: String? = null,
    val themeMode: String = "System", // "System", "Light", "Dark"
    val sim1SubId: Int? = null,
    val sim2SubId: Int? = null,
    val sim1CalibrationHint: String? = null,
    val sim2CalibrationHint: String? = null,
    val pendingNewCallsCount: Int = 0,
    val pendingMetadataUpdatesCount: Int = 0,
    val pendingPersonUpdatesCount: Int = 0,
    val pendingRecordingCount: Int = 0,
    val activeRecordings: List<com.miniclick.calltrackmanage.data.db.CallDataEntity> = emptyList(),
    val allowPersonalExclusion: Boolean = false,
    val allowChangingTrackingStartDate: Boolean = false,
    val allowUpdatingTrackingSims: Boolean = false,
    val defaultTrackingStartingDate: String? = null,
    val recordingCount: Int = 0,
    val isRecordingPathVerified: Boolean = false,
    val isRecordingPathCustom: Boolean = false,
    val callerIdEnabled: Boolean = false,
    val customLookupUrl: String = "",
    val customLookupEnabled: Boolean = false,
    val customLookupResponse: String? = null,
    val isFetchingCustomLookup: Boolean = false,
    val customLookupCallerIdEnabled: Boolean = false,
    val isRawView: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val callTrackEnabled: Boolean = true,
    val callRecordEnabled: Boolean = true,
    val planExpiryDate: String? = null,
    val allowedStorageGb: Float = 0f,
    val storageUsedBytes: Long = 0L,
    val userDeclinedRecording: Boolean = false,
    val isDialerEnabled: Boolean = true,
    val showRecordingEnablementDialog: Boolean = false,
    val showRecordingDisablementDialog: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val isSyncSetup: Boolean = false,
    val showSyncQueue: Boolean = false,
    val showRecordingQueue: Boolean = false,
    val showRecordingReminder: Boolean = true,
    val showUnknownNoteReminder: Boolean = true,
    val isGoogleDialer: Boolean = false,
    val showDialButton: Boolean = false,
    val callActionBehavior: String = "Direct",
    val isSystemDefaultDialer: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val isReattaching: Boolean = false,
    val reAttachProgress: String? = null,
    
    // Unified Modal States
    val showPermissionsModal: Boolean = false,
    val showCloudSyncModal: Boolean = false,
    val showAccountInfoModal: Boolean = false,
    val showExcludedModal: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val showContactModal: Boolean = false,
    val showCreateOrgModal: Boolean = false,
    val showJoinOrgModal: Boolean = false,
    val showTrackSimModal: Boolean = false,
    val showResetConfirmDialog: Boolean = false,
    val showCustomLookupModal: Boolean = false,
    val showWhatsappModal: Boolean = false,
    val showTrackingSettings: Boolean = false,
    val showExtrasScreen: Boolean = false,
    val showDataManagementScreen: Boolean = false,
    val showDevicePermissionGuide: Boolean = false,
    val contactSubject: String = "",
    val accountEditField: String? = null,
    val lookupPhoneNumber: String? = null,
    val skippedSteps: Set<String> = emptySet(),
    val isTrackStartDateSet: Boolean = false,
    val isSetupGuideCompleted: Boolean = false,
    val syncQueueCount: Int = 0,
    val uploadOverMobile: Boolean = false,
    val isUploadOverMobileForced: Boolean = false,
    val agreementAccepted: Boolean = false
)

@dagger.hilt.android.lifecycle.HiltViewModel
class SettingsViewModel @javax.inject.Inject constructor(
    application: Application,
    private val settingsRepository: com.miniclick.calltrackmanage.data.SettingsRepository,
    private val callDataRepository: com.miniclick.calltrackmanage.data.CallDataRepository,
    private val recordingRepository: com.miniclick.calltrackmanage.data.RecordingRepository,
    private val networkObserver: com.miniclick.calltrackmanage.util.network.NetworkConnectivityObserver
) : AndroidViewModel(application) {

    private val permissionManager = PermissionManager(application)
    private val simManager = SimManager(application, settingsRepository, viewModelScope)
    private val syncManager = SyncManager(application, settingsRepository, viewModelScope)
    private val dataManager = DataManager(application, callDataRepository, settingsRepository, recordingRepository, viewModelScope)
    private val trackingManager = TrackingManager(application, settingsRepository, callDataRepository, recordingRepository, viewModelScope)
    private val generalSettingsManager = GeneralSettingsManager(application, settingsRepository, viewModelScope)
    private val lookupManager = LookupManager(settingsRepository, viewModelScope)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val initialOrgId = settingsRepository.getOrganisationId()
        _uiState.update { it.copy(
            isSyncSetup = initialOrgId.isNotEmpty(),
            isSetupGuideCompleted = settingsRepository.isSetupGuideCompleted(),
            agreementAccepted = settingsRepository.isAgreementAccepted()
        ) }

        // STARTUP OPTIMIZATION: Stagger heavy work into phases to prevent recomposition storms
        
        // Phase 0 (Immediate Background): Load basic tracking settings
        viewModelScope.launch(Dispatchers.IO) {
            val trackStartDate = settingsRepository.getTrackStartDate()
            val isTrackStartDateSet = settingsRepository.isTrackStartDateSet()
            val simSelection = settingsRepository.getSimSelection()
            val skippedSteps = settingsRepository.getSkippedSteps()
            // Load caller phones together with simSelection to prevent SetupGuide flicker
            val callerPhone1 = settingsRepository.getCallerPhoneSim1()
            val callerPhone2 = settingsRepository.getCallerPhoneSim2()
            
            _uiState.update { it.copy(
                trackStartDate = trackStartDate,
                isTrackStartDateSet = isTrackStartDateSet,
                simSelection = simSelection,
                skippedSteps = skippedSteps,
                callerPhoneSim1 = callerPhone1,
                callerPhoneSim2 = callerPhone2
            ) }
        }

        // Phase 1 (150ms): Load full settings and other details
        viewModelScope.launch {
            kotlinx.coroutines.delay(150)
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                val userId = settingsRepository.getUserId()
                val pairingCode = if (initialOrgId.isNotEmpty()) "$initialOrgId-$userId" else ""
                val sim1Phone = settingsRepository.getCallerPhoneSim1()
                val sim2Phone = settingsRepository.getCallerPhoneSim2()
                val recEnabled = settingsRepository.isCallRecordEnabled()
                val declined = settingsRepository.isUserDeclinedRecording()

                _uiState.update { it.copy(
                    pairingCode = pairingCode,
                    callerPhoneSim1 = sim1Phone,
                    callerPhoneSim2 = sim2Phone,
                    callRecordEnabled = recEnabled,
                    userDeclinedRecording = declined
                ) }

                loadSettings()
                permissionManager.checkPermissions()
            }
        }
        
        // Phase 2 (300ms): Start observers
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            startDeferredObservers()
        }
        
        // Sync count observers are lightweight (use conflate), start immediately
        viewModelScope.launch {
            settingsRepository.getSetupGuideCompletedFlow().collect { completed ->
                _uiState.update { it.copy(isSetupGuideCompleted = completed) }
            }
        }

        observeSyncCounts()
    }
    
    /**
     * Deferred observers that don't need to run immediately.
     */
    private fun startDeferredObservers() {
        // Observe Network Status
        viewModelScope.launch {
            networkObserver.observe().collect { isAvailable ->
                _uiState.update { it.copy(isNetworkAvailable = isAvailable) }
            }
        }

        // Observe Permissions
        viewModelScope.launch {
            permissionManager.permissions.collect { perms ->
                _uiState.update { it.copy(permissions = perms) }
            }
        }
        viewModelScope.launch {
            permissionManager.isOverlayPermissionGranted.collect { granted: Boolean ->
                _uiState.update { it.copy(isOverlayPermissionGranted = granted) }
            }
        }
        viewModelScope.launch {
            permissionManager.isIgnoringBatteryOptimizations.collect { ignoring: Boolean ->
                _uiState.update { it.copy(isIgnoringBatteryOptimizations = ignoring) }
            }
        }

        // Observe SIM info
        viewModelScope.launch {
            simManager.availableSims.collect { sims: List<SimInfo> ->
                _uiState.update { it.copy(availableSims = sims) }
            }
        }
        viewModelScope.launch {
            simManager.sim1SubId.collect { id: Int? ->
                _uiState.update { it.copy(sim1SubId = id) }
            }
        }
        viewModelScope.launch {
            simManager.sim2SubId.collect { id: Int? ->
                _uiState.update { it.copy(sim2SubId = id) }
            }
        }
        viewModelScope.launch {
            simManager.sim1CalibrationHint.collect { hint: String? ->
                _uiState.update { it.copy(sim1CalibrationHint = hint) }
            }
        }
        viewModelScope.launch {
            simManager.sim2CalibrationHint.collect { hint: String? ->
                _uiState.update { it.copy(sim2CalibrationHint = hint) }
            }
        }

        // Observe SIM Phone Numbers (auto-detected)
        viewModelScope.launch {
            settingsRepository.getCallerPhoneSim1Flow().collect { phone ->
                _uiState.update { it.copy(callerPhoneSim1 = phone) }
            }
        }
        viewModelScope.launch {
            settingsRepository.getCallerPhoneSim2Flow().collect { phone ->
                _uiState.update { it.copy(callerPhoneSim2 = phone) }
            }
        }

        // Observe tracking info
        viewModelScope.launch {
            recordingRepository.getRecordingCountFlow().collect { count: Int ->
                _uiState.update { it.copy(recordingCount = count) }
            }
        }
        viewModelScope.launch {
            trackingManager.isSyncing.collect { syncing: Boolean ->
                _uiState.update { it.copy(isSyncing = syncing) }
            }
        }
        viewModelScope.launch {
            trackingManager.lastSyncStats.collect { stats: String? ->
                _uiState.update { it.copy(lastSyncStats = stats ?: "") }
            }
        }
        viewModelScope.launch {
            trackingManager.lastSyncTime.collect { time: Long ->
                _uiState.update { it.copy(lastSyncTime = time) }
            }
        }
        viewModelScope.launch {
            trackingManager.syncQueueCount.collect { count: Int ->
                _uiState.update { it.copy(syncQueueCount = count) }
            }
        }

        viewModelScope.launch {
            trackingManager.recordingPath.collect { path ->
                _uiState.update { it.copy(recordingPath = path) }
            }
        }
        viewModelScope.launch {
            trackingManager.isRecordingPathVerified.collect { verified ->
                _uiState.update { it.copy(isRecordingPathVerified = verified) }
            }
        }
        viewModelScope.launch {
            trackingManager.isRecordingPathCustom.collect { custom ->
                _uiState.update { it.copy(isRecordingPathCustom = custom) }
            }
        }

        // Observe Lookup info
        viewModelScope.launch {
            lookupManager.customLookupResponse.collect { resp: String? ->
                _uiState.update { it.copy(customLookupResponse = resp) }
            }
        }
        viewModelScope.launch {
            lookupManager.isFetchingCustomLookup.collect { fetching: Boolean ->
                _uiState.update { it.copy(isFetchingCustomLookup = fetching) }
            }
        }

        // Observe WhatsApp Apps
        viewModelScope.launch {
            generalSettingsManager.availableWhatsappApps.collect { apps: List<AppInfo> ->
                _uiState.update { it.copy(availableWhatsappApps = apps) }
            }
        }

        observeExcludedPersons()
    }

    private fun observeSyncCounts() {
        viewModelScope.launch {
            callDataRepository.getPendingNewCallsCountFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { count ->
                    _uiState.update { it.copy(pendingNewCallsCount = count) }
                }
        }
        viewModelScope.launch {
            callDataRepository.getPendingMetadataUpdatesCountFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { count ->
                    _uiState.update { it.copy(pendingMetadataUpdatesCount = count) }
                }
        }
        viewModelScope.launch {
            callDataRepository.getPendingPersonUpdatesCountFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { count ->
                    _uiState.update { it.copy(pendingPersonUpdatesCount = count) }
                }
        }
        viewModelScope.launch {
            callDataRepository.getPendingRecordingSyncCountFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { count ->
                    _uiState.update { it.copy(pendingRecordingCount = count) }
                }
        }
        viewModelScope.launch {
            callDataRepository.getActiveRecordingSyncsFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { recordings ->
                    _uiState.update { it.copy(activeRecordings = recordings) }
                }
        }
    }

    private fun observeExcludedPersons() {
        viewModelScope.launch {
            callDataRepository.getExcludedPersonsFlow().collect { excluded ->
                _uiState.update { it.copy(excludedPersons = excluded) }
            }
        }
    }

    fun resetSyncStatus() {
        dataManager.resetSyncStatus {
            _uiState.update { it.copy(lastSyncStats = "History cleared. Click Sync Now to retry.") }
        }
    }

    fun onResume() {
        permissionManager.checkPermissions()
        simManager.fetchSimInfo()
        generalSettingsManager.fetchWhatsappApps()
    }

    private fun loadSettings() {
        // Reconstruct pairing code from stored org ID and user ID
        val orgId = settingsRepository.getOrganisationId()
        val userId = settingsRepository.getUserId()
        val pairingCode = if (orgId.isNotEmpty() && userId.isNotEmpty()) "$orgId-$userId" else ""
        
        // Note: Default track start date is handled in SettingsRepository.getTrackStartDate()
        // which returns yesterday if not set. We don't auto-set it here so the onboarding
        // step can still show to let users choose their preferred start date.
        
        _uiState.update { currentState ->
            val newPairingCode = if (orgId.isNotEmpty() && userId.isNotEmpty()) "$orgId-$userId" else currentState.pairingCode
            val isSyncActive = orgId.isNotEmpty()
            
            currentState.copy(
                simSelection = settingsRepository.getSimSelection(),
                trackStartDate = settingsRepository.getTrackStartDate(),
                recordingPath = recordingRepository.getRecordingPath(),
                pairingCode = newPairingCode,
                callerPhoneSim1 = settingsRepository.getCallerPhoneSim1(),
                callerPhoneSim2 = settingsRepository.getCallerPhoneSim2(),
                whatsappPreference = settingsRepository.getWhatsappPreference(),
                themeMode = settingsRepository.getThemeMode(),
                lastSyncTime = settingsRepository.getLastSyncTime(),
                sim1SubId = settingsRepository.getSim1SubscriptionId(),
                sim2SubId = settingsRepository.getSim2SubscriptionId(),
                sim1CalibrationHint = settingsRepository.getSim1CalibrationHint(),
                sim2CalibrationHint = settingsRepository.getSim2CalibrationHint(),
                allowPersonalExclusion = settingsRepository.isAllowPersonalExclusion(),
                allowChangingTrackingStartDate = settingsRepository.isAllowChangingTrackStartDate(),
                allowUpdatingTrackingSims = settingsRepository.isAllowUpdatingTrackSims(),
                defaultTrackingStartingDate = settingsRepository.getDefaultTrackStartDate(),
                callerIdEnabled = settingsRepository.isCallerIdEnabled(),
                customLookupUrl = settingsRepository.getCustomLookupUrl(),
                customLookupEnabled = settingsRepository.isCustomLookupEnabled(),
                customLookupCallerIdEnabled = settingsRepository.isCustomLookupCallerIdEnabled(),
                callTrackEnabled = settingsRepository.isCallTrackEnabled(),
                callRecordEnabled = settingsRepository.isCallRecordEnabled(),
                planExpiryDate = settingsRepository.getPlanExpiryDate(),
                allowedStorageGb = settingsRepository.getAllowedStorageGb(),
                storageUsedBytes = settingsRepository.getStorageUsedBytes(),
                userDeclinedRecording = settingsRepository.isUserDeclinedRecording(),
                isDialerEnabled = settingsRepository.isDialerEnabled(),
                showRecordingReminder = settingsRepository.isShowRecordingReminder(),
                showUnknownNoteReminder = settingsRepository.isShowUnknownNoteReminder(),
                isGoogleDialer = isGoogleDialer(),
                isSystemDefaultDialer = isSystemDefaultDialer(),
                // Auto-enable dial button if user is already default dialer, otherwise use saved preference
                showDialButton = if (isSystemDefaultDialer()) true else settingsRepository.isShowDialButton(),
                callActionBehavior = settingsRepository.getCallActionBehavior(),
                isSyncSetup = isSyncActive,
                skippedSteps = settingsRepository.getSkippedSteps(),
                isTrackStartDateSet = settingsRepository.isTrackStartDateSet(),
                isSetupGuideCompleted = settingsRepository.isSetupGuideCompleted(),
                uploadOverMobile = settingsRepository.isUploadOverMobileAllowed(),
                isUploadOverMobileForced = settingsRepository.isForceUploadOverMobile(),
                agreementAccepted = settingsRepository.isAgreementAccepted()
            )
        }
        refreshRecordingPathInfo()
    }

    private fun isGoogleDialer(): Boolean {
        val telecomManager = getApplication<Application>().getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
        return telecomManager?.defaultDialerPackage == "com.google.android.dialer"
    }

    private fun isSystemDefaultDialer(): Boolean {
        val telecomManager = getApplication<Application>().getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
        return telecomManager?.defaultDialerPackage == getApplication<Application>().packageName
    }

    fun updateShowDialButton(show: Boolean) {
        generalSettingsManager.updateShowDialButton(show)
        _uiState.update { it.copy(showDialButton = show) }
    }

    fun updateCallActionBehavior(behavior: String) {
        generalSettingsManager.updateCallActionBehavior(behavior)
        _uiState.update { it.copy(callActionBehavior = behavior) }
    }
    
    fun updateThemeMode(mode: String) {
        generalSettingsManager.updateThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }


    private fun checkPermissions() {
        val ctx = getApplication<Application>()
        val sdkInt = android.os.Build.VERSION.SDK_INT
        
        val permissionsToCheck = mutableListOf(
            android.Manifest.permission.READ_CALL_LOG to "Read Call Log",
            android.Manifest.permission.READ_CONTACTS to "Read Contacts",
            android.Manifest.permission.READ_PHONE_STATE to "Read Phone State",
            android.Manifest.permission.POST_NOTIFICATIONS to "Notifications"
        )
        
        if (sdkInt >= android.os.Build.VERSION_CODES.R) {
            permissionsToCheck.add(android.Manifest.permission.READ_PHONE_NUMBERS to "Phone Number Access")
        }

        if (sdkInt >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permissionsToCheck.add(android.Manifest.permission.READ_MEDIA_AUDIO to "Read Audio")
        } else {
            // Older versions
            permissionsToCheck.add(android.Manifest.permission.READ_EXTERNAL_STORAGE to "Read Storage")
        }

        val states = permissionsToCheck.map { (perm, name) ->
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(ctx, perm) == 
                          android.content.pm.PackageManager.PERMISSION_GRANTED
            PermissionState(name, granted, perm)
        }
        
        val hasOverlay = if (sdkInt >= android.os.Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(ctx)
        } else {
            true
        }

        _uiState.update { it.copy(permissions = states, isOverlayPermissionGranted = hasOverlay) }
    }

    fun fetchSimInfo() {
        simManager.fetchSimInfo()
    }

    fun fetchWhatsappApps() {
        generalSettingsManager.fetchWhatsappApps()
    }

    /**
     * Update Caller ID setting.
     * Returns true if overlay permission is needed (caller should open settings).
     */
    fun updateCallerIdEnabled(enabled: Boolean): Boolean {
        val ctx = getApplication<Application>()
        
        if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Check if overlay permission is granted
            val hasOverlayPermission = android.provider.Settings.canDrawOverlays(ctx)
            
            if (!hasOverlayPermission) {
                // Permission not granted - return true to signal that caller should open settings
                return true
            }
        }
        
        // Permission is granted (or we're disabling, or old Android) - save the setting
        settingsRepository.setCallerIdEnabled(enabled)
        _uiState.update { it.copy(callerIdEnabled = enabled) }
        return false
    }

    fun updateCustomLookupUrl(url: String) {
        lookupManager.updateCustomLookupUrl(url)
        _uiState.update { it.copy(customLookupUrl = url) }
    }

    fun updateCustomLookupEnabled(enabled: Boolean) {
        lookupManager.updateCustomLookupEnabled(enabled)
        _uiState.update { it.copy(customLookupEnabled = enabled) }
    }

    fun updateCustomLookupCallerIdEnabled(enabled: Boolean) {
        lookupManager.updateCustomLookupCallerIdEnabled(enabled)
        _uiState.update { it.copy(customLookupCallerIdEnabled = enabled) }
    }

    fun toggleRawView(isRaw: Boolean) {
        _uiState.update { it.copy(isRawView = isRaw) }
    }

    fun fetchCustomLookup(url: String) {
        lookupManager.fetchCustomLookup(url)
    }

    /**
     * Fetch custom lookup data for a specific phone number using the configured URL template.
     */
    fun fetchCustomLookupForPhone(phoneNumber: String) {
        lookupManager.fetchCustomLookupForPhone(phoneNumber)
    }

    fun clearCustomLookupResponse() {
        lookupManager.clearCustomLookupResponse()
    }


    fun updateWhatsappPreference(packageName: String) {
        generalSettingsManager.updateWhatsappPreference(packageName)
        _uiState.update { it.copy(whatsappPreference = packageName) }
    }

    fun updateSimSelection(selection: String) {
        settingsRepository.setSimSelection(selection)
        _uiState.update { it.copy(simSelection = selection) }
    }

    fun updateTrackStartDate(date: Long) {
        trackingManager.updateTrackStartDate(date)
        _uiState.update { it.copy(trackStartDate = date) }
    }

    fun isTrackStartDateSet(): Boolean = settingsRepository.isTrackStartDateSet()

    fun toggleRecordingDialog(show: Boolean) {
        _uiState.update { it.copy(showRecordingEnablementDialog = show) }
    }

    fun toggleRecordingDisableDialog(show: Boolean) {
        _uiState.update { it.copy(showRecordingDisablementDialog = show) }
    }

    fun updateCallRecordEnabled(enabled: Boolean, scanOld: Boolean = false) {
        trackingManager.updateCallRecordEnabled(enabled, scanOld, generalSettingsManager.isGoogleDialer())
        _uiState.update { it.copy(
            callRecordEnabled = enabled,
            showRecordingEnablementDialog = false,
            showRecordingDisablementDialog = false
        ) }
    }

    fun toggleSyncQueue(show: Boolean) {
        _uiState.update { it.copy(showSyncQueue = show) }
    }
    
    fun toggleRecordingQueue(show: Boolean) {
        _uiState.update { it.copy(showRecordingQueue = show) }
    }

    // Unified Modal Toggle Functions
    fun togglePermissionsModal(show: Boolean) {
        _uiState.update { it.copy(showPermissionsModal = show) }
    }

    fun toggleCloudSyncModal(show: Boolean) {
        _uiState.update { it.copy(showCloudSyncModal = show) }
    }

    fun toggleAccountInfoModal(show: Boolean, editField: String? = null) {
        _uiState.update { it.copy(showAccountInfoModal = show, accountEditField = editField) }
    }

    fun toggleExcludedModal(show: Boolean) {
        _uiState.update { it.copy(showExcludedModal = show) }
    }

    fun toggleClearDataDialog(show: Boolean) {
        _uiState.update { it.copy(showClearDataDialog = show) }
    }

    fun toggleContactModal(show: Boolean, subject: String = "") {
        _uiState.update { it.copy(showContactModal = show, contactSubject = subject) }
    }

    fun toggleCreateOrgModal(show: Boolean) {
        _uiState.update { it.copy(showCreateOrgModal = show) }
    }

    fun toggleJoinOrgModal(show: Boolean) {
        _uiState.update { it.copy(showJoinOrgModal = show) }
    }

    fun toggleTrackSimModal(show: Boolean) {
        _uiState.update { it.copy(showTrackSimModal = show) }
        if (!show) fetchSimInfo()
    }

    fun toggleResetConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showResetConfirmDialog = show) }
    }

    fun toggleCustomLookupModal(show: Boolean) {
        _uiState.update { it.copy(showCustomLookupModal = show) }
    }

    fun toggleWhatsappModal(show: Boolean) {
        if (show) fetchWhatsappApps()
        _uiState.update { it.copy(showWhatsappModal = show) }
    }

    fun toggleTrackingSettings(show: Boolean) {
        _uiState.update { it.copy(showTrackingSettings = show) }
    }

    fun setSetupComplete(completed: Boolean) {
        settingsRepository.setSetupGuideCompleted(completed)
        _uiState.update { it.copy(isSetupGuideCompleted = completed) }
        
        if (completed) {
            // Trigger sync now that setup is finally done
            com.miniclick.calltrackmanage.service.SyncService.start(getApplication())
            com.miniclick.calltrackmanage.worker.CallSyncWorker.runNow(getApplication())
        }
    }

    fun toggleExtrasScreen(show: Boolean) {
        _uiState.update { it.copy(showExtrasScreen = show) }
    }

    fun toggleDataManagementScreen(show: Boolean) {
        _uiState.update { it.copy(showDataManagementScreen = show) }
    }

    fun toggleDevicePermissionGuide(show: Boolean) {
        _uiState.update { it.copy(showDevicePermissionGuide = show) }
    }

    fun showPhoneLookup(phone: String?) {
        _uiState.update { it.copy(lookupPhoneNumber = phone) }
    }

    fun updatePlanExpiryDate(date: String?) {
        settingsRepository.setPlanExpiryDate(date)
        _uiState.update { it.copy(planExpiryDate = date) }
    }
    
    fun updateRecordingPath(path: String) {
        trackingManager.updateRecordingPath(path)
    }

    fun clearCustomRecordingPath() {
        trackingManager.clearCustomRecordingPath()
    }

    fun updateUserDeclinedRecording(declined: Boolean) {
        settingsRepository.setUserDeclinedRecording(declined)
        _uiState.update { it.copy(userDeclinedRecording = declined) }
    }

    fun setStepSkipped(step: String, skipped: Boolean = true) {
        settingsRepository.setStepSkipped(step, skipped)
        _uiState.update { it.copy(skippedSteps = settingsRepository.getSkippedSteps()) }
    }

    fun setAgreementAccepted(accepted: Boolean) {
        settingsRepository.setAgreementAccepted(accepted)
        _uiState.update { it.copy(agreementAccepted = accepted) }
    }

    private fun refreshRecordingPathInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = recordingRepository.getPathInfo()
            _uiState.update { 
                it.copy(
                    recordingPath = info.effectivePath,
                    isRecordingPathVerified = info.isVerified,
                    isRecordingPathCustom = info.isCustom,
                    recordingCount = info.recordingCount
                )
            }
        }
    }

    fun updateDialerEnabled(enabled: Boolean) {
        settingsRepository.setDialerEnabled(enabled)
        _uiState.update { it.copy(isDialerEnabled = enabled) }
    }

    fun updatePairingCode(code: String) {
        // Just update the UI state, don't save to repository yet.
        // Saving will happen after verification in saveAccountInfo.
        _uiState.update { it.copy(pairingCode = code.uppercase().trim()) }
    }

    fun updateCallerPhoneSim1(phone: String) {
        // Save to repository for Track Calls settings
        // These are independent of the Join Org verification flow
        settingsRepository.setCallerPhoneSim1(phone)
        _uiState.update { it.copy(callerPhoneSim1 = phone) }
    }

    fun updateCallerPhoneSim2(phone: String) {
        // Save to repository for Track Calls settings
        // These are independent of the Join Org verification flow
        settingsRepository.setCallerPhoneSim2(phone)
        _uiState.update { it.copy(callerPhoneSim2 = phone) }
    }

    fun updateShowRecordingReminder(show: Boolean) {
        settingsRepository.setShowRecordingReminder(show)
        _uiState.update { it.copy(showRecordingReminder = show) }
    }

    fun updateShowUnknownNoteReminder(show: Boolean) {
        settingsRepository.setShowUnknownNoteReminder(show)
        _uiState.update { it.copy(showUnknownNoteReminder = show) }
    }

    fun updateUploadOverMobile(allowed: Boolean) {
        settingsRepository.setUploadOverMobileAllowed(allowed)
        _uiState.update { it.copy(uploadOverMobile = allowed) }
    }

    fun resetVerificationState() {
        _uiState.update {
            it.copy(
                verificationStatus = null,
                verifiedOrgName = null,
                verifiedEmployeeName = null
            )
        }
    }

    fun verifyPairingCodeOnly(pairingCode: String) {
        val trimmedCode = pairingCode.trim().uppercase()
        
        // Reset previous verification
        _uiState.update { it.copy(verificationStatus = "") }
        
        val deviceId = android.provider.Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        syncManager.verifyPairingCode(
            pairingCode = trimmedCode,
            deviceId = deviceId,
            action = "verify_pairing_code",
            onProgress = { loading -> _uiState.update { it.copy(isVerifying = loading) } },
            onResult = { success: Boolean, employeeName: String?, settings: com.miniclick.calltrackmanage.network.EmployeeSettingsDto?, plan: com.miniclick.calltrackmanage.network.PlanInfoDto?, error: String? ->
                if (success) {
                    val parts = trimmedCode.split("-")
                    _uiState.update { 
                        it.copy(
                            verificationStatus = "verified",
                            verifiedOrgName = parts[0],
                            verifiedEmployeeName = employeeName,
                            pairingCode = trimmedCode
                        )
                    }
                    // We don't call loadSettings here yet because we haven't SAVED to repo
                    // But we want to ensure any ui depending on pairingCode is updated
                } else {
                    _uiState.update { it.copy(verificationStatus = "failed") }
                    Toast.makeText(getApplication(), error ?: "Verification failed", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    fun retryRecordingUpload(compositeId: String) {
        trackingManager.retryRecordingUpload(compositeId)
    }

    fun syncCallManually() {
        trackingManager.syncCallManually()
    }

    fun addExcludedNumber(phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        viewModelScope.launch {
            callDataRepository.updateExclusion(phoneNumber.trim(), true)
            Toast.makeText(getApplication(), "Number excluded: $phoneNumber", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Add excluded number with specific exclusion type
     * @param isNoTracking - true for "No Tracking" (excludeFromSync=true, excludeFromList=true)
     *                       false for "Excluded from lists" (excludeFromSync=false, excludeFromList=true)
     */
    fun addExcludedNumberWithType(phoneNumber: String, isNoTracking: Boolean) {
        if (phoneNumber.isBlank()) return
        viewModelScope.launch {
            if (isNoTracking) {
                // "No Tracking" - stop tracking and hide from list
                callDataRepository.updateExclusionType(phoneNumber.trim(), excludeFromSync = true, excludeFromList = true)
            } else {
                // "Excluded from lists" - keep tracking but hide from UI
                callDataRepository.updateExclusionType(phoneNumber.trim(), excludeFromSync = false, excludeFromList = true)
            }
            val typeLabel = if (isNoTracking) "No Tracking" else "Excluded from lists"
            Toast.makeText(getApplication(), "$phoneNumber: $typeLabel", Toast.LENGTH_SHORT).show()
        }
    }

    fun addExcludedNumbers(numbers: String) {
        if (numbers.isBlank()) return
        val numberList = numbers.split(",", "\n", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        viewModelScope.launch {
            numberList.forEach { number ->
                callDataRepository.updateExclusion(number, true)
            }
            Toast.makeText(getApplication(), "${numberList.size} numbers excluded", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Add multiple numbers with specific exclusion type
     */
    fun addExcludedNumbersWithType(numbers: String, isNoTracking: Boolean) {
        if (numbers.isBlank()) return
        val numberList = numbers.split(",", "\n", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        viewModelScope.launch {
            numberList.forEach { number ->
                if (isNoTracking) {
                    callDataRepository.updateExclusionType(number, excludeFromSync = true, excludeFromList = true)
                } else {
                    callDataRepository.updateExclusionType(number, excludeFromSync = false, excludeFromList = true)
                }
            }
            val typeLabel = if (isNoTracking) "No Tracking" else "Excluded from lists"
            Toast.makeText(getApplication(), "${numberList.size} numbers set to: $typeLabel", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Update exclusion type for an existing excluded number
     */
    fun updateExclusionType(phoneNumber: String, isNoTracking: Boolean) {
        viewModelScope.launch {
            if (isNoTracking) {
                callDataRepository.updateExclusionType(phoneNumber, excludeFromSync = true, excludeFromList = true)
            } else {
                callDataRepository.updateExclusionType(phoneNumber, excludeFromSync = false, excludeFromList = true)
            }
        }
    }

    private fun getBatteryLevel(): Int {
        return try {
            val ctx = getApplication<Application>()
            val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                ctx.registerReceiver(null, ifilter)
            }
            val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        } catch (e: Exception) {
            -1
        }
    }

    fun unexcludeNumber(phoneNumber: String) {
        viewModelScope.launch {
            callDataRepository.removeExclusion(phoneNumber)
            Toast.makeText(getApplication(), "Exclusion removed: $phoneNumber", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearAllAppData(onComplete: () -> Unit) {
        _uiState.update { it.copy(isVerifying = true) } // Use isVerifying for general loading
        dataManager.clearAllData(
            onProgress = { loading -> _uiState.update { it.copy(isVerifying = loading) } },
            onComplete = { success ->
                if (success) {
                    loadSettings()
                    onComplete()
                }
            }
        )
    }

    fun saveAccountInfo(onSuccess: () -> Unit = {}) {
        val phone1 = _uiState.value.callerPhoneSim1.trim()
        val phone2 = _uiState.value.callerPhoneSim2.trim()
        
        if (phone1.isEmpty() && phone2.isEmpty()) {
            Toast.makeText(getApplication(), "Please enter at least one phone number", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceId = android.provider.Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        syncManager.verifyPairingCode(
            pairingCode = _uiState.value.pairingCode,
            deviceId = deviceId,
            onProgress = { loading -> _uiState.update { it.copy(isVerifying = loading) } },
            onResult = { success: Boolean, employeeName: String?, settings: com.miniclick.calltrackmanage.network.EmployeeSettingsDto?, plan: com.miniclick.calltrackmanage.network.PlanInfoDto?, error: String? ->
                if (success) {
                    val parts = _uiState.value.pairingCode.split("-")
                    val orgId = parts[0]
                    val userId = parts[1]
                    
                    syncManager.savePairingInfo(orgId, userId)
                    settingsRepository.setCallerPhoneSim1(phone1)
                    settingsRepository.setCallerPhoneSim2(phone2)
                    
                    _uiState.update { 
                        it.copy(
                            pairingCode = "$orgId-$userId",
                            callerPhoneSim1 = phone1,
                            callerPhoneSim2 = phone2
                        )
                    }
                    
                    Toast.makeText(getApplication(), "✓ Welcome, $employeeName!", Toast.LENGTH_LONG).show()
                    loadSettings()
                    onSuccess()
                } else {
                    Toast.makeText(getApplication(), "✗ $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    fun connectVerifiedOrganisation(onSuccess: () -> Unit = {}) {
        val currentState = _uiState.value
        Log.d("SettingsViewModel", "connectVerifiedOrganisation: status=${currentState.verificationStatus}, code=${currentState.pairingCode}")
        
        if (currentState.verificationStatus != "verified") {
            Log.w("SettingsViewModel", "connectVerifiedOrganisation: Not verified, cannot connect")
            return
        }
        
        val pairingCode = currentState.pairingCode
        if (pairingCode.isEmpty()) {
            Toast.makeText(getApplication(), "Error: Pairing code is missing. Please verify again.", Toast.LENGTH_SHORT).show()
            return
        }

        val parts = pairingCode.split("-")
        if (parts.size >= 2) {
            val orgId = parts[0].trim()
            val userId = parts[1].trim()
            
            Log.i("SettingsViewModel", "Saving pairing info: ORG=$orgId, USER=$userId")
            syncManager.savePairingInfo(orgId, userId)
            
            // Refresh settings to update isSyncSetup and other state
            loadSettings()
            
            Toast.makeText(getApplication(), "✓ Connected to ${currentState.verifiedOrgName ?: orgId}!", Toast.LENGTH_LONG).show()
            
            // Reset verification state for next time
            resetVerificationState()
            
            onSuccess()
        } else {
            Log.e("SettingsViewModel", "connectVerifiedOrganisation: Invalid code format in state: $pairingCode")
            Toast.makeText(getApplication(), "Error: Invalid pairing code format", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportLogs() {
        viewModelScope.launch {
            com.miniclick.calltrackmanage.util.system.LogExporter.exportAndShareLogs(getApplication())
        }
    }
    
    fun leaveOrganisation() {
        settingsRepository.setOrganisationId("")
        settingsRepository.setUserId("")
        _uiState.update { it.copy(pairingCode = "") }
        Toast.makeText(getApplication(), "Left organisation successfully", Toast.LENGTH_SHORT).show()
    }
    
    data class VerificationCall(
        val number: String,
        val date: Long,
        val subscriptionId: Int,
        val contactName: String? = null
    )

    fun fetchRecentSystemCalls(onResult: (List<VerificationCall>) -> Unit) {
        val ctx = getApplication<Application>()
        
        // Log permission status
        val hasCallLogPermission = androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CALL_LOG) == 
                           android.content.pm.PackageManager.PERMISSION_GRANTED
        Log.d("SettingsViewModel", "fetchRecentSystemCalls: has READ_CALL_LOG = $hasCallLogPermission")
        
        if (!hasCallLogPermission) {
            Log.e("SettingsViewModel", "fetchRecentSystemCalls: Permission denied")
            onResult(emptyList())
            return
        }
        
        // Check if we have contacts permission for name lookup fallback
        val hasContactsPermission = androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CONTACTS) == 
                           android.content.pm.PackageManager.PERMISSION_GRANTED

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val calls = mutableListOf<VerificationCall>()
            val subIdColumns = listOf("subscription_id", "sub_id", "sim_id", "sim_slot", "phone_id")
            val sortOrder = "${android.provider.CallLog.Calls.DATE} DESC" // Try without LIMIT first for compatibility
            
            try {
                Log.d("SettingsViewModel", "Querying CallLog.Calls.CONTENT_URI...")
                ctx.contentResolver.query(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    null, // Fetch all columns
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val totalRows = cursor.count
                    Log.d("SettingsViewModel", "CallLog Query successful. Rows found: $totalRows")
                    
                    // Log columns for debugging if few calls found or issues identifying sim
                    if (totalRows > 0) {
                        val allCols = cursor.columnNames.joinToString(", ")
                        Log.d("SettingsViewModel", "Available columns: $allCols")
                    }

                    val numberIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                    val dateIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE)
                    val cachedNameIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
                    
                    var subIdIdx = -1
                    for (col in subIdColumns) {
                        val idx = cursor.getColumnIndex(col)
                        if (idx != -1) {
                            subIdIdx = idx
                            Log.d("SettingsViewModel", "Identified subId column: $col at index $idx")
                            break
                        }
                    }
                    
                    if (subIdIdx == -1 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                         subIdIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.PHONE_ACCOUNT_ID)
                         if (subIdIdx != -1) Log.d("SettingsViewModel", "Falling back to phone_account_id at index $subIdIdx")
                    }

                    var count = 0
                    while(cursor.moveToNext() && count < 20) {
                        val number = cursor.getString(numberIdx) ?: "Unknown"
                        val date = cursor.getLong(dateIdx)
                        
                        // Get cached name from call log first
                        var contactName: String? = null
                        if (cachedNameIdx != -1) {
                            contactName = cursor.getString(cachedNameIdx)
                        }
                        
                        // Fallback: lookup contact name if we have permission and cached name is empty
                        if (contactName.isNullOrBlank() && hasContactsPermission && number != "Unknown") {
                            contactName = lookupContactName(ctx, number)
                        }
                        
                        var subId = -1
                        if (subIdIdx != -1) {
                            try {
                                subId = cursor.getInt(subIdIdx)
                            } catch (e: Exception) {
                                val strVal = cursor.getString(subIdIdx)
                                subId = strVal?.toIntOrNull() ?: -1
                                if (subId == -1 && !strVal.isNullOrEmpty()) {
                                    // Hash string IDs (like "subscription:1") if they aren't numeric
                                    subId = strVal.hashCode()
                                    Log.d("SettingsViewModel", "Hashed non-numeric subId '$strVal' to $subId")
                                }
                            }
                        }
                        
                        calls.add(VerificationCall(number, date, subId, contactName))
                        count++
                    }
                } ?: Log.e("SettingsViewModel", "CallLog query returned NULL cursor")
                
            } catch (e: SecurityException) {
                Log.e("SettingsViewModel", "SecurityException reading call log: ${e.message}")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "General error reading call log", e)
            }
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Log.d("SettingsViewModel", "Returning ${calls.size} calls to UI")
                onResult(calls)
            }
        }
    }
    
    /**
     * Look up contact name from phone number using Contacts provider.
     * Returns null if not found or error.
     */
    private fun lookupContactName(ctx: android.content.Context, phoneNumber: String): String? {
        return try {
            val uri = android.net.Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            ctx.contentResolver.query(
                uri,
                arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            Log.w("SettingsViewModel", "Contact lookup failed for $phoneNumber: ${e.message}")
            null
        }
    }
    
    fun setSimCalibration(simIndex: Int, subscriptionId: Int, hint: String) {
        simManager.setSimCalibration(simIndex, subscriptionId, hint)
    }

    fun resetOnboarding() {
        settingsRepository.setOnboardingCompleted(false)
        val ctx = getApplication<Application>()
        Toast.makeText(ctx, "Restarting app to show onboarding...", Toast.LENGTH_SHORT).show()
        
        // Restart the app more robustly
        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val mainIntent = android.content.Intent.makeRestartActivityTask(intent?.component)
        ctx.startActivity(mainIntent)
        
        // Ensure process is terminated to force fresh state
        java.lang.System.exit(0)
    }

    private fun parseBooleanSettings(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        if (value is Number) return value.toInt() == 1
        val s = value.toString().trim().lowercase()
        return s == "1" || s == "true" || s == "1.0"
    }

    // ============================================
    // DATA EXPORT & IMPORT
    // ============================================

    data class BackupData(
        val calls: List<com.miniclick.calltrackmanage.data.db.CallDataEntity>,
        val persons: List<com.miniclick.calltrackmanage.data.db.PersonDataEntity>,
        val exportDate: Long = System.currentTimeMillis(),
        val appVersion: String
    )

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                val calls = callDataRepository.getAllCalls()
                val persons = callDataRepository.getAllPersons()
                val packageInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
                val appVersion = packageInfo.versionName ?: "Unknown"
                
                val backup = BackupData(calls, persons, appVersion = appVersion)
                val json = GsonBuilder().setPrettyPrinting().create().toJson(backup)
                
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(json)
                        }
                    }
                }
                Toast.makeText(getApplication(), "Data exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to export data", e)
                Toast.makeText(getApplication(), "Failed to export: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportDataCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val calls = callDataRepository.getAllCalls()
                val sb = StringBuilder()
                // Header
                sb.append("Date,Number,Name,Type,Duration,Note,Tag\n")
                
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                
                calls.forEach { call ->
                    val date = sdf.format(java.util.Date(call.callDate))
                    val type = when(call.callType) {
                        android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        android.provider.CallLog.Calls.MISSED_TYPE -> "Missed"
                        android.provider.CallLog.Calls.REJECTED_TYPE -> "Rejected"
                        android.provider.CallLog.Calls.BLOCKED_TYPE -> "Blocked"
                        else -> "Unknown"
                    }
                    
                    // Escape CSV fields
                    val name = (call.contactName ?: "").replace("\"", "\"\"")
                    val note = (call.callNote ?: "").replace("\"", "\"\"")
                    val tag = "" // Tag is not available in CallDataEntity
                    
                    sb.append("$date,${call.phoneNumber},\"$name\",$type,${call.duration},\"$note\",\"$tag\"\n")
                }
                
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(sb.toString())
                        }
                    }
                }
                Toast.makeText(getApplication(), "CSV exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to export CSV", e)
                Toast.makeText(getApplication(), "Failed to export: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                        InputStreamReader(inputStream).use { reader ->
                            reader.readText()
                        }
                    } ?: ""
                }
                
                if (json.isBlank()) {
                    Toast.makeText(getApplication(), "Empty file", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val backup = GsonBuilder().create().fromJson(json, BackupData::class.java)
                
                if (backup.calls.isEmpty() && backup.persons.isEmpty()) {
                    Toast.makeText(getApplication(), "No data found in file", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                withContext(Dispatchers.IO) {
                    callDataRepository.importData(backup.calls, backup.persons)
                }
                
                Toast.makeText(getApplication(), "Data imported: ${backup.calls.size} calls, ${backup.persons.size} persons", Toast.LENGTH_LONG).show()
                loadSettings()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to import data", e)
                Toast.makeText(getApplication(), "Failed to import: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ============================================
    // TROUBLESHOOTING
    // ============================================

    fun reImportCallHistory() {
        viewModelScope.launch {
            try {
                // We keep existing data but force a fresh scan from system call log
                // The repository handles duplicates by using compositeId as primary key
                withContext(Dispatchers.IO) {
                    callDataRepository.syncFromSystemCallLog()
                }
                Toast.makeText(getApplication(), "Re-imported calls from system log", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun reImportRecordings() {
        viewModelScope.launch {
            try {
                // Reset recording sync status for calls that have recordings or failed
                withContext(Dispatchers.IO) {
                    callDataRepository.resetSkippedRecordings()
                }
                // Trigger recording worker
                RecordingUploadWorker.runNow(getApplication())
                Toast.makeText(getApplication(), "Recording sync reset and started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Re-attach recordings for ALL calls using the advanced matching logic.
     * This iterates through all existing logs in the app database and tries to find a matching recording file.
     */
    fun reAttachAllRecordings() {
        // Trigger background worker
        com.miniclick.calltrackmanage.worker.ReattachRecordingsWorker.runNow(getApplication())
        Toast.makeText(getApplication(), "Started background recording scan. Check notification for progress.", Toast.LENGTH_LONG).show()
        // We don't need to manually update state here as the worker handles it separately
        // But if we want to show loading specifically for this button, we'd need to observe the worker info.
        // For now, simple "Started" message is sufficient given user request for background processing.
    }

    fun recheckRecordings() {
        viewModelScope.launch {
            try {
                // Re-verify path and scan
                refreshRecordingPathInfo()
                // Also trigger a system log sync which looks for recording paths
                withContext(Dispatchers.IO) {
                    callDataRepository.syncFromSystemCallLog()
                }
                Toast.makeText(getApplication(), "Recording scan completed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * RESET EVERYTHING FOR SYNC: Implements "Forgot server, sync statuses of local data"
     */
    fun resetEverythingForSync() {
        viewModelScope.launch {
            try {
                callDataRepository.resetAllSyncStatuses()
                loadSettings()
                Toast.makeText(getApplication(), "All sync statuses reset. Ready for full sync.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Reset failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * FETCH META DATA ALL: Implements "Fetch meta data all"
     */
    fun fetchMetaDataAll() {
        viewModelScope.launch {
            try {
                callDataRepository.resetMetadataSync()
                // Trigger sync worker
                CallSyncWorker.runNow(getApplication())
                Toast.makeText(getApplication(), "Metadata sync reset. Fetching updates...", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
