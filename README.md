# Bharat Wealth — Android starter project

A native Android app starter for Indian stocks and mutual funds, built with Kotlin + Jetpack Compose.

## Included in this first build
- Home dashboard and sample portfolio valuation
- NSE stock watchlist with a best-effort Yahoo Finance quote/history fetch
- Mutual fund scheme search and latest NAV lookup through MFAPI.in
- Manual portfolio holdings and P&L calculation
- SIP future-value calculator
- Light/dark theme toggle

## Important data notes
- Yahoo Finance's chart endpoint is unofficial and may rate-limit, change, or block requests. Quotes may be delayed and are not exchange-certified.
- MFAPI.in is a community API based on AMFI NAV data; it is not an investment recommendation.
- NIFTY/SENSEX tiles are placeholders in this starter and are not live index quotes yet.
- Portfolio entries are currently held in app memory and reset when the app process is restarted. Persistent Room storage, real index feeds, historical fund returns, and a production-grade licensed market-data provider are next steps.
- No trading, order placement, broker login, or investment advice is included.

## Build and install on an Android phone
1. Install **Android Studio** on a computer.
2. Extract this folder and open `BharatWealth` in Android Studio.
3. Allow Gradle sync and install the Android SDK Platform 35 if prompted.
4. Connect your Android phone with USB debugging enabled (or use Android Studio's wireless debugging).
5. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. Android Studio creates the APK under `app/build/outputs/apk/debug/`. Transfer it to your phone and install it. You may need to allow installation from that source.

## Requirements
- Android Studio with JDK 17
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Android SDK 35

## App identity
- Name: Bharat Wealth
- Application ID: `in.bharatwealth.app`
- Minimum Android: 8.0 (API 26)
