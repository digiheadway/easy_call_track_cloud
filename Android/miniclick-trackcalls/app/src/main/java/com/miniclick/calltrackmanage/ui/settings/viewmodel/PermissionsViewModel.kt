package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.ui.settings.PermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val permissionManager = PermissionManager(application)

    val permissions: StateFlow<List<PermissionState>> = permissionManager.permissions
    val isOverlayPermissionGranted: StateFlow<Boolean> = permissionManager.isOverlayPermissionGranted

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        permissionManager.checkPermissions()
    }

    fun isOverlayPermissionNeeded(): Boolean = permissionManager.isOverlayPermissionNeeded()
}
