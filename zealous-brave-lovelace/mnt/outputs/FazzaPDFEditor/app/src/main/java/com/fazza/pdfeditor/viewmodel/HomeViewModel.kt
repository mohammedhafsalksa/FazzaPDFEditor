package com.fazza.pdfeditor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fazza.pdfeditor.data.model.RecentFile
import com.fazza.pdfeditor.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    val recentFiles: StateFlow<List<RecentFile>> = repository
        .getRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onFileOpened(filePath: String) {
        viewModelScope.launch {
            repository.addRecentFile(filePath)
        }
    }

    fun removeFromRecent(filePath: String) {
        viewModelScope.launch {
            repository.removeRecentFile(filePath)
        }
    }
}
