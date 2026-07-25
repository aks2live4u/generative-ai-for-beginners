package com.accounting.engine.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.accounting.engine.domain.BalanceSheet
import com.accounting.engine.domain.ProfitAndLossStatement
import com.accounting.engine.repository.AccountingRepository
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 4: exports the ledger (CSV) and the two headline statements (PDF), writing into the
 * app's own external-files directory so no storage permission is required.
 */
object ReportExporter {

    private val currency = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun exportLedgerToCsv(context: Context, snapshot: AccountingRepository.LedgerSnapshot): File {
        val accountsById = snapshot.accounts.associateBy { it.id }
        val lineItemsByJournalEntry = snapshot.lineItems.groupBy { it.journalEntryId }

        val file = reportFile(context, "ledger", "csv")
        file.bufferedWriter().use { writer ->
            writer.appendLine("Date,Description,Account,Type,Amount")
            snapshot.journalEntries.forEach { entry ->
                val date = dateFormat.format(Date(entry.timestamp))
                lineItemsByJournalEntry[entry.id].orEmpty().forEach { line ->
                    val accountName = accountsById[line.accountId]?.name ?: "Unknown"
                    writer.appendLine(
                        listOf(
                            date,
                            csvEscape(entry.description),
                            csvEscape(accountName),
                            line.type.name,
                            line.amount.toString()
                        ).joinToString(",")
                    )
                }
            }
        }
        return file
    }

    fun exportStatementsToPdf(
        context: Context,
        profitAndLoss: ProfitAndLossStatement,
        balanceSheet: BalanceSheet
    ): File {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 12f }

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = 40f
        fun line(text: String, paint: Paint = bodyPaint, gap: Float = 18f) {
            canvas.drawText(text, 40f, y, paint)
            y += gap
        }

        line("Profit & Loss Statement", titlePaint, 28f)
        profitAndLoss.revenueBalances.forEach { line("${it.account.name}: ${currency.format(it.balance)}") }
        profitAndLoss.expenseBalances.forEach { line("${it.account.name}: -${currency.format(it.balance)}") }
        line("Net Profit: ${currency.format(profitAndLoss.netProfit)}", bodyPaint, 28f)

        line("Balance Sheet", titlePaint, 28f)
        line("Total Assets: ${currency.format(balanceSheet.totalAssets)}")
        line("Total Liabilities: ${currency.format(balanceSheet.totalLiabilities)}")
        line("Total Equity (incl. net profit): ${currency.format(balanceSheet.totalEquity)}")
        line(if (balanceSheet.isBalanced) "Integrity check: OK" else "Integrity check: FAILED")

        document.finishPage(page)

        val file = reportFile(context, "statements", "pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun reportFile(context: Context, prefix: String, extension: String): File {
        val dir = context.getExternalFilesDir("reports") ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "${prefix}_$timestamp.$extension")
    }

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
}
