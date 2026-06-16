package com.fazza.pdfeditor.viewmodel

import android.graphics.Bitmap
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

data class EditorUiState(
    val filePath: String = "",
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean? = null,
    val addTextMode: Boolean = false,
    val pendingTextX: Float = 0f,
    val pendingTextY: Float = 0f,
    val textInput: String = "",
    val fontSize: Float = 14f,
    val errorMessage: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _pageCache = mutableMapOf<Int, Bitmap>()

    fun loadPdf(filePath: String) {
        _uiState.update { it.copy(filePath = filePath, isLoading = true) }
        viewModelScope.launch {
            val count = repository.getPageCount(filePath)
            _uiState.update { it.copy(pageCount = count, isLoading = false) }
        }
    }

    suspend fun getPageBitmap(pageIndex: Int, width: Int, height: Int): Bitmap? {
        _pageCache[pageIndex]?.let { return it }
        val bitmap = repository.renderPage(_uiState.value.filePath, pageIndex, width, height)
        if (bitmap != null) _pageCache[pageIndex] = bitmap
        return bitmap
    }

    fun enterAddTextMode(x: Float, y: Float) {
        _uiState.update { it.copy(addTextMode = true, pendingTextX = x, pendingTextY = y, textInput = "") }
    }

    fun cancelAddText() {
        _uiState.update { it.copy(addTextMode = false, textInput = "") }
    }

    fun setTextInput(text: String) {
        _uiState.update { it.copy(textInput = text) }
    }

    fun setFontSize(size: Float) {
        _uiState.update { it.copy(fontSize = size) }
    }

    fun setCurrentPage(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun commitTextAdd(outputPath: String) {
        val state = _uiState.value
        if (state.textInput.isBlank()) {
            _uiState.update { it.copy(addTextMode = false) }
            return
        }
        _uiState.update { it.copy(isSaving = true, addTextMode = false) }
        viewModelScope.launch {
            val result = repository.addTextToPdf(
                inputPath = state.filePath,
                pageIndex = state.currentPage,
                text = state.textInput,
                x = state.pendingTextX,
                y = state.pendingTextY,
                fontSize = state.fontSize,
                outputPath = outputPath
            )
            _pageCache.clear() // invalidate cache after edit
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveSuccess = result.isSuccess,
                    errorMessage = result.exceptionOrNull()?.message,
                    filePath = if (result.isSuccess) outputPath else it.filePath
                )
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(saveSuccess = null, errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        _pageCache.values.forEach { it.recycle() }
        _pageCache.clear()
    }
}
