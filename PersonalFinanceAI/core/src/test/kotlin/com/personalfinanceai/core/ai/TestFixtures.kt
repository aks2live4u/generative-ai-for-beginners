package com.personalfinanceai.core.ai

import com.personalfinanceai.core.model.Category
import com.personalfinanceai.core.model.PaymentMethod
import com.personalfinanceai.core.model.Transaction
import com.personalfinanceai.core.model.TransactionSource
import com.personalfinanceai.core.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId

fun tx(
    amount: Double,
    category: Category,
    type: TransactionType,
    date: LocalDate,
    merchant: String = category.displayName
): Transaction = Transaction(
    amount = amount,
    date = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
    merchant = merchant,
    category = category,
    type = type,
    paymentMethod = PaymentMethod.UPI,
    source = TransactionSource.SMS,
    rawText = "test"
)
