package com.personalfinanceai.core.model

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
    MISCELLANEOUS
}
