package com.personalfinanceai.core.categorize

import com.personalfinanceai.core.model.Category

/**
 * Maps a merchant name / free text to a [Category] using keyword matching.
 * Pure lookup table approach: deterministic, offline, no ML model required for MVP.
 */
object CategoryEngine {

    // Ordered so more specific brand matches are checked before generic group fallbacks.
    private val merchantKeywords: List<Pair<List<String>, Category>> = listOf(
        listOf("swiggy", "zomato", "eatsure", "dominos", "pizza hut") to Category.FOOD_DELIVERY,
        listOf("uber eats") to Category.FOOD_DELIVERY,
        listOf("bigbasket", "blinkit", "zepto", "grofers", "dunzo", "jiomart") to Category.GROCERIES,
        listOf("restaurant", "cafe", "starbucks", "barista", "mcdonald", "kfc", "burger") to Category.RESTAURANTS,
        listOf("amazon") to Category.AMAZON,
        listOf("flipkart") to Category.FLIPKART,
        listOf("myntra") to Category.MYNTRA,
        listOf("uber", "ola", "rapido") to Category.RIDE_SHARING,
        listOf("indianoil", "hpcl", "bharat petroleum", "petrol", "fuel pump", "shell") to Category.FUEL,
        listOf("electricity", "bescom", "tneb", "mseb", "power bill", "discom") to Category.ELECTRICITY,
        listOf("water board", "water bill") to Category.WATER,
        listOf("airtel broadband", "jio fiber", "act fibernet", "broadband", "wifi") to Category.INTERNET,
        listOf("airtel", "jio", "vodafone", "vi prepaid", "bsnl", "mobile recharge") to Category.MOBILE,
        listOf("netflix", "hotstar", "prime video", "sonyliv", "zee5", "sun nxt") to Category.OTT,
        listOf("pvr", "inox", "bookmyshow", "cinepolis") to Category.MOVIES,
        listOf("steam", "playstation", "xbox", "epic games", "google play games") to Category.GAMES,
        listOf("pharmacy", "apollo", "medplus", "netmeds", "1mg", "pharmeasy") to Category.MEDICINES,
        listOf("hospital", "clinic", "diagnostic", "doctor", "practo") to Category.DOCTORS,
        listOf("lic", "insurance", "policybazaar", "hdfc life", "icici prudential") to Category.INSURANCE,
        listOf("coursera", "udemy", "byjus", "unacademy", "upgrad") to Category.COURSES,
        listOf("kindle", "books", "crossword", "sapna book") to Category.BOOKS,
        listOf("emi", "loan installment") to Category.EMI,
        listOf("credit card payment", "card bill", "cc payment") to Category.CREDIT_CARD_BILL,
        listOf("mutual fund", "zerodha", "groww", "upstox", "sip", "nps") to Category.INVESTMENT_OUTFLOW,
        listOf("salary", "payroll") to Category.SALARY,
        listOf("bonus", "incentive") to Category.BONUS,
        listOf("freelance", "upwork", "fiverr") to Category.FREELANCE,
        listOf("interest credited", "savings interest") to Category.INTEREST,
        listOf("dividend") to Category.DIVIDENDS,
        listOf("refund", "reversal", "cashback") to Category.REFUND
    )

    /**
     * Resolves a category from merchant/description text. Falls back to [Category.MISCELLANEOUS]
     * when nothing matches, never throws.
     */
    fun categorize(text: String): Category {
        val lower = text.lowercase()
        for ((keywords, category) in merchantKeywords) {
            if (keywords.any { lower.contains(it) }) return category
        }
        return Category.MISCELLANEOUS
    }
}
