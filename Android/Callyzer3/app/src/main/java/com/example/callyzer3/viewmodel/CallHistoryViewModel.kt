package com.example.callyzer3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callyzer3.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CallHistoryViewModel(private val repository: CallHistoryRepository) : ViewModel() {

    private val _allCalls = MutableStateFlow<List<Pair<CallLog, Contact?>>>(emptyList())
    val allCalls: StateFlow<List<Pair<CallLog, Contact?>>> = _allCalls

    private val _missedCalls = MutableStateFlow<List<Pair<CallLog, Contact?>>>(emptyList())
    val missedCalls: StateFlow<List<Pair<CallLog, Contact?>>> = _missedCalls

    private val _neverAttendedCalls = MutableStateFlow<List<Pair<CallLog, Contact?>>>(emptyList())
    val neverAttendedCalls: StateFlow<List<Pair<CallLog, Contact?>>> = _neverAttendedCalls

    private val _rejectedCalls = MutableStateFlow<List<Pair<CallLog, Contact?>>>(emptyList())
    val rejectedCalls: StateFlow<List<Pair<CallLog, Contact?>>> = _rejectedCalls

    init {
        loadAllCallLogs()
    }

    private fun loadAllCallLogs() {
        viewModelScope.launch {
            val allLogs = repository.getCallLogsWithContactInfo()
            _allCalls.value = allLogs

            _missedCalls.value = allLogs.filter { it.first.callStatus == CallStatus.MISSED }
            _neverAttendedCalls.value = allLogs.filter {
                it.first.callStatus == CallStatus.MISSED && it.first.duration == 0L
            }
            _rejectedCalls.value = allLogs.filter { it.first.callStatus == CallStatus.REJECTED }
        }
    }

    fun getAllCalls(): List<Pair<CallLog, Contact?>> = _allCalls.value

    fun getMissedCalls(): List<Pair<CallLog, Contact?>> = _missedCalls.value

    fun getNeverAttendedCalls(): List<Pair<CallLog, Contact?>> = _neverAttendedCalls.value

    fun getRejectedCalls(): List<Pair<CallLog, Contact?>> = _rejectedCalls.value

    fun getContact(phoneNumber: String): Contact? {
        return repository.getContact(phoneNumber)
    }

    fun excludeNumber(phoneNumber: String) {
        viewModelScope.launch {
            repository.addExcludedNumber(phoneNumber)
            loadAllCallLogs() // Refresh the lists
        }
    }

    fun addOrUpdateContactNote(phoneNumber: String, note: String) {
        viewModelScope.launch {
            val existingContact = repository.getContact(phoneNumber)
            val contact = if (existingContact != null) {
                existingContact.copy(notes = note)
            } else {
                Contact(
                    phoneNumber = phoneNumber,
                    notes = note
                )
            }
            repository.addOrUpdateContact(contact)
            loadAllCallLogs() // Refresh the lists
        }
    }

    fun refreshCallLogs() {
        loadAllCallLogs()
    }

    // Add some sample data for demo purposes
    fun addSampleData() {
        viewModelScope.launch {
            val sampleCalls = listOf(
                CallLog(
                    phoneNumber = "+1234567890",
                    contactName = "John Doe",
                    callType = CallType.INCOMING,
                    callStatus = CallStatus.ANSWERED,
                    duration = 120,
                    timestamp = java.time.LocalDateTime.now().minusHours(2)
                ),
                CallLog(
                    phoneNumber = "+1987654321",
                    contactName = "Jane Smith",
                    callType = CallType.MISSED,
                    callStatus = CallStatus.MISSED,
                    duration = 0,
                    timestamp = java.time.LocalDateTime.now().minusHours(1)
                ),
                CallLog(
                    phoneNumber = "+1555123456",
                    contactName = "Bob Johnson",
                    callType = CallType.INCOMING,
                    callStatus = CallStatus.REJECTED,
                    duration = 0,
                    timestamp = java.time.LocalDateTime.now().minusMinutes(30)
                )
            )

            sampleCalls.forEach { repository.addCallLog(it) }

            // Add some contacts with notes
            val sampleContacts = listOf(
                Contact(
                    phoneNumber = "+1234567890",
                    name = "John Doe",
                    notes = "Important client - follow up on project"
                ),
                Contact(
                    phoneNumber = "+1987654321",
                    name = "Jane Smith",
                    notes = "Family member"
                )
            )

            sampleContacts.forEach { repository.addOrUpdateContact(it) }

            loadAllCallLogs()
        }
    }
}
