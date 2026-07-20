package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.webkit.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.viewmodel.BrowserTab
import com.example.viewmodel.BrowserViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    tab: BrowserTab,
    viewModel: BrowserViewModel,
    onScrollChanged: ((scrollY: Int, oldScrollY: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember(tab.id) { viewModel.getOrCreateWebView(tab.id, context) }

    LaunchedEffect(webView, onScrollChanged) {
        webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            onScrollChanged?.invoke(scrollY, oldScrollY)
        }
    }

    // Dialog flags for custom in-site permissions
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingPermissionsToRequest by remember { mutableStateOf<List<String>>(emptyList()) }

    var showLocationDialog by remember { mutableStateOf(false) }
    var pendingLocationCallback by remember { mutableStateOf<GeolocationPermissions.Callback?>(null) }
    var pendingLocationOrigin by remember { mutableStateOf("") }

    // File Chooser launcher
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.let { callback ->
            if (uris.isNotEmpty()) {
                callback.onReceiveValue(uris.toTypedArray())
            } else {
                callback.onReceiveValue(null)
            }
            filePathCallback = null
        }
    }

    // Android standard runtime permission request launcher
    val systemPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Handle generic permissions (Camera, Microphone)
        val allGranted = results.values.all { it }
        if (allGranted) {
            pendingPermissionRequest?.let { request ->
                val resources = request.resources
                request.grant(resources)
            }
        } else {
            pendingPermissionRequest?.deny()
        }
        pendingPermissionRequest = null
        showPermissionDialog = false
    }

    // Android Location Permission Request Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        pendingLocationCallback?.let { callback ->
            callback.invoke(pendingLocationOrigin, isGranted, false)
        }
        pendingLocationCallback = null
        showLocationDialog = false
    }

    // Update clients whenever state changes
    LaunchedEffect(tab.id) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                url?.let {
                    viewModel.updateTabLoadingState(tab.id, isLoading = true, progress = 10)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                url?.let {
                    viewModel.updateTabLoadingState(tab.id, isLoading = false, progress = 100)
                    viewModel.updateTabUrlAndTitle(tab.id, url, view?.title ?: "")
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                
                // Intercept HTTP and Malicious checks on navigation
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    if (viewModel.safeBrowsingEnabled.value && url.startsWith("http://")) {
                        viewModel.loadUrl(url)
                        return true
                    }
                }
                return false
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                // To support standard browser experience without SSL issues on standard sites:
                handler?.proceed()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                viewModel.updateTabLoadingState(tab.id, isLoading = newProgress < 100, progress = newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let {
                    viewModel.updateTabUrlAndTitle(tab.id, webView.url ?: tab.url, it)
                }
            }

            // Handle Camera/Microphone permission request from website
            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                val resources = request.resources
                val neededPermissions = mutableListOf<String>()
                
                if (resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                    neededPermissions.add(Manifest.permission.CAMERA)
                }
                if (resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                    neededPermissions.add(Manifest.permission.RECORD_AUDIO)
                }

                if (neededPermissions.isEmpty()) {
                    request.grant(resources)
                    return
                }

                pendingPermissionRequest = request
                pendingPermissionsToRequest = neededPermissions
                showPermissionDialog = true
            }

            // Handle Geolocation permission request from website
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                if (origin == null || callback == null) return
                pendingLocationOrigin = origin
                pendingLocationCallback = callback
                showLocationDialog = true
            }

            // Handle HTML File Chooser (<input type="file">)
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallbackValue: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = filePathCallbackValue
                fileChooserLauncher.launch("*/*")
                return true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(if (tab.isPrivate) Color(0xFF1C1C1E) else Color.White)) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize(),
            update = { /* Configuration is preserved in the saved instance */ }
        )

        // Web Page loading indicator (thin iOS style blue bar under address/top bar)
        if (tab.isLoading && tab.progress < 100) {
            LinearProgressIndicator(
                progress = { tab.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.TopStart),
                color = if (tab.isPrivate) Color(0xFF007AFF) else Color(0xFF007AFF),
                trackColor = Color.Transparent,
            )
        }

        // Custom iOS-Style dialog for Web Camera/Audio access
        if (showPermissionDialog && pendingPermissionRequest != null) {
            AlertDialog(
                onDismissRequest = {
                    pendingPermissionRequest?.deny()
                    showPermissionDialog = false
                },
                icon = { Icon(Icons.Default.Warning, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Camera & Microphone Access") },
                text = {
                    Text("The website \"${tab.title}\" is requesting access to your camera or microphone. Allow access?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val contextCompatList = pendingPermissionsToRequest.map {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }
                            if (contextCompatList.all { it }) {
                                pendingPermissionRequest?.grant(pendingPermissionRequest!!.resources)
                                showPermissionDialog = false
                            } else {
                                systemPermissionLauncher.launch(pendingPermissionsToRequest.toTypedArray())
                            }
                        }
                    ) {
                        Text("Allow")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingPermissionRequest?.deny()
                            showPermissionDialog = false
                        }
                    ) {
                        Text("Deny")
                    }
                }
            )
        }

        // Custom iOS-Style dialog for Web Geolocation access
        if (showLocationDialog && pendingLocationCallback != null) {
            AlertDialog(
                onDismissRequest = {
                    pendingLocationCallback?.invoke(pendingLocationOrigin, false, false)
                    showLocationDialog = false
                },
                icon = { Icon(Icons.Default.Warning, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Location Access") },
                text = {
                    Text("The website at $pendingLocationOrigin is requesting your location. Allow access?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                pendingLocationCallback?.invoke(pendingLocationOrigin, true, false)
                                showLocationDialog = false
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    ) {
                        Text("Allow")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingLocationCallback?.invoke(pendingLocationOrigin, false, false)
                            showLocationDialog = false
                        }
                    ) {
                        Text("Deny")
                    }
                }
            )
        }
    }
}
