# Project Summary

A web demo of the [Jetpack Palette](https://developer.android.com/develop/ui/views/graphics/palette-colors) library, built using its multiplatform port [kmpalette](https://github.com/jordond/kmpalette).

**Live demo:** https://saket.github.io/palette-demo/

## Tech Stack

- **Language:** Kotlin 2.0.20
- **UI Framework:** Compose HTML 1.7.0 (JetBrains Compose for Web)
- **Target:** JavaScript/Browser (Kotlin/JS with IR compiler)
- **Palette Library:** [kmpalette](https://github.com/jordond/kmpalette) 3.1.0

## Project Structure

```
palette-demo/
├── composeApp/
│   ├── src/jsMain/kotlin/
│   │   ├── App.kt          # Main UI - image picker, palette display, drag-drop
│   │   └── ImagePicker.kt  # File picking and image preprocessing utilities
│   └── build.gradle.kts
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

## Key Features

1. **Image Input:** Drag-and-drop or click-to-upload image selection
2. **Palette Extraction:** Extracts vibrant, muted, and dominant color swatches
3. **Color Display:** Shows extracted colors in clickable swatch cards (click to copy hex)
4. **Theme Support:** Respects system light/dark mode preferences

## Technical Notes

- Uses Skiko/WASM for kmpalette's bitmap processing (`onWasmReady`)
- Implements custom nearest-neighbor image scaling to work around [kmpalette bug #224](https://github.com/jordond/kmpalette/issues/224) where bilinear interpolation averages out vibrant colors
- Global drag-drop uses CSS class toggling to avoid re-render flicker in Safari

## Commands

```bash
# Run development server
./gradlew jsBrowserDevelopmentRun

# Build production bundle
./gradlew jsBrowserDistribution

# Build check (compile only)
./gradlew jsMainClasses
```
