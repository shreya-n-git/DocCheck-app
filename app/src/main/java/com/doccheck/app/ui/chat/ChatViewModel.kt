package com.doccheck.app.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doccheck.app.data.model.ChatMessage
import com.doccheck.app.data.model.ChatRole
import com.doccheck.app.data.model.ScanResult
import com.doccheck.app.data.repository.ChatRepository
import com.doccheck.app.data.repository.ChatStreamEvent
import com.doccheck.app.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val scanRepository: ScanRepository
) : ViewModel() {

    private val _attachedScan = MutableStateFlow<ScanResult?>(null)
    val attachedScan: StateFlow<ScanResult?> = _attachedScan.asStateFlow()

    private val _streamingMessage = MutableStateFlow<ChatMessage?>(null)
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    val messages: StateFlow<List<ChatMessage>> = combine(
        chatRepository.observeMessages(),
        _streamingMessage
    ) { persisted, streaming ->
        if (streaming != null) persisted + streaming else persisted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun attachScanContext(scan: ScanResult?) {
        if (scan != null) {
            _attachedScan.value = scan
        }
    }

    fun clearAttachedScan() {
        _attachedScan.value = null
    }

    fun attachAndAnalyzeImage(uri: Uri) {
        viewModelScope.launch {
            val result = scanRepository.analyzeDocument(uri, null)
            result.onSuccess { scan ->
                _attachedScan.value = scan
            }.onFailure { err ->
                _errorEvents.emit(err.localizedMessage ?: "Failed to attach document")
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            _streamingMessage.value = ChatMessage(
                id = "streaming_temp",
                role = ChatRole.ASSISTANT,
                text = "",
                timestamp = System.currentTimeMillis(),
                isStreaming = true
            )

            chatRepository.sendMessage(trimmed, _attachedScan.value).collect { event ->
                when (event) {
                    is ChatStreamEvent.Token -> {
                        _streamingMessage.value = _streamingMessage.value?.copy(
                            text = event.partialText,
                            isStreaming = true
                        )
                    }
                    is ChatStreamEvent.Completed -> {
                        _streamingMessage.value = null
                        _isSending.value = false
                    }
                    is ChatStreamEvent.Error -> {
                        _streamingMessage.value = null
                        _isSending.value = false
                        _errorEvents.emit(event.errorMessage)
                    }
                }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearChat()
        }
    }

    fun buildExportTranscript(): String {
        return messages.value.joinToString("\n\n") { msg ->
            val speaker = if (msg.role == ChatRole.USER) "You" else "DocCheck AI"
            "$speaker: ${msg.text}"
        }
    }
}

