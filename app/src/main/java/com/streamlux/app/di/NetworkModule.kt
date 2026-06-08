package com.streamlux.app.di

import android.content.Context
import com.streamlux.app.data.api.TmdbApi
import com.streamlux.app.data.api.VylaApi
import com.streamlux.app.utils.Constants
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideVylaApi(okHttpClient: OkHttpClient): VylaApi {
        return Retrofit.Builder()
            .baseUrl("https://missourimonster-vyla.hf.space/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VylaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTmdbApi(okHttpClient: OkHttpClient): TmdbApi {
        return Retrofit.Builder()
            .baseUrl(Constants.API_GATEWAY_BASE)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSerpApiService(okHttpClient: OkHttpClient): com.streamlux.app.data.api.SerpApiService {
        return Retrofit.Builder()
            .baseUrl(Constants.API_GATEWAY_BASE)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.streamlux.app.data.api.SerpApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideStreamLuxApi(okHttpClient: OkHttpClient): com.streamlux.app.data.api.StreamLuxApi {
        return Retrofit.Builder()
            .baseUrl(Constants.API_GATEWAY_BASE)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.streamlux.app.data.api.StreamLuxApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        // Increased cache to 100 MB for high-traffic resilience
        val cacheSizeBytes = 100L * 1024L * 1024L
        val cache = Cache(context.cacheDir.resolve("http_cache"), cacheSizeBytes)

        // Retry interceptor with exponential backoff for 429/503 responses
        val retryInterceptor = Interceptor { chain ->
            var request = chain.request()
            var response: Response? = null
            var lastException: IOException? = null
            val maxRetries = 3

            for (attempt in 0..maxRetries) {
                try {
                    response?.close()
                    response = chain.proceed(request)

                    // If server returns 429 (rate limited) or 503 (overloaded), retry with backoff
                    if (response.code == 429 || response.code == 503) {
                        if (attempt < maxRetries) {
                            response.close()
                            // Exponential backoff: 1s, 2s, 4s
                            val backoffMs = (1000L * (1L shl attempt))
                            Thread.sleep(backoffMs)
                            continue
                        }
                    }
                    return@Interceptor response
                } catch (e: IOException) {
                    lastException = e
                    if (attempt < maxRetries) {
                        val backoffMs = (1000L * (1L shl attempt))
                        Thread.sleep(backoffMs)
                    }
                }
            }

            // If all retries failed, either return the last response or throw
            response ?: throw (lastException ?: IOException("All $maxRetries retries failed"))
        }

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
            .addInterceptor(retryInterceptor)
            .addInterceptor(offlineCacheInterceptor)
            .addNetworkInterceptor(onlineCacheInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
