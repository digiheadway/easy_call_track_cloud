package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.miniclick.calltrackmanage.ui.settings.PermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Handles all permission-related logic extracted from SettingsViewModel.
 */
class PermissionManager(
    private val application: Application
) {
    private val _permissions = MutableStateFlow<List<PermissionState>>(emptyList())
    val permissions: StateFlow<List<PermissionState>> = _permissions.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    /**
     * Re-checks all required permissions and updates state.
     */
    fun checkPermissions() {
        val ctx = application
        val sdkInt = Build.VERSION.SDK_INT
        
        val permissionsToCheck = mutableListOf(
            android.Manifest.permission.READ_CALL_LOG to "Read Call Log",
            android.Manifest.permission.READ_CONTACTS to "Read Contacts",
            android.Manifest.permission.READ_PHONE_STATE to "Read Phone State",
            android.Manifest.permission.POST_NOTIFICATIONS to "Notifications"
        )
        
        if (sdkInt >= Build.VERSION_CODES.R) {
            permissionsToCheck.add(android.Manifest.permission.READ_PHONE_NUMBERS to "Phone Number Access")
        }

        if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToCheck.add(android.Manifest.permission.READ_MEDIA_AUDIO to "Read Audio")
        } else {
            permissionsToCheck.add(android.Manifest.permission.READ_EXTERNAL_STORAGE to "Read Storage")
        }

        val states = permissionsToCheck.map { (perm, name) ->
            val granted = ContextCompat.checkSelfPermission(ctx, perm) == 
                          android.content.pm.PackageManager.PERMISSION_GRANTED
            PermissionState(name, granted, perm)
        }
        
        val hasOverlay = if (sdkInt >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(ctx)
        } else {
            true
        }

        _permissions.update { states }
        _isOverlayPermissionGranted.update { hasOverlay }
    }

    /**
     * Checks if overlay permission is required but not granted.
     */
    fun isOverlayPermissionNeeded(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !Settings.canDrawOverlays(application)
        } else {
            false
        }
    }
}
