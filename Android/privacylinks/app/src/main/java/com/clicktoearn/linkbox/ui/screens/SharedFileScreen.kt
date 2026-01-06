package com.clicktoearn.linkbox.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clicktoearn.linkbox.data.remote.FirestoreAsset
import com.clicktoearn.linkbox.ui.components.MarkdownContent
import com.clicktoearn.linkbox.utils.findActivity
import androidx.compose.ui.platform.LocalContext
import com.clicktoearn.linkbox.utils.setScreenshotDisabled
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import kotlinx.coroutines.launch
import com.clicktoearn.linkbox.ads.AdsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedFileScreen(
    ownerId: String,
    assetId: String,
    title: String,
    viewModel: LinkBoxViewModel,
    onBack: () -> Unit
) {
    var asset by remember { mutableStateOf<FirestoreAsset?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ownerId, assetId) {
        isLoading = true
        asset = viewModel.getCloudAsset(assetId)
        isLoading = false
    }

    // Screenshot blocking based on allowScreenCapture setting
    // Screenshot blocking - Always enabled

    
    DisposableEffect(Unit) {
        viewModel.enableScreenshotProtection()
        
        onDispose {
            viewModel.disableScreenshotProtection()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = asset?.name ?: title, 
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Screenshot protection always enabled - lock icon removed
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (asset == null) {
                    Text(
                        text = "Failed to load content.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (!asset!!.sharingEnabled) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sharing has been disabled for this asset.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        MarkdownContent(content = asset!!.content)
                        Spacer(modifier = Modifier.height(16.dp))
                        AdsManager.AdaptiveBannerAdView()
                    }
                }
            }
        }
    }
}
