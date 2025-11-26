package palette.demo

import androidx.compose.runtime.*
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.css.*
import org.jetbrains.skiko.wasm.onWasmReady
import com.kmpalette.loader.ByteArrayLoader
import com.kmpalette.palette.graphics.Palette
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.accept
import kotlinx.coroutines.delay

fun main() {
    // We still need Skia loaded for kmpalette to work, even if we render DOM
    onWasmReady {
        renderComposable(rootElementId = "root") {
            App()
        }
    }
}

@Composable
fun App() {
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var palette by remember { mutableStateOf<Palette?>(null) }
    val scope = rememberCoroutineScope()

    // Global Styles
    Style {
        "body" {
            fontFamily("system-ui", "-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "Oxygen", "Ubuntu", "Cantarell", "Open Sans", "Helvetica Neue", "sans-serif")
            backgroundColor(Color("#121212"))
            color(Color("#ffffff"))
            margin(0.px)
            padding(0.px)
            display(DisplayStyle.Flex)
            justifyContent(JustifyContent.Center)
            minHeight(100.vh)
        }
    }

    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Center)
            maxWidth(1000.px)
            width(100.percent)
            padding(40.px)
            gap(32.px)
        }
    }) {
        // Header
        Div({
            style {
                textAlign("center")
            }
        }) {
            H1({
                style {
                    fontSize(3.cssRem)
                    marginBottom(8.px)
                    color(Color("#ffffff"))
                }
            }) {
                Text("Palette Generator")
            }
            P({
                style {
                    fontSize(1.2.cssRem)
                    color(Color("#aaaaaa"))
                    margin(0.px)
                }
            }) {
                Text("Extract beautiful color schemes from your images.")
            }
        }

        // Image Drop Zone / Display
        Div({
            style {
                width(100.percent)
                maxWidth(600.px)
                backgroundColor(Color("#1e1e1e"))
                borderRadius(24.px)
                overflow("hidden")
                border(2.px, LineStyle.Dashed, Color("#333333"))
                position(Position.Relative)
                cursor("pointer")
                property("transition", "all 0.2s ease")
            }
            
            onClick {
                scope.launch {
                    val pair = pickImage()
                    if (pair != null) {
                        imageUrl = pair.first
                        try {
                            // kmpalette logic (Headless)
                            val bitmap = ByteArrayLoader.load(pair.second)
                            palette = Palette.from(bitmap).generate()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }) {
            if (imageUrl != null) {
                Img(src = imageUrl!!) {
                    style {
                        width(100.percent)
                        display(DisplayStyle.Block) // Remove bottom gap
                    }
                }
                // Change Image Overlay
                Div({
                    style {
                        position(Position.Absolute)
                        bottom(16.px)
                        right(16.px)
                        backgroundColor(Color("rgba(0,0,0,0.7)"))
                        color(Color.white)
                        padding(8.px, 16.px)
                        borderRadius(20.px)
                        fontSize(0.9.cssRem)
                        fontWeight("bold")
                    }
                }) {
                    Text("Change Image")
                }
            } else {
                Div({
                    style {
                        height(300.px)
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        justifyContent(JustifyContent.Center)
                        alignItems(AlignItems.Center)
                        gap(16.px)
                    }
                }) {
                    Text("Click to upload image")
                }
            }
        }

        // Palette Grid
        if (palette != null) {
            val swatches = listOfNotNull(
                palette!!.vibrantSwatch?.let { it to "Vibrant" },
                palette!!.darkVibrantSwatch?.let { it to "Dark Vibrant" },
                palette!!.lightVibrantSwatch?.let { it to "Light Vibrant" },
                palette!!.mutedSwatch?.let { it to "Muted" },
                palette!!.darkMutedSwatch?.let { it to "Dark Muted" },
                palette!!.lightMutedSwatch?.let { it to "Light Muted" },
                palette!!.dominantSwatch?.let { it to "Dominant" }
            )

            Div({
                style {
                    display(DisplayStyle.Grid)
                    gridTemplateColumns("repeat(auto-fill, minmax(160px, 1fr))")
                    gap(24.px)
                    width(100.percent)
                }
            }) {
                swatches.forEach { (swatch, name) ->
                    SwatchCard(swatch, name)
                }
            }
        }
    }
}

@Composable
fun SwatchCard(swatch: com.kmpalette.palette.graphics.Palette.Swatch, name: String) {
    val hexColor = "#" + (swatch.rgb.toUInt().toString(16).takeLast(6).uppercase())
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Div({
        style {
            backgroundColor(Color("#1e1e1e")) // Card bg
            borderRadius(16.px)
            overflow("hidden")
            cursor("pointer")
            property("box-shadow", "0 4px 6px rgba(0,0,0,0.1)")
            property("transition", "transform 0.2s ease")
        }
        onClick {
            // Copy to clipboard
            kotlinx.browser.window.navigator.clipboard.writeText(hexColor)
            scope.launch {
                copied = true
                delay(2000)
                copied = false
            }
        }
    }) {
        // Color Block
        Div({
            style {
                height(120.px)
                backgroundColor(Color("rgb(${swatch.rgb shr 16 and 0xFF}, ${swatch.rgb shr 8 and 0xFF}, ${swatch.rgb and 0xFF})"))
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.Center)
                alignItems(AlignItems.Center)
            }
        }) {
            if (copied) {
                Div({
                    style {
                        backgroundColor(Color("rgba(0,0,0,0.5)"))
                        color(Color.white)
                        padding(8.px, 16.px)
                        borderRadius(20.px)
                        fontWeight("bold")
                    }
                }) {
                    Text("Copied!")
                }
            }
        }

        // Info Block
        Div({
            style {
                padding(16.px)
            }
        }) {
            Div({
                style {
                    fontSize(0.8.cssRem)
                    fontWeight("bold")
                    color(Color("rgba(255,255,255,0.6)"))
                    marginBottom(4.px)
                }
            }) {
                Text(name)
            }
            Div({
                style {
                    fontSize(1.2.cssRem)
                    fontWeight("bold")
                    color(Color.white)
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.SpaceBetween)
                }
            }) {
                Text(hexColor)
                // Copy Icon (Unicode)
                Span({ style { fontSize(1.cssRem); opacity(0.5) } }) {
                    Text("📋")
                }
            }
        }
    }
}
