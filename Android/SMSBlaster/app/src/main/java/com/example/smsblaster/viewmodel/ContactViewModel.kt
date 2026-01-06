package com.example.smsblaster.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsblaster.data.AppDatabase
import com.example.smsblaster.data.model.Contact
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri

@OptIn(ExperimentalCoroutinesApi::class)
class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val contactDao = AppDatabase.getDatabase(application).contactDao()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    val contacts: StateFlow<List<Contact>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                contactDao.getAllContacts()
            } else {
                contactDao.searchContacts("%$query%")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val contactCount: StateFlow<Int> = contactDao.getContactCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    private val _selectedContactIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedContactIds: StateFlow<Set<Long>> = _selectedContactIds.asStateFlow()
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun addContact(
        name: String,
        phone: String,
        customKeys: Map<String, String> = emptyMap(),
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val contact = Contact(
                name = name,
                phone = phone,
                customKeys = customKeys,
                tags = tags
            )
            contactDao.insertContact(contact)
        }
    }
    
    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            contactDao.updateContact(contact.copy(updatedAt = System.currentTimeMillis()))
        }
    }
    
    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            contactDao.deleteContact(contact)
        }
    }
    
    fun toggleContactSelection(contactId: Long) {
        _selectedContactIds.update { currentSelection ->
            if (contactId in currentSelection) {
                currentSelection - contactId
            } else {
                currentSelection + contactId
            }
        }
    }
    
    fun selectAllContacts() {
        viewModelScope.launch {
            val allIds = contacts.value.map { it.id }.toSet()
            _selectedContactIds.value = allIds
        }
    }
    
    fun clearSelection() {
        _selectedContactIds.value = emptySet()
    }
    
    suspend fun getContactById(id: Long): Contact? {
        return contactDao.getContactById(id)
    }
    
    suspend fun getContactsByIds(ids: List<Long>): List<Contact> {
        return contactDao.getContactsByIds(ids)
    }

    fun importContacts(uri: Uri, onResult: (Int, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val reader = inputStream?.bufferedReader()
                val contactsToImport = mutableListOf<Contact>()
                
                reader?.useLines { lines ->
                    lines.forEachIndexed { index, line ->
                        if (line.isBlank()) return@forEachIndexed
                        
                        // Try to handle simple comma-separated values
                        val parts = line.split(",").map { it.trim().removeSurrounding("\"") }
                        
                        if (index == 0 && parts.any { it.equals("name", ignoreCase = true) || it.equals("phone", ignoreCase = true) }) {
                            // This looks like a header line, skip it
                            return@forEachIndexed
                        }
                        
                        if (parts.size >= 2) {
                            val name = parts[0]
                            val phone = parts[1]
                            
                            if (name.isNotEmpty() && phone.isNotEmpty()) {
                                contactsToImport.add(
                                    Contact(
                                        name = name,
                                        phone = phone
                                    )
                                )
                            }
                        }
                    }
                }
                
                if (contactsToImport.isNotEmpty()) {
                    contactDao.insertContacts(contactsToImport)
                }
                
                withContext(Dispatchers.Main) {
                    onResult(contactsToImport.size, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(0, e.message ?: "Unknown error during import")
                }
            }
        }
    }
}
