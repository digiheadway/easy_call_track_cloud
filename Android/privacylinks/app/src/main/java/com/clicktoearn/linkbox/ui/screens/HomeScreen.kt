package com.clicktoearn.linkbox.ui.screens

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clicktoearn.linkbox.ui.Screen
import com.clicktoearn.linkbox.ui.components.PointsHeaderButton
import com.clicktoearn.linkbox.ui.components.WalletBottomSheet
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data classes for Home content
data class FeaturedContent(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val url: String, // Can be a full URL or a privacy link URL
    val category: String,
    val gradient: List<Color> = listOf(
        Color(0xFF667EEA),
        Color(0xFF764BA2)
    )
) {
    /**
     * Check if this is a privacy link that should open in SharedContent screen
     * Privacy links have formats like:
     * - https://privacy.be6.in/{token}
     * - https://privacy.be6.in/open?token={token}
     * - linkbox://{token}
     */
    fun isPrivacyLink(): Boolean {
        return try {
            val uri = Uri.parse(url)
            val token = extractToken()
            token != null && (
                uri.host == "privacy.be6.in" ||
                uri.scheme == "linkbox"
            )
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extract token from various URL formats
     */
    fun extractToken(): String? {
        return try {
            val uri = Uri.parse(url)
            // Check query parameter first
            uri.getQueryParameter("token")?.takeIf { it.isNotEmpty() }
            // Then check path segment
                ?: uri.pathSegments.lastOrNull()?.takeIf { 
                    it.isNotEmpty() && it !in listOf("open.php", "open", "index.html")
                }
        } catch (e: Exception) {
            null
        }
    }
}

data class HomeSection(
    val title: String,
    val icon: ImageVector,
    val items: List<FeaturedContent>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: LinkBoxViewModel, navController: NavController) {
    var isRefreshing by remember { mutableStateOf(false) }
    var showWalletSheet by remember { mutableStateOf(false) }
    val pointsSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    // Use ViewModel-backed state for persistence across tab switches
    val isHomeLoading by viewModel.isHomeLoading.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isPremium = userProfile?.isPremium == true
    
    // Load content only once (on first launch or if not yet loaded)
    LaunchedEffect(Unit) {
        if (!viewModel.isHomeContentLoaded()) {
            // delay(600) - Removed artificial delay
            viewModel.loadHomeContent(getSampleHomeSections())
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Discover",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    PointsHeaderButton(viewModel = viewModel) { showWalletSheet = true }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    delay(800)
                    viewModel.refreshHomeContent(getSampleHomeSections())
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val renderItems by viewModel.homeRenderItems.collectAsState()
            
            if (isHomeLoading && renderItems.isEmpty()) {
                // Show shimmer loading state only on first load
                HomeShimmerContent()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(renderItems) { item ->
                        when (item) {
                            is com.clicktoearn.linkbox.data.remote.HomeRenderItem.Section -> {
                                HomeSectionContent(
                                    section = item.data,
                                    navController = navController
                                )
                            }
                            is com.clicktoearn.linkbox.data.remote.HomeRenderItem.Ad -> {
                                if (!isPremium) {
                                    // Wrapped ad container for consistent layout
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Box(modifier = Modifier.padding(8.dp)) {
                                            when (item.type) {
                                                "ad_native_video" -> com.clicktoearn.linkbox.ads.AdsManager.NativeVideoAdView()
                                                "ad_native" -> com.clicktoearn.linkbox.ads.AdsManager.NativeAdView()
                                                "ad_banner" -> com.clicktoearn.linkbox.ads.AdsManager.AdaptiveBannerAdView()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showWalletSheet) {
            WalletBottomSheet(
                viewModel = viewModel,
                onDismiss = { showWalletSheet = false },
                sheetState = pointsSheetState
            )
        }
    }
}

@Composable
fun HomeSectionContent(
    section: HomeSection,
    navController: NavController
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Full-screen horizontal scroll content (one item per row)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(section.items) { item ->
                FeaturedContentCard(
                    content = item,
                    onClick = {
                        // Smart navigation: Privacy links go to SharedContent, regular URLs to WebView
                        com.clicktoearn.linkbox.analytics.AnalyticsManager.logContentCardClick(item.title, item.url)
                        if (item.isPrivacyLink()) {
                            val token = item.extractToken()
                            if (token != null) {
                                navController.navigate(Screen.SharedContent.createRoute(token))
                            }
                        } else {
                            // Regular URL - open in WebView
                            val encodedUrl = java.net.URLEncoder.encode(item.url, "UTF-8")
                            val encodedTitle = java.net.URLEncoder.encode(item.title, "UTF-8")
                            // Default: allow screenshot, expose URL
                            navController.navigate("webview/$encodedUrl/$encodedTitle/true/true")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FeaturedContentCard(
    content: FeaturedContent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = content.gradient,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            // Decorative circles
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-20).dp, y = (-20).dp)
                    .clip(RoundedCornerShape(60.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Category chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = content.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column {
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = content.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Play/View icon
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "View",
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = (-16).dp)
            )
        }
    }
}

// ==================== Shimmer Components ====================

@Composable
fun HomeShimmerContent() {
    val shimmerBrush = rememberHomeShimmerBrush()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 4 sections with shimmer - using stable items API
        items(count = 4) {
            HomeShimmerSection(shimmerBrush)
        }
    }
}

@Composable
fun HomeShimmerSection(brush: Brush) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header shimmer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon shimmer
            Spacer(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Title shimmer
            Spacer(
                modifier = Modifier
                    .width(120.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Cards shimmer - horizontal scroll simulation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(2) {
                HomeShimmerCard(brush)
            }
        }
    }
}

@Composable
fun HomeShimmerCard(brush: Brush) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Category chip shimmer
                Spacer(
                    modifier = Modifier
                        .width(80.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
                
                Column {
                    // Title shimmer
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Description shimmer
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    )
                }
            }
        }
    }
}

@Composable
fun rememberHomeShimmerBrush(): Brush {
    // Use highly visible colors - surfaceVariant for base, onSurface for highlight
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    
    val shimmerColors = listOf(
        baseColor,
        highlightColor,
        baseColor
    )

    val transition = rememberInfiniteTransition(label = "home_shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "home_shimmer_anim"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}

// ==================== Sample Data ====================

private fun getSampleHomeSections(): List<HomeSection> {
    return listOf(
        HomeSection(
            title = "Supers",
            icon = Icons.Default.Star,
            items = listOf(
                FeaturedContent(
                    id = "super_1",
                    title = "Premium Collection",
                    description = "Exclusive curated content for you",
                    url = "https://privacy.be6.in/super_premium_001", // Privacy link
                    category = "Featured",
                    gradient = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
                ),
                FeaturedContent(
                    id = "super_2",
                    title = "Top Trending",
                    description = "Most popular content this week",
                    url = "https://privacy.be6.in/super_trending_002", // Privacy link
                    category = "Trending",
                    gradient = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                ),
                FeaturedContent(
                    id = "super_3",
                    title = "Editor's Choice",
                    description = "Hand-picked just for you",
                    url = "https://privacy.be6.in/super_editors_003", // Privacy link
                    category = "Choice",
                    gradient = listOf(Color(0xFF11998E), Color(0xFF38EF7D))
                )
            )
        ),
        HomeSection(
            title = "Featured Apps",
            icon = Icons.Default.Apps,
            items = listOf(
                FeaturedContent(
                    id = "app_1",
                    title = "Productivity Suite",
                    description = "Boost your daily workflow",
                    url = "https://play.google.com/store/apps/details?id=com.example.productivity", // Regular URL
                    category = "Apps",
                    gradient = listOf(Color(0xFF4776E6), Color(0xFF8E54E9))
                ),
                FeaturedContent(
                    id = "app_2",
                    title = "Creative Tools",
                    description = "Design and create with ease",
                    url = "https://play.google.com/store/apps/details?id=com.example.creative", // Regular URL
                    category = "Apps",
                    gradient = listOf(Color(0xFFee0979), Color(0xFFff6a00))
                ),
                FeaturedContent(
                    id = "app_3",
                    title = "Utility Pack",
                    description = "Essential tools for everyone",
                    url = "https://play.google.com/store/apps/details?id=com.example.utility", // Regular URL
                    category = "Apps",
                    gradient = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))
                )
            )
        ),
        HomeSection(
            title = "Featured Games",
            icon = Icons.Default.SportsEsports,
            items = listOf(
                FeaturedContent(
                    id = "game_1",
                    title = "Adventure Quest",
                    description = "Epic journey awaits you",
                    url = "https://play.google.com/store/apps/details?id=com.example.adventure", // Regular URL
                    category = "Games",
                    gradient = listOf(Color(0xFFf093fb), Color(0xFFf5576c))
                ),
                FeaturedContent(
                    id = "game_2",
                    title = "Puzzle Master",
                    description = "Challenge your mind",
                    url = "https://play.google.com/store/apps/details?id=com.example.puzzle", // Regular URL
                    category = "Games",
                    gradient = listOf(Color(0xFF4568DC), Color(0xFFB06AB3))
                ),
                FeaturedContent(
                    id = "game_3",
                    title = "Racing Legends",
                    description = "Speed and thrills",
                    url = "https://play.google.com/store/apps/details?id=com.example.racing", // Regular URL
                    category = "Games",
                    gradient = listOf(Color(0xFFFC466B), Color(0xFF3F5EFB))
                )
            )
        ),
        HomeSection(
            title = "Featured Content",
            icon = Icons.Default.AutoAwesome,
            items = listOf(
                FeaturedContent(
                    id = "content_1",
                    title = "Learning Hub",
                    description = "Expand your knowledge",
                    url = "https://privacy.be6.in/open?token=content_learning_001", // Privacy link with query param
                    category = "Content",
                    gradient = listOf(Color(0xFF0575E6), Color(0xFF021B79))
                ),
                FeaturedContent(
                    id = "content_2",
                    title = "Entertainment Zone",
                    description = "Fun and relaxation",
                    url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ", // Regular URL (example)
                    category = "Content",
                    gradient = listOf(Color(0xFFeb3349), Color(0xFFf45c43))
                ),
                FeaturedContent(
                    id = "content_3",
                    title = "Lifestyle Guide",
                    description = "Tips for better living",
                    url = "https://privacy.be6.in/content_lifestyle_003", // Privacy link
                    category = "Content",
                    gradient = listOf(Color(0xFF1FA2FF), Color(0xFF12D8FA), Color(0xFFA6FFCB))
                )
            )
        )
    )
}
