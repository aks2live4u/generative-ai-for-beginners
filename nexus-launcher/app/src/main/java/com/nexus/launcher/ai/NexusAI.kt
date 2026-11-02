package com.nexus.launcher.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.NotFoundException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.beta.messages.BetaMessage
import com.anthropic.models.beta.messages.BetaMessageParam
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.BetaWebSearchTool20260209
import com.anthropic.models.beta.messages.MessageCreateParams
import com.nexus.launcher.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * The AI Command Centre's brain: Claude, through the official Anthropic SDK.
 *
 * - Claude Opus 5.5 at low effort keeps spoken replies quick.
 * - Server-side refusal fallbacks (`fallbacks: "default"`) are on.
 * - The web search server tool lets it answer weather, news and other live questions.
 * - It acts on the phone by adding `[[verb:argument]]` tags, which the launcher runs.
 */
class NexusAI(private val settings: SettingsStore) {
    private var client: AnthropicClient? = null
    private var clientKey: String? = null
    private val history = mutableListOf<BetaMessageParam>()

    fun reset() = synchronized(history) { history.clear() }

    /** One turn of the ongoing voice conversation. */
    suspend fun ask(prompt: String, deviceContext: String, appNames: List<String>): NexusReply =
        withContext(Dispatchers.IO) {
            val userTurn = BetaMessageParam.ofUser("[$deviceContext]\n$prompt")
            val conversation = synchronized(history) { history.toList() } + userTurn
            val (text, turn) = run(conversation, systemPrompt(appNames), allowTools = true)
            synchronized(history) {
                history += userTurn
                history += turn
                // Keep the conversation short: about the last 12 turns.
                while (history.size > 24) history.removeAt(0)
                // A conversation must start with a user message.
                while (history.isNotEmpty() && isAssistant(history.first())) history.removeAt(0)
            }
            parse(text)
        }

    /** Stand-alone summary of the current notifications (not added to the conversation). */
    suspend fun summarizeNotifications(lines: List<String>): String = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("Here are my current phone notifications, newest first:")
            lines.take(40).forEach { appendLine("- $it") }
            append("Give me a spoken briefing: what needs me now, what can wait. Three sentences at most.")
        }
        val (text, _) = run(listOf(BetaMessageParam.ofUser(prompt)), BRIEFING_SYSTEM, allowTools = false)
        parse(text).text
    }

    private fun run(
        messages: List<BetaMessageParam>,
        system: String,
        allowTools: Boolean,
    ): Pair<String, List<BetaMessageParam>> {
        val s = settings.state.value
        if (s.apiKey.isBlank()) throw NexusAIException("Add your Anthropic API key in Settings to wake NEXUS.")
        val c = clientFor(s.apiKey)
        val turn = mutableListOf<BetaMessageParam>()
        val text = StringBuilder()
        var response: BetaMessage
        var rounds = 0
        try {
            do {
                val params = MessageCreateParams.builder()
                    .model(s.model)
                    .maxTokens(4096L)
                    .system(system)
                    .messages(messages + turn)
                    .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.LOW).build())
                    // Server-side refusal fallback: if this model declines, the API retries on a fallback model.
                    .addBeta("server-side-fallback-2026-07-01")
                    .fallbacksDefault()
                    .apply {
                        if (allowTools && s.webSearch) addTool(BetaWebSearchTool20260209.builder().maxUses(3L).build())
                    }
                    .build()
                response = c.beta().messages().create(params)
                turn += response.toParam()
                response.content().forEach { block -> block.text().ifPresent { text.append(it.text()) } }
                rounds++
                // Web search can pause a long turn; send it back to let Claude continue.
            } while (response.stopReason().orElse(null) == BetaStopReason.PAUSE_TURN && rounds < 4)
        } catch (e: UnauthorizedException) {
            throw NexusAIException("That API key was rejected. Check it in Settings.")
        } catch (e: PermissionDeniedException) {
            throw NexusAIException("This API key can't use ${s.model}.")
        } catch (e: NotFoundException) {
            throw NexusAIException("Model \"${s.model}\" wasn't found. Check the model ID in Settings.")
        } catch (e: RateLimitException) {
            throw NexusAIException("Rate limited. Give me a moment and try again.")
        } catch (e: BadRequestException) {
            throw NexusAIException("The request was rejected: ${e.message}")
        } catch (e: AnthropicServiceException) {
            throw NexusAIException("Claude is having trouble right now (HTTP ${e.statusCode()}).")
        } catch (e: AnthropicIoException) {
            throw NexusAIException("I can't reach the network.")
        }
        if (response.stopReason().orElse(null) == BetaStopReason.REFUSAL && text.isBlank()) {
            return "I can't help with that one." to turn
        }
        return text.toString().trim() to turn
    }

    private fun clientFor(key: String): AnthropicClient {
        val existing = client
        if (existing != null && clientKey == key) return existing
        existing?.close()
        return AnthropicOkHttpClient.builder().apiKey(key).build().also {
            client = it
            clientKey = key
        }
    }

    private fun isAssistant(p: BetaMessageParam): Boolean = p.isAssistant()

    private fun systemPrompt(appNames: List<String>) = """
        You are NEXUS, the AI built into this Android phone's home screen. You're calm, precise and
        quietly witty, like a capable assistant from the near future. You speak to the user through
        text-to-speech, so:
        - Answer in one to three short sentences unless they ask for more.
        - No markdown, bullet points, emoji or URLs. Write numbers and times the way they're spoken.
        - Use web search for anything live (weather, news, scores, opening hours). Don't guess.

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
            val text = raw.replace(TAG, "").replace(Regex("\n{3,}"), "\n\n").trim()
            return NexusReply(text, actions)
        }
    }
}
