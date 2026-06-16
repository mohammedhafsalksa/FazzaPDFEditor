package com.fazza.pdfeditor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fazza.pdfeditor.utils.FileUtils
import com.fazza.pdfeditor.viewmodel.EditorViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    filePath: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(filePath) { viewModel.loadPdf(filePath) }

    LaunchedEffect(uiState.saveSuccess, uiState.errorMessage) {
        uiState.saveSuccess?.let { success ->
            snackbarHostState.showSnackbar(if (success) "Text added and saved" else uiState.errorMessage ?: "Error")
            viewModel.clearStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit: ${File(filePath).name}",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                actions = {
                    if (uiState.pageCount > 0) {
                        Text(
                            "${uiState.currentPage + 1} / ${uiState.pageCount}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            EditorToolbar(
                addTextMode = uiState.addTextMode,
                onAddTextToggle = {
                    if (uiState.addTextMode) viewModel.cancelAddText()
                    else viewModel.enterAddTextMode(0f, 0f)
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                EditorPageList(
                    filePath = uiState.filePath.ifEmpty { filePath },
                    pageCount = uiState.pageCount,
                    currentPage = uiState.currentPage,
                    addTextMode = uiState.addTextMode,
                    onPageVisible = viewModel::setCurrentPage,
                    onTap = { offset, pageIndex ->
                        if (uiState.addTextMode) {
                            viewModel.enterAddTextMode(offset.x, offset.y)
                        }
                    },
                    getPageBitmap = viewModel::getPageBitmap
                )
            }

            if (uiState.isSaving) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Saving changes…")
                        }
                    }
                }
            }

            // Text input panel at bottom when in add-text mode and a position is chosen
            AnimatedVisibility(
                visible = uiState.addTextMode && uiState.pendingTextX > 0,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Add Text to Page ${uiState.currentPage + 1}", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.textInput,
                            onValueChange = viewModel::setTextInput,
                            label = { Text("Enter text") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 3
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = viewModel::cancelAddText,
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Button(
                                onClick = {
                                    val out = FileUtils.generateOutputPath(context, "edited")
                                    viewModel.commitTextAdd(out)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = uiState.textInput.isNotBlank()
                            ) { Text("Apply") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorPageList(
    filePath: String,
    pageCount: Int,
    currentPage: Int,
    addTextMode: Boolean,
    onPageVisible: (Int) -> Unit,
    onTap: (androidx.compose.ui.geometry.Offset, Int) -> Unit,
    getPageBitmap: suspend (Int, Int, Int) -> android.graphics.Bitmap?
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val pageWidth = with(density) { 360.dp.toPx().toInt() }
    val pageHeight = (pageWidth * 1.414f).toInt()

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onPageVisible(listState.firstVisibleItemIndex)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Color(0xFF808080)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(pageCount) { pageIndex ->
            var bitmap by remember(pageIndex, filePath) { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(pageIndex, filePath) {
                bitmap = getPageBitmap(pageIndex, pageWidth, pageHeight)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(pageWidth.toFloat() / pageHeight.toFloat())
                        .pointerInput(addTextMode, pageIndex) {
                            if (addTextMode) {
                                detectTapGestures { offset -> onTap(offset, pageIndex) }
                            }
                        }
                ) {
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Page ${pageIndex + 1}",
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    if (addTextMode) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1565C0).copy(alpha = 0.05f))
                                .border(2.dp, Color(0xFF1565C0), RoundedCornerShape(4.dp))
                        )
                        Text(
                            "Tap to place text",
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1565C0)
                        )
                    }

                    // Page number chip
                    Card(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Text(
                            "${pageIndex + 1}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun EditorToolbar(
    addTextMode: Boolean,
    onAddTextToggle: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Edit Tools:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            FilterChip(
                selected = addTextMode,
                onClick = onAddTextToggle,
                label = { Text("Add Text") },
                leadingIcon = {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
            // Placeholder for future tools
            FilterChip(
                selected = false,
                onClick = { /* Coming soon */ },
                label = { Text("Images") },
                leadingIcon = {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                },
                enabled = false
            )
        }
    }
}
