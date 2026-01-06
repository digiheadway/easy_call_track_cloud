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
import com.google.gson.GsonBuilder

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
    val simSelection: String = "Both", // "Both", "Sim1", "Sim2"
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
    val showRecordingQueue: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)
    private val callDataRepository = CallDataRepository.getInstance(application)
    private val recordingRepository = com.miniclick.calltrackmanage.data.RecordingRepository.getInstance(application)
    private val networkObserver = com.miniclick.calltrackmanage.util.NetworkConnectivityObserver(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadSettings()
            checkPermissions()
        }

        // Observe Network Status
        viewModelScope.launch {
            networkObserver.observe().collect { isAvailable ->
                _uiState.update { it.copy(isNetworkAvailable = isAvailable) }
            }
        }
        // NOTE: fetchSimInfo() and fetchWhatsappApps() are NOT called here
        // They are loaded lazily when user accesses those specific features
        // - fetchSimInfo() called when SIM/Call tracking settings opened
        // - fetchWhatsappApps() called when WhatsApp preference UI shown
        observeExcludedPersons()
        observeSyncCounts()
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
        viewModelScope.launch {
            callDataRepository.clearSyncStatus()
            _uiState.update { it.copy(lastSyncStats = "History cleared. Click Sync Now to retry.") }
        }
    }

    fun onResume() {
        checkPermissions()
        fetchSimInfo()
        fetchWhatsappApps()
    }

    private fun loadSettings() {
        // Reconstruct pairing code from stored org ID and user ID
        val orgId = settingsRepository.getOrganisationId()
        val userId = settingsRepository.getUserId()
        val pairingCode = if (orgId.isNotEmpty() && userId.isNotEmpty()) "$orgId-$userId" else ""
        
        _uiState.update {
            it.copy(
                simSelection = settingsRepository.getSimSelection(),
                trackStartDate = settingsRepository.getTrackStartDate(),
                recordingPath = recordingRepository.getRecordingPath(),
                pairingCode = pairingCode,
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
                isSyncSetup = orgId.isNotEmpty()
            )
        }
        refreshRecordingPathInfo()
    }
    
    fun updateThemeMode(mode: String) {
        settingsRepository.setThemeMode(mode)
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
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            if (androidx.core.app.ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_PHONE_STATE) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return@launch
            }

            try {
                val subscriptionManager = androidx.core.content.ContextCompat.getSystemService(ctx, android.telephony.SubscriptionManager::class.java)
                val infoList = subscriptionManager?.activeSubscriptionInfoList
                if (infoList != null) {
                    val sims = infoList.map { 
                        SimInfo(
                            slotIndex = it.simSlotIndex, 
                            displayName = it.displayName.toString(), 
                            carrierName = it.carrierName.toString(),
                            subscriptionId = it.subscriptionId
                        )
                    }.sortedBy { it.slotIndex }
                    
                    _uiState.update { it.copy(availableSims = sims) }
                    
                    // Update UI state with detected IDs, but DON'T save to repository yet
                    // User must manually "Identify/Calibrate" to confirm the mapping
                    sims.find { it.slotIndex == 0 }?.let { sim ->
                        val currentSim1Id = settingsRepository.getSim1SubscriptionId()
                        _uiState.update { state -> state.copy(sim1SubId = currentSim1Id) }
                        
                        // Try to auto-access phone number if not already set (this is safe to auto-populate)
                        if (settingsRepository.getCallerPhoneSim1().isBlank()) {
                            val number = getSimNumber(ctx, sim.subscriptionId)
                            if (!number.isNullOrBlank()) {
                                settingsRepository.setCallerPhoneSim1(number)
                                _uiState.update { it.copy(callerPhoneSim1 = number) }
                            }
                        }
                    }
                    sims.find { it.slotIndex == 1 }?.let { sim ->
                        val currentSim2Id = settingsRepository.getSim2SubscriptionId()
                        _uiState.update { state -> state.copy(sim2SubId = currentSim2Id) }
                        
                        // Try to auto-access phone number if not already set
                        if (settingsRepository.getCallerPhoneSim2().isBlank()) {
                            val number = getSimNumber(ctx, sim.subscriptionId)
                            if (!number.isNullOrBlank()) {
                                settingsRepository.setCallerPhoneSim2(number)
                                _uiState.update { it.copy(callerPhoneSim2 = number) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getSimNumber(ctx: Context, subId: Int): String? {
        // Requires READ_PHONE_NUMBERS or READ_PHONE_STATE depending on OS version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_PHONE_NUMBERS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return null
        } else if (androidx.core.app.ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_PHONE_STATE) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) return null

        return try {
            val subscriptionManager = androidx.core.content.ContextCompat.getSystemService(ctx, android.telephony.SubscriptionManager::class.java)
            if (subscriptionManager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    subscriptionManager.getPhoneNumber(subId)
                } else {
                    // Best effort for older versions
                    @Suppress("DEPRECATION")
                    subscriptionManager.getActiveSubscriptionInfo(subId)?.number
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun fetchWhatsappApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val packageManager = ctx.packageManager
            
            // Known WhatsApp package names (including common clones)
            val knownWhatsappPackages = listOf(
                "com.whatsapp",                    // Regular WhatsApp
                "com.whatsapp.w4b",                // WhatsApp Business
                "com.whatsapp.w4b.clone",          // Cloned Business
                "com.whatsapp.clone",              // Cloned WhatsApp
                "com.gbwhatsapp",                  // GB WhatsApp
                "com.whatsapp1",                   // Dual WhatsApp
                "com.whatsapp2",                   // Dual WhatsApp 2
                "com.dual.whatsapp",               // Dual Space WhatsApp
                "com.parallel.space.pro",          // Parallel Space
                "com.lbe.parallel.intl",           // Parallel Space International
                "com.ludashi.dualspace",           // Dual Space
                // OnePlus Parallel Apps patterns
                "com.oneplus.clone.whatsapp",
                "com.oneplus.clone.com.whatsapp",
                "com.oneplus.clone.com.whatsapp.w4b",
                // Xiaomi Second Space / Dual Apps
                "com.miui.securitycore.whatsapp",
                "com.miui.clone.whatsapp",
                // Samsung Dual Messenger
                "com.samsung.android.game.cloudgame.whatsapp",
                // Huawei App Twin
                "com.huawei.clone.whatsapp"
            )
            
            val apps = mutableListOf<AppInfo>()
            
            // Method 1: Query by intent with MATCH_ALL to get all handlers
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://wa.me/")
            }
            
            try {
                packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_ALL)
                    .forEach { resolveInfo ->
                        val packageName = resolveInfo.activityInfo.packageName
                        // Filter out browsers (like Chrome) that catch generic wa.me links
                        if (packageName.contains("whatsapp", ignoreCase = true) || 
                            packageName.contains("com.whatsapp", ignoreCase = true)) {
                            val label = resolveInfo.loadLabel(packageManager).toString()
                            apps.add(AppInfo(label, packageName))
                        }
                    }
            } catch (e: Exception) { e.printStackTrace() }
            
            // Method 2: Query with MATCH_UNINSTALLED_PACKAGES to catch clones in other profiles
            try {
                packageManager.queryIntentActivities(intent, 
                    android.content.pm.PackageManager.MATCH_ALL or 
                    android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
                ).forEach { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    if (packageName.contains("whatsapp", ignoreCase = true) || 
                        packageName.contains("com.whatsapp", ignoreCase = true)) {
                        val label = resolveInfo.loadLabel(packageManager).toString()
                        if (apps.none { it.packageName == packageName }) {
                            apps.add(AppInfo(label, packageName))
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            // Method 3: Check known package names directly
            knownWhatsappPackages.forEach { packageName ->
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    apps.add(AppInfo(label, packageName))
                } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                    // Package not installed, skip
                }
            }
            
            // Method 4: Search ALL installed apps for WhatsApp (catches OnePlus clones)
            @Suppress("DEPRECATION")
            try {
                packageManager.getInstalledApplications(
                    android.content.pm.PackageManager.GET_META_DATA or
                    android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
                ).filter { 
                    it.packageName.contains("whatsapp", ignoreCase = true) ||
                    (it.packageName.contains("clone", ignoreCase = true) && 
                     packageManager.getApplicationLabel(it).toString().contains("whatsapp", ignoreCase = true))
                }.forEach { appInfo ->
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    apps.add(AppInfo(label, appInfo.packageName))
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            // Method 5: Check for apps in work profile / managed profiles
            try {
                val userManager = androidx.core.content.ContextCompat.getSystemService(ctx, android.os.UserManager::class.java)
                val launcherApps = androidx.core.content.ContextCompat.getSystemService(ctx, android.content.pm.LauncherApps::class.java)
                
                userManager?.userProfiles?.forEach { userHandle ->
                    listOf("com.whatsapp", "com.whatsapp.w4b").forEach { pkg ->
                        try {
                            val activityList = launcherApps?.getActivityList(pkg, userHandle)
                            activityList?.forEach { launcherActivity ->
                                val label = launcherActivity.label.toString()
                                val suffix = if (userHandle != android.os.Process.myUserHandle()) " (Clone)" else ""
                                apps.add(AppInfo("$label$suffix", "${pkg}#${userHandle.hashCode()}"))
                            }
                        } catch (e: Exception) { /* Skip */ }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            
            val uniqueApps = apps
                .distinctBy { it.packageName }
                .sortedBy { it.label }

            android.util.Log.d("SettingsViewModel", "Found WhatsApp apps: ${uniqueApps.map { "${it.label} (${it.packageName})" }}")
            _uiState.update { it.copy(availableWhatsappApps = uniqueApps) }
        }
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
        settingsRepository.setCustomLookupUrl(url)
        _uiState.update { it.copy(customLookupUrl = url) }
    }

    fun updateCustomLookupEnabled(enabled: Boolean) {
        settingsRepository.setCustomLookupEnabled(enabled)
        _uiState.update { it.copy(customLookupEnabled = enabled) }
    }

    fun updateCustomLookupCallerIdEnabled(enabled: Boolean) {
        settingsRepository.setCustomLookupCallerIdEnabled(enabled)
        _uiState.update { it.copy(customLookupCallerIdEnabled = enabled) }
    }

    fun toggleRawView(isRaw: Boolean) {
        _uiState.update { it.copy(isRawView = isRaw) }
    }

    fun fetchCustomLookup(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingCustomLookup = true, customLookupResponse = "Fetching...") }
            try {
                val response = com.miniclick.calltrackmanage.network.NetworkClient.api.fetchData(url)
                if (response.isSuccessful) {
                    val body = response.body()
                    val json = GsonBuilder().setPrettyPrinting().create().toJson(body)
                    _uiState.update { it.copy(customLookupResponse = json) }
                } else {
                    val error = "Error: ${response.code()} ${response.message()}"
                    _uiState.update { it.copy(customLookupResponse = error) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(customLookupResponse = "Error: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isFetchingCustomLookup = false) }
            }
        }
    }

    /**
     * Fetch custom lookup data for a specific phone number using the configured URL template.
     */
    fun fetchCustomLookupForPhone(phoneNumber: String) {
        val baseUrl = settingsRepository.getCustomLookupUrl().ifEmpty { 
            "https://prop.digiheadway.in/api/calls/caller_id.php?phone={phone}"
        }
        val url = if (baseUrl.contains("{phone}")) {
            baseUrl.replace("{phone}", phoneNumber)
        } else if (baseUrl.contains("phone=")) {
            // If it already has a phone param but not a placeholder, try to replace it or append it
            // Simple heuristic: if it ends with =, append. Otherwise try to replace the value.
            if (baseUrl.endsWith("=")) baseUrl + phoneNumber else baseUrl
        } else {
            // fallback: append as query param if possible
            val separator = if (baseUrl.contains("?")) "&" else "?"
            baseUrl + separator + "phone=" + phoneNumber
        }
        fetchCustomLookup(url)
    }

    fun clearCustomLookupResponse() {
        _uiState.update { it.copy(customLookupResponse = null) }
    }


    fun updateWhatsappPreference(packageName: String) {
        settingsRepository.setWhatsappPreference(packageName)
        _uiState.update { it.copy(whatsappPreference = packageName) }
    }

    fun updateSimSelection(selection: String) {
        settingsRepository.setSimSelection(selection)
        _uiState.update { it.copy(simSelection = selection) }
    }

    fun updateTrackStartDate(date: Long) {
        settingsRepository.setTrackStartDate(date)
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
        viewModelScope.launch {
            settingsRepository.setCallRecordEnabled(enabled)
            if (enabled) {
                if (scanOld) {
                    // Start from beginning/track start date
                    settingsRepository.setRecordingLastEnabledTimestamp(0L)
                    // Reset statuses in DB so they are picked up again
                    callDataRepository.resetSkippedRecordings()
                    
                    // Trigger a system log sync to find any missing recording paths for these reset items
                    withContext(Dispatchers.IO) {
                        callDataRepository.syncFromSystemCallLog()
                    }
                    
                    android.widget.Toast.makeText(getApplication(), "Scanning all past calls for recordings...", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    settingsRepository.setRecordingLastEnabledTimestamp(System.currentTimeMillis())
                }
            }
            _uiState.update { it.copy(
                callRecordEnabled = enabled,
                showRecordingEnablementDialog = false,
                showRecordingDisablementDialog = false
            ) }
            
            if (enabled) {
                RecordingUploadWorker.runNow(getApplication())
            }
        }
    }

    fun toggleSyncQueue(show: Boolean) {
        _uiState.update { it.copy(showSyncQueue = show) }
    }
    
    fun toggleRecordingQueue(show: Boolean) {
        _uiState.update { it.copy(showRecordingQueue = show) }
    }

    fun updatePlanExpiryDate(date: String?) {
        settingsRepository.setPlanExpiryDate(date)
        _uiState.update { it.copy(planExpiryDate = date) }
    }
    
    fun updateRecordingPath(path: String) {
        recordingRepository.setCustomPath(path)
        refreshRecordingPathInfo()
        _uiState.update { it.copy(recordingPath = path) }
    }

    fun clearCustomRecordingPath() {
        recordingRepository.clearCustomPath()
        refreshRecordingPathInfo()
    }

    fun updateUserDeclinedRecording(declined: Boolean) {
        settingsRepository.setUserDeclinedRecording(declined)
        _uiState.update { it.copy(userDeclinedRecording = declined) }
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
        val ctx = getApplication<Application>()
        val trimmedCode = pairingCode.trim().uppercase()
        
        // Reset previous verification
        resetVerificationState()
        
        // Client-side validation
        if (!trimmedCode.contains("-")) {
            _uiState.update { it.copy(verificationStatus = "failed") }
            Toast.makeText(ctx, "Invalid format. Use: ORGID-USERID", Toast.LENGTH_SHORT).show()
            return
        }

        val parts = trimmedCode.split("-", limit = 2)
        if (parts.size != 2) {
            _uiState.update { it.copy(verificationStatus = "failed") }
            Toast.makeText(ctx, "Invalid format. Use: ORGID-USERID", Toast.LENGTH_SHORT).show()
            return
        }
        
        val orgId = parts[0].trim()
        val userId = parts[1].trim()

        if (orgId.isEmpty() || userId.isEmpty()) {
            _uiState.update { it.copy(verificationStatus = "failed") }
            Toast.makeText(ctx, "Both ORGID and USERID are required", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate ORGID format (letters/numbers only)
        if (!orgId.matches(Regex("^[A-Z0-9]+$"))) {
            _uiState.update { it.copy(verificationStatus = "failed") }
            Toast.makeText(ctx, "ORGID must contain only letters and numbers", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate USERID format (numbers only)
        if (!userId.matches(Regex("^[0-9]+$"))) {
            _uiState.update { it.copy(verificationStatus = "failed") }
            Toast.makeText(ctx, "USERID must be a number", Toast.LENGTH_SHORT).show()
            return
        }

        // All client-side validations passed - now verify with backend
        val deviceId = android.provider.Settings.Secure.getString(
            ctx.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true) }
            try {
                Log.d("SettingsViewModel", "Verifying pairing code (check only): $orgId-$userId")
                
                val deviceModel = android.os.Build.MODEL
                val osVersion = android.os.Build.VERSION.RELEASE
                val batteryPct = getBatteryLevel()

                val response = com.miniclick.calltrackmanage.network.NetworkClient.api.verifyPairingCode(
                    action = "verify_pairing_code",
                    orgId = orgId,
                    userId = userId,
                    deviceId = deviceId,
                    deviceModel = deviceModel,
                    osVersion = osVersion,
                    batteryLevel = if (batteryPct >= 0) batteryPct else null
                )

                if (response.isSuccessful && response.body()?.get("success") == true) {
                    val employeeName = response.body()?.get("employee_name")?.toString() ?: "Unknown"
                    @Suppress("UNCHECKED_CAST")
                    val settings = response.body()?.get("settings") as? Map<String, Any>
                    
                    if (settings != null) {
                        val allowChanging = parseBooleanSettings(settings["allow_changing_tracking_start_date"])
                        val defaultDateStr = settings["default_tracking_starting_date"] as? String

                        settingsRepository.setAllowPersonalExclusion(parseBooleanSettings(settings["allow_personal_exclusion"]))
                        settingsRepository.setAllowChangingTrackStartDate(allowChanging)
                        settingsRepository.setAllowUpdatingTrackSims(parseBooleanSettings(settings["allow_updating_tracking_sims"]))
                        settingsRepository.setDefaultTrackStartDate(defaultDateStr)
                        settingsRepository.setCallTrackEnabled(parseBooleanSettings(settings["call_track"]))
                        settingsRepository.setCallRecordEnabled(parseBooleanSettings(settings["call_record_crm"]))

                        if (!allowChanging && !defaultDateStr.isNullOrBlank()) {
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                sdf.parse(defaultDateStr)?.let { date ->
                                    settingsRepository.setTrackStartDate(date.time)
                                }
                            } catch (e: Exception) {
                                Log.e("SettingsViewModel", "Failed to parse default date: $defaultDateStr", e)
                            }
                        }
                    }


                    @Suppress("UNCHECKED_CAST")
                    val planData = response.body()?.get("plan") as? Map<String, Any>
                    if (planData != null) {
                        settingsRepository.setPlanExpiryDate(planData["expiry_date"] as? String)
                        settingsRepository.setAllowedStorageGb((planData["allowed_storage_gb"] as? Number)?.toFloat() ?: 0f)
                        settingsRepository.setStorageUsedBytes((planData["storage_used_bytes"] as? Number)?.toLong() ?: 0L)
                    }

                    // For now, use ORGID as org name (could be fetched from backend in future)
                    _uiState.update { 
                        it.copy(
                            verificationStatus = "verified",
                            verifiedOrgName = orgId,
                            verifiedEmployeeName = employeeName,
                            pairingCode = trimmedCode,
                            allowPersonalExclusion = settingsRepository.isAllowPersonalExclusion(),
                            allowChangingTrackingStartDate = settingsRepository.isAllowChangingTrackStartDate(),
                            allowUpdatingTrackingSims = settingsRepository.isAllowUpdatingTrackSims(),
                            defaultTrackingStartingDate = settingsRepository.getDefaultTrackStartDate(),
                            callTrackEnabled = settingsRepository.isCallTrackEnabled(),
                            callRecordEnabled = settingsRepository.isCallRecordEnabled(),
                            planExpiryDate = settingsRepository.getPlanExpiryDate(),
                            allowedStorageGb = settingsRepository.getAllowedStorageGb(),
                            storageUsedBytes = settingsRepository.getStorageUsedBytes()
                        )
                    }
                    
                    Log.d("SettingsViewModel", "Verification successful: $employeeName from $orgId")
                } else {
                    val error = response.body()?.get("error")?.toString() 
                        ?: response.body()?.get("message")?.toString()
                        ?: "Invalid pairing code"
                    
                    _uiState.update { it.copy(verificationStatus = "failed") }
                    Toast.makeText(ctx, error, Toast.LENGTH_LONG).show()
                    Log.e("SettingsViewModel", "Verification failed: $error")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(verificationStatus = "failed") }
                Toast.makeText(ctx, "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("SettingsViewModel", "Verification error", e)
            } finally {
                _uiState.update { it.copy(isVerifying = false) }
            }
        }
    }

    fun retryRecordingUpload(compositeId: String) {
        viewModelScope.launch {
            // Reset status to PENDING so the worker will pick it up
            callDataRepository.updateRecordingSyncStatus(compositeId, com.miniclick.calltrackmanage.data.db.RecordingSyncStatus.PENDING)
            callDataRepository.updateSyncError(compositeId, null) // Clear error message
            
            // Trigger the worker immediately
            val recordingRequest = OneTimeWorkRequestBuilder<RecordingUploadWorker>().build()
            WorkManager.getInstance(getApplication()).enqueue(recordingRequest)
            
            Toast.makeText(getApplication(), "Retrying upload...", Toast.LENGTH_SHORT).show()
        }
    }


    fun syncCallManually() {
        val ctx = getApplication<Application>()
        val orgId = settingsRepository.getOrganisationId()
        val userId = settingsRepository.getUserId()
        val callerPhone1 = settingsRepository.getCallerPhoneSim1()
        val callerPhone2 = settingsRepository.getCallerPhoneSim2()

        // Check pairing code first
        if (orgId.isEmpty() || userId.isEmpty()) {
            Toast.makeText(ctx, "❌ Pairing Code not set\nPlease join an organisation first", Toast.LENGTH_LONG).show()
            return
        }
        
        // Then check phone numbers
        if (callerPhone1.isEmpty() && callerPhone2.isEmpty()) {
            Toast.makeText(ctx, "❌ Caller Phone not set\nPlease set at least one SIM phone number in Track Calls settings", Toast.LENGTH_LONG).show()
            return
        }

        // Start both workers: fast metadata sync first, then recording upload
        val syncRequest = OneTimeWorkRequestBuilder<CallSyncWorker>().build()
        val recordingRequest = OneTimeWorkRequestBuilder<RecordingUploadWorker>().build()
        val workManager = WorkManager.getInstance(ctx)
        
        _uiState.update { it.copy(isSyncing = true, lastSyncStats = "Refreshing...") }
        
        viewModelScope.launch {
            // Refresh from system first
            callDataRepository.syncFromSystemCallLog()
            
            // Then enqueue workers
            workManager.enqueue(syncRequest)
            workManager.enqueue(recordingRequest)
        }
        
        // Observe the work status
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(syncRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        androidx.work.WorkInfo.State.SUCCEEDED -> {
                            val total = workInfo.outputData.getInt("total_calls", 0)
                            val synced = workInfo.outputData.getInt("synced_now", 0)
                            val now = System.currentTimeMillis()
                            settingsRepository.setLastSyncTime(now)
                            // Reload settings as sync might have updated config
                            loadSettings()
                            _uiState.update { it.copy(
                                isSyncing = false, 
                                lastSyncStats = "Success! Found $total calls, uploaded $synced.",
                                lastSyncTime = now
                            )}
                        }
                        androidx.work.WorkInfo.State.FAILED -> {
                            _uiState.update { it.copy(isSyncing = false, lastSyncStats = "Failed to sync.") }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun addExcludedNumber(phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        viewModelScope.launch {
            callDataRepository.updateExclusion(phoneNumber.trim(), true)
            Toast.makeText(getApplication(), "Number excluded: $phoneNumber", Toast.LENGTH_SHORT).show()
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
            callDataRepository.updateExclusion(phoneNumber, false)
        }
    }

    fun clearAllAppData(onComplete: () -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            try {
                // Clear database
                callDataRepository.deleteAllData()
                
                // Clear settings
                settingsRepository.clearAllSettings()
                
                // Clear recording path
                recordingRepository.clearCustomPath()
                
                Toast.makeText(ctx, "All app data cleared successfully", Toast.LENGTH_SHORT).show()
                
                // Reload settings to reset UI
                loadSettings()
                
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(ctx, "Failed to clear data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveAccountInfo(onSuccess: () -> Unit = {}) {
        val ctx = getApplication<Application>()
        val pairingCode = _uiState.value.pairingCode.trim()
        val phone1 = _uiState.value.callerPhoneSim1.trim()
        val phone2 = _uiState.value.callerPhoneSim2.trim()
        
        // Validate required fields
        if (pairingCode.isEmpty()) {
            Toast.makeText(ctx, "Please enter a Pairing Code", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (phone1.isEmpty() && phone2.isEmpty()) {
            Toast.makeText(ctx, "Please enter at least one phone number", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate pairing code format (must be ORGID-USERID)
        if (!pairingCode.contains("-")) {
            Toast.makeText(ctx, "Invalid format. Use: ORGID-USERID (e.g., GOOGLE-123)", Toast.LENGTH_LONG).show()
            return
        }

        val parts = pairingCode.split("-", limit = 2)
        if (parts.size != 2) {
            Toast.makeText(ctx, "Invalid format. Use: ORGID-USERID (e.g., GOOGLE-123)", Toast.LENGTH_LONG).show()
            return
        }
        
        val orgId = parts[0].trim()
        val userId = parts[1].trim()

        // Validate both parts are not empty
        if (orgId.isEmpty() || userId.isEmpty()) {
            Toast.makeText(ctx, "Invalid format. Both ORGID and USERID are required", Toast.LENGTH_LONG).show()
            return
        }

        // Validate ORGID format (letters/numbers only, no special chars)
        if (!orgId.matches(Regex("^[A-Z0-9]+$"))) {
            Toast.makeText(ctx, "ORGID must contain only letters and numbers", Toast.LENGTH_LONG).show()
            return
        }

        // Validate USERID format (numbers only)
        if (!userId.matches(Regex("^[0-9]+$"))) {
            Toast.makeText(ctx, "USERID must be a number", Toast.LENGTH_LONG).show()
            return
        }

        val deviceId = android.provider.Settings.Secure.getString(
            ctx.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true) }
            try {
                Log.d("SettingsViewModel", "Verifying pairing code: $orgId-$userId with device: $deviceId")
                
                val response = com.miniclick.calltrackmanage.network.NetworkClient.api.verifyPairingCode(
                    action = "verify_pairing_code",
                    orgId = orgId,
                    userId = userId,
                    deviceId = deviceId
                )

                Log.d("SettingsViewModel", "Verification response: ${response.code()}, body: ${response.body()}")

                if (response.isSuccessful && response.body()?.get("success") == true) {
                    // SUCCESS! Now save everything to repository
                    settingsRepository.setOrganisationId(orgId)
                    settingsRepository.setUserId(userId)
                    settingsRepository.setCallerPhoneSim1(phone1)
                    settingsRepository.setCallerPhoneSim2(phone2)
                    
                    // Update UI state with saved values
                    _uiState.update { 
                        it.copy(
                            pairingCode = "$orgId-$userId",
                            callerPhoneSim1 = phone1,
                            callerPhoneSim2 = phone2
                        )
                    }
                    
                    val employeeName = response.body()?.get("employee_name")?.toString() ?: "User"
                    @Suppress("UNCHECKED_CAST")
                    val settings = response.body()?.get("settings") as? Map<String, Any>
                    
                    if (settings != null) {
                        val allowChanging = parseBooleanSettings(settings["allow_changing_tracking_start_date"])
                        val defaultDateStr = settings["default_tracking_starting_date"] as? String

                        settingsRepository.setAllowPersonalExclusion(parseBooleanSettings(settings["allow_personal_exclusion"]))
                        settingsRepository.setAllowChangingTrackStartDate(allowChanging)
                        settingsRepository.setAllowUpdatingTrackSims(parseBooleanSettings(settings["allow_updating_tracking_sims"]))
                        settingsRepository.setDefaultTrackStartDate(defaultDateStr)
                        settingsRepository.setCallTrackEnabled(parseBooleanSettings(settings["call_track"]))
                        settingsRepository.setCallRecordEnabled(parseBooleanSettings(settings["call_record_crm"]))

                        if (!allowChanging && !defaultDateStr.isNullOrBlank()) {
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                sdf.parse(defaultDateStr)?.let { date ->
                                    settingsRepository.setTrackStartDate(date.time)
                                }
                            } catch (e: Exception) {
                                Log.e("SettingsViewModel", "Failed to parse default date: $defaultDateStr", e)
                            }
                        }
                    }

                    val message = response.body()?.get("message")?.toString() ?: "Pairing successful"
                    
                    Toast.makeText(ctx, "✓ $message\nWelcome, $employeeName!", Toast.LENGTH_LONG).show()
                    Log.d("SettingsViewModel", "Pairing successful for employee: $employeeName")
                    
                    // Reload to update UI with new enterprise settings
                    loadSettings()
                    
                    onSuccess()
                } else {
                    // FAILED - Don't save anything
                    val error = response.body()?.get("error")?.toString() 
                        ?: response.body()?.get("message")?.toString()
                        ?: response.errorBody()?.string()
                        ?: "Verification failed. Please check your pairing code."
                    
                    Log.e("SettingsViewModel", "Verification failed: $error")
                    Toast.makeText(ctx, "✗ $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Verification error", e)
                Toast.makeText(ctx, "✗ Network error: ${e.localizedMessage}\nPlease check your internet connection.", Toast.LENGTH_LONG).show()
            } finally {
                _uiState.update { it.copy(isVerifying = false) }
            }
        }
    }

    fun connectVerifiedOrganisation(onSuccess: () -> Unit = {}) {
        val ctx = getApplication<Application>()
        val pairingCode = _uiState.value.pairingCode.trim()
        
        // Ensure it's already verified
        if (_uiState.value.verificationStatus != "verified") {
            Toast.makeText(ctx, "Please verify pairing code first", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Parse the pairing code
        val parts = pairingCode.split("-", limit = 2)
        if (parts.size != 2) {
            Toast.makeText(ctx, "Invalid pairing code format", Toast.LENGTH_SHORT).show()
            return
        }
        
        val orgId = parts[0].trim()
        val userId = parts[1].trim()
        
        // Save to repository
        settingsRepository.setOrganisationId(orgId)
        settingsRepository.setUserId(userId)
        
        // Update UI state
        _uiState.update {
            it.copy(pairingCode = "$orgId-$userId")
        }
        
        Toast.makeText(ctx, "✓ Connected to ${_uiState.value.verifiedOrgName}!", Toast.LENGTH_LONG).show()
        Log.d("SettingsViewModel", "Connected to organization: $orgId as user: $userId")
        
        onSuccess()
    }

    fun exportLogs() {
        viewModelScope.launch {
            com.miniclick.calltrackmanage.util.LogExporter.exportAndShareLogs(getApplication())
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
        val subscriptionId: Int
    )

    fun fetchRecentSystemCalls(onResult: (List<VerificationCall>) -> Unit) {
        val ctx = getApplication<Application>()
        
        // Log permission status
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_CALL_LOG) == 
                           android.content.pm.PackageManager.PERMISSION_GRANTED
        Log.d("SettingsViewModel", "fetchRecentSystemCalls: has READ_CALL_LOG = $hasPermission")
        
        if (!hasPermission) {
            Log.e("SettingsViewModel", "fetchRecentSystemCalls: Permission denied")
            onResult(emptyList())
            return
        }

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
                        
                        calls.add(VerificationCall(number, date, subId))
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
    
    fun setSimCalibration(simIndex: Int, subscriptionId: Int, hint: String) {
        if (simIndex == 0) { // Sim 1
            settingsRepository.setSim1SubscriptionId(subscriptionId)
            settingsRepository.setSim1CalibrationHint(hint)
            _uiState.update { it.copy(sim1SubId = subscriptionId, sim1CalibrationHint = hint) }
            Log.d("SettingsViewModel", "Calibrated Sim1 to SubId: $subscriptionId with hint: $hint")
        } else if (simIndex == 1) { // Sim 2
            settingsRepository.setSim2SubscriptionId(subscriptionId)
            settingsRepository.setSim2CalibrationHint(hint)
            _uiState.update { it.copy(sim2SubId = subscriptionId, sim2CalibrationHint = hint) }
            Log.d("SettingsViewModel", "Calibrated Sim2 to SubId: $subscriptionId with hint: $hint")
        }
        // Refresh SIM info
        fetchSimInfo()
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
}
