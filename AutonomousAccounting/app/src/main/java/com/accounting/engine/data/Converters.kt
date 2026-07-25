package com.accounting.engine.data

import androidx.room.TypeConverter
import com.accounting.engine.data.entity.AccountType
import com.accounting.engine.data.entity.LineType
import com.accounting.engine.data.entity.TransactionSource

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromLineType(value: LineType): String = value.name

    @TypeConverter
    fun toLineType(value: String): LineType = LineType.valueOf(value)

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource): String = value.name

    @TypeConverter
    fun toTransactionSource(value: String): TransactionSource = TransactionSource.valueOf(value)
}
