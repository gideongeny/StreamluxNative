package com.streamlux.app.di

import android.content.Context
import com.streamlux.app.data.api.TmdbApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTmdbApi(okHttpClient: OkHttpClient): TmdbApi {
        return Retrofit.Builder()
            .baseUrl("https://streamlux.vercel.app/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideStreamLuxApi(okHttpClient: OkHttpClient): com.streamlux.app.data.api.StreamLuxApi {
        return Retrofit.Builder()
            .baseUrl("https://streamlux.vercel.app/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.streamlux.app.data.api.StreamLuxApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val cacheSizeBytes = 30L * 1024L * 1024L
        val cache = Cache(context.cacheDir.resolve("http_cache"), cacheSizeBytes)

        val offlineCacheInterceptor = Interceptor { chain ->
            var request = chain.request()
            if (request.header("Cache-Control").isNullOrBlank()) {
                request = request.newBuilder()
                    .header("Cache-Control", "public, max-stale=604800")
                    .build()
            }
            chain.proceed(request)
        }

        val onlineCacheInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val url = chain.request().url

            val maxAgeSeconds = when {
                url.encodedPath.endsWith("/tmdb") && url.queryParameter("endpoint")?.contains("search/") == true -> 2 * 60 * 60
                url.encodedPath.endsWith("/tmdb") && url.queryParameter("endpoint")?.contains("/videos") == true -> 48 * 60 * 60
                url.encodedPath.endsWith("/tmdb") -> 48 * 60 * 60
                url.encodedPath.endsWith("/music/search") -> 15 * 60
                url.encodedPath.endsWith("/music/trending") -> 60 * 60
                url.encodedPath.endsWith("/sports/live") -> 2 * 60
                url.encodedPath.endsWith("/sports/upcoming") -> 10 * 60
                else -> 5 * 60
            }

            response.newBuilder()
                .header("Cache-Control", "public, max-age=$maxAgeSeconds")
                .removeHeader("Pragma")
                .build()
        }

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(offlineCacheInterceptor)
            .addNetworkInterceptor(onlineCacheInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
