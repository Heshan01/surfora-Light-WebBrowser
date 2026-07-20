package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BookmarksHistoryScreen
import com.example.ui.BrowserScreen
import com.example.ui.SettingsScreen
import com.example.ui.TabSwitcherScreen
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BrowserViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemDark
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val activeTab by viewModel.activeTab.collectAsState()

                // Handle Android Native System Back Button Press
                BackHandler(enabled = true) {
                    when {
                        currentScreen != Screen.Browser -> {
                            // If on Settings, Tab Switcher, or Bookmarks, return to main browser viewport
                            viewModel.currentScreen.value = Screen.Browser
                        }
                        activeTab?.canGoBack == true -> {
                            // If inside the webview we can go back, trigger WebView backward navigation
                            viewModel.goBack()
                        }
                        else -> {
                            // Fallback to closing/minimizing activity
                            finish()
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    // Modern slide/fade transitions when navigating screens
                    Crossfade(
                        targetState = currentScreen,
                        label = "ScreenTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { screen ->
                        when (screen) {
                            Screen.Browser -> {
                                BrowserScreen(viewModel = viewModel)
                            }
                            Screen.TabSwitcher -> {
                                TabSwitcherScreen(viewModel = viewModel)
                            }
                            Screen.BookmarksHistory -> {
                                BookmarksHistoryScreen(viewModel = viewModel)
                            }
                            Screen.Settings -> {
                                SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
