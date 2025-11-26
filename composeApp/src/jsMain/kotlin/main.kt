package palette.demo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import org.jetbrains.skiko.wasm.onWasmReady

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    println("Initializing App...")
    onWasmReady {
        println("Wasm Ready! Starting CanvasBasedWindow...")
        CanvasBasedWindow(title = "Palette Demo", canvasElementId = "ComposeTarget") {
            App()
        }
    }
}
