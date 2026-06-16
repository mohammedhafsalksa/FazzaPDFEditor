package com.fazza.pdfeditor.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.fazza.pdfeditor.data.model.Annotation
import com.fazza.pdfeditor.ui.theme.*
import com.fazza.pdfeditor.utils.FileUtils
import com.fazza.pdfeditor.viewmodel.AnnotationTool
import com.fazza.pdfeditor.viewmodel.PdfViewerViewModel
import kotlinx.coroutines.launch
import java.io.File

enum class AnnotationType { HIGHLIGHT, UNDERLINE, DRAW, TEXT, ERASER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    filePath: String,
    onBack: () -> Unit,
    onEditRequested: () -> Unit,
    viewModel: PdfViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val annotations by viewModel.annotations.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTextDialog by remember { mutableStateOf(false) }
    var pendingTextOffset by remember { mutableStateOf(Offset.Zero) }
    var textInputValue by remember { mutableStateOf("") }

    LaunchedEffect(filePath) {
        viewModel.loadPdf(filePath)
    }

    // Snackbar for save result
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.saveSuccess) {
        uiState.saveSuccess?.let { success ->
            snackbarHostState.showSnackbar(
                if (success) "PDF saved successfully" else "Error saving PDF"
            )
            viewModel.clearSaveStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = File(filePath).name,
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
                    // Page indicator
                    if (uiState.pageCount > 0) {
                        Text(
                            text = "${uiState.currentPage + 1} / ${uiState.pageCount}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    IconButton(onClick = onEditRequested) {
                        Icon(Icons.Default.Edit, "Edit PDF", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = {
                        val outputPath = FileUtils.generateOutputPath(context, "annotated")
                        viewModel.saveAnnotatedPdf(outputPath)
                    }) {
                        Icon(Icons.Default.Save, "Save", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = {
                        val file = File(filePath)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share PDF"))
                    }) {
                        Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        bottomBar = {
            AnnotationToolbar(
                selectedTool = uiState.selectedTool,
                selectedColor = Color(uiState.selectedColor),
                onToolSelected = viewModel::setTool,
                onColorClick = viewModel::toggleColorPicker
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                PdfPageList(
                    filePath = filePath,
                    pageCount = uiState.pageCount,
                    annotations = annotations,
                    selectedTool = uiState.selectedTool,
                    selectedColor = Color(uiState.selectedColor),
                    strokeWidth = uiState.strokeWidth,
                    onPageVisible = viewModel::setCurrentPage,
                    onAnnotationAdded = viewModel::addAnnotation,
                    onTextPositionPicked = { offset ->
                        pendingTextOffset = offset
                        showTextDialog = true
                    },
                    getPageBitmap = viewModel::getPageBitmap
                )
            }

            // Color picker overlay
            AnimatedVisibility(
                visible = uiState.showColorPicker,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)
            ) {
                ColorPickerRow(
                    onColorSelected = viewModel::setColor
                )
            }
        }

        // Text annotation dialog
        if (showTextDialog) {
            AlertDialog(
                onDismissRequest = { showTextDialog = false },
                title = { Text("Add Text") },
                text = {
                    OutlinedTextField(
                        value = textInputValue,
                        onValueChange = { textInputValue = it },
                        label = { Text("Enter text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (textInputValue.isNotBlank()) {
                            viewModel.addAnnotation(
                                Annotation(
                                    pdfPath = filePath,
                                    pageIndex = uiState.currentPage,
                                    type = "TEXT",
                                    color = uiState.selectedColor,
                                    x = pendingTextOffset.x,
                                    y = pendingTextOffset.y,
                                    width = 200f,
                                    height = 20f,
                                    text = textInputValue
                                )
                            )
                            textInputValue = ""
                            showTextDialog = false
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showTextDialog = false; textInputValue = "" }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun PdfPageList(
    filePath: String,
    pageCount: Int,
    annotations: List<Annotation>,
    selectedTool: AnnotationTool,
    selectedColor: Color,
    strokeWidth: Float,
    onPageVisible: (Int) -> Unit,
    onAnnotationAdded: (Annotation) -> Unit,
    onTextPositionPicked: (Offset) -> Unit,
    getPageBitmap: suspend (Int, Int, Int) -> android.graphics.Bitmap?
) {
    val listState = rememberLazyListState()

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
            PdfPageItem(
                filePath = filePath,
                pageIndex = pageIndex,
                annotations = annotations.filter { it.pageIndex == pageIndex },
                selectedTool = selectedTool,
                selectedColor = selectedColor,
                strokeWidth = strokeWidth,
                onAnnotationAdded = onAnnotationAdded,
                onTextPositionPicked = onTextPositionPicked,
                getPageBitmap = getPageBitmap
            )
        }
    }
}

@Composable
private fun PdfPageItem(
    filePath: String,
    pageIndex: Int,
    annotations: List<Annotation>,
    selectedTool: AnnotationTool,
    selectedColor: Color,
    strokeWidth: Float,
    onAnnotationAdded: (Annotation) -> Unit,
    onTextPositionPicked: (Offset) -> Unit,
    getPageBitmap: suspend (Int, Int, Int) -> android.graphics.Bitmap?
) {
    val density = LocalDensity.current
    val pageWidth = with(density) { 360.dp.toPx().toInt() }
    val pageHeight = (pageWidth * 1.414f).toInt() // A4 ratio

    var bitmap by remember(pageIndex) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var drawPaths by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentPath by remember { mutableStateOf(listOf<Offset>()) }

    LaunchedEffect(pageIndex) {
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
        ) {
            // PDF page bitmap
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Box(
                Modifier.fillMaxSize().background(Color.White),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            // Annotation overlay canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedTool, filePath, pageIndex) {
                        detectTransformGestures { _, pan, zoom, _ -> }
                    }
                    .pointerInput(selectedTool, filePath, pageIndex) {
                        when (selectedTool) {
                            AnnotationTool.DRAW -> {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        currentPath = currentPath + change.position
                                    },
                                    onDragEnd = {
                                        if (currentPath.size > 1) {
                                            drawPaths = drawPaths + listOf(currentPath)
                                            val pointsJson = currentPath.joinToString(";") {
                                                "${it.x},${it.y}"
                                            }
                                            onAnnotationAdded(
                                                Annotation(
                                                    pdfPath = filePath,
                                                    pageIndex = pageIndex,
                                                    type = "DRAW",
                                                    color = selectedColor.value.toLong(),
                                                    x = currentPath.minOf { it.x },
                                                    y = currentPath.minOf { it.y },
                                                    width = currentPath.maxOf { it.x } - currentPath.minOf { it.x },
                                                    height = currentPath.maxOf { it.y } - currentPath.minOf { it.y },
                                                    drawPoints = pointsJson
                                                )
                                            )
                                            currentPath = emptyList()
                                        }
                                    }
                                )
                            }
                            AnnotationTool.HIGHLIGHT, AnnotationTool.UNDERLINE -> {
                                detectDragGestures(
                                    onDragStart = { offset -> currentPath = listOf(offset) },
                                    onDrag = { change, _ -> currentPath = currentPath + change.position },
                                    onDragEnd = {
                                        if (currentPath.size > 1) {
                                            val startX = currentPath.first().x
                                            val startY = currentPath.first().y
                                            val endX = currentPath.last().x
                                            val height = if (selectedTool == AnnotationTool.HIGHLIGHT) 20f else 3f
                                            onAnnotationAdded(
                                                Annotation(
                                                    pdfPath = filePath,
                                                    pageIndex = pageIndex,
                                                    type = selectedTool.name,
                                                    color = selectedColor.value.toLong(),
                                                    x = minOf(startX, endX),
                                                    y = startY,
                                                    width = Math.abs(endX - startX),
                                                    height = height
                                                )
                                            )
                                            currentPath = emptyList()
                                        }
                                    }
                                )
                            }
                            AnnotationTool.TEXT -> {
                                detectTapGestures { offset ->
                                    onTextPositionPicked(offset)
                                }
                            }
                            else -> {}
                        }
                    }
            ) {
                // Draw saved annotations
                annotations.forEach { ann ->
                    when (ann.type) {
                        "HIGHLIGHT" -> {
                            drawRect(
                                color = Color(ann.color.toInt()).copy(alpha = 0.4f),
                                topLeft = Offset(ann.x, ann.y),
                                size = androidx.compose.ui.geometry.Size(ann.width, ann.height)
                            )
                        }
                        "UNDERLINE" -> {
                            drawLine(
                                color = Color(ann.color.toInt()),
                                start = Offset(ann.x, ann.y + ann.height),
                                end = Offset(ann.x + ann.width, ann.y + ann.height),
                                strokeWidth = strokeWidth
                            )
                        }
                        "DRAW" -> {
                            ann.drawPoints?.let { pts ->
                                val points = pts.split(";").mapNotNull { pt ->
                                    val parts = pt.split(",")
                                    if (parts.size == 2) Offset(parts[0].toFloat(), parts[1].toFloat()) else null
                                }
                                drawPath(points, Color(ann.color.toInt()), strokeWidth)
                            }
                        }
                        "TEXT" -> {
                            // Text drawn via Canvas drawContext nativeCanvas
                            drawContext.canvas.nativeCanvas.drawText(
                                ann.text ?: "",
                                ann.x,
                                ann.y,
                                android.graphics.Paint().apply {
                                    color = ann.color.toInt()
                                    textSize = 36f
                                }
                            )
                        }
                    }
                }

                // Draw current path (live preview)
                drawPath(currentPath, selectedColor, strokeWidth)
            }
        }
    }
}

private fun DrawScope.drawPath(points: List<Offset>, color: Color, strokeWidth: Float) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(
        width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round
    ))
}

@Composable
private fun AnnotationToolbar(
    selectedTool: AnnotationTool,
    selectedColor: Color,
    onToolSelected: (AnnotationTool) -> Unit,
    onColorClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(icon = Icons.Default.BorderColor, label = "Highlight",
                selected = selectedTool == AnnotationTool.HIGHLIGHT,
                tint = HighlightYellow) { onToolSelected(AnnotationTool.HIGHLIGHT) }

            ToolButton(icon = Icons.Default.FormatUnderlined, label = "Underline",
                selected = selectedTool == AnnotationTool.UNDERLINE,
                tint = HighlightRed) { onToolSelected(AnnotationTool.UNDERLINE) }

            ToolButton(icon = Icons.Default.Create, label = "Draw",
                selected = selectedTool == AnnotationTool.DRAW,
                tint = HighlightBlue) { onToolSelected(AnnotationTool.DRAW) }

            ToolButton(icon = Icons.Default.TextFields, label = "Text",
                selected = selectedTool == AnnotationTool.TEXT,
                tint = AnnotationBlack) { onToolSelected(AnnotationTool.TEXT) }

            // Color dot
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onColorClick)
            )
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) tint.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) tint else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = if (selected) tint else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ColorPickerRow(onColorSelected: (Long) -> Unit) {
    val colors = listOf(
        0xFFFFEB3B to "Yellow",
        0xFF66BB6A to "Green",
        0xFFEF5350 to "Red",
        0xFF42A5F5 to "Blue",
        0xFF212121 to "Black",
        0xFFFFFFFF to "White"
    )
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { (color, name) ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onColorSelected(color.toLong()) }
                )
            }
        }
    }
}
