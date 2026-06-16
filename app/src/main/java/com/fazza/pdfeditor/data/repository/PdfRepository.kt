package com.fazza.pdfeditor.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.fazza.pdfeditor.data.db.AnnotationDao
import com.fazza.pdfeditor.data.db.RecentFileDao
import com.fazza.pdfeditor.data.model.Annotation
import com.fazza.pdfeditor.data.model.RecentFile
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
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
    init {
        PDFBoxResourceLoader.init(context)
    }

    // ─── Recent Files ────────────────────────────────────────────────────────

    fun getRecentFiles(): Flow<List<RecentFile>> = recentFileDao.getAllRecentFiles()

    suspend fun addRecentFile(filePath: String) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext
        val pageCount = getPageCount(filePath)
        val recent = RecentFile(
            filePath = filePath,
            fileName = file.name,
            fileSize = file.length(),
            pageCount = pageCount,
            lastOpenedAt = System.currentTimeMillis()
        )
        recentFileDao.insert(recent)
    }

    suspend fun removeRecentFile(filePath: String) {
        recentFileDao.deleteByPath(filePath)
    }

    // ─── Page Count ──────────────────────────────────────────────────────────

    suspend fun getPageCount(filePath: String): Int = withContext(Dispatchers.IO) {
        try {
            val fd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close()
            fd.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    // ─── Page Rendering ──────────────────────────────────────────────────────

    suspend fun renderPage(
        filePath: String,
        pageIndex: Int,
        width: Int,
        height: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val fd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            if (pageIndex >= renderer.pageCount) {
                renderer.close(); fd.close()
                return@withContext null
            }
            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    // ─── Merge PDFs ───────────────────────────────────────────────────────────

    suspend fun mergePdfs(inputPaths: List<String>, outputPath: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val outputDoc = PDDocument()
                inputPaths.forEach { path ->
                    val doc = PDDocument.load(File(path))
                    doc.pages.forEach { page ->
                        outputDoc.addPage(page)
                    }
                    doc.close()
                }
                outputDoc.save(outputPath)
                outputDoc.close()
                Result.success(outputPath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── Split PDF ────────────────────────────────────────────────────────────

    suspend fun splitPdf(
        inputPath: String,
        fromPage: Int,
        toPage: Int,
        outputPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val doc = PDDocument.load(File(inputPath))
            val outputDoc = PDDocument()
            val lastPage = minOf(toPage - 1, doc.numberOfPages - 1)
            for (i in fromPage - 1..lastPage) {
                outputDoc.addPage(doc.pages[i])
            }
            outputDoc.save(outputPath)
            outputDoc.close()
            doc.close()
            Result.success(outputPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Add Text to PDF ─────────────────────────────────────────────────────

    suspend fun addTextToPdf(
        inputPath: String,
        pageIndex: Int,
        text: String,
        x: Float,
        y: Float,
        fontSize: Float = 12f,
        outputPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val doc = PDDocument.load(File(inputPath))
            val page = doc.pages[pageIndex]
            val contentStream = PDPageContentStream(
                doc, page, PDPageContentStream.AppendMode.APPEND, true, true
            )
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, fontSize)
            contentStream.newLineAtOffset(x, page.mediaBox.height - y)
            contentStream.showText(text)
            contentStream.endText()
            contentStream.close()
            doc.save(outputPath)
            doc.close()
            Result.success(outputPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Annotations ─────────────────────────────────────────────────────────

    fun getAnnotationsForPdf(pdfPath: String): Flow<List<Annotation>> =
        annotationDao.getAnnotationsForPdf(pdfPath)

    suspend fun saveAnnotation(annotation: Annotation): Long =
        annotationDao.insert(annotation)

    suspend fun deleteAnnotation(annotation: Annotation) =
        annotationDao.delete(annotation)

    suspend fun clearAnnotations(pdfPath: String) =
        annotationDao.deleteAllForPdf(pdfPath)

    // ─── Export annotated PDF ─────────────────────────────────────────────────

    suspend fun exportAnnotatedPdf(
        inputPath: String,
        annotations: List<Annotation>,
        outputPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val doc = PDDocument.load(File(inputPath))
            annotations.groupBy { it.pageIndex }.forEach { (pageIdx, pageAnnotations) ->
                if (pageIdx < doc.numberOfPages) {
                    val page = doc.pages[pageIdx]
                    val cs = PDPageContentStream(
                        doc, page, PDPageContentStream.AppendMode.APPEND, true, true
                    )
                    pageAnnotations.forEach { ann ->
                        val color = android.graphics.Color.valueOf(ann.color.toInt())
                        cs.setNonStrokingColor(
                            color.red(), color.green(), color.blue()
                        )
                        when (ann.type) {
                            "HIGHLIGHT" -> {
                                cs.setGraphicsStateParameters(
                                    com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState().apply {
                                        nonStrokingAlphaConstant = 0.4f
                                    }
                                )
                                cs.addRect(ann.x, page.mediaBox.height - ann.y - ann.height,
                                    ann.width, ann.height)
                                cs.fill()
                            }
                            "UNDERLINE" -> {
                                cs.setStrokingColor(color.red(), color.green(), color.blue())
                                cs.setLineWidth(1.5f)
                                cs.moveTo(ann.x, page.mediaBox.height - ann.y - ann.height)
                                cs.lineTo(ann.x + ann.width, page.mediaBox.height - ann.y - ann.height)
                                cs.stroke()
                            }
                            "TEXT" -> {
                                cs.beginText()
                                cs.setFont(PDType1Font.HELVETICA, 12f)
                                cs.newLineAtOffset(ann.x, page.mediaBox.height - ann.y)
                                cs.showText(ann.text ?: "")
                                cs.endText()
                            }
                        }
                    }
                    cs.close()
                }
            }
            doc.save(outputPath)
            doc.close()
            Result.success(outputPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
