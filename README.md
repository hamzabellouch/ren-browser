# Ren Browser

<h3 align="center"> Modern, Privacy-First Android Web Browser with Built-in Tor, AdBlock, Anti-Fingerprinting & Sandbox Isolation.</h3>

<p align="center">
Fast, lightweight, and privacy-focused Android browser built with Kotlin and Jetpack Compose.
</p>

<img width="2752" height="1536" alt="Ren" src="https://github.com/user-attachments/assets/98bc9617-ce6a-4d74-b95f-1322b4f11df0" />



## 🌟 Overview

**Ren Browser** is a modern, high-performance, and privacy-first web browser built from the ground up for Android. Designed for users who prioritize digital autonomy and data security, Ren Browser provides comprehensive browsing privacy without compromising speed, usability, or aesthetics.

Unlike traditional browsers laden with telemetry SDKs, ad trackers, and background sync services, Ren Browser operates with a **Zero-Tracking, Privacy-by-Default** philosophy. Everything is executed locally on-device with robust security controls, including native Tor onion routing, Cloudflare 1.1.1.1 / WARP DoH, advanced cosmetic & network ad blocking, bookmarks & session persistence, WebRTC protection, anti-fingerprinting, and custom sandboxing.

Built following **Modern Android Development (MAD)** best practices using Kotlin and Jetpack Compose with Material Design 3 and Dynamic Color support.


## 📸 Screenshots

<div align="center">
  <div>
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/1.jpg" width="30%" alt="Screenshot 1" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/2.jpg" width="30%" alt="Screenshot 2" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/3.jpg" width="30%" alt="Screenshot 3" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/4.jpg" width="30%" alt="Screenshot 4" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/5.jpg" width="30%" alt="Screenshot 5" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/6.jpg" width="30%" alt="Screenshot 6" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/7.jpg" width="30%" alt="Screenshot 7" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/8.jpg" width="30%" alt="Screenshot 8" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/9.jpg" width="30%" alt="Screenshot 9" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/10.jpg" width="30%" alt="Screenshot 10" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/11.jpg" width="30%" alt="Screenshot 11" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/12.jpg" width="30%" alt="Screenshot 12" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/13.jpg" width="30%" alt="Screenshot 13" />
    <img src="https://github.com/hamzabellouch/ren/blob/main/Images/14.jpg" width="30%" alt="Screenshot 14" />
  </div>
</div>

<br>


## 🛡️ Key Features & Privacy Engine

| Feature | Component / Engine | Privacy & Performance | Description |
| :--- | :--- | :--- | :--- |
| **RenTor Onion Engine** | `RenTorEngine` & `RenTorManager` | **Anonymity & Censorship Bypass** | Full Tor onion routing with custom bridge support (obfs4, Snowflake, Meek), circuit isolation, and native `.onion` browsing. |
| **Cloudflare WARP & DoH** | `CloudflareEngine` & `WarpManager` | **Encrypted DNS & Tunneling** | Integrated 1.1.1.1 DNS-over-HTTPS resolution and WARP fast encrypted routing mode. |
| **Advanced AdBlock** | `AdBlockManager` | **High Efficiency** | Blocks trackers, malicious domains, and ads at the network level, with CSS cosmetic element hiding. |
| **Bookmarks & Favorites** | `BookmarkManager` | **100% Local Storage** | Organize, search, export, and manage bookmarks seamlessly via bottom sheets and views. |
| **Session & Tab Recovery** | `TabSessionManager` | **State Persistence** | Automatic tab session state saving and restoration across device reboots and app launches. |
| **Smart Search Suggestions** | `SearchSuggestionManager` | **Debounced & Multi-Engine** | Real-time autocomplete suggestions supporting DuckDuckGo, Google, Bing, Startpage, and more. |
| **Anti-Fingerprinting** | `AntiFingerprintManager` | **High Protection** | Spoofs canvas, audio, battery API, and device parameters to prevent digital fingerprinting. |
| **Site Sandbox Isolation** | `SandboxManager` & `SiteConfigManager` | **Zero Leakage** | Isolates individual site storage, cookies, and permissions (JS, geolocation, camera, mic). |
| **WebRTC Protection** | `WebRtcManager` | **IP Leak Prevention** | Prevents real IP address leaks through WebRTC peer connections. |
| **Incognito Mode** | `IncognitoNotificationManager` | **Zero History** | Ephemeral browsing sessions with instant notification-based session clearing. |
| **Custom Userscripts** | `ScriptManager` | **On-Device Execution** | Install and manage custom user scripts for page enhancement and ad customization. |
| **QR Code Tools** | `QrCodeGenerator` & Scanner | **100% On-Device** | Instant hardware-accelerated QR code scanning and custom QR generation. |
| **Reader Mode & Translate** | `ReaderModeManager` & `TranslationManager` | **Distraction-Free** | Clean reading view without clutter, plus integrated translation support. |
| **Bilingual Localization** | Arabic & English | **Native UI** | Fully localized interface with native RTL support for Arabic and dynamic LTR switching. |
| **Zero Telemetry** | Pure Local Logic | **Complete Privacy** | No analytics SDKs, no diagnostic data collection, no background telemetry. |


## 🛠 Tech Stack & Architecture

Ren Browser follows clean architecture and modern Android design patterns:

* **Language & Concurrency:** 100% Kotlin, Kotlin Coroutines, and StateFlow / SharedFlow.
* **UI & Design:** Jetpack Compose, Material Design 3 (Material You), Dynamic Theming, and Smooth Transitions.
* **Privacy & Network Stack:**
  - `RenTor Engine` & `TorController` for SOCKS5 onion routing and bridge configurations.
  - `Cloudflare 1.1.1.1 Engine` for DNS-over-HTTPS (DoH) encrypted lookups and WARP integration.
  - `Android WebView` with custom request interceptors, script injectors, and cookie sandboxing.
  - `OkHttp 4` with connection pooling and secure HTTP client interceptors.
* **Hardware & Utility Engines:**
  - `CameraX` & `ZXing Core` for hardware-accelerated QR code scanning and matrix generation.
* **Storage & Persistence:**
  - SharedPreferences and local SQLite storage strictly kept on-device with zero cloud dependencies.
* **Testing:**
  - JUnit 4 & Robolectric testing suite for core privacy managers (`RenTorTest`, `BookmarkManagerTest`, `SearchSuggestionManagerTest`, `TabSessionManagerTest`).


## 🔥 Installation

1. Go to the Releases page:
   https://github.com/hamzabellouch/ren-browser/releases

2. Download the latest `.apk` file.

3. Install the application on your Android device.

4. Make sure that `Install from unknown sources` is enabled in your Android settings.


## 🔨 Building & Testing from Source

To build and test Ren Browser locally, make sure you have the latest version of Android Studio installed (Java 17+ recommended).

1. Clone the repository:
   ```bash
   git clone https://github.com/hamzabellouch/ren-browser.git
   cd ren-browser
   ```
2. Open the project in Android Studio.
3. Sync Gradle dependencies.
4. Run the unit test suite:
   ```bash
   ./gradlew test
   ```
5. Build and run the application on your device or emulator.



> [!WARNING]
> There is always a possibility of error, so we assume no responsibility for any inaccuracies.


### <a name="Copyright©2026"></a> Copyright © 2026

Thank you for checking out Ren Browser. If you have any feedback or suggestions, feel free to contact us:
hamzabellouchcontact@gmail.com

Stay connected and follow us on:  
[WhatsApp](https://whatsapp.com/channel/0029Vb7MArw0LKZMpjjqOk2P) | [Facebook](https://facebook.com/hamzabellouch0) | [Instagram](https://instagram.com/hamzabellouch0) | [Twitter](https://twitter.com/hamzabellouch0) | [Telegram](https://t.me/hammzabellouch) | [LinkedIn](https://www.linkedin.com/in/hamzabellouch)
