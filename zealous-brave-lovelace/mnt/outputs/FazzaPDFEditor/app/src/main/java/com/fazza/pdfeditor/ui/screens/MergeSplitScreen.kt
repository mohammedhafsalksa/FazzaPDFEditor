package com.fazza.pdfeditor.ui.screens

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fazza.pdfeditor.utils.FileUtils
import com.fazza.pdfeditor.viewmodel.MergeSplitViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeSplitScreen(
    onBack: () -> Unit,
    viewModel: MergeSplitViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        (uiState.successMessage ?: uiState.errorMessage)?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val mergeFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                FileUtils.getFilePathFromUri(context, it)?.let { path ->
                    viewModel.addFileForMerge(path)
                }
            }
        }
    }

    val splitFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            FileUtils.getFilePathFromUri(context, it)?.let { path ->
                viewModel.setSplitFile(path)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Merge / Split", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Row
            TabRow(selectedTabIndex = uiState.activeTab) {
                Tab(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    text = { Text("Merge PDFs") },
                    icon = { Icon(Icons.Default.MergeType, null) }
                )
                Tab(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    text = { Text("Split PDF") },
                    icon = { Icon(Icons.Default.CallSplit, null) }
                )
            }

            when (uiState.activeTab) {
                0 -> MergeTab(
                    selectedFiles = uiState.selectedFiles,
                    isProcessing = uiState.isProcessing,
                    onAddFiles = { mergeFilePicker.launch(arrayOf("application/pdf")) },
                    onRemoveFile = viewModel::removeFileFromMerge,
                    onMoveUp = viewModel::moveFileUp,
                    onMoveDown = viewModel::moveFileDown,
                    onMerge = {
                        val out = FileUtils.generateOutputPath(context, "merged")
                        viewModel.mergePdfs(out)
                    }
                )
                1 -> SplitTab(
                    splitFilePath = uiState.splitFilePath,
                    fromPage = uiState.fromPage,
                    toPage = uiState.toPage,
                    pageCount = uiState.pageCount,
                    isProcessing = uiState.isProcessing,
                    onPickFile = { splitFilePicker.launch(arrayOf("application/pdf")) },
                    onFromPageChange = viewModel::setFromPage,
                    onToPageChange = viewModel::setToPage,
                    onSplit = {
                        val out = FileUtils.generateOutputPath(context, "split")
                        viewModel.splitPdf(out)
                    }
                )
            }
        }
    }
}

@Composable
private fun MergeTab(
    selectedFiles: List<String>,
    isProcessing: Boolean,
    onAddFiles: () -> Unit,
    onRemoveFile: (String) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onMerge: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Instructions
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add PDFs in the order you want them merged. Drag to reorder.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Add files button
        OutlinedButton(
            onClick = onAddFiles,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Add PDF Files")
        }

        // File list
        if (selectedFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No files selected.\nTap 'Add PDF Files' to begin.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(selectedFiles) { index, path ->
                    MergeFileItem(
                        filePath = path,
                        index = index,
                        total = selectedFiles.size,
                        onRemove = { onRemoveFile(path) },
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) }
                    )
                }
            }
        }

        // Merge button
        Button(
            onClick = onMerge,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = selectedFiles.size >= 2 && !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Merging…")
            } else {
                Icon(Icons.Default.MergeType, null)
                Spacer(Modifier.width(8.dp))
                Text("Merge ${selectedFiles.size} PDFs")
            }
        }
    }
}

@Composable
private fun MergeFileItem(
    filePath: String,
    index: Int,
    total: Int,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val file = File(filePath)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${index + 1}",
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                    .wrapContentSize(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                file.name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, "Move up", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onMoveDown, enabled = index < total - 1, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, "Move down", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SplitTab(
    splitFilePath: String,
    fromPage: Int,
    toPage: Int,
    pageCount: Int,
    isProcessing: Boolean,
    onPickFile: () -> Unit,
    onFromPageChange: (Int) -> Unit,
    onToPageChange: (Int) -> Unit,
    onSplit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Select a PDF and choose the page range to extract.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // File picker
        OutlinedButton(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FolderOpen, null)
            Spacer(Modifier.width(8.dp))
            Text(if (splitFilePath.isEmpty()) "Select PDF File" else File(splitFilePath).name,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        AnimatedVisibility(visible = splitFilePath.isNotEmpty() && pageCount > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total pages: $pageCount", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(16.dp))

                        Text("From Page: $fromPage", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = fromPage.toFloat(),
                            onValueChange = { onFromPageChange(it.toInt()) },
                            valueRange = 1f..pageCount.toFloat(),
                            steps = pageCount - 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        Text("To Page: $toPage", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = toPage.toFloat(),
                            onValueChange = { onToPageChange(it.toInt()) },
                            valueRange = 1f..pageCount.toFloat(),
                            steps = pageCount - 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text(
                            "Will extract ${toPage - fromPage + 1} page(s) (pages $fromPage to $toPage)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = onSplit,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isProcessing && splitFilePath.isNotEmpty()
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Splitting…")
                    } else {
                        Icon(Icons.Default.CallSplit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Split PDF")
                    }
                }
            }
        }
    }
}
