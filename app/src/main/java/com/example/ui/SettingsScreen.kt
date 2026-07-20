package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.Screen

import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val isPrivate = viewModel.isPrivateMode.collectAsState().value
    val searchEngine by viewModel.searchEngine.collectAsState()
    val safeBrowsingEnabled by viewModel.safeBrowsingEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }
    val isDark = isPrivate || isDarkTheme

    val bgPrimary = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val cardBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)

    var showEnginePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Safari Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                IconButton(
                    onClick = { viewModel.currentScreen.value = Screen.Browser },
                    modifier = Modifier.testTag("settings_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                }
            }
        },
        containerColor = bgPrimary,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Search Engine Group
            Text(
                "SEARCH ENGINE",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEnginePicker = true }
                        .padding(16.dp)
                        .testTag("search_engine_setting_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Search Engine", color = textPrimary, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(searchEngine, color = textSecondary, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = textSecondary)
                    }
                }
            }

            // Section 1.5: Appearance Group
            Text(
                "APPEARANCE",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemePicker = true }
                        .padding(16.dp)
                        .testTag("appearance_setting_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = Color(0xFF5856D6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Theme", color = textPrimary, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val displayTheme = when (themeMode) {
                            "light" -> "Light"
                            "dark" -> "Dark"
                            else -> "System Default"
                        }
                        Text(displayTheme, color = textSecondary, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = textSecondary)
                    }
                }
            }

            // Section 2: Privacy and Security
            Text(
                "PRIVACY & SECURITY",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Safe Browsing Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Fraudulent Website Warning", color = textPrimary, fontWeight = FontWeight.Medium)
                                Text("Check for unsecure or malicious sites", color = textSecondary, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = safeBrowsingEnabled,
                            onCheckedChange = { viewModel.setSafeBrowsing(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF34C759)
                            ),
                            modifier = Modifier.testTag("safe_browsing_switch")
                        )
                    }

                    HorizontalDivider(color = textSecondary.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    // Clear Cache & Cookies Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.clearCacheAndCookies() }
                            .padding(16.dp)
                            .testTag("clear_data_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Clear History and Website Data", color = Color(0xFFFF3B30), fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = textSecondary)
                    }
                }
            }

            // Section 3: About Surfora Light
            Text(
                "ABOUT",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Application Name", color = textPrimary, fontWeight = FontWeight.Medium)
                        Text("Surfora light", color = textSecondary)
                    }
                    HorizontalDivider(color = textSecondary.copy(alpha = 0.1f), thickness = 0.5.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", color = textPrimary, fontWeight = FontWeight.Medium)
                        Text("1.0.0 (iOS 26 Theme)", color = textSecondary)
                    }
                    HorizontalDivider(color = textSecondary.copy(alpha = 0.1f), thickness = 0.5.dp)
                    Text(
                        "Surfora light is built on Kotlin & Jetpack Compose, delivering a lightweight, secure browsing environment with a sleek modern design.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Search Engine Picker Dialog
    if (showEnginePicker) {
        AlertDialog(
            onDismissRequest = { showEnginePicker = false },
            title = { Text("Select Default Search Engine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Google", "DuckDuckGo", "Bing").forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSearchEngine(engine)
                                    showEnginePicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(engine, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                            if (searchEngine == engine) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF007AFF))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEnginePicker = false }) {
                    Text("Cancel", color = Color(0xFF007AFF))
                }
            }
        )
    }

    // Theme Picker Dialog
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("Select Appearance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        "system" to "System Default",
                        "light" to "Light",
                        "dark" to "Dark"
                    ).forEach { (mode, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemePicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                            if (themeMode == mode) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF007AFF))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) {
                    Text("Cancel", color = Color(0xFF007AFF))
                }
            }
        )
    }
}
