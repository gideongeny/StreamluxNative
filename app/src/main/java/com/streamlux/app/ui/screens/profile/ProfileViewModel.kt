package com.streamlux.app.ui.screens.profile

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.streamlux.app.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val settingsManager: com.streamlux.app.data.local.SettingsManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _isNightMode = MutableStateFlow(settingsManager.isNightModeEnabled)
    val isNightMode: StateFlow<Boolean> = _isNightMode

    private val _isBackgroundAudio = MutableStateFlow(settingsManager.isBackgroundAudioEnabled)
    val isBackgroundAudio: StateFlow<Boolean> = _isBackgroundAudio

    private val _isAutoplay = MutableStateFlow(settingsManager.isAutoplayEnabled)
    val isAutoplay: StateFlow<Boolean> = _isAutoplay

    init {
        _currentUser.value = auth.currentUser
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    fun toggleNightMode(enabled: Boolean) {
        settingsManager.isNightModeEnabled = enabled
        _isNightMode.value = enabled
    }

    fun toggleBackgroundAudio(enabled: Boolean) {
        settingsManager.isBackgroundAudioEnabled = enabled
        _isBackgroundAudio.value = enabled
    }

    fun toggleAutoplay(enabled: Boolean) {
        settingsManager.isAutoplayEnabled = enabled
        _isAutoplay.value = enabled
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(Constants.GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is GoogleIdTokenCredential) {
                    val idToken = credential.idToken
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            auth.signInAnonymously()
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
