package com.streamlux.app.ui.screens.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache

@OptIn(UnstableApi::class)
@Composable
fun NativeHlsPlayer(
    url: String,
    simpleCache: SimpleCache?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Create specific headers that match the StreamLux Website Identity
    val dataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .setDefaultRequestProperties(mapOf(
            "Referer" to "https://streamlux-67a84.web.app/",
            "Origin" to "https://streamlux-67a84.web.app",
            "X-Requested-With" to ""
        ))

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaSourceFactory = if (simpleCache != null) {
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(simpleCache)
                    .setUpstreamDataSourceFactory(dataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                HlsMediaSource.Factory(cacheDataSourceFactory)
            } else {
                HlsMediaSource.Factory(dataSourceFactory)
            }

            val hlsMediaSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(url))
            
            setMediaSource(hlsMediaSource)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                setBackgroundColor(android.graphics.Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}
