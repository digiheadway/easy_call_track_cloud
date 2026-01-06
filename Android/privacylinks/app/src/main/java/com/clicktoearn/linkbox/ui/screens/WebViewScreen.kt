package com.clicktoearn.linkbox.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.URLUtil
import android.os.Environment
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.clicktoearn.linkbox.utils.findActivity
import com.clicktoearn.linkbox.utils.setScreenshotDisabled
import kotlinx.coroutines.launch
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import com.clicktoearn.linkbox.ads.AdsManager
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    title: String,
    viewModel: LinkBoxViewModel,
    onBack: () -> Unit,
    allowScreenCapture: Boolean = true,
    exposeUrl: Boolean = true
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Local snackbarState removed in favor of global viewModel.showMessage
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Web navigation states
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var displayUrl by remember { mutableStateOf(url) }

    // === Ad Timer Logic ===
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var nextAdTarget by remember { mutableStateOf<Long?>(null) }
    var isAdShowing by remember { mutableStateOf(false) }
    var isResumed by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Initialize Target
    LaunchedEffect(Unit) {
        nextAdTarget = AdsManager.getNextWebViewAdTime(elapsedSeconds)
    }

    // Lifecycle Observer
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            isResumed = event == androidx.lifecycle.Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Timer Loop
    LaunchedEffect(isAdShowing, isResumed) {
        if (isAdShowing || !isResumed) return@LaunchedEffect
        
        while (isActive) {
            delay(1000)
            elapsedSeconds++
            
            val target = nextAdTarget
            if (target != null) {
                val diff = target - elapsedSeconds
                
                if (diff == 5L) {
                    Toast.makeText(context, "Showing Ad in 5 seconds", Toast.LENGTH_SHORT).show()
                }
                
                if (diff <= 0L) {
                    isAdShowing = true
                    val activity = context.findActivity()
                    if (activity != null) {
                        AdsManager.showInterstitialAd(
                            activity = activity,
                            placementKey = "webview_$target",
                            onAdDismissed = {
                                isAdShowing = false
                                nextAdTarget = AdsManager.getNextWebViewAdTime(elapsedSeconds)
                            }
                        )
                    } else {
                        isAdShowing = false
                        nextAdTarget = AdsManager.getNextWebViewAdTime(elapsedSeconds)
                    }
                }
            }
        }
    }

    // Screenshot blocking - Always enabled
    DisposableEffect(Unit) {
        viewModel.enableScreenshotProtection()
        
        onDispose {
            viewModel.disableScreenshotProtection()
        }
    }

    // Handle device back button - navigate within WebView first
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }


    Scaffold(
        // snackbarHost = { SnackbarHost(snackbarHostState) }, // Using global snackbar host
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (!allowScreenCapture) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Screenshots Blocked",
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (exposeUrl) {
                            Text(
                                text = displayUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { 
                        // Update navigation stats before showing menu
                        canGoBack = webView?.canGoBack() == true
                        canGoForward = webView?.canGoForward() == true
                        showMenu = true 
                    }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        // Back
                        DropdownMenuItem(
                            text = { Text("Back") },
                            onClick = {
                                webView?.goBack()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                            enabled = canGoBack
                        )
                        // Forward
                        DropdownMenuItem(
                            text = { Text("Forward") },
                            onClick = {
                                webView?.goForward()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                            enabled = canGoForward
                        )
                        // Refresh
                        DropdownMenuItem(
                            text = { Text("Refresh") },
                            onClick = {
                                webView?.reload()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) }
                        )
                        // Downloads
                        DropdownMenuItem(
                            text = { Text("Downloads") },
                            onClick = {
                                try {
                                    val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                } catch (e: Exception) {
                                    scope.launch {
                                        viewModel.showMessage("Cannot open downloads")
                                    }
                                }
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) }
                        )
                        HorizontalDivider()
                        // Copy Link
                        DropdownMenuItem(
                            text = { Text("Copy Link") },
                            onClick = {
                                clipboardManager.setText(AnnotatedString(displayUrl))
                                scope.launch { viewModel.showMessage("Link copied") }
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                            enabled = exposeUrl
                        )
                        // Open in Chrome
                        DropdownMenuItem(
                            text = { Text("Open in Chrome") },
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(displayUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                } catch (e: Exception) {
                                    scope.launch {
                                        viewModel.showMessage("Cannot open browser")
                                    }
                                }
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.OpenInBrowser, contentDescription = null) },
                            enabled = exposeUrl
                        )
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                            url?.let { displayUrl = it }
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    
                    // Set Download Listener
                    setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                        try {
                            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                            val filename = URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype)
                            
                            request.setMimeType(mimetype)
                            // Get cookies for the request
                            val cookies = CookieManager.getInstance().getCookie(downloadUrl)
                            request.addRequestHeader("cookie", cookies)
                            request.addRequestHeader("User-Agent", userAgent)
                            
                            request.setDescription("Downloading file...")
                            request.setTitle(filename)
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            
                            val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                            
                            scope.launch {
                                viewModel.showMessage("Starting download: $filename")
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                viewModel.showMessage("Download failed: ${e.message}")
                            }
                        }
                    }
                    
                    // Disable long press context menu if exposeUrl is false (prevent "Copy Link")
                    if (!exposeUrl) {
                        setOnLongClickListener { true }
                        isLongClickable = false
                    }
                    
                    loadUrl(url)
                    webView = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
