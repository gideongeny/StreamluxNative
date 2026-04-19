# Keep MainActivity to prevent ClassNotFoundException in release builds
-keep class com.streamlux.app.MainActivity { *; }

# Hilt rules
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class * extends androidx.lifecycle.ViewModel
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep class * {
    @dagger.hilt.android.AndroidEntryPoint *;
}

# Firebase rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * { @androidx.room.Dao *; }
-keep class * { @androidx.room.Entity *; }

# Keep all classes in the app package to be safe with reflection-heavy libraries
-keep class com.streamlux.app.** { *; }
