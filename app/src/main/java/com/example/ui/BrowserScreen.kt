package com.example.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.BrowserTab
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.Screen
import com.example.viewmodel.WarningType
import coil.compose.AsyncImage
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val isPrivate by viewModel.isPrivateMode.collectAsState()
    val tabsList by viewModel.tabs.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val isBookmarked by viewModel.isCurrentPageBookmarked().collectAsState(initial = false)
    val warningInfo by viewModel.warningInfo.collectAsState()
    val areBarsVisible by viewModel.areBarsVisible.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val addressFocusRequester = remember { FocusRequester() }

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }
    val isDark = isPrivate || isDarkTheme

    // Color definitions based on mode (Normal vs Private)
    val bgPrimary = if (isDark) Color(0xFF151517) else Color(0xFFF2F2F7)
    val toolbarBg = if (isDark) Color(0xDD1C1C1E) else Color(0xDDF8F8F8)
    val cardBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)
    val inputBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE3E3E9)

    // Request address bar focus when searching gets toggled
    LaunchedEffect(isSearching) {
        if (isSearching) {
            addressFocusRequester.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(bgPrimary)) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Top Minimal Bar: Displays domain security status and a Reload button
            AnimatedVisibility(
                visible = areBarsVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                MinimalTopBar(
                    activeTab = activeTab,
                    isPrivate = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onReload = { viewModel.reload() },
                    onSettings = { viewModel.currentScreen.value = Screen.Settings }
                )
            }

            // 2. Main WebView Container (Scrollable viewport)
            val isStartPage = activeTab?.url == "about:blank" || activeTab?.url?.isBlank() == true
            Box(
                modifier = if (isStartPage) {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .shadow(16.dp, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                        .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                        .background(cardBg)
                } else {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                }
            ) {
                if (activeTab != null) {
                    if (isStartPage) {
                        SafariStartPage(
                            viewModel = viewModel,
                            isPrivate = isDark,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        BrowserWebView(
                            tab = activeTab!!,
                            viewModel = viewModel,
                            onScrollChanged = { scrollY, oldScrollY ->
                                val delta = scrollY - oldScrollY
                                if (scrollY <= 5) {
                                    viewModel.areBarsVisible.value = true
                                } else if (delta > 15) {
                                    viewModel.areBarsVisible.value = false
                                } else if (delta < -15) {
                                    viewModel.areBarsVisible.value = true
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Empty state helper
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF007AFF))
                    }
                }

                // 3. Address Bar suggestions Overlay (shown when typing/searching)
                if (isSearching) {
                    AddressSuggestionsOverlay(
                        suggestions = suggestions,
                        isPrivate = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        bgPrimary = bgPrimary,
                        onSuggestionSelect = { query ->
                            viewModel.urlInput.value = query
                            viewModel.handleUrlSubmit(query)
                            focusManager.clearFocus()
                        }
                    )
                }
            }

            // 4. Combined Bottom Controls (Floating rounded search bar & toolbar row)
            if (!isStartPage || isSearching) {
                AnimatedVisibility(
                    visible = areBarsVisible || isSearching,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        color = toolbarBg,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 8.dp)
                    ) {
                    // Floating Address Bar row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Floating pill address bar capsule with shadow, translucency, and border
                        val addressBarBg = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
                        val addressBarBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(26.dp))
                                .background(addressBarBg, RoundedCornerShape(26.dp))
                                .border(1.dp, addressBarBorder, RoundedCornerShape(26.dp))
                                .clickable { viewModel.isSearching.value = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Search else (if (activeTab?.url?.startsWith("https://") == true) Icons.Default.Lock else Icons.Default.Search),
                                contentDescription = "Search Status",
                                tint = if (!isSearching && activeTab?.url?.startsWith("https://") == true) Color(0xFF34C759) else textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            if (isSearching) {
                                // Real Input text field when active
                                TextField(
                                    value = urlInput,
                                    onValueChange = { viewModel.urlInput.value = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(addressFocusRequester)
                                        .testTag("address_text_input"),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    ),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                                    placeholder = { Text("Search or enter website", color = textSecondary, fontSize = 15.sp) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Go
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onGo = {
                                            viewModel.handleUrlSubmit(urlInput)
                                            focusManager.clearFocus()
                                        }
                                    ),
                                    singleLine = true,
                                )
                            } else {
                                // Styled read-only text container mirroring iOS design when inactive
                                Text(
                                    text = if (activeTab?.url == "about:blank" || activeTab?.url?.isBlank() == true) "Search or enter website" else getShortDisplayDomain(activeTab?.url ?: ""),
                                    fontSize = 15.sp,
                                    color = if (activeTab?.url == "about:blank" || activeTab?.url?.isBlank() == true) textSecondary else textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (isSearching && urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.urlInput.value = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Clear", tint = textSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Close/Cancel button next to focused Search Bar
                        if (isSearching) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cancel",
                                color = Color(0xFF007AFF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.isSearching.value = false
                                        viewModel.urlInput.value = activeTab?.url ?: ""
                                        focusManager.clearFocus()
                                    }
                                    .testTag("search_cancel_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Action Toolbar Row (Back, Forward, Share, Bookmarks, Tab Switcher)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button
                        IconButton(
                            onClick = { viewModel.goBack() },
                            enabled = activeTab?.canGoBack == true,
                            modifier = Modifier.testTag("toolbar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = if (activeTab?.canGoBack == true) Color(0xFF007AFF) else textSecondary.copy(alpha = 0.4f)
                            )
                        }

                        // Forward Button
                        IconButton(
                            onClick = { viewModel.goForward() },
                            enabled = activeTab?.canGoForward == true,
                            modifier = Modifier.testTag("toolbar_forward_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (activeTab?.canGoForward == true) Color(0xFF007AFF) else textSecondary.copy(alpha = 0.4f)
                            )
                        }

                        // Share Page Button
                        IconButton(
                            onClick = {
                                activeTab?.url?.let { shareUrl ->
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareUrl)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share URL")
                                    context.startActivity(shareIntent)
                                }
                            },
                            enabled = activeTab?.url != "about:blank" && activeTab?.url?.isNotBlank() == true,
                            modifier = Modifier.testTag("toolbar_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = if (activeTab?.url != "about:blank" && activeTab?.url?.isNotBlank() == true) Color(0xFF007AFF) else textSecondary.copy(alpha = 0.4f)
                            )
                        }

                        // Bookmarks Toggle button
                        IconButton(
                            onClick = { viewModel.toggleCurrentPageBookmark() },
                            enabled = activeTab?.url != "about:blank" && activeTab?.url?.isNotBlank() == true,
                            modifier = Modifier.testTag("toolbar_bookmark_toggle")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (activeTab?.url != "about:blank" && activeTab?.url?.isNotBlank() == true) Color(0xFF007AFF) else textSecondary.copy(alpha = 0.4f)
                            )
                        }

                        // Library / Bookmarks list
                        IconButton(
                            onClick = { viewModel.currentScreen.value = Screen.BookmarksHistory },
                            modifier = Modifier.testTag("toolbar_library_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Book,
                                contentDescription = "Library",
                                tint = Color(0xFF007AFF)
                            )
                        }

                        // Tabs Switcher Button with overlapping iOS square count style badge
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { viewModel.currentScreen.value = Screen.TabSwitcher }
                                .testTag("toolbar_tabs_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(1.8.dp, Color(0xFF007AFF), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabsList.size.toString(),
                                    color = Color(0xFF007AFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    // 5. Fraudulent / Non-HTTPS Website custom warning alert (Safe Browsing)
    if (warningInfo.type != WarningType.NONE) {
        SafeBrowsingAlert(
            warningInfo = warningInfo,
            isPrivate = isDark,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onDismiss = { viewModel.warningInfo.value = com.example.viewmodel.WarningInfo() }
        )
    }
}
}

@Composable
fun MinimalTopBar(
    activeTab: BrowserTab?,
    isPrivate: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onReload: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Settings button
        IconButton(
            onClick = onSettings,
            modifier = Modifier.size(36.dp).testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Browser Settings",
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Center: The Sleek Domain Bar Capsule
        val domain = getShortDisplayDomain(activeTab?.url ?: "")
        val hasSecure = activeTab?.url?.startsWith("https://") == true
        val capsuleBg = if (isPrivate) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.5f)
        val capsuleBorder = if (isPrivate) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(capsuleBg)
                .border(1.dp, capsuleBorder, CircleShape)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (hasSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Security Status",
                tint = if (hasSecure) Color(0xFF34C759) else textSecondary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (domain.isBlank() || domain == "about:blank") "surfora" else domain,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary.copy(alpha = 0.8f)
            )
        }

        // Right: Reload button
        IconButton(
            onClick = onReload,
            modifier = Modifier.size(36.dp).testTag("reload_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reload Page",
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AddressSuggestionsOverlay(
    suggestions: List<String>,
    isPrivate: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    bgPrimary: Color,
    onSuggestionSelect: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgPrimary.copy(alpha = 0.95f))
    ) {
        if (suggestions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Search suggestion results will appear as you type", fontSize = 14.sp, color = textSecondary, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(suggestions) { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSuggestionSelect(suggestion) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = suggestion,
                            fontSize = 15.sp,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider(color = textSecondary.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
fun SafeBrowsingAlert(
    warningInfo: com.example.viewmodel.WarningInfo,
    isPrivate: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onDismiss: () -> Unit
) {
    val isMalicious = warningInfo.type == WarningType.MALICIOUS
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = if (isMalicious) Color(0xFFFF3B30) else Color(0xFFFFCC00),
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = if (isMalicious) "Suspicious Website Blocked" else "Unsecured Connection Warning",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isMalicious) {
                        "Surfora light blocked this page because it matches a known malicious domain lists. Going further may expose your device to malware or phishing threats."
                    } else {
                        "The link you are trying to open uses HTTP. Your connection is unencrypted, meaning your private data (passwords, credit cards) could be intercepted by third parties."
                    },
                    fontSize = 14.sp,
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = warningInfo.url,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { warningInfo.onConfirm() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMalicious) Color(0xFFFF3B30) else Color(0xFFFFCC00),
                    contentColor = if (isMalicious) Color.White else Color.Black
                ),
                modifier = Modifier.testTag("safebrowsing_proceed_button")
            ) {
                Text("Proceed Anyway", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("safebrowsing_back_button")
            ) {
                Text("Go Back (Safe)", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SafariStartPage(
    viewModel: BrowserViewModel,
    isPrivate: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    // Normal Mode: soft peach, soft pink, soft purple, soft blue
    // Private Mode: dark slate/indigo/purple
    val startPageGradientColors = if (isPrivate) {
        listOf(Color(0xFF1E1428), Color(0xFF111726), Color(0xFF1A151F))
    } else {
        listOf(
            Color(0xFFFBE5E5), // Soft peach/coral
            Color(0xFFFFF0E6), // Pastel orange
            Color(0xFFEBE3F5), // Soft purple
            Color(0xFFE1F5FE)  // Pastel light blue
        )
    }

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(startPageGradientColors))
    ) {
        // Main content scrollable Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 90.dp) // Leave space for bottom bar
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Favorites Header
            Text(
                text = "Favorites",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Favorites Grid
            val favorites = listOf(
                Triple("Google", "https://www.google.com", "G"),
                Triple("Wikipedia", "https://www.wikipedia.org", "W"),
                Triple("YouTube", "https://www.youtube.com", "Y"),
                Triple("9to5Mac", "https://9to5mac.com", "9"),
                Triple("Surfora", "https://surfora.com", "S")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                favorites.forEach { fav ->
                    var isSuccess by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.urlInput.value = fav.second
                                viewModel.handleUrlSubmit(fav.second)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .shadow(2.dp, RoundedCornerShape(14.dp))
                                .background(
                                    if (isPrivate) Color(0xFF2C2C2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.7f),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isPrivate) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.6f),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isSuccess) {
                                Text(
                                    text = fav.third,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPrivate) Color.White else Color(0xFF037EEB)
                                )
                            }
                            AsyncImage(
                                model = "https://www.google.com/s2/favicons?sz=128&domain=${getShortDisplayDomain(fav.second)}",
                                contentDescription = fav.first,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                onState = { state ->
                                    isSuccess = state is coil.compose.AsyncImagePainter.State.Success
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = fav.first,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Privacy Report Card
            Text(
                text = "Privacy Report",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .background(
                        if (isPrivate) Color(0xFF2C2C2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.7f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        if (isPrivate) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.6f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Shield Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isPrivate) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield",
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "135 Trackers",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "In the last seven days, Surfora has prevented 135 trackers from profiling you.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Reading List Section
            Text(
                text = "Reading List",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            val readingListItems = listOf(
                Pair("Physicists Reveal a Quantum Geometry That Exists Outside Space", "quantamagazine.org"),
                Pair("India Trip - Group Travel | Atlas Obscura Adventures", "atlasobscura.com")
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                readingListItems.forEachIndexed { idx, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(16.dp))
                            .background(
                                if (isPrivate) Color(0xFF2C2C2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.7f),
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                if (isPrivate) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.6f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                val url = "https://" + item.second
                                viewModel.urlInput.value = url
                                viewModel.handleUrlSubmit(url)
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Circular badge on the left
                            val letter = if (idx == 0) "P" else "I"
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isPrivate) Color(0xFF151517) else Color(0xFFE5E5EA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.first,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.second,
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. FLOATING BOTTOM BAR ONLY ON START PAGE (Image 4 Styling)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Arrow Button (translucent coral-pink circular button)
                val backEnabled = viewModel.activeTab.value?.canGoBack == true
                val buttonBgColor = if (isPrivate) Color(0xFFFFCCDD).copy(alpha = 0.15f) else Color(0xFFFFB3D1).copy(alpha = 0.45f)
                
                IconButton(
                    onClick = { viewModel.goBack() },
                    enabled = backEnabled,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (backEnabled) buttonBgColor else buttonBgColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (backEnabled) Color(0xFF007AFF) else textSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Middle: Custom search capsule styled with a white translucent background
                val capsuleBorder = if (isPrivate) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .height(48.dp)
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                        .background(
                            if (isPrivate) Color(0xFF2C2C2E).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f),
                            RoundedCornerShape(24.dp)
                        )
                        .border(1.dp, capsuleBorder, RoundedCornerShape(24.dp))
                        .clickable { viewModel.isSearching.value = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Search or enter website",
                        fontSize = 13.sp,
                        color = textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Right: More Options Button (translucent coral-pink circular button)
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(buttonBgColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More Options",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Dropdown options matching more Options in image 4
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Library (Bookmarks/History)") },
                            onClick = {
                                showMenu = false
                                viewModel.currentScreen.value = Screen.BookmarksHistory
                            },
                            leadingIcon = { Icon(Icons.Outlined.Book, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Tabs Switcher") },
                            onClick = {
                                showMenu = false
                                viewModel.currentScreen.value = Screen.TabSwitcher
                            },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Browser Settings") },
                            onClick = {
                                showMenu = false
                                viewModel.currentScreen.value = Screen.Settings
                            },
                            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}
