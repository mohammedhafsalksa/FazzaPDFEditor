package com.fazza.pdfeditor.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fazza.pdfeditor.ui.screens.AnnotationType

@Entity(tableName = "annotations")
data class Annotation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pdfPath: String,
    val pageIndex: Int,
    val type: String, // HIGHLIGHT, UNDERLINE, DRAW, TEXT
    val color: Long,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val text: String? = null,         // for TEXT type
    val drawPoints: String? = null,   // JSON serialized list of points for DRAW type
    val createdAt: Long = System.currentTimeMillis()
)
