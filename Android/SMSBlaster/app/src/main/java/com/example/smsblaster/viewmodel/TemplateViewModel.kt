package com.example.smsblaster.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsblaster.data.AppDatabase
import com.example.smsblaster.data.model.Template
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateViewModel(application: Application) : AndroidViewModel(application) {
    private val templateDao = AppDatabase.getDatabase(application).templateDao()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    val templates: StateFlow<List<Template>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                templateDao.getAllTemplates()
            } else {
                templateDao.searchTemplates("%$query%")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val templateCount: StateFlow<Int> = templateDao.getTemplateCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun addTemplate(name: String, content: String) {
        viewModelScope.launch {
            val template = Template(
                name = name,
                content = content
            )
            templateDao.insertTemplate(template)
        }
    }
    
    fun updateTemplate(template: Template) {
        viewModelScope.launch {
            templateDao.updateTemplate(template.copy(updatedAt = System.currentTimeMillis()))
        }
    }
    
    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            templateDao.deleteTemplate(template)
        }
    }
    
    suspend fun getTemplateById(id: Long): Template? {
        return templateDao.getTemplateById(id)
    }
}
