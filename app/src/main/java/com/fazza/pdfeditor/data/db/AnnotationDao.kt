package com.fazza.pdfeditor.data.db

import androidx.room.*
import com.fazza.pdfeditor.data.model.Annotation
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE pdfPath = :pdfPath ORDER BY pageIndex ASC")
    fun getAnnotationsForPdf(pdfPath: String): Flow<List<Annotation>>

    @Query("SELECT * FROM annotations WHERE pdfPath = :pdfPath AND pageIndex = :pageIndex")
    suspend fun getAnnotationsForPage(pdfPath: String, pageIndex: Int): List<Annotation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: Annotation): Long

    @Delete
    suspend fun delete(annotation: Annotation)

    @Query("DELETE FROM annotations WHERE pdfPath = :pdfPath")
    suspend fun deleteAllForPdf(pdfPath: String)

    @Update
    suspend fun update(annotation: Annotation)
}
