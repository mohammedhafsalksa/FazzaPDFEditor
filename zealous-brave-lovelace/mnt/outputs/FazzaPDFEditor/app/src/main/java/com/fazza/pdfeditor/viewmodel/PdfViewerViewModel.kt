package com.fazza.pdfeditor.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fazza.pdfeditor.data.model.Annotation
import com.fazza.pdfeditor.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ViewerUiState(
    val filePath: String = "",
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val isLoading: Boolean = true,
    val scale: Float = 1f,
    val selectedTool: AnnotationTool = AnnotationTool.NONE,
    val selectedColor: Long = 0xFFFFEB3B,
    val strokeWidth: Float = 4f,
    val showColorPicker: Boolean = false,
    val saveSuccess: Boolean? = null
)

enum class AnnotationTool { NONE, HIGHLIGHT, UNDERLINE, DRAW, TEXT, ERASER }

@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private val _pageCache = mutableMapOf<Int, Bitmap>()

    val annotations: StateFlow<List<Annotation>> = _uiState
        .filter { it.filePath.isNotEmpty() }
        .flatMapLatest { state ->
            repository.getAnnotationsForPdf(state.filePath)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun setCurrentPage(page: Int) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun setTool(tool: AnnotationTool) {
        _uiState.update { it.copy(selectedTool = if (it.selectedTool == tool) AnnotationTool.NONE else tool) }
    }

    fun setColor(color: Long) {
        _uiState.update { it.copy(selectedColor = color, showColorPicker = false) }
    }

    fun setScale(scale: Float) {
        _uiState.update { it.copy(scale = scale.coerceIn(0.5f, 4f)) }
    }

    fun toggleColorPicker() {
        _uiState.update { it.copy(showColorPicker = !it.showColorPicker) }
    }

    fun setStrokeWidth(width: Float) {
        _uiState.update { it.copy(strokeWidth = width) }
    }

    fun addAnnotation(annotation: Annotation) {
        viewModelScope.launch {
            repository.saveAnnotation(annotation)
        }
    }

    fun deleteAnnotation(annotation: Annotation) {
        viewModelScope.launch {
            repository.deleteAnnotation(annotation)
        }
    }

    fun saveAnnotatedPdf(outputPath: String) {
        viewModelScope.launch {
            val result = repository.exportAnnotatedPdf(
                _uiState.value.filePath,
                annotations.value,
                outputPath
            )
            _uiState.update { it.copy(saveSuccess = result.isSuccess) }
        }
    }

    fun clearSaveStatus() {
        _uiState.update { it.copy(saveSuccess = null) }
    }

    override fun onCleared() {
        super.onCleared()
        _pageCache.values.forEach { it.recycle() }
        _pageCache.clear()
    }
}
