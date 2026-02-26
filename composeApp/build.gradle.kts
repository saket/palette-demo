import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js(IR) {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static(project.rootDir.path)
                }
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        val jsMain by getting {
            dependencies {
                // Compose HTML for DOM rendering
                implementation(libs.compose.html.core)
                
                // Compose UI only for ImageBitmap (used by kmpalette)
                implementation(libs.compose.ui)
                
                // Coroutines
                implementation(libs.kotlin.coroutines.core)
                
                // kmpalette
                implementation(libs.kmpalette.core)
            }
        }
    }
}
