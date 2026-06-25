package com.finsight.core.model

/**
 * All transaction categories supported by the app, grouped under income/expense
 * per the product blueprint.
 */
enum class Category(val displayName: String, val group: CategoryGroup) {
    // Income
    SALARY("Salary", CategoryGroup.INCOME),
    BONUS("Bonus", CategoryGroup.INCOME),
    FREELANCE("Freelance", CategoryGroup.INCOME),
    RENT_INCOME("Rent", CategoryGroup.INCOME),
    INTEREST("Interest", CategoryGroup.INCOME),
    DIVIDENDS("Dividends", CategoryGroup.INCOME),
    INVESTMENT_INCOME("Investments", CategoryGroup.INCOME),
    REFUND("Refunds", CategoryGroup.INCOME),

    // Food
    RESTAURANTS("Restaurants", CategoryGroup.FOOD),
    FOOD_DELIVERY("Food Delivery", CategoryGroup.FOOD),
    GROCERIES("Groceries", CategoryGroup.FOOD),

    // Shopping
    AMAZON("Amazon", CategoryGroup.SHOPPING),
    FLIPKART("Flipkart", CategoryGroup.SHOPPING),
    MYNTRA("Myntra", CategoryGroup.SHOPPING),
    SHOPPING_OTHER("Shopping", CategoryGroup.SHOPPING),

    // Transportation
    RIDE_SHARING("Ride Sharing", CategoryGroup.TRANSPORTATION),
    FUEL("Fuel", CategoryGroup.TRANSPORTATION),
    TRANSPORTATION_OTHER("Transportation", CategoryGroup.TRANSPORTATION),

    // Utilities
    ELECTRICITY("Electricity", CategoryGroup.UTILITIES),
    WATER("Water", CategoryGroup.UTILITIES),
    INTERNET("Internet", CategoryGroup.UTILITIES),
    MOBILE("Mobile", CategoryGroup.UTILITIES),

    // Entertainment
    OTT("OTT", CategoryGroup.ENTERTAINMENT),
    MOVIES("Movies", CategoryGroup.ENTERTAINMENT),
    GAMES("Games", CategoryGroup.ENTERTAINMENT),

    // Health
    MEDICINES("Medicines", CategoryGroup.HEALTH),
    DOCTORS("Doctors", CategoryGroup.HEALTH),
    INSURANCE("Insurance", CategoryGroup.HEALTH),

    // Education
    BOOKS("Books", CategoryGroup.EDUCATION),
    COURSES("Courses", CategoryGroup.EDUCATION),

    // EMI / Credit
    EMI("EMI", CategoryGroup.FINANCIAL),
    CREDIT_CARD_BILL("Credit Card Bill", CategoryGroup.FINANCIAL),
    INVESTMENT_OUTFLOW("Investments", CategoryGroup.FINANCIAL),

    // Cash withdrawn from an ATM is its own category rather than falling into Miscellaneous: the
    // SMS only ever tells us cash left the account, not what it was spent on (it may have been
    // handed to a family member, kept as float, or spent in ways no SMS will ever capture) - lumping
    // it into Miscellaneous spending falsely inflates "unexplained personal spending" totals.
    ATM_WITHDRAWAL("ATM Withdrawal", CategoryGroup.TRANSFERS),

    MISCELLANEOUS("Miscellaneous", CategoryGroup.MISCELLANEOUS);

    companion object {
        fun fromDisplayName(name: String): Category? = entries.find { it.displayName.equals(name, ignoreCase = true) }
    }
}

enum class CategoryGroup {
    INCOME,
    FOOD,
    SHOPPING,
    TRANSPORTATION,
    UTILITIES,
    ENTERTAINMENT,
    HEALTH,
    EDUCATION,
    FINANCIAL,
    // Cash withdrawals and similar money-movement events that aren't themselves a purchase -
    // excluded from "personal spending" aggregates the same way INCOME is, since the SMS only
    // confirms money left the account, not what category it was actually spent in.
    TRANSFERS,
    MISCELLANEOUS
}
