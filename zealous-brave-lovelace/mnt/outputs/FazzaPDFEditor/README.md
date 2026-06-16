# Fazza PDF Editor - Android App

A full-featured PDF editor for Android built with Kotlin and Jetpack Compose.

## Features
- **View PDFs** – Smooth page-by-page scrolling with zoom support
- **Annotate** – Highlight, underline, freehand draw, and add text annotations
- **Edit Text** – Tap any page to insert text at a precise position
- **Merge PDFs** – Combine multiple PDFs with drag-to-reorder
- **Split PDFs** – Extract a page range into a new PDF
- **Recent Files** – Quick access to recently opened documents
- **Share** – Share any PDF directly from the app

## Tech Stack
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose |
| DI | Hilt |
| Database | Room |
| PDF Render | Android PdfRenderer |
| PDF Manipulation | PDFBox for Android |
| Async | Coroutines + Flow |
| Architecture | MVVM + Clean Architecture |

## Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Gradle 8.4+

### Steps
1. Clone / unzip this project
2. Open in Android Studio: `File → Open → FazzaPDFEditor/`
3. Let Gradle sync complete (downloads ~50MB of dependencies)
4. Connect an Android device (API 26+) or start an emulator
5. Click **Run ▶**

### Minimum Requirements
- Android 8.0 (API 26) or higher
- ~100MB storage for app + cache

## Project Structure
```
app/src/main/
├── java/com/fazza/pdfeditor/
│   ├── MainActivity.kt          # Entry point
│   ├── FazzaApp.kt              # Application class
│   ├── Navigation.kt            # Nav graph
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── HomeScreen.kt        # Recent files & quick actions
│   │   │   ├── PdfViewerScreen.kt   # View + annotate PDFs
│   │   │   ├── PdfEditorScreen.kt   # Edit text in PDFs
│   │   │   └── MergeSplitScreen.kt  # Merge & split PDFs
│   │   └── theme/               # Color, Type, Theme
│   ├── viewmodel/               # ViewModels (MVVM)
│   ├── data/
│   │   ├── model/               # RecentFile, Annotation
│   │   ├── db/                  # Room DAOs
│   │   └── repository/          # PdfRepository
│   ├── di/                      # Hilt modules
│   └── utils/                   # FileUtils
└── res/                         # Resources
```

## Output Files
Processed PDFs are saved to:
`/Android/data/com.fazza.pdfeditor/files/FazzaPDF/`

## License
© 2024 Fazza PDF Editor. All rights reserved.
