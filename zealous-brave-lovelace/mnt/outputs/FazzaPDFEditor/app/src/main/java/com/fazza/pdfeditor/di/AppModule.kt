package com.fazza.pdfeditor.di

import android.content.Context
import androidx.room.Room
import com.fazza.pdfeditor.data.db.AppDatabase
import com.fazza.pdfeditor.data.db.AnnotationDao
import com.fazza.pdfeditor.data.db.RecentFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fazza_pdf_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideRecentFileDao(db: AppDatabase): RecentFileDao = db.recentFileDao()

    @Provides
    @Singleton
    fun provideAnnotationDao(db: AppDatabase): AnnotationDao = db.annotationDao()
}
