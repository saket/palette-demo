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
            fontFamily("Space Grotesk", "system-ui", "sans-serif")
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
                Text("palette")
            }
            P({
                style {
                    fontSize(1.2.cssRem)
                    color(Color("#aaaaaa"))
                    margin(0.px)
                }
            }) {
                Text("A demo of the ")
                A(href = "https://developer.android.com/develop/ui/views/graphics/palette-colors", {
                    style {
                        color(Color("#aaaaaa"))
                    }
                    attr("target", "_blank")
                }) {
                    Text("Jetpack Palette")
                }
                Text(" library")
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
                        maxHeight(400.px)
                        property("object-fit", "contain")
                        display(DisplayStyle.Block)
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
    
    // Calculate if text should be light or dark based on background luminance
    val r = (swatch.rgb shr 16) and 0xFF
    val g = (swatch.rgb shr 8) and 0xFF
    val b = swatch.rgb and 0xFF
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
    val textColor = if (luminance > 0.5) "rgba(0,0,0,0.8)" else "rgba(255,255,255,0.9)"
    val textColorMuted = if (luminance > 0.5) "rgba(0,0,0,0.5)" else "rgba(255,255,255,0.6)"

    Div({
        style {
            borderRadius(16.px)
            overflow("hidden")
            cursor("pointer")
            property("box-shadow", "0 4px 6px rgba(0,0,0,0.1)")
            property("transition", "transform 0.2s ease")
            backgroundColor(Color("rgb($r, $g, $b)"))
            padding(16.px)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            justifyContent(JustifyContent.SpaceBetween)
            minHeight(120.px)
        }
        onClick {
            kotlinx.browser.window.navigator.clipboard.writeText(hexColor)
            scope.launch {
                copied = true
                delay(2000)
                copied = false
            }
        }
    }) {
        // Swatch name at top
        Div({
            style {
                fontSize(0.75.cssRem)
                fontWeight("600")
                color(Color(textColorMuted))
                property("text-transform", "uppercase")
                property("letter-spacing", "0.05em")
            }
        }) {
            Text(name)
        }
        
        // Hex color and copy icon at bottom
        Div({
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                justifyContent(JustifyContent.SpaceBetween)
            }
        }) {
            Div({
                style {
                    fontSize(1.1.cssRem)
                    fontWeight("bold")
                    color(Color(textColor))
                }
            }) {
                Text(if (copied) "Copied!" else hexColor)
            }
            
            // Material 3 content_copy icon (SVG)
            Svg(viewBox = "0 0 24 24", {
                style {
                    width(18.px)
                    height(18.px)
                    property("fill", textColorMuted)
                }
            }) {
                Path("M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z")
            }
        }
    }
}

@Composable
fun Svg(viewBox: String, attrs: AttrBuilderContext<org.w3c.dom.svg.SVGElement>? = null, content: @Composable ElementScope<org.w3c.dom.svg.SVGElement>.() -> Unit) {
    TagElement(
        elementBuilder = { document.createElementNS("http://www.w3.org/2000/svg", "svg") as org.w3c.dom.svg.SVGElement },
        applyAttrs = {
            attr("viewBox", viewBox)
            attrs?.invoke(this)
        },
        content = content
    )
}

@Composable
fun ElementScope<org.w3c.dom.svg.SVGElement>.Path(d: String) {
    TagElement(
        elementBuilder = { document.createElementNS("http://www.w3.org/2000/svg", "path") as org.w3c.dom.svg.SVGPathElement },
        applyAttrs = { attr("d", d) },
        content = {}
    )
}
