package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.entity.Bookmark
import com.example.data.entity.HistoryEntry
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarksHistoryScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val isPrivate = viewModel.isPrivateMode.collectAsState().value
    val themeMode by viewModel.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }
    val isDark = isPrivate || isDarkTheme

    var selectedTabIdx by remember { mutableIntStateOf(0) } // 0 = Bookmarks, 1 = History

    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()

    val bgPrimary = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val cardBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Library",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    IconButton(
                        onClick = { viewModel.currentScreen.value = Screen.Browser },
                        modifier = Modifier.testTag("library_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom iOS-style segmented Tab Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE3E3E9))
                        .padding(2.dp)
                ) {
                    // Bookmarks Tab Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (selectedTabIdx == 0) {
                                    if (isDark) Color(0xFF636366) else Color.White
                                } else Color.Transparent
                            )
                            .clickable { selectedTabIdx = 0 }
                            .testTag("bookmarks_tab_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Book,
                                contentDescription = null,
                                tint = if (selectedTabIdx == 0) textPrimary else textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Bookmarks",
                                color = if (selectedTabIdx == 0) textPrimary else textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // History Tab Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (selectedTabIdx == 1) {
                                    if (isDark) Color(0xFF636366) else Color.White
                                } else Color.Transparent
                            )
                            .clickable { selectedTabIdx = 1 }
                            .testTag("history_tab_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = if (selectedTabIdx == 1) textPrimary else textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "History",
                                color = if (selectedTabIdx == 1) textPrimary else textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        containerColor = bgPrimary,
        bottomBar = {
            if (selectedTabIdx == 1 && history.isNotEmpty()) {
                // Clear History Option on bottom of History tab
                Surface(
                    color = cardBg,
                    tonalElevation = 1.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.clearHistory() }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Browsing History", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTabIdx == 0) {
                // Bookmarks list
                if (bookmarks.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Book,
                        title = "No Bookmarks Yet",
                        description = "Tap the share or menu button while browsing to bookmark your favorite pages.",
                        color = textSecondary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bookmarks, key = { it.id }) { bookmark ->
                            BookmarkItem(
                                bookmark = bookmark,
                                cardBg = cardBg,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                onClick = {
                                    viewModel.loadUrl(bookmark.url)
                                    viewModel.currentScreen.value = Screen.Browser
                                },
                                onDelete = { viewModel.deleteBookmark(bookmark) }
                            )
                        }
                    }
                }
            } else {
                // History list
                if (history.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.History,
                        title = "No History",
                        description = "Pages you visit in normal tabs will be saved here.",
                        color = textSecondary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(history, key = { it.id }) { entry ->
                            HistoryItem(
                                entry = entry,
                                cardBg = cardBg,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                onClick = {
                                    viewModel.loadUrl(entry.url)
                                    viewModel.currentScreen.value = Screen.Browser
                                },
                                onDelete = { viewModel.deleteHistoryEntry(entry.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bookmark.url,
                    fontSize = 13.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Bookmark",
                    tint = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryItem(
    entry: HistoryEntry,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedTime = remember(entry.timestamp) { formatter.format(Date(entry.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.url,
                    fontSize = 13.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Light
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete History Entry",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 14.sp,
            color = color.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
