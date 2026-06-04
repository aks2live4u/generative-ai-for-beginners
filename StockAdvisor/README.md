# Stock Advisor Android App

An AI-powered Android app that analyzes stock, ETF, and mutual fund investment decisions using Claude AI with live web search.

## Features

- Enter any ticker symbol (AAPL, VOO, FXAIX, BTC-USD, etc.)
- Choose your intended action: **BUY**, **SELL**, or **HOLD**
- Receive a comprehensive AI analysis:
  - Live market data from Yahoo Finance (no API key needed)
  - Real-time web research via Claude's built-in web search tool
  - Verdict: **WISE** / **RISKY** / **NEUTRAL**
  - Sections: Market Analysis, Technical, Fundamental, Risk Factors, Recommendation

## Setup

1. Get a free Anthropic API key at https://console.anthropic.com
2. Open `local.properties` and replace the placeholder:
   ```
   ANTHROPIC_API_KEY=sk-ant-...your-key-here...
   ```
3. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Install on your device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Requirements

- Android 8.0+ (API 26+)
- Internet connection
- Anthropic API key

## Project Structure

```
app/src/main/java/com/stockadvisor/
├── MainActivity.kt
├── data/
│   ├── model/          # StockData, StockAnalysis, Verdict
│   ├── network/        # YahooFinanceApi, AnthropicService
│   └── repository/     # StockRepository
├── ui/
│   ├── navigation/     # NavGraph
│   ├── screens/        # SearchScreen, DecisionScreen, ResultScreen
│   └── theme/          # Dark theme (Color, Type, Theme)
└── viewmodel/          # StockViewModel + AnalysisState
```

## Tech Stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| AI | Claude API (claude-opus-4-5) with web_search tool |
| Market Data | Yahoo Finance API (free, no key needed) |
| HTTP | OkHttp |
| Architecture | MVVM + StateFlow |
| Min SDK | 26 (Android 8.0) |

## Cost Estimate

~$0.003–$0.01 per analysis query. Essentially free for personal use.

---

**Disclaimer:** For educational purposes only. Not financial advice.
