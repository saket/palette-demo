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
import kotlinx.coroutines.yield
import kotlinx.browser.window
import org.w3c.dom.DragEvent
import org.w3c.dom.url.URL
import org.w3c.files.File
import org.w3c.files.get

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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    suspend fun loadImage(file: File) {
        imageUrl = URL.createObjectURL(file)
        palette = null
        errorMessage = null
        try {
            val bytes = file.readBytes()
            console.log("Image bytes loaded: ${bytes.size}")
            val bitmap = ByteArrayLoader.load(bytes)
            console.log("Bitmap loaded: ${bitmap.width}x${bitmap.height}")
            val generatedPalette = Palette.from(bitmap).generate()
            console.log("Palette generated. Swatches: vibrant=${generatedPalette.vibrantSwatch}, muted=${generatedPalette.mutedSwatch}, dominant=${generatedPalette.dominantSwatch}")
            
            val hasSwatches = listOfNotNull(
                generatedPalette.vibrantSwatch,
                generatedPalette.darkVibrantSwatch,
                generatedPalette.lightVibrantSwatch,
                generatedPalette.mutedSwatch,
                generatedPalette.darkMutedSwatch,
                generatedPalette.lightMutedSwatch,
                generatedPalette.dominantSwatch
            ).isNotEmpty()
            
            if (hasSwatches) {
                palette = generatedPalette
            } else {
                errorMessage = "No colors could be extracted from this image."
            }
        } catch (e: Exception) {
            console.error("Error loading image: ${e.message}")
            e.printStackTrace()
            errorMessage = "Error: ${e.message}"
        }
    }
    
    // Global drag and drop on document body
    DisposableEffect(Unit) {
        val onDragOver: (DragEvent) -> Unit = { event ->
            event.preventDefault()
        }
        val onDragEnter: (DragEvent) -> Unit = { event ->
            event.preventDefault()
            isDragging = true
        }
        val onDragLeave: (DragEvent) -> Unit = { event ->
            event.preventDefault()
            if (event.relatedTarget == null) {
                isDragging = false
            }
        }
        val onDrop: (DragEvent) -> Unit = { event ->
            event.preventDefault()
            isDragging = false
            val file = event.dataTransfer?.files?.get(0) as? File
            if (file != null && file.type.startsWith("image/")) {
                scope.launch { loadImage(file) }
            }
        }
        
        document.body?.addEventListener("dragover", onDragOver.unsafeCast<(org.w3c.dom.events.Event) -> Unit>())
        document.body?.addEventListener("dragenter", onDragEnter.unsafeCast<(org.w3c.dom.events.Event) -> Unit>())
        document.body?.addEventListener("dragleave", onDragLeave.unsafeCast<(org.w3c.dom.events.Event) -> Unit>())
        document.body?.addEventListener("drop", onDrop.unsafeCast<(org.w3c.dom.events.Event) -> Unit>())
        
        onDispose {
            document.body?.removeEventListener("dragover", onDragOver.unsafeCast<(org.w3c.dom.events.Event) -> Unit>())
            document.body?.removeEventListener("dragenter", onDragEnter.unsafeCast<(org.w3c.dom.events.Event) -> Unit>())
            document.body?.removeEventListener("dragleave", onDragLeave.unsafeCast<(org.w3c.dom.events.Event) -> Unit>())
            document.body?.removeEventListener("drop", onDrop.unsafeCast<(org.w3c.dom.events.Event) -> Unit>())
        }
    }

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
                backgroundColor(if (isDragging) Color("#2a2a2a") else Color("#1e1e1e"))
                borderRadius(24.px)
                overflow("hidden")
                border(2.px, LineStyle.Dashed, if (isDragging) Color("#666666") else Color("#333333"))
                position(Position.Relative)
                cursor("pointer")
                property("transition", "all 0.2s ease")
            }
            
            onClick {
                scope.launch {
                    pickImage()?.let { loadImage(it.file) }
                }
            }
        }) {
            if (imageUrl != null) {
                Img(src = imageUrl!!) {
                    style {
                        width(100.percent)
                        minHeight(200.px)
                        maxHeight(400.px)
                        property("object-fit", "contain")
                        display(DisplayStyle.Block)
                    }
                }
                // Drag overlay
                if (isDragging) {
                    Div({
                        style {
                            position(Position.Absolute)
                            top(0.px)
                            left(0.px)
                            right(0.px)
                            bottom(0.px)
                            backgroundColor(Color("rgba(0,0,0,0.7)"))
                            display(DisplayStyle.Flex)
                            justifyContent(JustifyContent.Center)
                            alignItems(AlignItems.Center)
                            color(Color.white)
                            fontSize(1.2.cssRem)
                            fontWeight("bold")
                            property("pointer-events", "none")
                        }
                    }) {
                        Text("Drop to replace")
                    }
                } else {
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

        // Error message
        if (errorMessage != null) {
            Div({
                style {
                    backgroundColor(Color("#2a1a1a"))
                    border(1.px, LineStyle.Solid, Color("#ff6b6b"))
                    borderRadius(12.px)
                    padding(16.px, 24.px)
                    color(Color("#ff6b6b"))
                    width(100.percent)
                    maxWidth(600.px)
                    textAlign("center")
                }
            }) {
                Text(errorMessage!!)
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
