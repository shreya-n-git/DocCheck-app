package com.doccheck.app.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doccheck.app.data.local.SessionPreferences
import com.doccheck.app.data.model.ForensicModule
import com.doccheck.app.data.model.HomeStats
import com.doccheck.app.data.repository.ScanRepository
import com.doccheck.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeEvent {
    data class OpenResult(val scanId: String) : HomeEvent()
    data class ShowError(val message: String) : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    val stats: StateFlow<HomeStats> = scanRepository.observeHomeStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeStats(
                scanned = Constants.BASE_SCANNED_COUNT,
                tampered = Constants.BASE_TAMPERED_COUNT,
                accuracy = Constants.BASE_ACCURACY
            )
        )

    val isDarkMode: StateFlow<Boolean> = sessionPreferences.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun toggleDarkMode() {
        viewModelScope.launch {
            sessionPreferences.setDarkMode(!isDarkMode.value)
        }
    }

    fun analyzePickedImage(uri: Uri, preferredModule: ForensicModule?) {
        if (_isAnalyzing.value) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            val result = scanRepository.analyzeDocument(uri, preferredModule)
            _isAnalyzing.value = false
            result.onSuccess { scan ->
                _events.emit(HomeEvent.OpenResult(scan.id))
            }.onFailure { err ->
                _events.emit(HomeEvent.ShowError(err.localizedMessage ?: "Analysis failed"))
            }
        }
    }
}

