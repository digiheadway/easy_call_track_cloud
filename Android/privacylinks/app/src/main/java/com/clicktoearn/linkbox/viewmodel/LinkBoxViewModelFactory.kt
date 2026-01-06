package com.clicktoearn.linkbox.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clicktoearn.linkbox.LinkBoxApp
import com.clicktoearn.linkbox.data.FirestoreRepository
import com.clicktoearn.linkbox.data.LinkBoxRepository

class LinkBoxViewModelFactory(
    private val repository: FirestoreRepository,
    private val localRepository: LinkBoxRepository,
    private val app: LinkBoxApp
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LinkBoxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LinkBoxViewModel(repository, localRepository, app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
