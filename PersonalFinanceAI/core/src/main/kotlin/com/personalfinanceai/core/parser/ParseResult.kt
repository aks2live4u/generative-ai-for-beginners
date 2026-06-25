package com.personalfinanceai.core.parser

import com.personalfinanceai.core.model.Transaction

sealed class ParseResult {
    data class Success(val transaction: Transaction) : ParseResult()
    data class NotFinancial(val reason: String) : ParseResult()
}
