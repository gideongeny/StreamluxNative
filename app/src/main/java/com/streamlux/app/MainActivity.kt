package com.streamlux.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.gms.ads.MobileAds
import com.streamlux.app.ads.AdManager
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.streamlux.app.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        var isPlayingVideo = false
    }
    
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob SDK
        MobileAds.initialize(this) { initStatus ->
            // Pre-load ads after SDK is ready
            AdManager.loadInterstitial(this)
            AdManager.loadAppOpenAd(this)
        }

        setContent {
            val isNightMode by viewModel.isNightMode.collectAsState()
            
            com.streamlux.app.ui.theme.StreamLuxTheme(darkTheme = isNightMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    com.streamlux.app.ui.navigation.StreamLuxApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh settings in case they were changed in other activities/screens
        viewModel.refreshSettings()
        // Show App Open Ad when user returns to app
        AdManager.showAppOpenAd(this)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlayingVideo && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val params = android.app.PictureInPictureParams.Builder().build()
            enterPictureInPictureMode(params)
        }
    }
}
