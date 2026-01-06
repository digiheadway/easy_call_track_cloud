package com.example.callyzer3.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.callyzer3.data.CallHistoryRepository

class CallHistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CallHistoryViewModel(CallHistoryRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
