package com.example.callyzer3.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CallHistoryRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("call_history_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    companion object {
        private const val KEY_CALL_LOGS = "call_logs"
        private const val KEY_CONTACTS = "contacts"
        private const val KEY_EXCLUDED_NUMBERS = "excluded_numbers"
    }

    // Call Log operations
    fun getAllCallLogs(): List<CallLog> {
        val json = prefs.getString(KEY_CALL_LOGS, "[]") ?: "[]"
        val type = object : TypeToken<List<CallLog>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }.filter { !it.isExcluded }
    }

    fun getCallLogsByType(callType: CallType): List<CallLog> {
        return getAllCallLogs().filter { it.callType == callType || callType == CallType.ALL }
    }

    fun addCallLog(callLog: CallLog) {
        val currentLogs = getAllCallLogs().toMutableList()
        currentLogs.add(callLog)
        saveCallLogs(currentLogs)
    }

    fun updateCallLog(callLog: CallLog) {
        val currentLogs = getAllCallLogs().toMutableList()
        val index = currentLogs.indexOfFirst { it.id == callLog.id }
        if (index != -1) {
            currentLogs[index] = callLog
            saveCallLogs(currentLogs)
        }
    }

    fun deleteCallLog(callLogId: Long) {
        val currentLogs = getAllCallLogs().toMutableList()
        currentLogs.removeAll { it.id == callLogId }
        saveCallLogs(currentLogs)
    }

    private fun saveCallLogs(callLogs: List<CallLog>) {
        val json = gson.toJson(callLogs)
        prefs.edit().putString(KEY_CALL_LOGS, json).apply()
    }

    // Contact operations
    fun getAllContacts(): List<Contact> {
        val json = prefs.getString(KEY_CONTACTS, "[]") ?: "[]"
        val type = object : TypeToken<List<Contact>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }.filter { !it.isExcluded }
    }

    fun getContact(phoneNumber: String): Contact? {
        return getAllContacts().find { it.phoneNumber == phoneNumber }
    }

    fun addOrUpdateContact(contact: Contact) {
        val currentContacts = getAllContacts().toMutableList()
        val existingIndex = currentContacts.indexOfFirst { it.phoneNumber == contact.phoneNumber }

        val updatedContact = contact.copy(updatedAt = LocalDateTime.now())

        if (existingIndex != -1) {
            currentContacts[existingIndex] = updatedContact
        } else {
            currentContacts.add(updatedContact)
        }
        saveContacts(currentContacts)
    }

    fun deleteContact(phoneNumber: String) {
        val currentContacts = getAllContacts().toMutableList()
        currentContacts.removeAll { it.phoneNumber == phoneNumber }
        saveContacts(currentContacts)
    }

    private fun saveContacts(contacts: List<Contact>) {
        val json = gson.toJson(contacts)
        prefs.edit().putString(KEY_CONTACTS, json).apply()
    }

    // Exclude operations
    fun getExcludedNumbers(): Set<String> {
        return prefs.getStringSet(KEY_EXCLUDED_NUMBERS, emptySet()) ?: emptySet()
    }

    fun addExcludedNumber(phoneNumber: String) {
        val excluded = getExcludedNumbers().toMutableSet()
        excluded.add(phoneNumber)
        prefs.edit().putStringSet(KEY_EXCLUDED_NUMBERS, excluded).apply()
    }

    fun removeExcludedNumber(phoneNumber: String) {
        val excluded = getExcludedNumbers().toMutableSet()
        excluded.remove(phoneNumber)
        prefs.edit().putStringSet(KEY_EXCLUDED_NUMBERS, excluded).apply()
    }

    fun isNumberExcluded(phoneNumber: String): Boolean {
        return getExcludedNumbers().contains(phoneNumber)
    }

    // Utility functions
    fun getCallLogsWithContactInfo(): List<Pair<CallLog, Contact?>> {
        val callLogs = getAllCallLogs()
        return callLogs.map { callLog ->
            val contact = getContact(callLog.phoneNumber)
            Pair(callLog, contact)
        }
    }

    fun getMissedCalls(): List<CallLog> {
        return getAllCallLogs().filter { it.callStatus == CallStatus.MISSED }
    }

    fun getNeverAttendedCalls(): List<CallLog> {
        // For demo purposes, we'll consider calls with 0 duration as never attended
        return getAllCallLogs().filter {
            it.callStatus == CallStatus.MISSED && it.duration == 0L
        }
    }

    fun getRejectedCalls(): List<CallLog> {
        return getAllCallLogs().filter { it.callStatus == CallStatus.REJECTED }
    }

    fun clearAllData() {
        prefs.edit()
            .remove(KEY_CALL_LOGS)
            .remove(KEY_CONTACTS)
            .remove(KEY_EXCLUDED_NUMBERS)
            .apply()
    }
}
