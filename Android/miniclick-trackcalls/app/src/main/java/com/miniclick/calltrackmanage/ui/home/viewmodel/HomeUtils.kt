package com.miniclick.calltrackmanage.ui.home.viewmodel

import com.miniclick.calltrackmanage.ui.settings.SimInfo
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.ui.home.PersonGroup
import com.miniclick.calltrackmanage.util.formatting.getDateHeader
import com.miniclick.calltrackmanage.ui.settings.AppInfo
import com.miniclick.calltrackmanage.util.system.WhatsAppUtils
import android.content.Context
import android.telephony.SubscriptionManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

import java.util.Calendar

object HomeUtils {
    fun getStartOfDay(daysAgo: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return calendar.timeInMillis
    }

    fun getStartOfThisMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getStartOfPreviousMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1) // First day of previous month
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getSubIdForSim(simSel: String, state: HomeUiState): Int? {
        return when (simSel) {
            "Sim1" -> state.sim1SubId
            "Sim2" -> state.sim2SubId
            else -> null
        }
    }

    fun calculateFilterCount(uiState: HomeUiState): Int {
        var count = 0
        if (uiState.connectedFilter != ConnectedFilter.ALL) count++
        if (uiState.notesFilter != NotesFilter.ALL) count++
        if (uiState.personNotesFilter != PersonNotesFilter.ALL) count++
        if (uiState.contactsFilter != ContactsFilter.ALL) count++
        if (uiState.reviewedFilter != ReviewedFilter.ALL) count++
        if (uiState.customNameFilter != CustomNameFilter.ALL) count++
        if (uiState.minCallCount > 0) count++
        if (uiState.labelFilter.isNotEmpty()) count++
        return count
    }

    fun computeDateSummaries(currentState: HomeUiState): List<DateSectionSummary> {
        val items: List<Any> = if (currentState.viewMode == ViewMode.CALLS) {
            val filter = currentState.callTypeFilter
            currentState.tabFilteredLogs[filter] ?: emptyList()
        } else {
            val filter = currentState.personTabFilter
            currentState.tabFilteredPersons[filter]?.mapNotNull { currentState.personGroups[it.phoneNumber] } ?: emptyList()
        }

        if (items.isEmpty()) return emptyList()

        return if (currentState.viewMode == ViewMode.CALLS) {
            (items as List<CallDataEntity>).groupBy { getDateHeader(it.callDate) }
                .map { (label, logs) ->
                    val firstCall = logs.first()
                    val calendar = Calendar.getInstance().apply { timeInMillis = firstCall.callDate }
                    DateSectionSummary(
                        dateLabel = label,
                        dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(firstCall.callDate)),
                        totalCalls = logs.size,
                        uniqueCalls = logs.distinctBy { it.phoneNumber }.size,
                        dateMillis = firstCall.callDate,
                        month = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(firstCall.callDate)),
                        year = calendar.get(Calendar.YEAR)
                    )
                }
        } else {
            (items as List<PersonGroup>).groupBy { getDateHeader(it.lastCallDate) }
                .map { (label, groups) ->
                    val firstGroup = groups.first()
                    val calendar = Calendar.getInstance().apply { timeInMillis = firstGroup.lastCallDate }
                    DateSectionSummary(
                        dateLabel = label,
                        dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(firstGroup.lastCallDate)),
                        totalCalls = groups.sumOf { it.calls.size },
                        uniqueCalls = groups.size,
                        dateMillis = firstGroup.lastCallDate,
                        month = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(firstGroup.lastCallDate)),
                        year = calendar.get(Calendar.YEAR)
                    )
                }
        }
    }

    fun detectActiveSims(context: Context): List<SimInfo> {
        return if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            val sm = ContextCompat.getSystemService(context, SubscriptionManager::class.java)
            sm?.activeSubscriptionInfoList?.map { 
                SimInfo(it.simSlotIndex, it.displayName.toString(), it.carrierName.toString(), it.subscriptionId)
            }?.sortedBy { it.slotIndex } ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun fetchWhatsAppApps(context: Context): List<AppInfo> {
        return WhatsAppUtils.fetchAvailableWhatsappApps(context)
    }
}
