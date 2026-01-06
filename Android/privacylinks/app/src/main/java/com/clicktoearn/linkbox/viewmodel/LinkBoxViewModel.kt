package com.clicktoearn.linkbox.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clicktoearn.linkbox.data.FirestoreRepository
import com.clicktoearn.linkbox.data.LinkBoxRepository
import com.clicktoearn.linkbox.data.local.*
import com.clicktoearn.linkbox.data.remote.*
import com.clicktoearn.linkbox.utils.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

// UI Models
data class UiSharedContent(
    val token: String,
    val assetId: String,
    val ownerId: String,
    val ownerName: String,
    val name: String,
    val description: String,
    val assetType: String,
    val assetContent: String,
    val expiryDate: Long?,
    val pointCost: Int,
    val allowSaveCopy: Boolean,

    val allowFurtherSharing: Boolean,
    val allowScreenCapture: Boolean,
    val exposeUrl: Boolean,
    val chargeEveryTime: Boolean,
    val sharingEnabled: Boolean,
    val isActive: Boolean
)


data class UiHistoryItem(
    val token: String,
    val accessedAt: Long,
    val isStarred: Boolean,
    val isPaid: Boolean,
    val assetName: String,
    val assetType: String,
    val ownerName: String,
    val description: String,
    val pointCost: Int,
    val allowFurtherSharing: Boolean = true,
    val sharingEnabled: Boolean = true,
    val linkStatus: String = "ACTIVE",
    val expiryDate: Long? = null,
    val isDeleted: Boolean = false
)

// Extension functions to convert between Firestore and Entity models
fun FirestoreAsset.toEntity(): AssetEntity = AssetEntity(
    id = id,
    userId = ownerId,
    name = name,
    type = try { AssetType.valueOf(type) } catch (e: Exception) { AssetType.FILE },
    content = content,
    parentId = parentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pointCost = pointCost,
    allowSaveCopy = allowSaveCopy,
    shareOutsideApp = allowFurtherSharing,
    allowScreenCapture = allowScreenCapture,
    exposeUrl = exposeUrl,
    chargeEveryTime = chargeEveryTime,
    sharingEnabled = sharingEnabled
)

fun AssetEntity.toFirestore(): FirestoreAsset = FirestoreAsset(
    id = id,
    name = name,
    description = description,
    type = type.name,
    content = content,
    parentId = parentId,
    ownerId = userId,
    pointCost = pointCost,
    allowSaveCopy = allowSaveCopy,
    allowFurtherSharing = shareOutsideApp,
    allowScreenCapture = allowScreenCapture,
    exposeUrl = exposeUrl,
    chargeEveryTime = chargeEveryTime,
    sharingEnabled = sharingEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun FirestoreLink.toEntity(): SharingLinkEntity = SharingLinkEntity(
    id = id,
    assetId = assetId,
    token = token,
    name = name,
    expiry = expiryDate,
    status = status,
    views = views,
    users = users,
    newUsers = newUsers,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun SharingLinkEntity.toFirestore(): FirestoreLink = FirestoreLink(
    id = id,
    assetId = assetId,
    token = token,
    name = name,
    expiryDate = expiry,
    status = status,
    newUsers = newUsers,
    users = users,
    views = views,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun FirestoreHistory.toEntity(): HistoryEntity = HistoryEntity(
    token = token,
    accessedAt = accessedAt,
    isStarred = isStarred,
    isPaid = false // Paid status not currently synced from cloud history model, defaulting to false or handling locally
)

fun FirestoreUser.toEntity(): UserEntity = UserEntity(
    id = "current_user",
    username = username,
    photoUrl = photoUrl,
    points = points,
    accessToken = accessToken,
    remoteId = userId,
    isPremium = isPremium,
    premiumExpiry = premiumExpiry,
    isGuest = isGuest
)

@OptIn(ExperimentalCoroutinesApi::class)
class LinkBoxViewModel(
    private val repository: FirestoreRepository,
    private val localRepository: LinkBoxRepository,
    private val app: com.clicktoearn.linkbox.LinkBoxApp
) : ViewModel() {



    // ==================== User Profile ====================
    val userProfile: StateFlow<UserEntity?> = repository.userProfile
        .map { it?.toEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Cache premium status for instant synchronous checks (e.g. at app startup)
        viewModelScope.launch {
            repository.userProfile.collect { user ->
                app.savePremiumStatus(user?.isPremium == true)
            }
        }
    }

    val currentUserId: StateFlow<String?> = repository.userProfile
        .map { it?.userId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn: StateFlow<Boolean> = repository.userProfile
        .map { it != null && it.username.isNotBlank() && !it.isGuest }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Cached points for instant UI - starts with locally cached value, updates from server
    private val _cachedPoints = MutableStateFlow(app.getCachedPoints())
    val userPoints: StateFlow<Int> = combine(
        repository.userProfile.map { it?.points ?: 0 },
        _cachedPoints
    ) { serverPoints, cachedPoints ->
        // If server has data, use it and cache it
        if (serverPoints > 0 || repository.userProfile.value != null) {
            if (serverPoints != cachedPoints) {
                app.saveCachedPoints(serverPoints)
            }
            serverPoints
        } else {
            // Use cached value while loading
            cachedPoints
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), app.getCachedPoints())

    val isDarkMode: MutableStateFlow<Boolean> = MutableStateFlow(false)
    
    // isInitialized is now TRUE immediately - no blocking on network calls
    // Deep links and Home screen will show shimmers while data loads
    private val _isInitialized = MutableStateFlow(true)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // ==================== User Messages (Snackbars) ====================
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun showMessage(message: String) {
        viewModelScope.launch {
            _userMessage.emit(message)
        }
    }
    
    // ==================== Network Connectivity ====================
    private val networkMonitor = NetworkMonitor(app.applicationContext)
    val isConnected: StateFlow<Boolean> = networkMonitor.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    fun checkConnectivity() {
        // Trigger a manual connectivity check
        networkMonitor.checkConnectivity()
    }
    
    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
    }

    // ==================== Assets (Local-First) ====================
    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId

    // Local-first: Assets are stored in Room DB
    val assets: StateFlow<List<AssetEntity>> = _currentFolderId
        .flatMapLatest { folderId -> 
            localRepository.getAssets(folderId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All local assets (for folder picker etc.)
    private val _allLocalAssets = MutableStateFlow<List<AssetEntity>>(emptyList())

    // All folders for folder picker (Global list)
    val allFolders: StateFlow<List<AssetEntity>> = localRepository.getAllAssets()
        .map { list -> list.filter { it.type == AssetType.FOLDER } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAssets: StateFlow<List<AssetEntity>> = assets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Get current folder info from local DB
    val currentFolder: StateFlow<AssetEntity?> = _currentFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) flowOf<AssetEntity?>(null)
            else flow { 
                emit(localRepository.getAssetById(folderId))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Get parent folder ID of current folder (for back navigation)
    fun getParentFolderId(): String? {
        val current = currentFolder.value
        return current?.parentId
    }

    // ==================== Links (Local-First) ====================
    val allSharingLinks: StateFlow<List<SharingLinkEntity>> = localRepository.getAllSharingLinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getSharingLinksForAsset(assetId: String): Flow<List<SharingLinkEntity>> {
        return localRepository.getSharingLinksForAsset(assetId)
    }

    // ==================== History ====================
    private val _historyItems = MutableStateFlow<List<UiHistoryItem>>(emptyList())
    val history: StateFlow<List<UiHistoryItem>> = _historyItems.asStateFlow()

    // ==================== Shared Content ====================
    private val _sharedContent = MutableStateFlow<UiSharedContent?>(null)
    val sharedContent: StateFlow<UiSharedContent?> = _sharedContent

    private val _isLoadingShared = MutableStateFlow(false)
    val isLoadingShared: StateFlow<Boolean> = _isLoadingShared

    private var sharedContentJob: Job? = null
    
    // Cache for shared content - LRU-like cache with max 30 entries
    private val sharedContentCache = mutableMapOf<String, UiSharedContent>()
    private val cacheMaxSize = 30
    
    // Prefetch job for early loading
    private var prefetchJob: Job? = null
    private var currentPrefetchToken: String? = null

    // ==================== Home Content ====================
    // Hybrid Approach: Remote Config Layout + Local/DB Content
    
    private val _homeRenderItems = MutableStateFlow<List<HomeRenderItem>>(emptyList())
    val homeRenderItems: StateFlow<List<HomeRenderItem>> = _homeRenderItems.asStateFlow()
    
    private val _isHomeLoading = MutableStateFlow(true)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()
    
    private var homeContentLoaded = false
    
    fun loadHomeContent(sections: List<Any>) {
        if (!homeContentLoaded || _homeRenderItems.value.isEmpty()) {
            homeContentLoaded = true
            viewModelScope.launch(Dispatchers.Default) {
                parseHomeLayout(sections)
                _isHomeLoading.value = false
            }
        }
    }
    
    fun refreshHomeContent(sections: List<Any>) {
        viewModelScope.launch(Dispatchers.Default) {
            parseHomeLayout(sections)
            _isHomeLoading.value = false
        }
    }
    
    fun isHomeContentLoaded(): Boolean = homeContentLoaded && _homeRenderItems.value.isNotEmpty()

    private fun parseHomeLayout(sections: List<Any>) {
        try {
            val remoteConfig = com.google.firebase.ktx.Firebase.remoteConfig
            val layoutJson = remoteConfig.getString("home_layout_config")
            
            val validJson = if (layoutJson.isBlank()) {
                // Default Layout
                """
                [
                  {"type": "ad_native_video", "id": "hero_video", "visible": true},
                  {"type": "section", "id": "supers", "visible": true},
                  {"type": "ad_native", "id": "mid_native", "visible": true},
                  {"type": "section", "id": "apps", "visible": true},
                  {"type": "section", "id": "games", "visible": true},
                  {"type": "ad_banner", "id": "bottom_banner", "visible": true},
                  {"type": "section", "id": "content", "visible": true}
                ]
                """.trimIndent()
            } else {
                layoutJson
            }

            val renderItems = mutableListOf<HomeRenderItem>()
            val jsonArray = org.json.JSONArray(validJson)
            
            // Map sections by title or index (simple mapping strategy)
            // Supers -> 0, Apps -> 1, Games -> 2, Content -> 3
            // In a real app, IDs would be consistent. Here we map "supers" -> sections[0]
            
            // Cast list to HomeSection to access properties if needed
            // Ideally we should use a map, but we'll use index/ID matching
            
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val type = item.optString("type")
                val id = item.optString("id")
                val visible = item.optBoolean("visible", true)
                
                if (!visible) continue
                
                if (type == "section") {
                    // Find the matching section from our data source
                    val matchingSection = when (id) {
                        "supers" -> sections.getOrNull(0)
                        "apps" -> sections.getOrNull(1)
                        "games" -> sections.getOrNull(2)
                        "content" -> sections.getOrNull(3)
                        else -> null
                    }
                    
                    if (matchingSection != null && matchingSection is com.clicktoearn.linkbox.ui.screens.HomeSection) {
                        // Create copy with overrides if needed (conceptually)
                        renderItems.add(HomeRenderItem.Section(matchingSection))
                    }
                } else if (type.startsWith("ad_")) {
                    renderItems.add(HomeRenderItem.Ad(type))
                }
            }
            
            _homeRenderItems.value = renderItems
            
        } catch (e: Exception) {
            android.util.Log.e("LinkBoxViewModel", "Error parsing home layout", e)
            // Fallback: Just dump sections
            // We need to construct a fallback list
             val fallbackItems = sections.mapNotNull { 
                 if (it is com.clicktoearn.linkbox.ui.screens.HomeSection) HomeRenderItem.Section(it) else null
             }
             _homeRenderItems.value = fallbackItems
        }
    }

    // ==================== Refresh ====================
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // Global Screenshot Protection State
    private var screenshotBlockCount = AtomicInteger(0)
    private val _isScreenshotBlocked = MutableStateFlow(false)
    val isScreenshotBlocked = _isScreenshotBlocked.asStateFlow()

    fun enableScreenshotProtection() {
        if (screenshotBlockCount.incrementAndGet() > 0) {
            _isScreenshotBlocked.value = true
        }
    }

    fun disableScreenshotProtection() {
        if (screenshotBlockCount.decrementAndGet() <= 0) {
            screenshotBlockCount.set(0)
            _isScreenshotBlocked.value = false
        }
    }
    private var historyRefreshJob: Job? = null

    // ==================== Initialization ====================
    // OPTIMIZED: All network operations run in background
    // UI shows immediately with shimmers/cached data while data loads
    init {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("LinkBoxViewModel", "ViewModel created - Local-first, UI ready immediately")
        
        // Only sync with cloud if user is logged in
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Priority 1: Firebase Auth user (Google Login)
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    android.util.Log.d("LinkBoxViewModel", "Background: Firebase User detected: ${firebaseUser.uid}")
                    repository.fetchUserProfile()
                    syncCloudData()
                } else {
                    android.util.Log.d("LinkBoxViewModel", "No user logged in - using local-only mode")
                }
                
                android.util.Log.d("LinkBoxViewModel", "Init completed in ${System.currentTimeMillis() - startTime}ms")
                
                // Inject sample data for new users
                injectSampleData()
                
            } catch (e: Exception) {
                android.util.Log.e("LinkBoxViewModel", "Background initialization error (continuing with local data)", e)
            }
        }
        
        monitorLocalHistory()
    }

    private suspend fun syncCloudData() {
        coroutineScope {
            // Fetch from cloud (updates memory cache in repository)
            val assetsDeferred = async { repository.fetchAssets() }
            val linksDeferred = async { repository.fetchLinks() }
            val historyDeferred = async { repository.fetchHistory() }
            
            // Wait for fetches to complete
            assetsDeferred.await()
            linksDeferred.await()
            historyDeferred.await()
            
            // Sync down to local Room DB
            syncDownCloudData()
        }
    }
    
    /**
     * Pushes cloud data (from Repository memory/network) into Local Room DB.
     * This ensures the UI (observing Room) reflects the cloud state.
     */
    private suspend fun syncDownCloudData() {
        try {
            android.util.Log.d("LinkBoxViewModel", "Syncing cloud data to local storage...")
            
            // 1. Sync Assets
            val cloudAssets = repository.assets.value
            if (cloudAssets.isNotEmpty()) {
                val assetEntities = cloudAssets.map { it.toEntity() }
                localRepository.insertAssets(assetEntities)
                android.util.Log.d("LinkBoxViewModel", "Synced ${cloudAssets.size} assets from cloud")
            }
            
            // 2. Sync Links (includes View counts)
            val cloudLinks = repository.links.value
            if (cloudLinks.isNotEmpty()) {
                val linkEntities = cloudLinks.map { it.toEntity() }
                localRepository.insertSharingLinks(linkEntities)
                android.util.Log.d("LinkBoxViewModel", "Synced ${cloudLinks.size} links from cloud")
            }
            
            // 3. Sync History
            val cloudHistory = repository.history.value
            if (cloudHistory.isNotEmpty()) {
                android.util.Log.d("LinkBoxViewModel", "Syncing ${cloudHistory.size} history items from cloud: ${cloudHistory.map { it.token }}")
                val historyEntities = cloudHistory.map { it.toEntity() }
                localRepository.insertHistories(historyEntities)
                android.util.Log.d("LinkBoxViewModel", "Synced ${cloudHistory.size} history items into local DB")
            } else {
                 android.util.Log.d("LinkBoxViewModel", "Cloud history is empty")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("LinkBoxViewModel", "Error syncing cloud data to local", e)
        }
    }
    
    private suspend fun injectSampleData() {
        // Check if we've already injected sample data for this session
        if (app.hasInjectedSampleData()) return

        // Only add sample asset if local assets are empty
        val currentAssets = localRepository.getAssets(null).first()
        if (currentAssets.isEmpty()) {
            val sampleAsset = AssetEntity(
                id = "sample_get_started",
                name = "Get Started",
                type = AssetType.FILE,
                content = """
                    # Welcome to Private Files!
                    
                    This is your personal space to manage links, folders, and pages.
                    
                    ### Key Features:
                    - **Folders:** Organize your content.
                    - **Pages:** Create rich text content using Markdown.
                    - **Links:** Save and open your favorite URLs.
                    - **Sharing:** Securely share any asset with customizable settings.
                    - **Rewards:** Earn points by sharing content or watching ads.
                    
                    ### How to Share:
                    1. Click on the three dots next to any asset.
                    2. Select **Share**.
                    3. Customize your link (expiry, points cost, etc.).
                    4. Copy the link and share it anywhere!
                    
                    *Note: This sample file is deletable but cannot be shared.*
                """.trimIndent(),
                parentId = null,
                sharingEnabled = false, // Sample file cannot be shared
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            localRepository.insertAsset(sampleAsset)
        }
        
        // Only add sample history if local history is empty
        val currentHistory = localRepository.getHistory().first()
        if (currentHistory.isEmpty()) {
            val sampleHistory = HistoryEntity(
                token = "sample_google",
                accessedAt = System.currentTimeMillis() - 1000,
                isStarred = false,
                isPaid = true
            )
            localRepository.insertHistory(sampleHistory)
        }
        
        // Mark sample data as injected
        app.markSampleDataInjected()
    }

    /**
     * Monitor local history (Room DB) and fetch asset details from network.
     * Local-first: History tokens are stored locally, asset details fetched on demand.
     */
    private fun monitorLocalHistory() {
        viewModelScope.launch {
            localRepository.getHistory().collectLatest { localHistoryList ->
                updateLocalHistoryUi(localHistoryList)
            }
        }
    }

    fun refreshAll(isManual: Boolean = true) {
        viewModelScope.launch {
            if (isManual) _isRefreshing.value = true
            try {
                withTimeout(15000L) {
                    repository.refreshAll()
                    // CRITICAL: Sync fetched cloud data down to local Room DB so UI updates
                    syncDownCloudData()
                }
            } catch (e: Exception) {
                android.util.Log.e("LinkBoxViewModel", "refreshAll failed", e)
                if (e is TimeoutCancellationException) {
                     android.util.Log.w("LinkBoxViewModel", "refreshAll timed out")
                     showMessage("Refresh timed out")
                } else {
                     showMessage("Failed to refresh data")
                }
            } finally {
                if (isManual) _isRefreshing.value = false
            }
        }
    }

    /**
     * Update history UI from local history entries.
     * Fetches asset details from network/cache for display.
     */
    private suspend fun updateLocalHistoryUi(localHistoryList: List<HistoryEntity>, forceRefresh: Boolean = false) {
        try {
            android.util.Log.d("LinkBoxViewModel", "Processing ${localHistoryList.size} local history entries (forceRefresh=$forceRefresh)")
            
            // Get current items to reuse metadata if possible (in-memory cache)
            val currentItems = _historyItems.value.associateBy { it.token }
            val source = if (forceRefresh) com.google.firebase.firestore.Source.SERVER else com.google.firebase.firestore.Source.DEFAULT

            // Process items in parallel for efficiency
            val items = coroutineScope {
                localHistoryList.map { h ->
                    async(Dispatchers.IO) {
                        val cached = currentItems[h.token]
                        if (h.token == "sample_google") {
                            UiHistoryItem(
                                token = h.token,
                                accessedAt = h.accessedAt,
                                isStarred = h.isStarred,
                                isPaid = h.isPaid,
                                assetName = "Google",
                                assetType = AssetType.LINK.name,
                                ownerName = "Private Files",
                                description = "Search the world's information, including webpages, images, videos and more.",
                                pointCost = 0,
                                allowFurtherSharing = false,
                                sharingEnabled = true,
                                linkStatus = "ACTIVE",
                                expiryDate = null,
                                isDeleted = false
                            )
                        } else if (cached != null && !forceRefresh) {
                            // Reuse cached metadata, only update local fields
                            UiHistoryItem(
                                token = h.token,
                                accessedAt = h.accessedAt,
                                isStarred = h.isStarred,
                                isPaid = h.isPaid,
                                assetName = cached.assetName,
                                assetType = cached.assetType,
                                ownerName = cached.ownerName,
                                description = cached.description,
                                pointCost = cached.pointCost,
                                allowFurtherSharing = cached.allowFurtherSharing,
                                sharingEnabled = cached.sharingEnabled,
                                linkStatus = cached.linkStatus,
                                expiryDate = cached.expiryDate,
                                isDeleted = cached.isDeleted
                            )
                        } else {
                            // Fetch link and asset details from Firestore
                            try {
                                val link = repository.lookupByToken(h.token, source)
                                if (link == null) {
                                    android.util.Log.w("LinkBoxViewModel", "History Token ${h.token}: Link not found in Firestore (source=$source)")
                                }
                                
                                val asset = link?.let { repository.getAssetById(it.assetId, source) }
                                if (link != null && asset == null) {
                                     android.util.Log.w("LinkBoxViewModel", "History Token ${h.token}: Link found but Asset ${link.assetId} not found")
                                }
                                
                                val ownerName = asset?.let { getOwnerUsername(it.ownerId) } ?: "Unknown"
                                
                                val isDeleted = link == null || asset == null
                                if (isDeleted) {
                                    android.util.Log.d("LinkBoxViewModel", "History Token ${h.token} marked as isDeleted (link=$link, asset=$asset)")
                                }

                                UiHistoryItem(
                                    token = h.token,
                                    accessedAt = h.accessedAt,
                                    isStarred = h.isStarred,
                                    isPaid = h.isPaid,
                                    assetName = asset?.name ?: link?.name ?: "Loading...",
                                    assetType = asset?.type ?: "UNKNOWN",
                                    ownerName = ownerName,
                                    description = asset?.description ?: "",
                                    pointCost = asset?.pointCost ?: 0,
                                    allowFurtherSharing = asset?.allowFurtherSharing ?: true,
                                    sharingEnabled = asset?.sharingEnabled ?: true,
                                    linkStatus = link?.status ?: "UNKNOWN",
                                    expiryDate = link?.expiryDate,
                                    isDeleted = isDeleted
                                )
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                // Network error - show placeholder
                                UiHistoryItem(
                                    token = h.token,
                                    accessedAt = h.accessedAt,
                                    isStarred = h.isStarred,
                                    isPaid = h.isPaid,
                                    assetName = "Unavailable",
                                    assetType = "UNKNOWN",
                                    ownerName = "Unknown",
                                    description = "",
                                    pointCost = 0,
                                    allowFurtherSharing = true,
                                    sharingEnabled = true,
                                    linkStatus = "UNKNOWN",
                                    expiryDate = null,
                                    isDeleted = false // Network error, assume valid
                                )
                            }
                        }
                    }
                }.awaitAll()
            }
            _historyItems.value = items
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("LinkBoxViewModel", "Error updating local history items", e)
        }
    }

    private fun getSampleHistoryItems(): List<UiHistoryItem> {
        return emptyList()
    }
    
    /**
     * Save history entry to local Room DB.
     * Only stores token and local flags - asset details fetched on display.
     */
    private suspend fun saveLocalHistory(token: String, isPaid: Boolean = false) {
        try {
            val historyEntity = HistoryEntity(
                token = token,
                accessedAt = System.currentTimeMillis(),
                isStarred = false,
                isPaid = isPaid
            )
            localRepository.insertHistory(historyEntity)
            
            // Sync to cloud if logged in (repository.saveHistory handles the check)
            repository.saveHistory(token, isPaid)
        } catch (e: Exception) {
            android.util.Log.e("LinkBoxViewModel", "Failed to save local history", e)
        }
    }

    // Manual refresh for history screen (refreshes asset details from network)
    fun refreshHistory(isManual: Boolean = true) {
        historyRefreshJob?.cancel()
        historyRefreshJob = viewModelScope.launch {
            if (isManual) _isRefreshing.value = true
            try {
                // Add timeout to prevent infinite hanging on network calls
                // Add 500ms delay to ensure spinner is visible briefly (better UX and prevents glitchy toggles)
                if (isManual) kotlinx.coroutines.delay(500)
                
                withTimeout(15000L) {
                    // Get local history and force refresh asset details from network
                    val localHistory = localRepository.getHistory().first()
                    updateLocalHistoryUi(localHistory, forceRefresh = isManual)
                }
            } catch (e: Exception) {
                android.util.Log.e("LinkBoxViewModel", "refreshHistory failed", e)
                if (e is kotlinx.coroutines.TimeoutCancellationException) {
                     android.util.Log.w("LinkBoxViewModel", "refreshHistory timed out")
                     if (isManual) showMessage("Refresh timed out")
                } else {
                     if (isManual) showMessage("Failed to refresh history")
                }
            } finally {
                android.util.Log.d("LinkBoxViewModel", "refreshHistory finally block. isManual=$isManual")
                if (isManual) _isRefreshing.value = false
            }
        }
    }

    // ==================== Navigation ====================
    fun navigateToFolder(folderId: String?) {
        _currentFolderId.value = folderId
    }

    // ==================== Asset Operations (Local-First) ====================
    // Assets are saved to local Room DB - no account required
    // Cloud sync happens only when user is logged in and shares content
    
    fun addFile(name: String, content: String, parentId: String?) {
        viewModelScope.launch {
            val userId = repository.getUserId() ?: "current_user"
            val asset = AssetEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                type = AssetType.FILE,
                content = content,
                parentId = parentId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            localRepository.insertAsset(asset)
            
            // Sync to cloud if logged in
            if (isLoggedIn.value) {
                repository.saveAsset(asset.toFirestore())
            }
            showMessage("File created successfully")
        }
    }

    fun addFolder(name: String, parentId: String?) {
        viewModelScope.launch {
            val userId = repository.getUserId() ?: "current_user"
            val asset = AssetEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                type = AssetType.FOLDER,
                content = "",
                parentId = parentId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            localRepository.insertAsset(asset)
            
            // Sync to cloud if logged in
            if (isLoggedIn.value) {
                repository.saveAsset(asset.toFirestore())
            }
            showMessage("Folder created successfully")
        }
    }

    fun addLink(name: String, url: String, parentId: String?) {
        viewModelScope.launch {
            val userId = currentUserId.value ?: "current_user"
            val asset = AssetEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                type = AssetType.LINK,
                content = url,
                parentId = parentId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            localRepository.insertAsset(asset)
            
            // Sync to cloud if logged in
            if (isLoggedIn.value) {
                repository.saveAsset(asset.toFirestore())
            }
            showMessage("Link created successfully")
        }
    }

    fun updateAsset(asset: AssetEntity) {
        viewModelScope.launch {
            // Update locally
            val updated = asset.copy(updatedAt = System.currentTimeMillis())
            localRepository.insertAsset(updated) // insertAsset with REPLACE strategy
            
            // Sync to cloud if logged in
            if (isLoggedIn.value) {
                repository.saveAsset(updated.toFirestore())
            }
            showMessage("Asset updated")
        }
    }

    fun deleteAsset(asset: AssetEntity) {
        viewModelScope.launch {
            // Delete locally (recursive)
            deleteAssetRecursiveLocal(asset)
            
            // Sync delete to cloud if logged in (Firestore already handles recursion)
            if (isLoggedIn.value) {
                repository.deleteAsset(asset.id)
            }
            showMessage("Asset deleted")
        }
    }

    private suspend fun deleteAssetRecursiveLocal(asset: AssetEntity) {
        // 1. Get children if it's a folder
        if (asset.type == AssetType.FOLDER) {
            val children = localRepository.getAssets(asset.id).first()
            children.forEach { child ->
                deleteAssetRecursiveLocal(child)
            }
        }
        
        // 2. Delete the asset itself
        localRepository.deleteAsset(asset)
        
        // 3. Delete associated links
        localRepository.deleteSharingLinksForAsset(asset.id)
    }

    fun moveAsset(asset: AssetEntity, newParentId: String?) {
        viewModelScope.launch {
            val updated = asset.copy(parentId = newParentId, updatedAt = System.currentTimeMillis())
            localRepository.insertAsset(updated)
            
            // Sync to cloud if logged in
            if (isLoggedIn.value) {
                repository.updateAssetFields(asset.id, mapOf("parentId" to newParentId))
            }
        }
    }

    fun duplicateAsset(asset: AssetEntity) {
        viewModelScope.launch {
            duplicateAssetRecursive(asset, asset.parentId)
        }
    }

    private suspend fun duplicateAssetRecursive(asset: AssetEntity, newParentId: String?) {
        val newId = UUID.randomUUID().toString()
        val userId = currentUserId.value ?: "current_user"
        val newAsset = asset.copy(
            id = newId,
            userId = userId,
            name = if (asset.parentId == newParentId) "${asset.name} (Copy)" else asset.name,
            parentId = newParentId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        localRepository.insertAsset(newAsset)

        // Sync to cloud if logged in
        if (isLoggedIn.value) {
            repository.saveAsset(newAsset.toFirestore())
        }
        
        if (asset.type == AssetType.FOLDER) {
            // Duplicate children from local assets
            // Fetch actual children of the folder being duplicated
            val children = localRepository.getAssets(asset.id).first()
            children.forEach { child ->
                duplicateAssetRecursive(child, newId)
            }
        }
    }

    // ==================== Link Operations ====================
    // Sharing links need cloud storage for others to access, but also save locally for user's reference
    // Returns true if the link was created successfully (user must be logged in for cloud save)
    
    fun createSharingLinkFull(
        asset: AssetEntity, 
        name: String, 
        expiryDays: Int?,
        onSuccess: ((String) -> Unit)? = null,
        onRequireLogin: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val token = UUID.randomUUID().toString().substring(0, 8)
            val expiryDate = expiryDays?.let { System.currentTimeMillis() + (it * 24 * 60 * 60 * 1000L) }
            
            val linkEntity = SharingLinkEntity(
                id = UUID.randomUUID().toString(),
                assetId = asset.id,
                token = token,
                name = name,
                expiry = expiryDate,
                status = "ACTIVE",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Save locally first
            localRepository.insertSharingLink(linkEntity)
            
            // If logged in, also save to cloud for sharing
            if (isLoggedIn.value) {
                try {
                    // Upload asset to cloud first, then link
                    val success = localRepository.shareAssetCloud(linkEntity, asset)
                    
                    // Critical: If sharing a folder, recursively upload all its contents!
                    if (success && asset.type == AssetType.FOLDER) {
                        launch(Dispatchers.IO) {
                            try {
                                syncFolderContentsToCloud(asset)
                                android.util.Log.d("LinkBoxViewModel", "Recursive folder sync completed for ${asset.name}")
                            } catch (e: Exception) {
                                android.util.Log.e("LinkBoxViewModel", "Recursive folder sync failed", e)
                            }
                        }
                    }

                    if (success) {
                        showMessage("Link created")
                        onSuccess?.invoke(token)
                    } else {
                        // Cloud save failed, but local is saved
                        onSuccess?.invoke(token)
                        android.util.Log.w("LinkBoxViewModel", "Link saved locally, cloud sync failed")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LinkBoxViewModel", "Cloud sync error", e)
                    onSuccess?.invoke(token)
                }
            } else {
                // Not logged in - prompt login for cloud sharing
                onRequireLogin?.invoke()
            }
        }
    }

    private suspend fun syncFolderContentsToCloud(folder: AssetEntity) {
        val children = localRepository.getAssets(folder.id).first()
        if (children.isNotEmpty()) {
            android.util.Log.d("LinkBoxViewModel", "Syncing ${children.size} items in folder ${folder.name}")
            children.forEach { child ->
                localRepository.saveAssetToCloud(child)
                if (child.type == AssetType.FOLDER) {
                    syncFolderContentsToCloud(child)
                }
            }
        }
    }

    fun createSharingLink(
        asset: AssetEntity, 
        name: String,
        onSuccess: ((String) -> Unit)? = null,
        onRequireLogin: (() -> Unit)? = null
    ) {
        createSharingLinkFull(asset, name, null, onSuccess, onRequireLogin)
    }

    // Update asset's point cost (local-first)
    fun updateAssetPointCost(asset: AssetEntity, newPointCost: Int) {
        viewModelScope.launch {
            val updated = asset.copy(pointCost = newPointCost, updatedAt = System.currentTimeMillis())
            localRepository.insertAsset(updated)
            
            // Sync to cloud if logged in
            if (isLoggedIn.value) {
                repository.saveAsset(updated.toFirestore())
            }
        }
    }

    fun updateSharingLink(link: SharingLinkEntity) {
        viewModelScope.launch {
            // Update locally
            val updated = link.copy(updatedAt = System.currentTimeMillis())
            localRepository.updateSharingLink(updated)
            
            // Sync to cloud if logged in
            if (app.getSavedUsername() != null) {
                try {
                    localRepository.saveLinkToCloud(updated)
                } catch (e: Exception) {
                    android.util.Log.e("LinkBoxViewModel", "Cloud sync error for link update", e)
                }
            }
            showMessage("Link updated")
        }
    }

    fun deleteSharingLink(link: SharingLinkEntity) {
        viewModelScope.launch {
            // Delete locally
            localRepository.deleteSharingLink(link)
            
            // Delete from cloud if logged in
            if (app.getSavedUsername() != null) {
                try {
                    localRepository.deleteLinkFromCloud(link.token)
                } catch (e: Exception) {
                    android.util.Log.e("LinkBoxViewModel", "Cloud delete error", e)
                }
            }
            showMessage("Link deleted")
        }
    }

    fun submitReport(token: String, reason: String, contact: String) {
        viewModelScope.launch {
            try {
                val currentUserId = userProfile.value?.remoteId ?: userProfile.value?.id ?: "anonymous"
                val report = hashMapOf<String, Any>(
                    "token" to token,
                    "reason" to reason,
                    "contact" to contact,
                    "reportedBy" to currentUserId,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "pending"
                )
                repository.submitReport(report)
            } catch (e: Exception) {
                // Handle error silently or log
            }
        }
    }

    // ==================== Shared Content ====================

    /**
 * Prefetch shared content early for faster access.
 * This is called as soon as a deep link token is detected.
 * Uses cache-first strategy for instant display.
 */
fun prefetchSharedContent(token: String) {
    if (token.isBlank()) return
    
    if (currentPrefetchToken == token && prefetchJob?.isActive == true) {
        android.util.Log.d("LinkBoxViewModel", "Prefetch: Already in progress for token: $token")
        return
    }
    
    prefetchJob?.cancel()
    currentPrefetchToken = token
    prefetchJob = viewModelScope.launch(Dispatchers.IO) {
        try {
            // Check cache first
            val cached = synchronized(sharedContentCache) { sharedContentCache[token] }
            if (cached != null) {
                android.util.Log.d("LinkBoxViewModel", "Prefetch: Using cached content for token: $token")
                // Also update the main state if it's null, for instant UI
                if (_sharedContent.value == null) {
                    _sharedContent.value = cached
                }
                return@launch
            }
            
            android.util.Log.d("LinkBoxViewModel", "Prefetch: Starting prefetch for token: $token")
            
            // Fetch from cache first for speed
            val cacheLink = repository.lookupByToken(token, com.google.firebase.firestore.Source.CACHE)
            if (cacheLink != null && cacheLink.status == "ACTIVE") {
                val cacheAsset = repository.getAssetById(cacheLink.assetId, com.google.firebase.firestore.Source.CACHE)
                if (cacheAsset != null && cacheAsset.sharingEnabled) {
                    val ownerName = getOwnerUsername(cacheAsset.ownerId, com.google.firebase.firestore.Source.CACHE)
                    val content = uiSharedContentFrom(cacheLink, cacheAsset, ownerName)
                    addToCache(token, content)
                    android.util.Log.d("LinkBoxViewModel", "Prefetch: Found in Firestore cache for token: $token")
                    if (_sharedContent.value == null) _sharedContent.value = content
                    return@launch
                }
            }

            // Then server
            val serverResult = fetchSharedContentFromServer(token)
            if (serverResult != null) {
                addToCache(token, serverResult)
                android.util.Log.d("LinkBoxViewModel", "Prefetch: Completed from server for token: $token")
                if (_sharedContent.value == null) _sharedContent.value = serverResult
            } else {
                android.util.Log.w("LinkBoxViewModel", "Prefetch: Server returned null for token: $token")
            }
        } catch (e: Exception) {
            android.util.Log.e("LinkBoxViewModel", "Prefetch failed for token: $token", e)
        } finally {
            if (currentPrefetchToken == token) {
                currentPrefetchToken = null
            }
        }
    }
}
    
    private fun addToCache(token: String, content: UiSharedContent) {
    synchronized(sharedContentCache) {
        // Move to end (most recently used) by removing and re-adding
        sharedContentCache.remove(token)
        if (sharedContentCache.size >= cacheMaxSize) {
            val firstKey = sharedContentCache.keys.firstOrNull()
            if (firstKey != null) sharedContentCache.remove(firstKey)
        }
        sharedContentCache[token] = content
    }
}

private fun uiSharedContentFrom(link: com.clicktoearn.linkbox.data.remote.FirestoreLink, asset: com.clicktoearn.linkbox.data.remote.FirestoreAsset, ownerName: String): UiSharedContent {
    return UiSharedContent(
        token = link.token,
        assetId = link.assetId,
        ownerId = asset.ownerId,
        ownerName = ownerName,
        name = asset.name,
        description = asset.description,
        assetType = asset.type,
        assetContent = asset.content,
        expiryDate = link.expiryDate,
        pointCost = asset.pointCost,
        allowSaveCopy = asset.allowSaveCopy,
        allowFurtherSharing = asset.allowFurtherSharing,
        allowScreenCapture = asset.allowScreenCapture,
        exposeUrl = asset.exposeUrl,
        chargeEveryTime = asset.chargeEveryTime,
        sharingEnabled = asset.sharingEnabled,
        isActive = link.status == "ACTIVE"
    )
}
    
    fun loadSharedContent(token: String, forceRefresh: Boolean = false) {
    if (token.isBlank()) return
    
    sharedContentJob?.cancel()
    // Set loading state immediately (synchronously) to prevent UI flicker
    _isLoadingShared.value = true
    
    sharedContentJob = viewModelScope.launch {
        android.util.Log.d("LinkBoxViewModel", "loadSharedContent: token=$token, forceRefresh=$forceRefresh")
        
        // Clear previous content if loading a NEW token to avoid flicker
        if (_sharedContent.value?.token != token) {
            _sharedContent.value = null
        }
        if (!forceRefresh && currentPrefetchToken == token) {
            android.util.Log.d("LinkBoxViewModel", "loadSharedContent: Waiting for prefetch to complete...")
            try {
                prefetchJob?.join()
                android.util.Log.d("LinkBoxViewModel", "loadSharedContent: Prefetch joined and completed")
            } catch (e: Exception) {
                android.util.Log.w("LinkBoxViewModel", "loadSharedContent: Prefetch join failed", e)
            }
        }
        
        // Check cache first for instant display
        if (!forceRefresh) {
            val cached: UiSharedContent? = synchronized(sharedContentCache) {
                sharedContentCache[token]
            }
            if (cached != null) {
                android.util.Log.d("LinkBoxViewModel", "loadSharedContent: Found in memory cache for token: $token")
                _sharedContent.value = cached
                _isLoadingShared.value = false
                
                // Save to history in background
                launch { saveLocalHistory(token) }
                
                // Refresh from server in background to ensure data is fresh
                launch(Dispatchers.IO) {
                    try {
                        val freshContent = fetchSharedContentFromServer(token)
                        if (freshContent != null) {
                            addToCache(token, freshContent)
                            _sharedContent.value = freshContent
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("LinkBoxViewModel", "Background refresh failed", e)
                    }
                }
                return@launch
            }
        }
        
        if (!forceRefresh) {
            val cacheResult = fetchSharedContentFromCache(token)
            if (cacheResult != null) {
                android.util.Log.d("LinkBoxViewModel", "loadSharedContent: Found in Firestore disk cache for token: $token")
                _sharedContent.value = cacheResult
                addToCache(token, cacheResult)
                _isLoadingShared.value = false
                
                // Save to history in background
                launch { saveLocalHistory(token) }
                
                // Refresh from server in background
                launch(Dispatchers.IO) {
                    try {
                        val freshContent = fetchSharedContentFromServer(token)
                        if (freshContent != null) {
                            addToCache(token, freshContent)
                            _sharedContent.value = freshContent
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("LinkBoxViewModel", "Background refresh failed", e)
                    }
                }
                return@launch
            }
        }
        
        // Fallback to server fetch (or primary fetch if forced)
        android.util.Log.d("LinkBoxViewModel", "loadSharedContent: Fetching from server for token: $token")
        val serverResult = try {
            withTimeout(15000L) {
                fetchSharedContentFromServer(token)
            }
        } catch (e: Exception) {
            android.util.Log.e("LinkBoxViewModel", "loadSharedContent server fetch timed out/failed", e)
            null
        }
        
        if (serverResult != null) {
            android.util.Log.d("LinkBoxViewModel", "loadSharedContent: Server fetch success for token: $token")
            _sharedContent.value = serverResult
            addToCache(token, serverResult)
            saveLocalHistory(token)
        } else {
            android.util.Log.w("LinkBoxViewModel", "loadSharedContent: Server fetch returned null for token: $token")
            // ONLY set to null if we don't have ANY content (even from cache/prefetch)
            if (_sharedContent.value == null) {
                _sharedContent.value = null
            }
        }
        
        _isLoadingShared.value = false
    }
}
    
    /**
     * Helper function to get owner username efficiently.
     * Checks if owner is current user first to avoid unnecessary Firestore call.
     */
    private suspend fun getOwnerUsername(ownerId: String, source: com.google.firebase.firestore.Source? = null): String {
        if (ownerId.isBlank()) return "Unknown"

        // Check if it's the current user
        val currentUser = repository.userProfile.value
        if (currentUser != null && currentUser.userId == ownerId) {
            android.util.Log.d("LinkBoxViewModel", "getOwnerUsername: Owner is current user: ${currentUser.username}")
            return currentUser.username.ifBlank { currentUser.email.substringBefore("@").ifBlank { "Me" } }
        }
        
        // Otherwise fetch from Firestore
        var owner = repository.fetchUserById(ownerId, source)
        
        // Fallback: ownerId might be a username (legacy data support)
        // avoid network calls if we are strictly checking cache
        if (owner == null && source != com.google.firebase.firestore.Source.CACHE) {
            android.util.Log.w("LinkBoxViewModel", "getOwnerUsername: fetchUserById failed for $ownerId, trying findUserByUsername")
            owner = repository.findUserByUsername(ownerId)
        }
        
        return owner?.let { 
            it.username.ifBlank { it.email.substringBefore("@").ifBlank { "Unknown" } }
        } ?: "Unknown"
    }

    private suspend fun fetchSharedContentFromCache(token: String): UiSharedContent? {
        if (token == "sample_google") {
            return UiSharedContent(
                token = "sample_google",
                assetId = "sample_google_asset",
                ownerId = "linkbox_official",
                ownerName = "LinkBox",
                name = "Google",
                description = "Search the world's information, including webpages, images, videos and more.",
                assetType = AssetType.LINK.name,
                assetContent = "https://www.google.com",
                expiryDate = null,
                pointCost = 0,
                allowSaveCopy = false,
                allowFurtherSharing = false,
                allowScreenCapture = true,
                exposeUrl = true,
                chargeEveryTime = false,
                sharingEnabled = true,
                isActive = true
            )
        }
        return try {
            val link = repository.lookupByToken(token, com.google.firebase.firestore.Source.CACHE) ?: return null
            if (link.status != "ACTIVE") return null
            val isExpired = link.expiryDate?.let { it < System.currentTimeMillis() } ?: false
            if (isExpired) return null
            
            val asset = repository.getAssetById(link.assetId, com.google.firebase.firestore.Source.CACHE) ?: return null
            if (!asset.sharingEnabled) return null
            
            android.util.Log.d("LinkBoxViewModel", "fetchSharedContentFromCache: Fetching owner for asset.ownerId: ${asset.ownerId}")
            // Use CACHE source to avoid blocking on network
            val ownerName = getOwnerUsername(asset.ownerId, com.google.firebase.firestore.Source.CACHE)
            android.util.Log.d("LinkBoxViewModel", "fetchSharedContentFromCache: Owner username: $ownerName")
            
            return uiSharedContentFrom(link, asset, ownerName)
        } catch (e: Exception) {
            android.util.Log.e("LinkBoxViewModel", "fetchSharedContentFromCache: Error", e)
            null
        }
    }
    
    private suspend fun fetchSharedContentFromServer(token: String): UiSharedContent? {
        if (token == "sample_google") return fetchSharedContentFromCache(token)
        return try {
            val link = repository.lookupByToken(token, com.google.firebase.firestore.Source.SERVER) ?: return null
            if (link.status != "ACTIVE") return null
            val isExpired = link.expiryDate?.let { it < System.currentTimeMillis() } ?: false
            if (isExpired) return null
            
            val asset = repository.getAssetById(link.assetId, com.google.firebase.firestore.Source.SERVER) ?: return null
            if (!asset.sharingEnabled) return null
            
            android.util.Log.d("LinkBoxViewModel", "fetchSharedContentFromServer: Fetching owner for asset.ownerId: ${asset.ownerId}")
            val ownerName = getOwnerUsername(asset.ownerId)
            android.util.Log.d("LinkBoxViewModel", "fetchSharedContentFromServer: Owner fetched - username: $ownerName")
            
            return uiSharedContentFrom(link, asset, ownerName)
        } catch (e: Exception) {
            android.util.Log.e("LinkBoxViewModel", "fetchSharedContentFromServer: Error", e)
            null
        }
    }

    // Manual refresh for shared content
    fun refreshSharedContent(token: String) {
        loadSharedContent(token, forceRefresh = true)
    }

    // ==================== History Operations ====================
    fun deleteHistory(item: UiHistoryItem) {
        viewModelScope.launch {
            // Delete locally
            localRepository.deleteHistory(HistoryEntity(item.token))
            // Delete from cloud
            repository.deleteHistory(item.token)
        }
    }

    fun deleteHistoryItems(items: List<UiHistoryItem>) {
        viewModelScope.launch {
            items.forEach { item ->
                localRepository.deleteHistory(HistoryEntity(item.token))
                repository.deleteHistory(item.token)
            }
        }
    }

    fun updateHistory(item: UiHistoryItem) {
        // Optimistic UI update
        val currentHistory = _historyItems.value.toMutableList()
        val index = currentHistory.indexOfFirst { it.token == item.token }
        if (index >= 0) {
            currentHistory[index] = item
            _historyItems.value = currentHistory
        }

        viewModelScope.launch {
            val entity = HistoryEntity(
                token = item.token,
                accessedAt = item.accessedAt,
                isStarred = item.isStarred,
                isPaid = item.isPaid
            )
            localRepository.updateHistory(entity)
            
            val success = repository.updateHistoryStarred(item.token, item.isStarred)
            if (!success) refreshHistory(isManual = false)
        }
    }

    fun markAsPaid(token: String) {
        viewModelScope.launch {
            repository.updateHistoryPaidStatus(token, true)
            // Stream will update UI
        }
    }

    fun toggleHistoryStarred(item: UiHistoryItem) {
        viewModelScope.launch {
            val newStarred = !item.isStarred
            
            // Update locally
            val entity = HistoryEntity(
                token = item.token,
                accessedAt = item.accessedAt,
                isStarred = newStarred,
                isPaid = item.isPaid
            )
            localRepository.updateHistory(entity)
            
            // Update cloud
            repository.updateHistoryStarred(item.token, newStarred)
        }
    }

    /**
     * Toggle star status by token - works even if no history item exists yet.
     * Returns the new starred status.
     */
    fun toggleStarByToken(token: String, currentIsStarred: Boolean): Boolean {
        val newStarred = !currentIsStarred
        
        // Optimistic UI update for immediate feedback
        val currentHistory = _historyItems.value.toMutableList()
        val index = currentHistory.indexOfFirst { it.token == token }
        if (index >= 0) {
            currentHistory[index] = currentHistory[index].copy(isStarred = newStarred)
            _historyItems.value = currentHistory
        }

        viewModelScope.launch {
            // Update locally first
            val currentInRoom = localRepository.getHistory().first().find { it.token == token }
            if (currentInRoom != null) {
                localRepository.updateHistory(currentInRoom.copy(isStarred = newStarred))
            } else {
                // Should not happen as history entry should exist
                localRepository.insertHistory(HistoryEntity(token = token, isStarred = newStarred))
            }

            val success = repository.updateHistoryStarred(token, newStarred)
            android.util.Log.d("LinkBoxViewModel", "toggleStarByToken: token=$token, success=$success, newState=$newStarred")
            if (!success) {
                // Revert on failure
                refreshHistory(isManual = false)
            }
        }
        return newStarred
    }

    // ==================== Points ====================
    fun spendPoints(amount: Int) {
        viewModelScope.launch {
            val current = repository.userProfile.value?.points ?: 0
            if (current >= amount) {
                repository.updateUserPoints(current - amount)
            }
        }
    }

    fun payForAccess(amount: Int, ownerId: String, contentName: String) {
        viewModelScope.launch {
            val user = repository.userProfile.value
            val currentPoints = user?.points ?: 0
            if (currentPoints >= amount) {
                val ownerShare = (amount * 0.40).toInt()
                val payerId = user?.userId ?: "anonymous"
                
                val success = repository.processContentUnlock(payerId, ownerId, amount, ownerShare, contentName)
                
                if (success) {
                    showMessage("Content unlocked")
                } else {
                    showMessage("Failed to unlock content. Please try again.")
                }
            } else {
                showMessage("Insufficient points")
            }
        }
    }

    fun earnPoints(amount: Int) {
        viewModelScope.launch {
            // Update local cache first (for immediate UI and guests)
            val newPoints = _cachedPoints.value + amount
            _cachedPoints.value = newPoints
            app.saveCachedPoints(newPoints)
            
            // If logged in, also sync to cloud
            if (repository.userProfile.value != null) {
                repository.updateUserPoints(newPoints)
            }
            showMessage("You earned $amount points!")
        }
    }

    fun isOwner(ownerId: String): Boolean {
        return currentUserId.value == ownerId
    }

    fun getEffectiveCost(linkPointCost: Int, ownerId: String): Int {
        return if (isOwner(ownerId)) 0 else maxOf(0, linkPointCost)
    }

    // ==================== Auth ====================

    fun signInWithGoogle(credential: com.google.firebase.auth.AuthCredential, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // Store local points before sign in for migration (captured early)
                val localPoints = app.getCachedPoints()
                val beforeIsLoggedIn = isLoggedIn.value
                
                // Fast path: Only timeout the critical auth step (15s instead of 30s)
                val result = withTimeout(15000L) {
                    repository.signInWithGoogle(credential)
                }
                
                if (result.isSuccess) {
                    val user = repository.userProfile.value
                    if (user != null) {
                        app.saveUsername(user.username)
                    }
                    
                    // SUCCESS! Return immediately for fast UX
                    android.util.Log.d("LinkBoxViewModel", "signInWithGoogle: Auth successful, returning immediately")
                    onResult(true, null)
                    
                    // Background sync operations (non-blocking)
                    launch {
                        try {
                            // Migrate points if user was previously logged out/guest and has local points
                            if (!beforeIsLoggedIn && localPoints > 0 && user != null) {
                                android.util.Log.d("LinkBoxViewModel", "Background: Migrating $localPoints local points to account ${user.userId}")
                                repository.incrementUserPoints(user.userId, localPoints)
                                // Clear local cache after migration to avoid double counting
                                app.saveCachedPoints(0)
                                _cachedPoints.value = 0
                            }
                            
                            // Sync cloud data to local DB via refreshAll (in background)
                            android.util.Log.d("LinkBoxViewModel", "Background: Starting refreshAll sync")
                            refreshAll(isManual = false)
                        } catch (e: Exception) {
                            android.util.Log.e("LinkBoxViewModel", "Background sync after login failed", e)
                            // Don't affect login success - sync will retry on next app open
                        }
                    }
                } else {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                    onResult(false, errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = if (e is kotlinx.coroutines.TimeoutCancellationException) {
                    "Authentication timed out. Please check your internet connection."
                } else {
                    e.localizedMessage ?: "Sign-in failed"
                }
                android.util.Log.e("LinkBoxViewModel", "signInWithGoogle failed or timed out", e)
                onResult(false, errorMsg)
            }
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            val success = repository.updateUsername(newUsername)
            if (success) {
                // Update saved username
                app.saveUsername(newUsername)
            }
        }
    }
   
    // Use an application context when creating ViewModel in a real app, 
    // or pass BillingManager in constructor. For now, we assume we can get context or pass activity.
    // Ideally, BillingManager should be a singleton injected here.
    // We will initialize it lazily or through a method call from MainActivity/App.
    private var _billingManager: com.clicktoearn.linkbox.billing.BillingManager? = null
    val billingProductDetails = MutableStateFlow<Map<String, com.android.billingclient.api.ProductDetails>>(emptyMap())

    fun initializeBilling(context: android.content.Context) {
        if (_billingManager == null) {
            _billingManager = com.clicktoearn.linkbox.billing.BillingManager(context) { purchase ->
                // Handle successful purchase
                viewModelScope.launch {
                    purchase.products.forEach { productId ->
                        when (productId) {
                            com.clicktoearn.linkbox.billing.BillingManager.PRODUCT_PREMIUM_WEEKLY -> {
                                if (repository.userProfile.value != null) {
                                    repository.processSubscription(0, 7) // 0 cost as it's real money, 7 days
                                    refreshAll(isManual = false)
                                } else {
                                    showMessage("Please login to activate Premium")
                                }
                            }
                            com.clicktoearn.linkbox.billing.BillingManager.PRODUCT_POINTS_100 -> {
                                earnPoints(100)
                            }
                            com.clicktoearn.linkbox.billing.BillingManager.PRODUCT_POINTS_350 -> {
                                earnPoints(350)
                            }
                            com.clicktoearn.linkbox.billing.BillingManager.PRODUCT_POINTS_1000 -> {
                                earnPoints(1000)
                            }
                        }
                    }
                }
            }
            _billingManager?.startConnection()
            
            viewModelScope.launch {
                _billingManager?.productDetails?.collect {
                    billingProductDetails.value = it
                }
            }
        }
    }
    
    fun buyPremium(activity: android.app.Activity) {
        val manager = _billingManager
        if (manager == null) {
            showMessage("Billing not initialized")
            return
        }
        if (!manager.isConnected.value) {
            showMessage("Connecting to Google Play... please try again")
            manager.startConnection()
            return
        }
        
        val productId = com.clicktoearn.linkbox.billing.BillingManager.PRODUCT_PREMIUM_WEEKLY
        val details = manager.productDetails.value[productId]
        
        if (details != null) {
            manager.launchPurchaseFlow(activity, productId)
        } else {
            showMessage("Premium option not available for this account")
            android.util.Log.e("LinkBoxViewModel", "Product details not found for: $productId")
        }
    }

    fun buyPoints(activity: android.app.Activity, points: Int) {
        val manager = _billingManager
        if (manager == null) {
            showMessage("Billing not initialized")
            return
        }
        if (!manager.isConnected.value) {
            showMessage("Connecting to Google Play... please try again")
            manager.startConnection()
            return
        }

        val productId = when (points) {
            100 -> com.clicktoearn.linkbox.billing.BillingManager.PRODUCT_POINTS_100
            350 -> com.clicktoearn.linkbox.billing.BillingManager.PRODUCT_POINTS_350
            1000 -> com.clicktoearn.linkbox.billing.BillingManager.PRODUCT_POINTS_1000
            else -> null
        }
        
        if (productId != null) {
            val details = manager.productDetails.value[productId]
            if (details != null) {
                manager.launchPurchaseFlow(activity, productId)
            } else {
                showMessage("$points Points option not available")
                android.util.Log.e("LinkBoxViewModel", "Product details not found for: $productId")
            }
        } else {
            showMessage("Invalid point amount")
        }
    }

    fun logout(context: android.content.Context) {
        viewModelScope.launch {
            android.util.Log.d("LinkBoxViewModel", "Logout initiated. Suspending background syncs...")
            
            // 1. Clear local DB (Room) - This is the primary source for Assets, Links, and History
            localRepository.clearAllLocalData()
            
            // 2. Clear Cloud Cache (FirestoreRepository)
            // This clears the MutableStateFlows (_assets, _links, _history, _userProfile)
            repository.signOut()
            
            // 3. Clear Google Sign-In
            try {
                @Suppress("DEPRECATION")
                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                @Suppress("DEPRECATION")
                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                googleSignInClient.signOut()
            } catch (e: Exception) {
                android.util.Log.e("LinkBoxViewModel", "Google SignOut failed", e)
            }
            
            // 4. Clear Preferences and Reset Session Flags
            app.clearSavedUsername()
            app.clearCachedPoints()
            app.resetSessionFlags()
            
            // 5. Reset internal UI states immediately
            _cachedPoints.value = 0
            _historyItems.value = emptyList()
            _sharedContent.value = null
            _currentFolderId.value = null
            _isRefreshing.value = false
            
            // 6. Mandatory delay to ensure Room transactions and Flow propagations are complete
            // This prevents injectSampleData from seeing "old" data if the DB hasn't finished clearing
            kotlinx.coroutines.delay(300)
            
            // 7. Re-inject sample/intro data for the Guest experience
            injectSampleData()
            
            android.util.Log.d("LinkBoxViewModel", "Logout process complete. User is now a Guest.")
        }
    }

    // ==================== Shared Folder/File Access ====================
    // ==================== Shared Folder/File Access ====================
    suspend fun getCloudAsset(assetId: String): FirestoreAsset? {
        // Safe with timeout
        return try {
            withTimeout(15000L) {
                repository.getAssetById(assetId)
            }
        } catch (e: Exception) {
            android.util.Log.e("LinkBoxViewModel", "getCloudAsset failed/timeout", e)
            null
        }
    }

    suspend fun getSharedFolderContents(ownerId: String, folderId: String): List<FirestoreAsset>? {
        return try {
            android.util.Log.d("LinkBoxViewModel", "getSharedFolderContents: Fetching contents for folder=$folderId, owner=$ownerId")
            
            withTimeout(15000L) {
                val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("assets")
                    .whereEqualTo("ownerId", ownerId)
                    .whereEqualTo("parentId", folderId)
                    .get().await()
                
                val allAssets = snapshot.toObjects(FirestoreAsset::class.java)
                android.util.Log.d("LinkBoxViewModel", "getSharedFolderContents: Found ${allAssets.size} total assets")
                
                // Filter to only include assets with sharing enabled
                val sharedAssets = allAssets.filter { it.sharingEnabled }
                android.util.Log.d("LinkBoxViewModel", "getSharedFolderContents: ${sharedAssets.size} assets with sharing enabled")
                
                sharedAssets
            }
        } catch (e: Exception) {
            android.util.Log.e("LinkBoxViewModel", "getSharedFolderContents failed/timeout for folder=$folderId", e)
            null
        }
    }

    // ==================== Premium (stub) ====================
    // ==================== Premium & Account Linking ====================
    fun subscribe(useCoins: Boolean) {
        if (!useCoins) {
            // Money subscription not implemented yet
            return
        }
        
        viewModelScope.launch {
            // Cost: 2000 points, Duration: 7 days
            val success = repository.processSubscription(2000, 7)
            if (success) {
                refreshAll(isManual = false)
                showMessage("Subscription successful!")
            } else {
                showMessage("Failed to subscribe. Insufficient points?")
            }
        }
    }

    
    // ==================== Referral Tracking ====================
    
    /**
     * Tracks a referral when user accesses content via a token
     * @param token The sharing link token
     * @param isNewInstall True if this is a new app install
     */
    fun trackReferral(token: String, isNewInstall: Boolean) {
        viewModelScope.launch {
            repository.trackReferral(token, isNewInstall)
        }
    }
}
