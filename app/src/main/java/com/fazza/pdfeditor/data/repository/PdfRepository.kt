package com.fazza.pdfeditor.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.fazza.pdfeditor.data.db.AnnotationDao
import com.fazza.pdfeditor.data.db.RecentFileDao
import com.fazza.pdfeditor.data.model.Annotation
import com.fazza.pdfeditor.data.model.RecentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recentFileDao: RecentFileDao,
    private val annotationDao: AnnotationDao
) {

    // ── Recent Files ──────────────────────────────────────────────────────────

    fun getRecentFiles(): Flow<List<RecentFile>> = recentFileDao.getAllRecentFiles()

    suspend fun addRecentFile(filePath: String) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext
        recentFileDao.insert(
            RecentFile(
                filePath = filePath,
                fileName = file.name,
                fileSize = file.length(),
                pageCount = getPageCount(filePath),
                lastOpenedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeRecentFile(filePath: String) = recentFileDao.deleteByPath(filePath)

    // ── Page Count & Render ───────────────────────────────────────────────────

    suspend fun getPageCount(filePath: String): Int = withContext(Dispatchers.IO) {
        try {
            val fd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close(); fd.close()
            count
        } catch (e: Exception) { 0 }
    }

    suspend fun renderPage(
        filePath: String, pageIndex: Int, width: Int, height: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val fd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            if (pageIndex >= renderer.pageCount) { renderer.close(); fd.close(); return@withContext null }
            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close(); renderer.close(); fd.close()
            bitmap
        } catch (e: Exception) { null }
    }

    // ── Merge PDFs ────────────────────────────────────────────────────────────

    suspend fun mergePdfs(inputPaths: List<String>, outputPath: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val outDoc = PdfDocument()
                var pageNum = 1

                for (path in inputPaths) {
                    val fd = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fd)
                    for (i in 0 until renderer.pageCount) {
                        val srcPage = renderer.openPage(i)
                        val w = srcPage.width
                        val h = srcPage.height
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        srcPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        srcPage.close()

                        val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageNum++).create()
                        val outPage = outDoc.startPage(pageInfo)
                        outPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        bitmap.recycle()
                        outDoc.finishPage(outPage)
                    }
                    renderer.close(); fd.close()
                }

                File(outputPath).outputStream().use { outDoc.writeTo(it) }
                outDoc.close()
                Result.success(outputPath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── Split PDF ─────────────────────────────────────────────────────────────

    suspend fun splitPdf(
        inputPath: String, fromPage: Int, toPage: Int, outputPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val outDoc = PdfDocument()
            val fd = ParcelFileDescriptor.open(File(inputPath), ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            val last = minOf(toPage - 1, renderer.pageCount - 1)
            var pageNum = 1
            for (i in (fromPage - 1)..last) {
                val srcPage = renderer.openPage(i)
                val w = srcPage.width
                val h = srcPage.height
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                srcPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                srcPage.close()

                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageNum++).create()
                val outPage = outDoc.startPage(pageInfo)
                outPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                bitmap.recycle()
                outDoc.finishPage(outPage)
            }

            renderer.close(); fd.close()
            File(outputPath).outputStream().use { outDoc.writeTo(it) }
            outDoc.close()
            Result.success(outputPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Add Text (coming soon) ────────────────────────────────────────────────

    suspend fun addTextToPdf(
        inputPath: String, pageIndex: Int, text: String,
        x: Float, y: Float, fontSize: Float, outputPath: String
    ): Result<String> = Result.failure(Exception("Text editing coming soon!"))

    // ── Annotations ───────────────────────────────────────────────────────────

    fun getAnnotationsForPdf(pdfPath: String): Flow<List<Annotation>> =
        annotationDao.getAnnotationsForPdf(pdfPath)

    suspend fun saveAnnotation(annotation: Annotation): Long = annotationDao.insert(annotation)
    suspend fun deleteAnnotation(annotation: Annotation) = annotationDao.delete(annotation)
    suspend fun clearAnnotations(pdfPath: String) = annotationDao.deleteAllForPdf(pdfPath)

    suspend fun exportAnnotatedPdf(
        inputPath: String, annotations: List<Annotation>, outputPath: String
    ): Result<String> = Result.failure(Exception("Export coming soon!"))
}
