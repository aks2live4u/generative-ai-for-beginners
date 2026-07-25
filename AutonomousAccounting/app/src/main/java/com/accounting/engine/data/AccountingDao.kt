package com.accounting.engine.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.accounting.engine.data.entity.AccountEntity
import com.accounting.engine.data.entity.AccountType
import com.accounting.engine.data.entity.JournalEntryEntity
import com.accounting.engine.data.entity.LineItemEntity
import com.accounting.engine.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * [Dao] is declared as an abstract class rather than an interface so that
 * [getOrCreateAccount] can compose the primitive, Room-generated read/insert
 * methods into the "find sub-ledger, else auto-provision it under its master
 * root account" behaviour described by the Chart-of-Accounts pipeline stage.
 */
@Dao
abstract class AccountingDao {

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    abstract suspend fun findAccountByName(name: String): AccountEntity?

    @Insert
    abstract suspend fun insertAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts ORDER BY name")
    abstract fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY name")
    abstract suspend fun getAllAccounts(): List<AccountEntity>

    @Insert
    abstract suspend fun insertTransactionRecord(transaction: TransactionEntity)

    @Insert
    abstract suspend fun insertJournalEntry(journalEntry: JournalEntryEntity)

    @Insert
    abstract suspend fun insertLineItems(lineItems: List<LineItemEntity>)

    @Query("SELECT * FROM line_items")
    abstract fun observeLineItems(): Flow<List<LineItemEntity>>

    @Query("SELECT * FROM line_items")
    abstract suspend fun getAllLineItems(): List<LineItemEntity>

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    abstract fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    abstract suspend fun getAllJournalEntries(): List<JournalEntryEntity>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    abstract fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM line_items WHERE journalEntryId = :journalEntryId")
    abstract suspend fun getLineItemsForJournalEntry(journalEntryId: String): List<LineItemEntity>

    /** Sum(debits) - Sum(credits) posted to this account so far; sign depends on [AccountType.isDebitNormal]. */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE -amount END), 0.0) " +
            "FROM line_items WHERE accountId = :accountId"
    )
    abstract suspend fun getNetDebitBalance(accountId: String): Double

    /**
     * Returns the account named [name], auto-provisioning it (and, transitively,
     * its parent grouping ledger or master root account) if it does not yet exist.
     * [parentName], when supplied, is the intermediate grouping ledger (e.g.
     * "Loans & Advances") that the new sub-ledger should nest under; otherwise the
     * account is created directly beneath the master root account for [type].
     */
    @Transaction
    open suspend fun getOrCreateAccount(
        name: String,
        type: AccountType,
        parentName: String? = null
    ): AccountEntity {
        findAccountByName(name)?.let { return it }

        val parentId = if (parentName != null) {
            getOrCreateAccount(parentName, type, parentName = null).id
        } else {
            getOrCreateRootAccount(type).id
        }

        val account = AccountEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            parentAccountId = parentId,
            isSystemAccount = false,
            createdAt = System.currentTimeMillis()
        )
        insertAccount(account)
        return account
    }

    @Transaction
    open suspend fun getOrCreateRootAccount(type: AccountType): AccountEntity {
        val rootName = AccountType.rootAccountName(type)
        findAccountByName(rootName)?.let { return it }

        val account = AccountEntity(
            id = UUID.randomUUID().toString(),
            name = rootName,
            type = type,
            parentAccountId = null,
            isSystemAccount = true,
            createdAt = System.currentTimeMillis()
        )
        insertAccount(account)
        return account
    }
}
