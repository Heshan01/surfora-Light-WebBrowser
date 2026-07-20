package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.viewmodel.BrowserTab
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.Screen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabSwitcherScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val isPrivate by viewModel.isPrivateMode.collectAsState()
    val tabList by viewModel.tabs.collectAsState()
    val activeIndex by viewModel.activeTabIndex.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }
    val isDark = isPrivate || isDarkTheme

    val bgPrimary = if (isDark) Color(0xFF121212) else Color(0xFFF2F2F7)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)
    val activeBorderColor = if (isDark) Color(0xFF007AFF) else Color(0xFF007AFF)

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
                // Left: Close All Button
                TextButton(
                    onClick = {
                        val tabsToClose = tabList.toList()
                        tabsToClose.forEach { viewModel.closeTab(it.id) }
                    },
                    modifier = Modifier.testTag("close_all_tabs_button")
                ) {
                    Text(
                        text = "Close All",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Red
                    )
                }

                // Center: Title
                Text(
                    text = if (isPrivate) "Private" else "Tabs",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.testTag("tab_switcher_title")
                )
                
                // Right: Balanced placeholder
                Spacer(modifier = Modifier.width(60.dp))
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgPrimary)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = textSecondary.copy(alpha = 0.15f), thickness = 0.5.dp)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: "+" Add New Tab (circular grey button)
                    IconButton(
                        onClick = { viewModel.addNewTab() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                            .testTag("add_tab_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add New Tab", tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                    }

                    // Middle: iOS style sliding private / tab capsule picker (Private on left, X Tabs on right!)
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE3E3E9))
                            .padding(2.dp)
                    ) {
                        // Private Mode Button (Left)
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isPrivate) (if (isDark) Color(0xFF636366) else Color.White) else Color.Transparent)
                                .clickable { viewModel.togglePrivateMode(true) }
                                .padding(horizontal = 14.dp)
                                .testTag("private_tabs_capsule"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isPrivate) Color.White else textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Private",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPrivate) Color.White else textSecondary
                            )
                        }

                        // Normal Tabs Mode Button (Right)
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (!isPrivate) (if (isDark) Color(0xFF636366) else Color.White) else Color.Transparent)
                                .clickable { viewModel.togglePrivateMode(false) }
                                .padding(horizontal = 14.dp)
                                .testTag("normal_tabs_capsule"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${tabList.size} Tabs",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (!isPrivate) (if (isDark) Color.White else Color.Black) else textSecondary
                            )
                        }
                    }

                    // Right: Blue circular Done button with white checkmark (✓)
                    IconButton(
                        onClick = { viewModel.currentScreen.value = Screen.Browser },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF))
                            .testTag("tab_switcher_done_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = bgPrimary,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (tabList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(
                        imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Public,
                        contentDescription = null,
                        tint = textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isPrivate) "No Private Tabs Open" else "No Tabs Open",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to open a new tab and start browsing.",
                        color = textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(tabList, key = { _, tab -> tab.id }) { index, tab ->
                    val isActive = index == activeIndex
                    TabGridCard(
                        tab = tab,
                        isActive = isActive,
                        isPrivate = isDark,
                        activeBorderColor = activeBorderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onSelect = { viewModel.selectTab(index) },
                        onClose = { viewModel.closeTab(tab.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TabGridCard(
    tab: BrowserTab,
    isActive: Boolean,
    isPrivate: Boolean,
    activeBorderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val cardBg = if (isPrivate) Color(0xFF1C1C1E) else Color.White
    val borderModifier = if (isActive) {
        Modifier.border(2.dp, activeBorderColor, RoundedCornerShape(18.dp))
    } else {
        Modifier.border(0.5.dp, textSecondary.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        // Highly rounded visual preview card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .then(borderModifier)
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .clickable(onClick = onSelect)
        ) {
            // Simulated web page preview or nice representation
            val gradientColors = if (isPrivate) {
                listOf(Color(0xFF2C2C2E), Color(0xFF151517))
            } else {
                listOf(Color(0xFFF2F2F7), Color.White)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(gradientColors))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (tab.url.startsWith("https://")) Icons.Default.Lock else Icons.Default.Public,
                        contentDescription = null,
                        tint = if (isPrivate) Color(0xFF007AFF).copy(alpha = 0.5f) else Color(0xFF007AFF).copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (tab.title.isBlank() || tab.title == "New Tab") "Blank Page" else tab.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Circular close button (x) in the top-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isPrivate) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f))
                        .clickable { onClose() }
                        .testTag("close_tab_${tab.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tab",
                        tint = if (isPrivate) Color.White else Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title and favicon underneath the card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val domain = getShortDisplayDomain(tab.url)
            val firstChar = if (domain.isNotEmpty() && domain != "Blank Page") domain.first().uppercaseChar().toString() else "S"
            
            // Circular badge with favicon / single letter
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isPrivate) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = firstChar,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = if (domain == "Blank Page") "surfora.com" else domain,
                fontSize = 11.sp,
                color = textPrimary.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Utility to extract host for tab preview cards
fun getShortDisplayDomain(urlSpec: String): String {
    if (urlSpec.isBlank() || urlSpec == "about:blank") return "Blank Page"
    return try {
        val uri = android.net.Uri.parse(urlSpec)
        uri.host ?: urlSpec
    } catch (e: Exception) {
        urlSpec
    }
}
