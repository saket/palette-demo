package palette.demo

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min

data class PickedImage(
    val url: String,
    val file: File
)

suspend fun pickImage(): PickedImage? = suspendCancellableCoroutine { cont ->
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = "image/*"
    input.style.display = "none"
    
    input.onchange = {
        val file = input.files?.get(0)
        if (file != null) {
            val url = URL.createObjectURL(file)
            cont.resume(PickedImage(url, file))
        } else {
            cont.resume(null)
        }
        null
    }
    
    document.body?.appendChild(input)
    input.click()
    document.body?.removeChild(input)
}

suspend fun File.readBytes(): ByteArray = suspendCancellableCoroutine { cont ->
    val reader = FileReader()
    reader.onload = {
        try {
            val result = reader.result
            val arrayBuffer = result as ArrayBuffer
            val int8Array = Int8Array(arrayBuffer)
            val byteArray = ByteArray(int8Array.length)
            for (i in 0 until int8Array.length) {
                byteArray[i] = int8Array[i]
            }
            cont.resume(byteArray)
        } catch (e: Throwable) {
            cont.resumeWithException(e)
        }
        null
    }
    reader.onerror = {
        cont.resumeWithException(RuntimeException("Failed to read file"))
        null
    }
    reader.readAsArrayBuffer(this)
}

private const val MAX_IMAGE_DIMENSION = 2048

/**
 * Prepares an image for palette extraction by:
 * 1. Loading it via browser's Image element (supports all browser-native formats)
 * 2. Drawing to Canvas and exporting as PNG (guaranteed Skiko compatibility)
 * 3. Resizing if larger than MAX_IMAGE_DIMENSION
 */
class UnsupportedFormatException(val format: String) : Exception("$format images are not supported on this browser.")

suspend fun resizeImageForPalette(file: File): ByteArray {
    val format = file.type.substringAfter("/").uppercase().ifEmpty {
        file.name.substringAfterLast('.', "").uppercase()
    }
    val objectUrl = URL.createObjectURL(file)
    try {
        val img = loadHtmlImage(objectUrl, format)
        
        val originalWidth = img.naturalWidth
        val originalHeight = img.naturalHeight
        
        if (originalWidth == 0 || originalHeight == 0) {
            throw UnsupportedFormatException(format.ifEmpty { "Unknown" })
        }
        
        // Calculate new dimensions maintaining aspect ratio
        val scale = min(
            MAX_IMAGE_DIMENSION.toDouble() / originalWidth,
            MAX_IMAGE_DIMENSION.toDouble() / originalHeight
        ).coerceAtMost(1.0) // Don't upscale
        val newWidth = (originalWidth * scale).toInt()
        val newHeight = (originalHeight * scale).toInt()
        
        // Always convert through canvas to ensure PNG output that Skiko can decode
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = newWidth
        canvas.height = newHeight
        
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        ctx.drawImage(img, 0.0, 0.0, newWidth.toDouble(), newHeight.toDouble())
        
        return canvasToBytes(canvas)
    } finally {
        URL.revokeObjectURL(objectUrl)
    }
}

private suspend fun loadHtmlImage(src: String, format: String): HTMLImageElement = suspendCancellableCoroutine { cont ->
    val img = document.createElement("img") as HTMLImageElement
    img.onload = {
        cont.resume(img)
        null
    }
    img.onerror = { _, _, _, _, _ ->
        cont.resumeWithException(UnsupportedFormatException(format.ifEmpty { "Unknown" }))
        null
    }
    img.src = src
}

private fun canvasToBytes(canvas: HTMLCanvasElement): ByteArray {
    // Use toDataURL for synchronous, reliable PNG conversion
    val dataUrl = canvas.toDataURL("image/png")
    // Format: "data:image/png;base64,iVBORw0KGgo..."
    val base64 = dataUrl.substringAfter("base64,")
    return base64ToByteArray(base64)
}

private fun base64ToByteArray(base64: String): ByteArray {
    // Use browser's atob() to decode base64
    val binaryString = kotlinx.browser.window.asDynamic().atob(base64) as String
    return ByteArray(binaryString.length) { i ->
        binaryString[i].code.toByte()
    }
}
