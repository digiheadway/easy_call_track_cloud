package com.miniclick.calltrackmanage.ui.home.viewmodel

import android.app.Application
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.util.system.WhatsAppUtils

object CallFlowManager {

    fun initiateCall(
        number: String,
        availableSims: List<com.miniclick.calltrackmanage.ui.settings.SimInfo>,
        callActionBehavior: String,
        application: Application,
        onShowSimPicker: (String) -> Unit
    ) {
        if (number.isBlank()) return
        
        if (availableSims.size > 1) {
            onShowSimPicker(number)
        } else {
            val subId = availableSims.firstOrNull()?.subscriptionId
            val forceDialer = callActionBehavior == "Open in Dialpad"
            com.miniclick.calltrackmanage.util.call.CallUtils.makeCall(application, number, subId, forceDialer)
        }
    }

    fun handleWhatsAppClick(
        number: String,
        whatsappPreference: String,
        availableWhatsappApps: List<com.miniclick.calltrackmanage.ui.settings.AppInfo>,
        application: Application,
        onShowSelectionDialog: (String, List<com.miniclick.calltrackmanage.ui.settings.AppInfo>) -> Unit
    ) {
        if (whatsappPreference == "Always Ask") {
            var apps = availableWhatsappApps
            if (apps.isEmpty()) {
                apps = WhatsAppUtils.fetchAvailableWhatsappApps(application)
            }
            onShowSelectionDialog(number, apps)
        } else {
            WhatsAppUtils.openWhatsApp(application, number, whatsappPreference)
        }
    }
}
