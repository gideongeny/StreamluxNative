package com.streamlux.app.ui.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlux.app.data.model.TmdbItem
import com.streamlux.app.data.repository.TmdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val repository: TmdbRepository
) : ViewModel() {

    private val _collectionInfo = MutableStateFlow<TmdbItem?>(null)
    val collectionInfo: StateFlow<TmdbItem?> = _collectionInfo

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadCollection(collectionId: Int) {
        if (_collectionInfo.value?.id == collectionId) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val detail = repository.fetchDetail("/collection/$collectionId")
                _collectionInfo.value = detail
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isLoading.value = false
            }
        }
    }
}
