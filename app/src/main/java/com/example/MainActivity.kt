package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.mediaflow.core.security.UrlSanitizer
import com.example.mediaflow.domain.model.ThemeMode
import com.example.mediaflow.presentation.navigation.MediaFlowNavGraph
import com.example.mediaflow.presentation.navigation.Screen
import com.example.ui.theme.MediaFlowTheme

class MainActivity : ComponentActivity() {

    private var pendingSharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleShareIntent(intent)

        val app = application as MediaFlowApplication
        val container = app.container

        setContent {
            val settings by container.settingsRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.example.mediaflow.domain.model.AppSettings()
            )

            val isDark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MediaFlowTheme(darkTheme = isDark) {
                val navController = rememberNavController()

                LaunchedEffect(pendingSharedUrl) {
                    val url = pendingSharedUrl
                    if (url != null) {
                        navController.navigate(Screen.createAnalyzerRoute(url))
                        pendingSharedUrl = null
                    }
                }

                MediaFlowNavGraph(
                    navController = navController,
                    container = container
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            // Detect URL inside shared text (some share sheets include title + link)
            val extractedUrl = extractUrlFromText(text)
            if (extractedUrl != null && UrlSanitizer.isSupportedUrl(extractedUrl)) {
                pendingSharedUrl = extractedUrl
            }
        }
    }

    private fun extractUrlFromText(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            val words = trimmed.split("\\s+".toRegex())
            return words.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
        }
        val words = trimmed.split("\\s+".toRegex())
        return words.firstOrNull { it.contains("youtube.com") || it.contains("youtu.be") || it.contains("instagram.com") }
    }
}
