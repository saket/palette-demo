package palette.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import com.kmpalette.loader.ByteArrayLoader
import com.kmpalette.palette.graphics.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App() {
    MaterialTheme {
        var imageUrl by remember { mutableStateOf<String?>(null) }
        var palette by remember { mutableStateOf<Palette?>(null) }
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        val scope = rememberCoroutineScope()
        
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Palette Demo", style = MaterialTheme.typography.headlineMedium)
            
            Button(onClick = {
                scope.launch {
                    val pair = pickImage()
                    if (pair != null) {
                        imageUrl = pair.first
                        
                        // Generate palette directly from bytes
                        try {
                            // 1. Load ImageBitmap from bytes
                            val bitmap = ByteArrayLoader.load(pair.second)
                            imageBitmap = bitmap
                            
                            // 2. Generate Palette from bitmap
                            palette = Palette.from(bitmap).generate()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }) {
                Text("Pick Image")
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = "Selected Image",
                    modifier = Modifier.height(300.dp).fillMaxWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            
            palette?.let { pal ->
                // Filter out null swatches
                val swatches = listOfNotNull(
                    pal.vibrantSwatch?.let { it to "Vibrant" },
                    pal.darkVibrantSwatch?.let { it to "Dark Vibrant" },
                    pal.lightVibrantSwatch?.let { it to "Light Vibrant" },
                    pal.mutedSwatch?.let { it to "Muted" },
                    pal.darkMutedSwatch?.let { it to "Dark Muted" },
                    pal.lightMutedSwatch?.let { it to "Light Muted" },
                    pal.dominantSwatch?.let { it to "Dominant" }
                )

                if (swatches.isEmpty()) {
                    Text("No swatches extracted.")
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            val chunks = swatches.chunked(3)
                            chunks.forEach { chunk ->
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    chunk.forEach { (swatch, name) ->
                                        SwatchBox(swatch, name)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwatchBox(swatch: com.kmpalette.palette.graphics.Palette.Swatch?, name: String) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    
    if (swatch != null) {
        val hexColor = "#" + (swatch.rgb.toUInt().toString(16).takeLast(6).uppercase())
        var showCopied by remember { mutableStateOf(false) }
        
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 100.dp)
                .background(Color(swatch.rgb))
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(hexColor))
                    scope.launch {
                        showCopied = true
                        delay(3000)
                        showCopied = false
                    }
                }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = name,
                    color = Color(swatch.titleTextColor),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                
                val text = if (showCopied) "Copied!" else hexColor
                val fontWeight = if (showCopied) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = text,
                        color = Color(swatch.bodyTextColor),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = fontWeight
                    )
                    if (!showCopied) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color(swatch.bodyTextColor),
                            modifier = Modifier.size(32.dp) // Increased size
                        )
                    } else {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Copied",
                            tint = Color(swatch.bodyTextColor),
                            modifier = Modifier.size(32.dp) // Increased size
                        )
                    }
                }
            }
        }
    }
}
