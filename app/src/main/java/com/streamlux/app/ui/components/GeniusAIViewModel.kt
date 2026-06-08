package com.streamlux.app.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.streamlux.app.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class GeniusAIViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen

    private val _messages = MutableStateFlow(listOf(ChatMessage("bot", "Greetings! I am StreamLux Genius ✨ How can I elevate your streaming journey today?")))
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    fun setIsOpen(open: Boolean) {
        _isOpen.value = open
    }

    fun triggerAutoQuery(query: String) {
        if (_isOpen.value) return // Don't trigger if already open/active
        
        _isOpen.value = true
        sendMessage("I can't find '$query', can you help me find it or recommend something similar?")
    }

    fun sendMessage(text: String) {
        val userText = text.trim()
        if (userText.isEmpty() || _isTyping.value) return
        
        _messages.value = _messages.value + ChatMessage("user", userText)
        _isTyping.value = true

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    callGroqApi(_messages.value)
                }
                _messages.value = _messages.value + ChatMessage("bot", response)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("bot", "I'm having trouble connecting right now. Please try again! 🔄")
            } finally {
                _isTyping.value = false
            }
        }
    }

    private fun callGroqApi(history: List<ChatMessage>): String {
        val messagesArray = JSONArray()
        // System prompt
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", GroqSystemPrompt)
        })
        // Context
        history.takeLast(10).forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.role == "user") "user" else "assistant")
                put("content", msg.text)
            })
        }
        
        val body = JSONObject().apply {
            put("model", "llama-3.3-70b-versatile")
            put("messages", messagesArray)
            put("max_tokens", 300)
            put("temperature", 0.8)
        }

        val url = URL("https://api.groq.com/openai/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer ${Constants.GROQ_API_KEY}")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            outputStream.write(body.toString().toByteArray())
        }

        if (conn.responseCode != 200) {
            throw Exception("API Error: ${conn.responseCode}")
        }

        val resText = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(resText)
        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }
}
