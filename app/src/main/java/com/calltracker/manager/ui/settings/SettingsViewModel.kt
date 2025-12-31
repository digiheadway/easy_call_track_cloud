package com.calltracker.manager.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calltracker.manager.data.CallDataRepository
import com.calltracker.manager.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.calltracker.manager.worker.UploadWorker

data class PermissionState(
    val name: String,
    val isGranted: Boolean,
    val permission: String
)

data class SimInfo(
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String
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
    val excludedPersons: List<com.calltracker.manager.data.db.PersonDataEntity> = emptyList(),
    val lastSyncStats: String? = null,
    val whatsappPreference: String = "Always Ask",
    val availableWhatsappApps: List<AppInfo> = emptyList()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val callDataRepository = CallDataRepository(application)
    private val recordingRepository = com.calltracker.manager.data.RecordingRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        checkPermissions()
        fetchSimInfo()
        fetchWhatsappApps()
        observeExcludedPersons()
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
                whatsappPreference = settingsRepository.getWhatsappPreference()
            )
        }
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
        _uiState.update { it.copy(permissions = states) }
    }

    private fun fetchSimInfo() {
        val ctx = getApplication<Application>()
        if (androidx.core.app.ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_PHONE_STATE) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            val subscriptionManager = androidx.core.content.ContextCompat.getSystemService(ctx, android.telephony.SubscriptionManager::class.java)
            val infoList = subscriptionManager?.activeSubscriptionInfoList
            if (infoList != null) {
                val sims = infoList.map { 
                    SimInfo(it.simSlotIndex, it.displayName.toString(), it.carrierName.toString())
                }.sortedBy { it.slotIndex }
                _uiState.update { it.copy(availableSims = sims) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchWhatsappApps() {
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
                    val label = resolveInfo.loadLabel(packageManager).toString()
                    val packageName = resolveInfo.activityInfo.packageName
                    apps.add(AppInfo(label, packageName))
                }
        } catch (e: Exception) { e.printStackTrace() }
        
        // Method 2: Query with MATCH_UNINSTALLED_PACKAGES to catch clones in other profiles
        try {
            packageManager.queryIntentActivities(intent, 
                android.content.pm.PackageManager.MATCH_ALL or 
                android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
            ).forEach { resolveInfo ->
                val label = resolveInfo.loadLabel(packageManager).toString()
                val packageName = resolveInfo.activityInfo.packageName
                apps.add(AppInfo(label, packageName))
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
    
    fun updateRecordingPath(path: String) {
        recordingRepository.setRecordingPath(path)
        _uiState.update { it.copy(recordingPath = path) }
    }

    fun updatePairingCode(code: String) {
        // Parse pairing code: format is ORGID-USERID (e.g., GOOGLE-356)
        val upperCode = code.uppercase()
        
        if (upperCode.contains("-")) {
            val parts = upperCode.split("-", limit = 2)
            val orgId = parts[0].trim()
            val userId = parts.getOrElse(1) { "" }.trim()
            
            android.util.Log.d("SettingsViewModel", "Parsing pairing code: '$upperCode' -> orgId='$orgId', userId='$userId'")
            
            settingsRepository.setOrganisationId(orgId)
            settingsRepository.setUserId(userId)
        } else {
            // No hyphen yet - store the partial input as orgId for now
            android.util.Log.d("SettingsViewModel", "Pairing code incomplete (no hyphen): '$upperCode'")
            settingsRepository.setOrganisationId(upperCode.trim())
            settingsRepository.setUserId("")
        }
        
        _uiState.update { it.copy(pairingCode = upperCode) }
    }

    fun updateCallerPhoneSim1(phone: String) {
        settingsRepository.setCallerPhoneSim1(phone)
        _uiState.update { it.copy(callerPhoneSim1 = phone) }
    }

    fun updateCallerPhoneSim2(phone: String) {
        settingsRepository.setCallerPhoneSim2(phone)
        _uiState.update { it.copy(callerPhoneSim2 = phone) }
    }

    fun syncCallManually() {
        val ctx = getApplication<Application>()
        val orgId = settingsRepository.getOrganisationId()
        val userId = settingsRepository.getUserId()
        val callerPhone1 = settingsRepository.getCallerPhoneSim1()
        val callerPhone2 = settingsRepository.getCallerPhoneSim2()

        if (orgId.isEmpty() || userId.isEmpty() || (callerPhone1.isEmpty() && callerPhone2.isEmpty())) {
            Toast.makeText(ctx, "Please set Pairing Code and at least one Caller Phone", Toast.LENGTH_LONG).show()
            return
        }

        val request = OneTimeWorkRequestBuilder<UploadWorker>().build()
        val workManager = WorkManager.getInstance(ctx)
        
        _uiState.update { it.copy(isSyncing = true, lastSyncStats = "Starting...") }
        
        workManager.enqueue(request)
        
        // Observe the work status
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        androidx.work.WorkInfo.State.SUCCEEDED -> {
                            val total = workInfo.outputData.getInt("total_calls", 0)
                            val synced = workInfo.outputData.getInt("synced_now", 0)
                            _uiState.update { it.copy(
                                isSyncing = false, 
                                lastSyncStats = "Success! Found $total calls, uploaded $synced."
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
                recordingRepository.clearRecordingPath()
                
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

    fun saveAccountInfo() {
        val ctx = getApplication<Application>()
        val pairingCode = _uiState.value.pairingCode
        val phone1 = _uiState.value.callerPhoneSim1
        val phone2 = _uiState.value.callerPhoneSim2
        
        if (pairingCode.isEmpty() || (phone1.isEmpty() && phone2.isEmpty())) {
            Toast.makeText(ctx, "Please fill in Pairing Code and at least one Caller Phone", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!pairingCode.contains("-")) {
            Toast.makeText(ctx, "Invalid Pairing Code format. Use ORGID-USERID", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(ctx, "Account information saved", Toast.LENGTH_SHORT).show()
    }
    fun exportLogs() {
        viewModelScope.launch {
            com.calltracker.manager.util.LogExporter.exportAndShareLogs(getApplication())
        }
    }
}
