# IndiStock Advisor — Android App

AI-powered stock analysis app for Indian and global markets. Uses Claude AI for investment research and Yahoo Finance for live market data.

## Getting Started

1. Build the APK in Android Studio (or run `./gradlew assembleDebug`)
2. Install on your device
3. On first launch, enter your Anthropic API key (get one free at [console.anthropic.com](https://console.anthropic.com))
4. Search any stock by name or ticker and get AI analysis

The API key is stored only on your device. You can update it anytime via the settings icon on the home screen.

## Features

- Search any ticker — Indian stocks (RELIANCE, TCS, ZOMATO), US stocks (AAPL), ETFs (VOO), or indices (^NSEI)
- Choose action: **BUY**, **SELL**, or **HOLD**
- AI verdict: **WISE** / **RISKY** / **NEUTRAL**
- Full analysis: Market, Technical, Fundamental, Risk Factors, Recommendation

## Requirements

- Android 8.0+ (API 26+)
- Internet connection
- Anthropic API key (free tier available)

## Project Structure

```
app/src/main/java/com/stockadvisor/
├── MainActivity.kt
├── data/
│   ├── model/          # StockData, StockAnalysis, Verdict
│   ├── network/        # YahooFinanceApi, AnthropicService
│   ├── preferences/    # ApiKeyRepository (on-device key storage)
│   └── repository/     # StockRepository
├── ui/
│   ├── navigation/     # NavGraph
│   ├── screens/        # SearchScreen, DecisionScreen, ResultScreen, ApiKeySetupScreen
│   └── theme/          # Dark theme (Color, Type, Theme)
└── viewmodel/          # StockViewModel + AnalysisState
```

## Tech Stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| AI | Claude API (claude-sonnet-4-6) |
| Market Data | Yahoo Finance (free, no key needed) |
| HTTP | OkHttp |
| Architecture | MVVM + StateFlow |
| Min SDK | 26 (Android 8.0) |

## Cost

~$0.003–$0.01 per analysis query on the free Anthropic tier.

---

**Disclaimer:** For educational purposes only. Not financial advice.
