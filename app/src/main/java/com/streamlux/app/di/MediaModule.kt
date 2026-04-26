package com.streamlux.app.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun provideSimpleCache(@ApplicationContext context: Context): SimpleCache {
        val cacheSize = 500L * 1024 * 1024 // 500MB max cache
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)
        val databaseProvider = StandaloneDatabaseProvider(context)
        val cacheDir = File(context.cacheDir, "streamlux_media_cache")
        return SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }

    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context
    ): ExoPlayer {
        return ExoPlayer.Builder(context).build()
    }
}
