package com.fazza.pdfeditor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.fazza.pdfeditor.ui.theme.FazzaPDFEditorTheme
import com.fazza.pdfeditor.utils.FileUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FazzaPDFEditorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    // Handle incoming PDF intents
                    val incomingPath = intent?.let { resolveIncomingIntent(it) }

                    FazzaNavGraph(
                        navController = navController,
                        startFilePath = incomingPath
                    )
                }
            }
        }
    }

    private fun resolveIncomingIntent(intent: Intent): String? {
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    FileUtils.getFilePathFromUri(this, uri)
                }
            }
            else -> null
        }
    }
}
