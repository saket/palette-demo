package palette.demo

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun pickImage(): Pair<String, ByteArray>? = suspendCancellableCoroutine { cont ->
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = "image/*"
    input.style.display = "none"
    
    input.onchange = {
        val file = input.files?.get(0)
        if (file != null) {
            val url = URL.createObjectURL(file)
            
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
                    cont.resume(url to byteArray)
                } catch (e: Throwable) {
                    cont.resumeWithException(e)
                }
                null
            }
            reader.onerror = {
                cont.resumeWithException(RuntimeException("Failed to read file"))
                null
            }
            reader.readAsArrayBuffer(file)
        } else {
            cont.resume(null)
        }
        null
    }
    
    // Cleanup when cancelled (if possible) or just removing from DOM
    document.body?.appendChild(input)
    input.click()
    document.body?.removeChild(input)
}
