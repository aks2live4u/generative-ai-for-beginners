package com.finsight.core.parser

import com.finsight.core.model.Transaction

sealed class ParseResult {
    data class Success(val transaction: Transaction) : ParseResult()
    data class NotFinancial(val reason: String) : ParseResult()
}
