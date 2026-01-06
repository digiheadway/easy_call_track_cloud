package com.example.callyzer4.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.callyzer4.data.*
import com.example.callyzer4.repository.CallHistoryRepository
import com.example.callyzer4.utils.NoteStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallHistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = CallHistoryRepository(application)
    private val noteStorage = NoteStorage(application)
    
    private val _callGroups = MutableStateFlow<List<CallGroup>>(emptyList())
    val callGroups: StateFlow<List<CallGroup>> = _callGroups.asStateFlow()
    
    private val _filter = MutableStateFlow(CallFilter())
    val filter: StateFlow<CallFilter> = _filter.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _statistics = MutableStateFlow<Map<String, Any>>(emptyMap())
    val statistics: StateFlow<Map<String, Any>> = _statistics.asStateFlow()
    
    private val _notes = MutableStateFlow<Map<String, String>>(emptyMap())
    val notes: StateFlow<Map<String, String>> = _notes.asStateFlow()
    
    init {
        loadCallHistory()
        loadNotes()
    }
    
    fun loadCallHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val groups = repository.getGroupedCallHistory(_filter.value)
                _callGroups.value = groups
                
                val stats = repository.getCallStatistics()
                _statistics.value = stats
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadNotes() {
        viewModelScope.launch {
            try {
                val allNotes = noteStorage.getAllNotes()
                _notes.value = allNotes
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun saveNote(phoneNumber: String, note: String) {
        viewModelScope.launch {
            try {
                noteStorage.saveNote(phoneNumber, note)
                _notes.value = _notes.value + (phoneNumber to note)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun getNote(phoneNumber: String): String {
        return _notes.value[phoneNumber] ?: ""
    }
    
    fun updateFilter(newFilter: CallFilter) {
        _filter.value = newFilter
        loadCallHistory()
    }
    
    fun clearFilter() {
        _filter.value = CallFilter()
        loadCallHistory()
    }
}
