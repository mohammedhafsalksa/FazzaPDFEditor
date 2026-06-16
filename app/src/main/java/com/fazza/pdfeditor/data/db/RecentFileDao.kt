package com.fazza.pdfeditor.data.db

import androidx.room.*
import com.fazza.pdfeditor.data.model.RecentFile
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY lastOpenedAt DESC")
    fun getAllRecentFiles(): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recentFile: RecentFile)

    @Delete
    suspend fun delete(recentFile: RecentFile)

    @Query("DELETE FROM recent_files WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)

    @Query("SELECT * FROM recent_files WHERE filePath = :filePath")
    suspend fun getByPath(filePath: String): RecentFile?
}
