package palette.demo

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
