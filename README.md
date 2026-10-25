# Waseem Offline AI 🧠⚡
> **100% Offline Personal AI Assistant for Android**  
> Developed by **Waseem Asif** ([wasimasif761@gmail.com](mailto:wasimasif761@gmail.com))  
> Package: `com.waseemasif.offlineai` | Target: Android 10+ (Optimized for 4GB RAM devices)

---

## 🌟 اردو تعارف (Urdu Overview)
**وسیم آف لائن اے آئی (Waseem Offline AI)** ایک مکمل، جدید اور 100 فیصد آف لائن پرسنل اسسٹنٹ اینڈرائیڈ ایپ ہے۔ یہ ایپ انٹرنیٹ کے بغیر آپ کے موبائل کی میموری میں **Qwen 2.5** نیورل لینگویج ماڈل کو براہِ راست چلاتی ہے۔
- **اردو، عربی اور انگریزی** میں مکمل ترجمہ اور بات چیت۔
- **پروگرامنگ اور کوڈنگ:** Python، JavaScript، HTML/CSS کے کوڈ لکھنا اور درست کرنا۔
- **فارمیسی اور ادویات:** خوراک کے فارمولے (Clark's & Young's Rules) اور ڈرگ ری ایکشنز۔
- **بزنس ای میلز اور پرپوزلز:** پیشہ ورانہ دفتری خطوط اور تجارتی مشاورت۔
- **آف لائن RAG نالج بیس:** اپنے نوٹس یا کسی بھی ویب پیج کو محفوظ کر کے ماڈل کے حافظے کا حصہ بنائیں۔

---

## 🚀 Key Features

1. **100% Real On-Device AI (Zero Cloud Reliance)**
   - Powered by Google **MediaPipe GenAI `LlmInference`** runtime.
   - Runs quantized **Qwen 2.5 (1.5B / 0.5B)** causal language models locally.
   - Complete privacy: No chat data or prompts ever leave your device.

2. **In-App Model Downloader**
   - No manual adb or complex file transfers required.
   - Download the model with one tap using the in-app downloader powered by streaming OkHttp.
   - Shows real-time progress bar, downloaded megabytes, transfer speed (MB/s), and cancel button.
   - Automatically saved to:
     ```
     context.getExternalFilesDir(null) + "/qwen_model.bin"
     ```
   - Automatically hides the download banner once the model is installed and ready.

3. **Offline RAG (Retrieval-Augmented Generation) System**
   - Built on Android **Room Database** with Kotlin Symbol Processing (KSP).
   - **Update Knowledge Online:** Ingest documentation, medical notes, or coding snippets directly from any web URL.
   - **Custom Knowledge Paste:** Paste company policies, email templates, or private notes into the local database.
   - **Context Injection:** When chatting offline, the app automatically extracts relevant knowledge snippets based on your question keywords and injects them into the Qwen ChatML system prompt.

4. **ChatGPT-like Dark UI**
   - Modern Material Design 3 interface with deep obsidian black background (`#090D14`), glowing cyan accents (`#00E5FF`), and electric violet highlights.
   - Distinctive user and AI bubbles with dedicated code block syntax highlighting.
   - **Copy Button:** Instant one-tap copy button on every AI response.
   - **Quick Action Chips:** Pre-built prompts for Urdu translation, Arabic translation, Python scripting, pharmacy inquiries, and business correspondence.

5. **CI/CD Automation**
   - Complete GitHub Actions workflow (`.github/workflows/build.yml`) that automatically builds and outputs the APK artifact on every push.

---

## 🏗️ Technical Architecture & Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.2.10 |
| **UI Framework** | Jetpack Compose & Material 3 (BOM 2024.09.00) |
| **AI Runtime** | Google MediaPipe Tasks GenAI (`com.google.mediapipe:tasks-genai:0.10.14`) |
| **Neural Models** | Qwen 2.5 Instruct quantized (`.task` / `.bin` format) |
| **Database (RAG)** | Android Room 2.7.0 with SQLite & Flow |
| **HTTP Downloader** | OkHttp 4.10 (chunked streaming with atomic file renames) |
| **Asynchronous Engine** | Kotlin Coroutines & StateFlow (Unidirectional Data Flow) |

---

## 📦 AI Model Direct Links

The app downloads official LiteRT / MediaPipe bundles directly from the Hugging Face Hub:

- **Standard Model (Recommended, ~1.2 GB):**  
  [Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task](https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_seq128_q8_ekv4096.task)
  
- **Lite Model (For 4GB RAM devices / Fast testing, ~350 MB):**  
  [Qwen2.5-0.5B-Instruct_seq128_q8_ekv1280.task](https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_seq128_q8_ekv1280.task)

> **Manual Push via ADB (Alternative):**  
> If you prefer pushing the model manually via terminal:
> ```bash
> adb push Qwen2.5-1.5B-Instruct.task /sdcard/Android/data/com.waseemasif.offlineai/files/qwen_model.bin
> ```

---

## 🛠️ How to Build and Run

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or JDK 21
- Android SDK with Platform 36 (minSdk 24, targetSdk 36)

### Step 1: Clone the Repository
```bash
git clone https://github.com/<your-username>/waseem-offline-ai.git
cd waseem-offline-ai
```

### Step 2: Build the Debug APK
Using Gradle wrapper:
```bash
# On Linux / macOS
chmod +x gradlew
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug
```
The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Install on Device or Emulator
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📂 Project Structure

```
waseem-offline-ai/
├── .github/
│   └── workflows/
│       └── build.yml               # GitHub Actions CI workflow to build APK
├── app/
│   ├── build.gradle.kts            # App dependencies (MediaPipe, Room, Compose)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml # Permissions & Application declaration
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt    # Main UI, ChatGPT chat & ViewModels
│   │   │   │   ├── LLMHelper.kt       # MediaPipe Qwen inference & RAG context
│   │   │   │   ├── DatabaseHelper.kt  # Room DB, RAG retrieval & URL scraper
│   │   │   │   ├── ModelDownloader.kt # OkHttp progress downloader
│   │   │   │   └── ui/theme/          # Dark theme colors & typography
│   │   │   └── res/
│   │   │       ├── drawable/          # Custom AI adaptive logo assets
│   │   │       └── mipmap-*/          # Launcher icons (mdpi to xxxhdpi)
├── gradle/
│   ├── libs.versions.toml          # Gradle version catalog
│   └── wrapper/                    # Gradle wrapper binaries & configs
├── gradlew                         # Linux/macOS build script
├── gradlew.bat                     # Windows build script
├── LICENSE                         # MIT License
└── README.md                       # Documentation
```

---

## 👨‍💻 Developer & Contact

- **Lead Developer:** Waseem Asif
- **Email:** [wasimasif761@gmail.com](mailto:wasimasif761@gmail.com)
- **Role:** Full-Stack & On-Device AI Engineer

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
