package com.fazza.pdfeditor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fazza.pdfeditor.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MergeSplitUiState(
    val selectedFiles: List<String> = emptyList(),
    val splitFilePath: String = "",
    val fromPage: Int = 1,
    val toPage: Int = 1,
    val pageCount: Int = 0,
    val isProcessing: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val activeTab: Int = 0 // 0=Merge, 1=Split
)

@HiltViewModel
class MergeSplitViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MergeSplitUiState())
    val uiState: StateFlow<MergeSplitUiState> = _uiState.asStateFlow()

    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addFileForMerge(filePath: String) {
        _uiState.update { it.copy(selectedFiles = it.selectedFiles + filePath) }
    }

    fun removeFileFromMerge(filePath: String) {
        _uiState.update { it.copy(selectedFiles = it.selectedFiles - filePath) }
    }

    fun moveFileUp(index: Int) {
        val files = _uiState.value.selectedFiles.toMutableList()
        if (index > 0) { files.add(index - 1, files.removeAt(index)) }
        _uiState.update { it.copy(selectedFiles = files) }
    }

    fun moveFileDown(index: Int) {
        val files = _uiState.value.selectedFiles.toMutableList()
        if (index < files.size - 1) { files.add(index + 1, files.removeAt(index)) }
        _uiState.update { it.copy(selectedFiles = files) }
    }

    fun setSplitFile(filePath: String) {
        _uiState.update { it.copy(splitFilePath = filePath) }
        viewModelScope.launch {
            val count = repository.getPageCount(filePath)
            _uiState.update { it.copy(pageCount = count, fromPage = 1, toPage = count) }
        }
    }

    fun setFromPage(page: Int) {
        _uiState.update { it.copy(fromPage = page.coerceIn(1, it.toPage)) }
    }

    fun setToPage(page: Int) {
        _uiState.update { it.copy(toPage = page.coerceIn(it.fromPage, it.pageCount)) }
    }

    fun mergePdfs(outputPath: String) {
        val files = _uiState.value.selectedFiles
        if (files.size < 2) {
            _uiState.update { it.copy(errorMessage = "Select at least 2 PDFs to merge") }
            return
        }
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val result = repository.mergePdfs(files, outputPath)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    successMessage = if (result.isSuccess) "PDFs merged successfully" else null,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun splitPdf(outputPath: String) {
        val state = _uiState.value
        if (state.splitFilePath.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Select a PDF to split") }
            return
        }
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val result = repository.splitPdf(
                state.splitFilePath, state.fromPage, state.toPage, outputPath
            )
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    successMessage = if (result.isSuccess) "PDF split successfully" else null,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
