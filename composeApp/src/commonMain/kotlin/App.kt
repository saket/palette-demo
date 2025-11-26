package palette.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.kmpalette.loader.ByteArrayLoader
import com.kmpalette.palette.graphics.Palette
import kotlinx.coroutines.launch

@Composable
fun App() {
    MaterialTheme {
        var imageUrl by remember { mutableStateOf<String?>(null) }
        var palette by remember { mutableStateOf<Palette?>(null) }
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

            if (imageUrl != null) {
                val painter = rememberAsyncImagePainter(imageUrl!!)

                Image(
                    painter = painter,
                    contentDescription = "Selected Image",
                    modifier = Modifier.height(300.dp)
                )
            }
            
            palette?.let { pal ->
                Text("Extracted Colors:", style = MaterialTheme.typography.titleMedium)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwatchBox(pal.vibrantSwatch, "Vibrant")
                    SwatchBox(pal.darkVibrantSwatch, "Dark Vibrant")
                    SwatchBox(pal.lightVibrantSwatch, "Light Vibrant")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SwatchBox(pal.mutedSwatch, "Muted")
                    SwatchBox(pal.darkMutedSwatch, "Dark Muted")
                    SwatchBox(pal.lightMutedSwatch, "Light Muted")
                }
            }
        }
    }
}

@Composable
fun SwatchBox(swatch: com.kmpalette.palette.graphics.Palette.Swatch?, name: String) {
    if (swatch != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(swatch.rgb))
            )
            Text(name, style = MaterialTheme.typography.bodySmall)
        }
    }
}
