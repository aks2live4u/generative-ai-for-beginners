package com.nexus.launcher.ai

import com.nexus.launcher.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Something NEXUS can do on the phone, parsed from `[[verb:argument]]` tags in a reply. */
sealed interface NexusAction {
    data class OpenApp(val name: String) : NexusAction
    data class StartFocus(val minutes: Int) : NexusAction
    data object StopFocus : NexusAction
    data class Navigate(val screen: String) : NexusAction
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : NexusAction
    data class SetTimer(val seconds: Int, val label: String) : NexusAction
    data class Dial(val number: String) : NexusAction
    data class WebSearch(val query: String) : NexusAction
}

data class NexusReply(val text: String, val actions: List<NexusAction>)

class NexusAIException(message: String) : Exception(message)

/**
 * The AI Command Centre's brain: Google Gemini.
 *
 * - Grounding with Google Search answers live questions (weather, news, scores).
 * - Phone actions come back as `[[verb:argument]]` tags, which the launcher runs.
 * - If the configured model isn't available to the key, NEXUS picks a working
 *   Flash model from the key's model list and saves it.
 */
class NexusAI(
    private val settings: SettingsStore,
    private val gemini: GeminiClient = GeminiClient(),
) {
    private val history = mutableListOf<JSONObject>()

    fun reset() = synchronized(history) { history.clear() }

    /** One turn of the ongoing voice conversation. */
    suspend fun ask(prompt: String, deviceContext: String, appNames: List<String>): NexusReply =
        withContext(Dispatchers.IO) {
            val userTurn = GeminiClient.turn("user", "[$deviceContext]\n$prompt")
            val conversation = synchronized(history) { history.toList() } + userTurn
            val result = generate(conversation, systemPrompt(appNames), allowSearch = true)
            val reply = parse(replyText(result))
            synchronized(history) {
                history += userTurn
                history += result.content ?: GeminiClient.turn("model", reply.text)
                // Keep about the last 12 exchanges, always starting on a user turn.
                while (history.size > 24) history.removeAt(0)
                while (history.isNotEmpty() && history.first().optString("role") != "user") history.removeAt(0)
            }
            reply
        }

    /** Stand-alone summary of the current notifications (not added to the conversation). */
    suspend fun summarizeNotifications(lines: List<String>): String = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("Here are my current phone notifications, newest first:")
            lines.take(40).forEach { appendLine("- $it") }
            append("Give me a spoken briefing: what needs me now, what can wait. Three sentences at most.")
        }
        val result = generate(listOf(GeminiClient.turn("user", prompt)), BRIEFING_SYSTEM, allowSearch = false)
        parse(replyText(result)).text
    }

    /** Checks the key and model; returns a human-readable status line. */
    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        val s = settings.state.value
        if (s.apiKey.isBlank()) throw NexusAIException("Add your Gemini API key first.")
        val models = call { gemini.listModels(s.apiKey) }
        val model = GeminiClient.modelId(s.model)
        val chosen = if (model in models) model else {
            GeminiClient.pickFallbackModel(models)?.also { m -> settings.update { it.copy(model = m) } }
                ?: throw NexusAIException("This key can't use any Gemini chat models.")
        }
        val result = call { gemini.generate(s.apiKey, chosen, BRIEFING_SYSTEM, listOf(GeminiClient.turn("user", "Say: NEXUS online.")), googleSearch = false, maxOutputTokens = 256) }
        val heard = result.text.ifBlank { "(empty reply)" }
        val note = if (chosen != model) " Switched model from $model to $chosen." else ""
        "Connected to $chosen. It replied: \"$heard\".$note"
    }

    private fun generate(contents: List<JSONObject>, system: String, allowSearch: Boolean): GeminiClient.Result {
        val s = settings.state.value
        if (s.apiKey.isBlank()) throw NexusAIException("Add your Gemini API key in Settings to wake NEXUS.")
        var model = GeminiClient.modelId(s.model)
        var search = allowSearch && s.webSearch
        var attempt = 0
        while (true) {
            attempt++
            try {
                return gemini.generate(s.apiKey, model, system, contents, search)
            } catch (e: GeminiException) {
                when {
                    // Model retired or not enabled for this key: choose one that works and remember it.
                    e.code == 404 && attempt == 1 -> {
                        val fallback = runCatching { GeminiClient.pickFallbackModel(gemini.listModels(s.apiKey)) }.getOrNull()
                        if (fallback == null || fallback == model) throw friendly(e, model)
                        model = fallback
                        settings.update { it.copy(model = fallback) }
                    }
                    // Some models or regions reject Search grounding; answer without it.
                    e.code == 400 && search && e.message.orEmpty().contains("search", ignoreCase = true) && attempt <= 2 -> search = false
                    // Transient overload: one quick retry.
                    (e.code == 503 || e.code == 500) && attempt <= 2 -> Thread.sleep(800)
                    else -> throw friendly(e, model)
                }
            }
        }
    }

    private fun <T> call(block: () -> T): T = try {
        block()
    } catch (e: GeminiException) {
        throw friendly(e, GeminiClient.modelId(settings.state.value.model))
    }

    private fun replyText(r: GeminiClient.Result): String = when {
        r.blockReason != null -> "I can't help with that one."
        r.text.isNotBlank() -> r.text
        r.finishReason == "SAFETY" || r.finishReason == "PROHIBITED_CONTENT" -> "I can't help with that one."
        r.finishReason == "MAX_TOKENS" -> "That answer ran long. Try asking for a shorter version."
        else -> "Done."
    }

    private fun friendly(e: GeminiException, model: String): NexusAIException = NexusAIException(
        when {
            e.code == 0 -> "I can't reach the network."
            e.code == 400 && e.message.orEmpty().contains("API key", ignoreCase = true) -> "That Gemini API key isn't valid. Check it in Settings."
            e.code == 401 || e.code == 403 -> "This API key isn't allowed to use Gemini ($model). Check it in Google AI Studio."
            e.code == 404 -> "Model \"$model\" isn't available. Pick another in Settings."
            e.code == 429 -> "Gemini quota reached. Give it a minute and try again."
            e.code >= 500 -> "Gemini is having trouble right now (HTTP ${e.code})."
            else -> "Gemini rejected the request: ${e.message}"
        },
    )

    private fun systemPrompt(appNames: List<String>) = """
        You are NEXUS, the AI built into this Android phone's home screen. You're calm, precise and
        quietly witty, like a capable assistant from the near future. You speak to the user through
        text-to-speech, so:
        - Answer in one to three short sentences unless they ask for more.
        - No markdown, bullet points, emoji or URLs. Write numbers and times the way they're spoken.
        - Use Google Search for anything live (weather, news, scores, opening hours). Don't guess.

        You can act on the phone. When the user asks for one of these, say briefly what you're doing
        and put the tag on its own line at the end of your reply:
        [[open:App Name]]          open an installed app (use a name from the list below)
        [[focus:25]]               start Focus Mode for this many minutes
        [[focus:stop]]             end Focus Mode
        [[screen:home|apps|alerts|focus|command]]   show a NEXUS screen
        [[alarm:07:30|Label]]      set an alarm (24-hour time)
        [[timer:300|Label]]        start a countdown timer in seconds
        [[dial:+15551234567]]      open the dialler with a number (it never calls on its own)
        [[search:query]]           open a web search on the phone
        Only use a tag when the user asked for that action. Never invent apps that aren't listed.

        Each user message starts with bracketed device context (time, battery, focus, notifications).
        Use it when it's relevant; don't recite it.

        Installed apps: ${appNames.joinToString(", ")}
    """.trimIndent()

    companion object {
        private const val BRIEFING_SYSTEM =
            "You are NEXUS, a phone's built-in assistant. Your words are read aloud. " +
                "Be brief and plain: no markdown, lists or emoji. Group by what matters; never read every item."

        private val TAG = Regex("""\[\[(\w+):([^\]]*)]]""")

        fun parse(raw: String): NexusReply {
            val actions = TAG.findAll(raw).mapNotNull { m ->
                val verb = m.groupValues[1].lowercase()
                val arg = m.groupValues[2].trim()
                when (verb) {
                    "open" -> NexusAction.OpenApp(arg)
                    "focus" -> if (arg.equals("stop", true)) NexusAction.StopFocus
                    else arg.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 240)?.let { NexusAction.StartFocus(it) }
                    "screen" -> NexusAction.Navigate(arg.lowercase())
                    "alarm" -> {
                        val (time, label) = arg.split("|", limit = 2).let { it[0] to it.getOrElse(1) { "NEXUS" } }
                        val hm = time.split(":").mapNotNull { it.trim().toIntOrNull() }
                        if (hm.size == 2 && hm[0] in 0..23 && hm[1] in 0..59) NexusAction.SetAlarm(hm[0], hm[1], label.trim()) else null
                    }
                    "timer" -> {
                        val (sec, label) = arg.split("|", limit = 2).let { it[0] to it.getOrElse(1) { "NEXUS" } }
                        sec.trim().toIntOrNull()?.takeIf { it in 1..86_400 }?.let { NexusAction.SetTimer(it, label.trim()) }
                    }
                    "dial" -> NexusAction.Dial(arg.filter { it.isDigit() || it == '+' })
                    "search" -> NexusAction.WebSearch(arg)
                    else -> null
                }
            }.toList()
            // Gemini sometimes wraps tags in backticks; strip those too.
            val text = raw.replace(Regex("`*" + TAG.pattern + "`*"), "").replace(Regex("\n{3,}"), "\n\n").trim()
            return NexusReply(text, actions)
        }
    }
}
