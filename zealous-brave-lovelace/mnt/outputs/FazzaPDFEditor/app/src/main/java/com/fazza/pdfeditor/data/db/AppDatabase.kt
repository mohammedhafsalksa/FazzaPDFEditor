package com.fazza.pdfeditor.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fazza.pdfeditor.data.model.Annotation
import com.fazza.pdfeditor.data.model.RecentFile

@Database(
    entities = [RecentFile::class, Annotation::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentFileDao(): RecentFileDao
    abstract fun annotationDao(): AnnotationDao
}
