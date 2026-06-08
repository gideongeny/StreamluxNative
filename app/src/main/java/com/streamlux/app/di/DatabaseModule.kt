package com.streamlux.app.di

import android.content.Context
import androidx.room.Room
import com.streamlux.app.data.local.LibraryDao
import com.streamlux.app.data.local.LibraryDatabase
import com.streamlux.app.data.local.MIGRATION_4_5
import com.streamlux.app.data.local.MIGRATION_5_6
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLibraryDatabase(@ApplicationContext context: Context): LibraryDatabase {
        return Room.databaseBuilder(
            context,
            LibraryDatabase::class.java,
            "streamlux_library.db"
        )
        .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideLibraryDao(db: LibraryDatabase): LibraryDao {
        return db.libraryDao()
    }
}
