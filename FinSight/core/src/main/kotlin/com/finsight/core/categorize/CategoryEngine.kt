package com.finsight.core.categorize

import com.finsight.core.model.Category

/**
 * Maps a merchant name / free text to a [Category] using keyword matching.
 * Pure lookup table approach: deterministic, offline, no ML model required for MVP.
 */
object CategoryEngine {

    // Ordered so more specific brand matches are checked before generic group fallbacks.
    private val merchantKeywords: List<Pair<List<String>, Category>> = listOf(
        listOf("cash withdrawal", "cash wdl", "withdrawn at atm", "atm wdl", "atm cash", "atm") to Category.ATM_WITHDRAWAL,
        listOf("swiggy", "zomato", "eatsure", "dominos", "pizza hut") to Category.FOOD_DELIVERY,
        listOf("uber eats") to Category.FOOD_DELIVERY,
        listOf("bigbasket", "blinkit", "zepto", "grofers", "dunzo", "jiomart") to Category.GROCERIES,
        listOf("restaurant", "cafe", "starbucks", "barista", "mcdonald", "kfc", "burger") to Category.RESTAURANTS,
        listOf("amazon") to Category.AMAZON,
        listOf("flipkart") to Category.FLIPKART,
        listOf("myntra") to Category.MYNTRA,
        listOf("uber", "ola", "rapido") to Category.RIDE_SHARING,
        listOf("indianoil", "hpcl", "bharat petroleum", "petrol", "fuel pump", "shell") to Category.FUEL,
        listOf("irctc", "metro card", "metro recharge", "toll", "parking", "indigo", "spicejet", "vistara", "goair", "akasa air") to Category.TRANSPORTATION_OTHER,
        listOf("electricity", "bescom", "tneb", "mseb", "power bill", "discom") to Category.ELECTRICITY,
        listOf("water board", "water bill") to Category.WATER,
        listOf("airtel broadband", "jio fiber", "act fibernet", "broadband", "wifi") to Category.INTERNET,
        listOf("airtel", "jio", "vodafone", "vi prepaid", "bsnl", "mobile recharge") to Category.MOBILE,
        listOf("netflix", "hotstar", "prime video", "sonyliv", "zee5", "sun nxt") to Category.OTT,
        listOf("pvr", "inox", "bookmyshow", "cinepolis") to Category.MOVIES,
        listOf("steam", "playstation", "xbox", "epic games", "google play games") to Category.GAMES,
        listOf("pharmacy", "apollo", "medplus", "netmeds", "1mg", "pharmeasy") to Category.MEDICINES,
        listOf("hospital", "clinic", "diagnostic", "doctor", "practo") to Category.DOCTORS,
        listOf("lic india", "life insurance", "insurance", "policybazaar", "hdfc life", "icici prudential") to Category.INSURANCE,
        listOf("coursera", "udemy", "byjus", "unacademy", "upgrad") to Category.COURSES,
        listOf("kindle", "books", "crossword", "sapna book") to Category.BOOKS,
        listOf("emi", "loan installment") to Category.EMI,
        listOf("credit card payment", "card bill", "cc payment") to Category.CREDIT_CARD_BILL,
        // Checked before INVESTMENT_OUTFLOW: a redemption/payout SMS often also names the fund
        // platform (e.g. "Mutual fund redemption from Zerodha credited"), so the income signal
        // must win over the generic "mutual fund"/"zerodha" outflow keywords.
        listOf("redemption", "stock sale", "capital gain", "fund payout") to Category.INVESTMENT_INCOME,
        listOf("mutual fund", "zerodha", "groww", "upstox", "sip", "nps") to Category.INVESTMENT_OUTFLOW,
        listOf("mall", "retail store", "shopping", "decathlon", "ikea", "lifestyle store", "reliance trends") to Category.SHOPPING_OTHER,
        listOf("salary", "payroll") to Category.SALARY,
        listOf("bonus", "incentive") to Category.BONUS,
        listOf("freelance", "upwork", "fiverr") to Category.FREELANCE,
        listOf("rent received", "rental income", "tenant payment") to Category.RENT_INCOME,
        listOf("interest credited", "savings interest") to Category.INTEREST,
        listOf("dividend") to Category.DIVIDENDS,
        listOf("refund", "reversal", "cashback") to Category.REFUND
    )

    // Keywords short/generic enough to appear as substrings of unrelated words (e.g. "lic" inside
    // "police", "emi" inside "premium", "sip" inside "gossip") are matched on word boundaries only;
    // everything else (multi-word phrases, brand names) keeps the cheaper substring match.
    private val wordBoundaryOnlyKeywords = setOf("emi", "sip", "nps", "mall", "atm")

    private fun matches(text: String, keyword: String): Boolean {
        return if (keyword in wordBoundaryOnlyKeywords) {
            Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(text)
        } else {
            text.contains(keyword)
        }
    }

    /**
     * Resolves a category from merchant/description text. Falls back to [Category.MISCELLANEOUS]
     * when nothing matches, never throws.
     */
    fun categorize(text: String): Category {
        val lower = text.lowercase()
        for ((keywords, category) in merchantKeywords) {
            if (keywords.any { matches(lower, it) }) return category
        }
        return Category.MISCELLANEOUS
    }
}
