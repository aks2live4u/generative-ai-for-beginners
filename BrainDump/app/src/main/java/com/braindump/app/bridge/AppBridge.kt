package com.braindump.app.bridge

import android.webkit.JavascriptInterface
import com.braindump.app.MainActivity
import com.braindump.app.ai.ClaudeClient
import com.braindump.app.ai.GeminiClient
import com.braindump.app.data.PreferencesStore
import com.braindump.app.data.Quotes
import com.braindump.app.data.Thought
import com.braindump.app.data.ThoughtRepository
import com.braindump.app.files.MediaStorage
import com.braindump.app.security.SecureStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * The full JavaScript <-> Kotlin contract for Brain Dump. Every method here
 * is callable from assets/js as `AndroidBridge.<name>(...)`.
 *
 * Synchronous methods return a JSON string directly. Asynchronous methods
 * take a `requestId` and deliver their result later via
 * `window.__bridgeResolve(requestId, resultObject)` (see assets/js/bridge.js).
 */
class AppBridge(
    private val activity: MainActivity,
    private val repository: ThoughtRepository,
    private val prefsStore: PreferencesStore,
    private val secureStore: SecureStore,
    private val mediaStorage: MediaStorage
) {
    private val ioExecutor = Executors.newCachedThreadPool()

    // ---------------------------------------------------------------
    // Thoughts
    // ---------------------------------------------------------------

    @JavascriptInterface
    fun getThoughts(queryJson: String): String {
        val query = if (queryJson.isBlank()) JSONObject() else JSONObject(queryJson)
        val search = query.optString("search", "").ifBlank { null }
        val mood = query.optString("mood", "").ifBlank { null }
        val limit = if (query.has("limit")) query.getInt("limit") else 500
        val offset = if (query.has("offset")) query.getInt("offset") else 0
        val arr = JSONArray()
        repository.getThoughts(search, mood, limit, offset).forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    @JavascriptInterface
    fun getThought(id: Long): String {
        val thought = repository.getThought(id) ?: return errorJson("Thought not found")
        return thought.toJson().toString()
    }

    @JavascriptInterface
    fun addThought(json: String): String {
        val thought = Thought.fromJson(JSONObject(json))
        return repository.insertThought(thought.copy(id = 0)).toJson().toString()
    }

    @JavascriptInterface
    fun updateThought(id: Long, json: String): String {
        val thought = Thought.fromJson(JSONObject(json))
        val updated = repository.updateThought(id, thought) ?: return errorJson("Thought not found")
        return updated.toJson().toString()
    }

    @JavascriptInterface
    fun deleteThought(id: Long): String {
        repository.deleteThought(id).forEach { mediaStorage.delete(it) }
        return okJson()
    }

    @JavascriptInterface
    fun getStats(): String = repository.getStats().toString()

    @JavascriptInterface
    fun getMoodStats(): String = repository.getMoodStats().toString()

    // ---------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------

    @JavascriptInterface
    fun getSettings(): String = prefsStore.getSettings().toString()

    @JavascriptInterface
    fun saveSettings(json: String): String = prefsStore.saveSettings(JSONObject(json)).toString()

    // ---------------------------------------------------------------
    // Security
    // ---------------------------------------------------------------

    @JavascriptInterface
    fun getSecurityStatus(): String = JSONObject().apply {
        put("hasPin", secureStore.hasPin())
        put("biometricEnabled", secureStore.isBiometricEnabled())
        put("biometricAvailable", activity.biometricHelper.isAvailable())
    }.toString()

    @JavascriptInterface
    fun setPin(pin: String): String {
        if (pin.length < 4) return errorJson("PIN must be at least 4 digits")
        secureStore.setPin(pin)
        return okJson()
    }

    @JavascriptInterface
    fun verifyPin(pin: String): String = JSONObject().apply {
        put("valid", secureStore.verifyPin(pin))
    }.toString()

    @JavascriptInterface
    fun removePin(): String {
        secureStore.removePin()
        return okJson()
    }

    @JavascriptInterface
    fun setBiometricEnabled(enabled: Boolean): String {
        secureStore.setBiometricEnabled(enabled && activity.biometricHelper.isAvailable())
        return okJson()
    }

    // ---------------------------------------------------------------
    // AI configuration
    // ---------------------------------------------------------------

    @JavascriptInterface
    fun getAiStatus(): String = JSONObject().apply {
        put("hasClaudeKey", secureStore.getClaudeApiKey() != null)
        put("hasGeminiKey", secureStore.getGeminiApiKey() != null)
    }.toString()

    @JavascriptInterface
    fun saveApiKey(provider: String, key: String): String {
        when (provider) {
            "claude" -> secureStore.setClaudeApiKey(key)
            "gemini" -> secureStore.setGeminiApiKey(key)
            else -> return errorJson("Unknown provider")
        }
        return okJson()
    }

    @JavascriptInterface
    fun clearApiKey(provider: String): String {
        when (provider) {
            "claude" -> secureStore.setClaudeApiKey(null)
            "gemini" -> secureStore.setGeminiApiKey(null)
            else -> return errorJson("Unknown provider")
        }
        return okJson()
    }

    // ---------------------------------------------------------------
    // AI chat history
    // ---------------------------------------------------------------

    @JavascriptInterface
    fun getChatMessages(): String {
        val arr = JSONArray()
        repository.getChatMessages().forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    @JavascriptInterface
    fun addChatMessage(role: String, content: String): String =
        repository.addChatMessage(role, content).toJson().toString()

    @JavascriptInterface
    fun clearChatHistory(): String {
        repository.clearChatMessages()
        return okJson()
    }

    // ---------------------------------------------------------------
    // Misc
    // ---------------------------------------------------------------

    @JavascriptInterface
    fun getQuote(refresh: Boolean): String {
        val quote = if (refresh) Quotes.random() else Quotes.ofToday()
        return JSONObject().apply {
            put("text", quote.first)
            put("author", quote.second)
        }.toString()
    }

    @JavascriptInterface
    fun getAppInfo(): String = JSONObject().apply {
        put("versionName", activity.appVersionName())
        put("versionCode", activity.appVersionCode())
    }.toString()

    @JavascriptInterface
    fun deleteAllData(): String {
        repository.deleteAllThoughts().forEach { mediaStorage.delete(it) }
        mediaStorage.deleteAll()
        return okJson()
    }

    @JavascriptInterface
    fun removeImage(name: String): String {
        mediaStorage.delete(name)
        return okJson()
    }

    @JavascriptInterface
    fun getMediaBaseUrl(): String = "https://appassets.androidplatform.net/media/"

    // ---------------------------------------------------------------
    // Async: attachments
    // ---------------------------------------------------------------

    @JavascriptInterface
    fun pickImage(requestId: String, source: String) {
        activity.pickImage(source) { uri ->
            if (uri == null) {
                resolve(requestId, errorObj("cancelled"))
                return@pickImage
            }
            ioExecutor.execute {
                try {
                    val quality = prefsStore.getSettings().optString("imageQuality", "medium")
                    val name = mediaStorage.saveImage(uri, quality)
                    resolve(requestId, JSONObject().apply { put("success", true); put("name", name) })
                } catch (e: Exception) {
                    resolve(requestId, errorObj(e.message ?: "Failed to save image"))
                }
            }
        }
    }

    @JavascriptInterface
    fun startRecording(requestId: String) {
        activity.startRecording { success, error ->
            if (success) resolve(requestId, okObj())
            else resolve(requestId, errorObj(error ?: "Could not start recording"))
        }
    }

    @JavascriptInterface
    fun stopRecording(requestId: String) {
        activity.stopRecording { name, durationMs, error ->
            if (name != null) {
                resolve(
                    requestId,
                    JSONObject().apply {
                        put("success", true)
                        put("name", name)
                        put("durationMs", durationMs)
                    }
                )
            } else {
                resolve(requestId, errorObj(error ?: "Could not save recording"))
            }
        }
    }

    @JavascriptInterface
    fun cancelRecording(): String {
        activity.cancelRecording()
        return okJson()
    }

    // ---------------------------------------------------------------
    // Async: AI
    // ---------------------------------------------------------------

    @JavascriptInterface
    fun aiChat(requestId: String, payloadJson: String) {
        ioExecutor.execute {
            try {
                val apiKey = secureStore.getClaudeApiKey()
                if (apiKey == null) {
                    resolve(requestId, errorObj("No Claude API key configured. Add one in AI Assistant Setup."))
                    return@execute
                }
                val settings = prefsStore.getSettings()
                val payload = JSONObject(payloadJson)
                val history = payload.getJSONArray("messages")
                val includeContext = payload.optBoolean("includeJournalContext", false)

                var systemPrompt = settings.optString("aiSystemPrompt", "")
                if (includeContext) {
                    val recent = repository.getThoughts(null, null, 15, 0)
                    if (recent.isNotEmpty()) {
                        val sb = StringBuilder("\n\nRecent journal entries (most recent first), for context only:\n")
                        recent.forEach { t ->
                            val text = t.text.take(300)
                            if (text.isNotBlank()) sb.append("- ").append(text).append("\n")
                        }
                        systemPrompt += sb.toString()
                    }
                }

                val messages = JSONArray()
                for (i in 0 until history.length()) {
                    val m = history.getJSONObject(i)
                    messages.put(
                        JSONObject().apply {
                            put("role", m.getString("role"))
                            put("content", m.getString("content"))
                        }
                    )
                }

                val model = settings.optString("aiModel", "claude-sonnet-4-6")
                val result = ClaudeClient.sendMessage(apiKey, model, systemPrompt, messages, maxTokens = 1024)
                result.fold(
                    onSuccess = { text -> resolve(requestId, JSONObject().apply { put("success", true); put("text", text) }) },
                    onFailure = { e -> resolve(requestId, errorObj(e.message ?: "Request failed")) }
                )
            } catch (e: Exception) {
                resolve(requestId, errorObj(e.message ?: "Unexpected error"))
            }
        }
    }

    @JavascriptInterface
    fun aiRewrite(requestId: String, payloadJson: String) {
        ioExecutor.execute {
            try {
                val apiKey = secureStore.getClaudeApiKey()
                if (apiKey == null) {
                    resolve(requestId, errorObj("No Claude API key configured. Add one in AI Assistant Setup."))
                    return@execute
                }
                val settings = prefsStore.getSettings()
                val payload = JSONObject(payloadJson)
                val text = payload.optString("text", "")
                val instruction = payload.optString("instruction", "Improve this journal entry.")
                val images = mutableListOf<Pair<String, String>>()

                if (settings.optBoolean("useImageContext", true)) {
                    payload.optJSONArray("images")?.let { imageNames ->
                        for (i in 0 until imageNames.length()) {
                            try {
                                images.add("image/jpeg" to mediaStorage.readAsBase64(imageNames.getString(i)))
                            } catch (_: Exception) {
                                // Skip unreadable images rather than failing the whole request.
                            }
                        }
                    }
                }

                var prompt = "$instruction\n\n---\n$text"
                if (settings.optBoolean("useAudioContext", true)) {
                    val transcript = payload.optString("audioTranscript", "")
                    if (transcript.isNotBlank()) {
                        prompt += "\n\n(Voice note transcript: $transcript)"
                    }
                }

                val systemPrompt = settings.optString("aiSystemPrompt", "") +
                    "\n\nYou help the user write and refine personal journal entries. " +
                    "Respond with ONLY the resulting journal entry text - no preamble, no quotes, no explanations."

                val messages = JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", ClaudeClient.buildUserContent(prompt, images))
                    }
                )

                val model = settings.optString("aiModel", "claude-sonnet-4-6")
                val result = ClaudeClient.sendMessage(apiKey, model, systemPrompt, messages, maxTokens = 1024)
                result.fold(
                    onSuccess = { responseText ->
                        resolve(requestId, JSONObject().apply { put("success", true); put("text", responseText.trim()) })
                    },
                    onFailure = { e -> resolve(requestId, errorObj(e.message ?: "Request failed")) }
                )
            } catch (e: Exception) {
                resolve(requestId, errorObj(e.message ?: "Unexpected error"))
            }
        }
    }

    @JavascriptInterface
    fun transcribeAudio(requestId: String, audioName: String) {
        ioExecutor.execute {
            try {
                val apiKey = secureStore.getGeminiApiKey()
                if (apiKey == null) {
                    resolve(requestId, errorObj("No Gemini API key configured. Add one in AI Assistant Setup to enable transcription."))
                    return@execute
                }
                val settings = prefsStore.getSettings()
                val model = settings.optString("geminiModel", "gemini-2.0-flash")
                val base64Audio = mediaStorage.readAsBase64(audioName)
                val result = GeminiClient.transcribeAudio(apiKey, model, base64Audio, "audio/mp4")
                result.fold(
                    onSuccess = { transcript -> resolve(requestId, JSONObject().apply { put("success", true); put("transcript", transcript) }) },
                    onFailure = { e -> resolve(requestId, errorObj(e.message ?: "Transcription failed")) }
                )
            } catch (e: Exception) {
                resolve(requestId, errorObj(e.message ?: "Unexpected error"))
            }
        }
    }

    // ---------------------------------------------------------------
    // Async: security & data management
    // ---------------------------------------------------------------

    @JavascriptInterface
    fun authenticate(requestId: String) {
        activity.authenticateBiometric { success -> resolve(requestId, JSONObject().apply { put("success", success) }) }
    }

    @JavascriptInterface
    fun exportData(requestId: String) {
        ioExecutor.execute {
            try {
                val data = JSONObject()
                val thoughts = JSONArray()
                repository.getThoughts(null, null, Int.MAX_VALUE, 0).forEach { thoughts.put(it.toJson()) }
                data.put("thoughts", thoughts)
                data.put("settings", prefsStore.getSettings())
                data.put("exportedAt", System.currentTimeMillis())
                data.put("appVersion", activity.appVersionName())

                activity.exportData(data.toString(2)) { success, error ->
                    if (success) resolve(requestId, okObj())
                    else resolve(requestId, errorObj(error ?: "Export cancelled"))
                }
            } catch (e: Exception) {
                resolve(requestId, errorObj(e.message ?: "Export failed"))
            }
        }
    }

    @JavascriptInterface
    fun importData(requestId: String) {
        activity.importData { content, error ->
            if (content == null) {
                resolve(requestId, errorObj(error ?: "Import cancelled"))
                return@importData
            }
            ioExecutor.execute {
                try {
                    val data = JSONObject(content)
                    val thoughtsArr = data.optJSONArray("thoughts") ?: JSONArray()
                    var imported = 0
                    for (i in 0 until thoughtsArr.length()) {
                        val t = Thought.fromJson(thoughtsArr.getJSONObject(i))
                        repository.insertThought(t.copy(id = 0))
                        imported++
                    }
                    data.optJSONObject("settings")?.let { prefsStore.saveSettings(it) }
                    resolve(requestId, JSONObject().apply { put("success", true); put("imported", imported) })
                } catch (e: Exception) {
                    resolve(requestId, errorObj(e.message ?: "Import failed: invalid file"))
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private fun resolve(requestId: String, result: JSONObject) {
        val js = "window.__bridgeResolve && window.__bridgeResolve(\"$requestId\", $result)"
        activity.runOnUiThread { activity.webView.evaluateJavascript(js, null) }
    }

    private fun okJson() = JSONObject().apply { put("success", true) }.toString()
    private fun okObj() = JSONObject().apply { put("success", true) }
    private fun errorJson(message: String) = errorObj(message).toString()
    private fun errorObj(message: String) = JSONObject().apply { put("success", false); put("error", message) }
}
