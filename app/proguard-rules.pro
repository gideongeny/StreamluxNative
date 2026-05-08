# StreamLux Native - ProGuard/R8 Hardening Rules

# Keep MainActivity and standard Android entries
-keep class com.streamlux.app.MainActivity { *; }
-keep class com.streamlux.app.StreamLuxApplication { *; }

# Hilt rules
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class * extends androidx.lifecycle.ViewModel
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep class * {
    @dagger.hilt.android.AndroidEntryPoint *;
}

# Firebase & Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * { @androidx.room.Dao *; }
-keep class * { @androidx.room.Entity *; }
-keep class * extends androidx.room.Entity

# Retrofit & Gson (CRITICAL: Do not obfuscate API models)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-keep class com.streamlux.app.data.model.** { *; }
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-keep @retrofit2.http.* interface * { *; }

# OkHttp
-keep class okhttp3.** { *; }

# Coil
-keep class coil.** { *; }

# Native YouTube Player
-keep class com.pierfrancescosoffritti.androidyoutubeplayer.** { *; }

# Keep all classes in the app package to be safe with reflection-heavy libraries
-keep class com.streamlux.app.** { *; }
