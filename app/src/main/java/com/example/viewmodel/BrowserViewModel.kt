package com.example.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.BrowserDatabase
import com.example.data.entity.Bookmark
import com.example.data.entity.HistoryEntry
import com.example.data.repository.BrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "about:blank",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isPrivate: Boolean = false,
    var webView: WebView? = null
)

enum class Screen {
    Browser,
    TabSwitcher,
    BookmarksHistory,
    Settings
}

enum class WarningType {
    NONE,
    HTTP,
    MALICIOUS
}

data class WarningInfo(
    val type: WarningType = WarningType.NONE,
    val url: String = "",
    val tabId: String = "",
    val onConfirm: () -> Unit = {}
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BrowserDatabase.getDatabase(application)
    private val repository = BrowserRepository(database.browserDao())

    // Tabs management
    private val _normalTabs = MutableStateFlow<List<BrowserTab>>(listOf(BrowserTab(url = "about:blank")))
    private val _privateTabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    
    private val _activeNormalTabIndex = MutableStateFlow(0)
    private val _activePrivateTabIndex = MutableStateFlow(0)

    val isPrivateMode = MutableStateFlow(false)

    // Current navigation screen
    val currentScreen = MutableStateFlow(Screen.Browser)

    // Settings (using SharedPreferences)
    private val sharedPrefs = application.getSharedPreferences("surfora_settings", Context.MODE_PRIVATE)
    val searchEngine = MutableStateFlow(sharedPrefs.getString("search_engine", "Google") ?: "Google")
    val safeBrowsingEnabled = MutableStateFlow(sharedPrefs.getBoolean("safe_browsing", true))
    val themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "system") ?: "system")

    // UI Input state
    val urlInput = MutableStateFlow("")
    val isSearching = MutableStateFlow(false)
    val areBarsVisible = MutableStateFlow(true)
    
    // Search suggestions
    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    // Safe Browsing warnings state
    val warningInfo = MutableStateFlow(WarningInfo())

    // Known malicious domains list for local Safe Browsing verification
    private val maliciousDomains = setOf(
        "scam-site.com",
        "phishing-test.com",
        "malware-download.xyz",
        "unsafe-browser.net",
        "test-malicious.com"
    )

    // Observe Bookmarks and History
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntry>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined/Filtered active tabs
    val tabs: StateFlow<List<BrowserTab>> = combine(isPrivateMode, _normalTabs, _privateTabs) { privateMode, normal, privateList ->
        if (privateMode) privateList else normal
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _normalTabs.value)

    val activeTabIndex: StateFlow<Int> = combine(isPrivateMode, _activeNormalTabIndex, _activePrivateTabIndex) { privateMode, normalIdx, privateIdx ->
        if (privateMode) privateIdx else normalIdx
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeTab: StateFlow<BrowserTab?> = combine(tabs, activeTabIndex) { tabList, index ->
        if (tabList.isNotEmpty() && index in tabList.indices) tabList[index] else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _normalTabs.value.firstOrNull())

    init {
        // Observe url input changes to update suggestions with a simple debounce
        viewModelScope.launch {
            urlInput
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isNotBlank() && isSearching.value && !query.startsWith("http://") && !query.startsWith("https://")) {
                        fetchSuggestions(query)
                    } else {
                        _searchSuggestions.value = emptyList()
                    }
                }
        }
    }

    // Toggle private/incognito mode
    fun togglePrivateMode(active: Boolean) {
        isPrivateMode.value = active
        val currentTabs = if (active) _privateTabs.value else _normalTabs.value
        if (currentTabs.isEmpty()) {
            addNewTab("about:blank")
        } else {
            // Update URL input for the selected tab
            val idx = if (active) _activePrivateTabIndex.value else _activeNormalTabIndex.value
            if (idx in currentTabs.indices) {
                urlInput.value = currentTabs[idx].url
            }
        }
    }

    // Manage search engines
    fun setSearchEngine(engine: String) {
        searchEngine.value = engine
        sharedPrefs.edit().putString("search_engine", engine).apply()
    }

    fun setSafeBrowsing(enabled: Boolean) {
        safeBrowsingEnabled.value = enabled
        sharedPrefs.edit().putBoolean("safe_browsing", enabled).apply()
    }

    fun setThemeMode(mode: String) {
        themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    // Tabs Actions
    fun addNewTab(initialUrl: String = "about:blank") {
        areBarsVisible.value = true
        val newTab = BrowserTab(
            url = initialUrl,
            isPrivate = isPrivateMode.value
        )
        if (isPrivateMode.value) {
            val updated = _privateTabs.value + newTab
            _privateTabs.value = updated
            _activePrivateTabIndex.value = updated.size - 1
        } else {
            val updated = _normalTabs.value + newTab
            _normalTabs.value = updated
            _activeNormalTabIndex.value = updated.size - 1
        }
        urlInput.value = initialUrl
        currentScreen.value = Screen.Browser
    }

    fun closeTab(tabId: String) {
        areBarsVisible.value = true
        val privateModeActive = isPrivateMode.value
        val currentList = if (privateModeActive) _privateTabs.value else _normalTabs.value
        val currentIndex = if (privateModeActive) _activePrivateTabIndex.value else _activeNormalTabIndex.value
        
        val tabToClose = currentList.find { it.id == tabId }
        tabToClose?.webView?.let {
            it.stopLoading()
            it.destroy()
        }

        val updatedList = currentList.filter { it.id != tabId }
        val newIndex = when {
            updatedList.isEmpty() -> 0
            currentIndex >= updatedList.size -> updatedList.size - 1
            else -> currentIndex
        }

        if (privateModeActive) {
            _privateTabs.value = updatedList
            _activePrivateTabIndex.value = newIndex
            if (updatedList.isEmpty()) {
                addNewTab("about:blank")
            } else {
                urlInput.value = updatedList[newIndex].url
            }
        } else {
            _normalTabs.value = updatedList
            _activeNormalTabIndex.value = newIndex
            if (updatedList.isEmpty()) {
                addNewTab("about:blank")
            } else {
                urlInput.value = updatedList[newIndex].url
            }
        }
    }

    fun selectTab(index: Int) {
        areBarsVisible.value = true
        if (isPrivateMode.value) {
            if (index in _privateTabs.value.indices) {
                _activePrivateTabIndex.value = index
                urlInput.value = _privateTabs.value[index].url
            }
        } else {
            if (index in _normalTabs.value.indices) {
                _activeNormalTabIndex.value = index
                urlInput.value = _normalTabs.value[index].url
            }
        }
        currentScreen.value = Screen.Browser
    }

    // Lazy load WebView for each tab
    fun getOrCreateWebView(tabId: String, context: Context): WebView {
        val privateModeActive = isPrivateMode.value
        val currentList = if (privateModeActive) _privateTabs.value else _normalTabs.value
        val tab = currentList.find { it.id == tabId } ?: return WebView(context)

        tab.webView?.let { return it }

        val webView = createNewWebView(context, tabId, privateModeActive)
        val updated = currentList.map {
            if (it.id == tabId) it.copy(webView = webView) else it
        }

        if (privateModeActive) {
            _privateTabs.value = updated
        } else {
            _normalTabs.value = updated
        }

        // Trigger load url if it was defined
        if (tab.url.isNotBlank() && tab.url != "about:blank") {
            webView.loadUrl(tab.url)
        }

        return webView
    }

    private fun createNewWebView(context: Context, tabId: String, isPrivate: Boolean): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                // Set a modern Safari/iOS user agent
                userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
            }

            // Set background color based on theme
            setBackgroundColor(if (isPrivate) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt())

            // Download Listener
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                downloadFile(context, url, userAgent, contentDisposition, mimetype)
            }
        }
    }

    // Load URLs & handle search engines
    fun handleUrlSubmit(input: String) {
        val query = input.trim()
        if (query.isEmpty()) return

        var destinationUrl = ""
        if (query.startsWith("http://") || query.startsWith("https://")) {
            destinationUrl = query
        } else if (query.contains(".") && !query.contains(" ")) {
            destinationUrl = "https://$query"
        } else {
            // Build search engine query URL
            val searchBase = when (searchEngine.value) {
                "DuckDuckGo" -> "https://duckduckgo.com/?q="
                "Bing" -> "https://www.bing.com/search?q="
                else -> "https://www.google.com/search?q="
            }
            try {
                destinationUrl = searchBase + URLEncoder.encode(query, "UTF-8")
            } catch (e: Exception) {
                destinationUrl = searchBase + query
            }
        }

        loadUrl(destinationUrl)
    }

    fun loadUrl(url: String) {
        val privateModeActive = isPrivateMode.value
        val currentList = if (privateModeActive) _privateTabs.value else _normalTabs.value
        val currentIndex = if (privateModeActive) _activePrivateTabIndex.value else _activeNormalTabIndex.value
        
        if (currentList.isEmpty() || currentIndex !in currentList.indices) return
        val activeTab = currentList[currentIndex]

        // Safe browsing: HTTPS verification
        if (safeBrowsingEnabled.value && url.startsWith("http://")) {
            warningInfo.value = WarningInfo(
                type = WarningType.HTTP,
                url = url,
                tabId = activeTab.id,
                onConfirm = {
                    proceedWithUrlLoad(activeTab.id, url)
                    warningInfo.value = WarningInfo() // dismiss
                }
            )
            return
        }

        // Safe browsing: Malicious domain check
        if (safeBrowsingEnabled.value) {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: ""
            val isMalicious = maliciousDomains.any { host == it || host.endsWith(".$it") }
            if (isMalicious) {
                warningInfo.value = WarningInfo(
                    type = WarningType.MALICIOUS,
                    url = url,
                    tabId = activeTab.id,
                    onConfirm = {
                        // Users bypass malicious site block (not recommended, but supported if requested)
                        proceedWithUrlLoad(activeTab.id, url)
                        warningInfo.value = WarningInfo()
                    }
                )
                return
            }
        }

        proceedWithUrlLoad(activeTab.id, url)
    }

    private fun proceedWithUrlLoad(tabId: String, url: String) {
        val privateModeActive = isPrivateMode.value
        val currentList = if (privateModeActive) _privateTabs.value else _normalTabs.value
        val updated = currentList.map {
            if (it.id == tabId) {
                it.webView?.loadUrl(url)
                it.copy(url = url)
            } else {
                it
            }
        }
        if (privateModeActive) {
            _privateTabs.value = updated
        } else {
            _normalTabs.value = updated
        }
        urlInput.value = url
        isSearching.value = false
        areBarsVisible.value = true
    }

    // Web Navigation
    fun goBack() {
        areBarsVisible.value = true
        activeTab.value?.webView?.let {
            if (it.canGoBack()) {
                it.goBack()
            }
        }
    }

    fun goForward() {
        areBarsVisible.value = true
        activeTab.value?.webView?.let {
            if (it.canGoForward()) {
                it.goForward()
            }
        }
    }

    fun reload() {
        areBarsVisible.value = true
        activeTab.value?.webView?.reload()
    }

    // Called from WebClients to update tab state
    fun updateTabLoadingState(tabId: String, isLoading: Boolean, progress: Int) {
        val privateModeActive = isPrivateMode.value
        val currentList = if (privateModeActive) _privateTabs.value else _normalTabs.value
        val updated = currentList.map {
            if (it.id == tabId) {
                it.copy(
                    isLoading = isLoading,
                    progress = progress,
                    canGoBack = it.webView?.canGoBack() ?: false,
                    canGoForward = it.webView?.canGoForward() ?: false
                )
            } else {
                it
            }
        }
        if (privateModeActive) {
            _privateTabs.value = updated
        } else {
            _normalTabs.value = updated
        }
    }

    fun updateTabUrlAndTitle(tabId: String, url: String, title: String) {
        val privateModeActive = isPrivateMode.value
        val currentList = if (privateModeActive) _privateTabs.value else _normalTabs.value
        val updated = currentList.map {
            if (it.id == tabId) {
                // If it's the active tab, keep URL bar in sync
                val active = activeTab.value
                if (active != null && active.id == tabId && !isSearching.value) {
                    urlInput.value = url
                }
                it.copy(
                    url = url,
                    title = if (title.isNotBlank()) title else "New Tab",
                    canGoBack = it.webView?.canGoBack() ?: false,
                    canGoForward = it.webView?.canGoForward() ?: false
                )
            } else {
                it
            }
        }
        if (privateModeActive) {
            _privateTabs.value = updated
        } else {
            _normalTabs.value = updated
            // Add to history only if not private
            if (url.isNotBlank() && url != "about:blank") {
                viewModelScope.launch {
                    repository.addHistory(if (title.isNotBlank()) title else url, url)
                }
            }
        }
    }

    // Bookmarks management
    fun toggleCurrentPageBookmark() {
        val tab = activeTab.value ?: return
        if (tab.url.isBlank() || tab.url == "about:blank") return

        viewModelScope.launch {
            val isBookmarkedFlow = repository.isBookmarked(tab.url)
            val isCurrentlyBookmarked = isBookmarkedFlow.first()
            if (isCurrentlyBookmarked) {
                repository.deleteBookmarkByUrl(tab.url)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Removed from Bookmarks", Toast.LENGTH_SHORT).show()
                }
            } else {
                repository.addBookmark(if (tab.title.isNotBlank()) tab.title else tab.url, tab.url)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Added to Bookmarks", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun addBookmarkDirect(title: String, url: String) {
        viewModelScope.launch {
            repository.addBookmark(title, url)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
        }
    }

    fun isCurrentPageBookmarked(): Flow<Boolean> {
        val tab = activeTab.value ?: return flowOf(false)
        return repository.isBookmarked(tab.url)
    }

    // History management
    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Settings actions
    fun clearCacheAndCookies() {
        viewModelScope.launch {
            // Clear WebView Cache
            WebStorage.getInstance().deleteAllData()
            
            // Clear Cookies
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()

            withContext(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Browsing cache and cookies cleared", Toast.LENGTH_LONG).show()
            }
        }
    }

    // DownloadManager Integration
    private fun downloadFile(context: Context, url: String, userAgent: String, contentDisposition: String, mimetype: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimetype)
                addRequestHeader("User-Agent", userAgent)
                setDescription("Downloading file...")
                setTitle(Uri.parse(url).lastPathSegment ?: "Downloaded File")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(url).lastPathSegment ?: "download_file")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "Download started. Saving to Downloads folder.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Autocomplete fetcher for search suggestions
    private suspend fun fetchSuggestions(query: String) {
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val urlSpec = "https://suggestqueries.google.com/complete/search?client=chrome&q=$encoded"
                val url = URL(urlSpec)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val text = reader.readText()
                    reader.close()

                    // Format: ["query", ["suggestion1", "suggestion2", ...]]
                    val array = JSONArray(text)
                    if (array.length() > 1) {
                        val suggestionsJsonArray = array.getJSONArray(1)
                        val list = mutableListOf<String>()
                        for (i in 0 until suggestionsJsonArray.length()) {
                            list.add(suggestionsJsonArray.getString(i))
                        }
                        _searchSuggestions.value = list.take(6)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Safely destroy all active WebViews to prevent memory leaks
        _normalTabs.value.forEach { it.webView?.destroy() }
        _privateTabs.value.forEach { it.webView?.destroy() }
    }
}
