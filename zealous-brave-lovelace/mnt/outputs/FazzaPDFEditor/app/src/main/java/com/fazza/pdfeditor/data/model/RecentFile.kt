package com.fazza.pdfeditor.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val pageCount: Int,
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null
)
