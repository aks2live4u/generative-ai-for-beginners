package com.finsight.core.ai.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmartScanActionParserTest {

    @Test
    fun `parses a single merge action from the fenced JSON block`() {
        val response = """
            Likely duplicates: transaction 101 and 102 both record the same credit card bill payment.

            ```json
            {"merges":[{"keepId":101,"removeIds":[102]}]}
            ```
        """.trimIndent()
        assertEquals(listOf(DuplicateMergeAction(101, listOf(102))), SmartScanActionParser.parse(response))
    }

    @Test
    fun `parses multiple merge actions and multiple removeIds per group`() {
        val response = """
            ```json
            {"merges":[{"keepId":1,"removeIds":[2,3]},{"keepId":10,"removeIds":[11]}]}
            ```
        """.trimIndent()
        assertEquals(
            listOf(DuplicateMergeAction(1, listOf(2, 3)), DuplicateMergeAction(10, listOf(11))),
            SmartScanActionParser.parse(response)
        )
    }

    @Test
    fun `returns an empty list when there is no JSON block`() {
        val response = "Everything looks fine, no duplicates or anomalies found."
        assertTrue(SmartScanActionParser.parse(response).isEmpty())
    }

    @Test
    fun `ignores a removeId that is the same as keepId`() {
        val response = """```json
            {"merges":[{"keepId":5,"removeIds":[5]}]}
            ```"""
        assertTrue(SmartScanActionParser.parse(response).isEmpty())
    }
}
