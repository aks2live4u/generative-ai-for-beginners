package com.finsight.core.ai.llm

/** One duplicate group Smart Scan (AI) found: keep [keepId], delete every id in [removeIds]. */
data class DuplicateMergeAction(val keepId: Long, val removeIds: List<Long>)

/**
 * Parses the machine-readable "ACTIONS" block Smart Scan's Gemini prompt is instructed to append
 * after its plain-text findings (see SMART_SCAN_SYSTEM_INSTRUCTION), so the app can actually merge
 * the duplicates the AI identified instead of only showing them as text the user has to act on by
 * hand. The block is a fenced ```json ... ``` snippet of the form:
 * {"merges":[{"keepId":123,"removeIds":[124,125]}]}
 *
 * Deliberately tolerant: a missing/malformed block (or a model that ignores the instruction and
 * replies with prose only) yields an empty list rather than throwing, since Smart Scan must still
 * show its plain-text findings even when nothing is actionable.
 */
object SmartScanActionParser {
    private val jsonBlockRegex = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
    private val mergeRegex = Regex("\\{\\s*\"keepId\"\\s*:\\s*(\\d+)\\s*,\\s*\"removeIds\"\\s*:\\s*\\[([^]]*)]\\s*}")

    fun parse(responseText: String): List<DuplicateMergeAction> {
        val jsonBlock = jsonBlockRegex.find(responseText)?.groupValues?.get(1) ?: return emptyList()
        return mergeRegex.findAll(jsonBlock).mapNotNull { match ->
            val keepId = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val removeIds = match.groupValues[2].split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { it != keepId }
            if (removeIds.isEmpty()) null else DuplicateMergeAction(keepId, removeIds)
        }.toList()
    }
}
