"""Lightweight salary baseline lookup.

There is no free, compliant, real-time salary API with global coverage, so
this ships a small curated baseline table (annual, USD) for common titles,
adjusted by a country cost-of-labor multiplier. It is deliberately
approximate -- treat it as a directional estimate, not ground truth. When an
OpenAI key is configured, NegotiationAgent/SalaryInsight blend this baseline
with a qualitative AI commentary; without a key they fall back to the table
alone.
"""

_BASELINE_USD = {
    "cloud operations manager": (85000, 115000, 145000),
    "site reliability engineering manager": (110000, 145000, 180000),
    "cloud service delivery manager": (90000, 120000, 150000),
    "devops manager": (100000, 130000, 165000),
    "it operations manager": (80000, 105000, 135000),
    "cloud infrastructure manager": (95000, 125000, 155000),
    "service management": (75000, 100000, 130000),
    "software engineer": (85000, 115000, 150000),
    "engineering manager": (120000, 155000, 195000),
    "product manager": (100000, 135000, 175000),
    "data scientist": (95000, 130000, 170000),
}

_COUNTRY_MULTIPLIER = {
    "united states": 1.0,
    "united kingdom": 0.72,
    "germany": 0.75,
    "india": 0.28,
    "united arab emirates": 0.85,
    "canada": 0.82,
    "australia": 0.88,
    "netherlands": 0.8,
    "singapore": 0.85,
}

_CURRENCY_BY_COUNTRY = {
    "united states": "USD",
    "united kingdom": "GBP",
    "germany": "EUR",
    "india": "INR",
    "united arab emirates": "AED",
    "canada": "CAD",
    "australia": "AUD",
    "netherlands": "EUR",
    "singapore": "SGD",
}

_FX_FROM_USD = {
    "USD": 1.0,
    "GBP": 0.79,
    "EUR": 0.92,
    "INR": 83.0,
    "AED": 3.67,
    "CAD": 1.36,
    "AUD": 1.5,
    "SGD": 1.34,
}


def _closest_title_key(title: str) -> str:
    title_lower = title.lower()
    best_key, best_overlap = "software engineer", 0
    for key in _BASELINE_USD:
        overlap = len(set(key.split()) & set(title_lower.split()))
        if overlap > best_overlap:
            best_key, best_overlap = key, overlap
    return best_key


def estimate_salary(title: str, country: str = "") -> dict:
    key = _closest_title_key(title)
    low_usd, mid_usd, high_usd = _BASELINE_USD[key]

    country_key = (country or "").strip().lower()
    multiplier = _COUNTRY_MULTIPLIER.get(country_key, 1.0)
    currency = _CURRENCY_BY_COUNTRY.get(country_key, "USD")
    fx = _FX_FROM_USD.get(currency, 1.0)

    def convert(usd_value: float) -> float:
        return round(usd_value * multiplier * fx, -2)

    demand = "high" if key in {"devops manager", "site reliability engineering manager", "cloud infrastructure manager"} else "medium"

    return {
        "matched_title": key,
        "estimated_min": convert(low_usd),
        "estimated_median": convert(mid_usd),
        "estimated_max": convert(high_usd),
        "currency": currency,
        "market_demand": demand,
    }
