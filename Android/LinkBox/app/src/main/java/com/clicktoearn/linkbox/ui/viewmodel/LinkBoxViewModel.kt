package com.clicktoearn.linkbox.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.clicktoearn.linkbox.data.database.AppDatabase
import com.clicktoearn.linkbox.data.dao.EntityWithSharing
import com.clicktoearn.linkbox.data.entity.*
import com.clicktoearn.linkbox.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.clicktoearn.linkbox.data.remote.model.*

/**
 * Main ViewModel for the LinkBox application.
 * 
 * Provides a clean API for UI to interact with the data layer.
 * Manages:
 * - Entity (Links, Pages, Folders) operations
 * - Sharing links
 * - Joined links
 * - User points system
 * - User profile
 * - App settings and subscription
 * 
 * All repositories are initialized from the Room database.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LinkBoxViewModel(application: Application) : AndroidViewModel(application) {
    
    // ==================== REPOSITORIES ====================
    
    private val database = AppDatabase.getDatabase(application)
    
    private val userProfileRepository = UserProfileRepository(database.userProfileDao())
    private val appSettingsRepository = AppSettingsRepository(database.appSettingsDao())
    private val analyticsRepository = AnalyticsRepository(database.analyticsDao())
    private val firebaseAuthRepository = FirebaseAuthRepository()
    private val firestoreRepository = FirestoreRepository()

    private val linkBoxRepository = LinkBoxRepository(database.linkBoxDao(), firestoreRepository)
    private val dataMigrationManager = com.clicktoearn.linkbox.data.manager.DataMigrationManager(linkBoxRepository, firestoreRepository)
    
    // Backward compatibility - keep PreferencesRepository for migration
    private val preferencesRepository = PreferencesRepository(application)
    
    // ==================== STATE ====================
    
    private val _currentOwnerId = MutableStateFlow<Long?>(null)
    val currentOwnerId = _currentOwnerId.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId = _currentFolderId.asStateFlow()

    private val _currentFolderName = MutableStateFlow("My Workspace")
    val currentFolderName = _currentFolderName.asStateFlow()
    
    // Error state for UI feedback
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // ==================== USER PROFILE STATE ====================
    
    val userProfile = userProfileRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val userName = userProfileRepository.getUserName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")
    
    val userEmail = userProfileRepository.getUserEmail()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    
    // ==================== APP SETTINGS STATE ====================
    
    val isDarkMode = appSettingsRepository.isDarkMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val isNotificationsEnabled = appSettingsRepository.isNotificationsEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    
    val appSettings = appSettingsRepository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // ==================== SUBSCRIPTION STATE ====================
    
    val isSubscriptionActive = appSettingsRepository.isSubscriptionActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val subscriptionExpiryTime = appSettingsRepository.getSubscriptionExpiry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val subscriptionRemainingTime = appSettingsRepository.getSubscriptionRemainingTime()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    
    private val _showSubscriptionDialog = MutableStateFlow(false)
    val showSubscriptionDialog = _showSubscriptionDialog.asStateFlow()
    
    // ==================== ENTITIES STATE ====================
    
    private val _myEntitiesSearchQuery = MutableStateFlow("")
    val myEntitiesSearchQuery = _myEntitiesSearchQuery.asStateFlow()

    private val _myEntitiesSortOption = MutableStateFlow(SortOption.NAME)
    val myEntitiesSortOption = _myEntitiesSortOption.asStateFlow()

    private val _myEntitiesSortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val myEntitiesSortDirection = _myEntitiesSortDirection.asStateFlow()

    private val _myEntitiesEntityTypeFilter = MutableStateFlow<EntityType?>(null)
    val myEntitiesEntityTypeFilter = _myEntitiesEntityTypeFilter.asStateFlow()

    private val _myEntitiesShowOnlyStarred = MutableStateFlow(false)
    val myEntitiesShowOnlyStarred = _myEntitiesShowOnlyStarred.asStateFlow()

    val myEntities: StateFlow<List<EntityWithSharing>> = combine(
        combine(_currentOwnerId, _currentFolderId, _myEntitiesShowOnlyStarred) { ownerId, folderId, showStarred -> 
            Triple(ownerId, folderId, showStarred) 
        },
        _myEntitiesSearchQuery,
        combine(_myEntitiesSortOption, _myEntitiesSortDirection, _myEntitiesEntityTypeFilter) { option, direction, type -> 
            Triple(option, direction, type) 
        }
    ) { dataTriple, query, triple ->
        DataState(dataTriple.first, dataTriple.second, dataTriple.third, query, triple.first, triple.second, triple.third)
    }.flatMapLatest { state ->
        if (state.ownerId != null) {
            linkBoxRepository.getEntitiesWithSharing(state.ownerId).map { items ->
                var filtered = items.filter { it.entity.parentId == state.folderId }
                
                if (state.showStarred) {
                    filtered = filtered.filter { it.entity.isPinned }
                }

                if (state.filterType != null) {
                    filtered = filtered.filter { it.entity.type == state.filterType }
                }
                
                if (state.query.isNotBlank()) {
                    filtered = filtered.filter { 
                        it.entity.name.contains(state.query, ignoreCase = true) ||
                        it.entity.value?.contains(state.query, ignoreCase = true) == true
                    }
                }

                when (state.sortOption) {
                    SortOption.NAME -> {
                        filtered = if (state.sortDirection == SortDirection.ASCENDING) {
                            filtered.sortedBy { it.entity.name.lowercase() }
                        } else {
                            filtered.sortedByDescending { it.entity.name.lowercase() }
                        }
                    }
                    SortOption.CREATED -> {
                        filtered = if (state.sortDirection == SortDirection.ASCENDING) {
                            filtered.sortedBy { it.entity.createdAt }
                        } else {
                            filtered.sortedByDescending { it.entity.createdAt }
                        }
                    }
                    SortOption.MODIFIED -> {
                        filtered = if (state.sortDirection == SortDirection.ASCENDING) {
                            filtered.sortedBy { it.entity.updatedAt }
                        } else {
                            filtered.sortedByDescending { it.entity.updatedAt }
                        }
                    }
                    SortOption.OPENED -> {
                        // For entities, we might not have 'last opened' yet, but we can treat modified as opened for now if not tracked separately
                        // Looking at EntityItem, it doesn't have lastOpened. Let's use updatedAt.
                        filtered = if (state.sortDirection == SortDirection.ASCENDING) {
                            filtered.sortedBy { it.entity.updatedAt }
                        } else {
                            filtered.sortedByDescending { it.entity.updatedAt }
                        }
                    }
                }
                filtered
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<EntityItem>> = _currentOwnerId.flatMapLatest { ownerId ->
        if (ownerId != null) {
            linkBoxRepository.getEntitiesWithSharing(ownerId).map { items ->
                items.filter { it.entity.type == EntityType.FOLDER }.map { it.entity }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val allSharings = linkBoxRepository.getAllSharings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // ==================== JOINED LINKS STATE ====================
    
    private val _joinedLinksSearchQuery = MutableStateFlow("")
    val joinedLinksSearchQuery = _joinedLinksSearchQuery.asStateFlow()
    
    private val _joinedLinksSortOption = MutableStateFlow(SortOption.NAME)
    val joinedLinksSortOption = _joinedLinksSortOption.asStateFlow()

    private val _joinedLinksEntityTypeFilter = MutableStateFlow<EntityType?>(null)
    val joinedLinksEntityTypeFilter = _joinedLinksEntityTypeFilter.asStateFlow()

    private val _joinedLinksSortDirection = MutableStateFlow(SortDirection.ASCENDING)
    val joinedLinksSortDirection = _joinedLinksSortDirection.asStateFlow()

    private val _joinedLinksShowOnlyStarred = MutableStateFlow(false)
    val joinedLinksShowOnlyStarred = _joinedLinksShowOnlyStarred.asStateFlow()

    fun setJoinedLinksFilter(type: EntityType?) {
        _joinedLinksEntityTypeFilter.value = type
    }
    
    val joinedLinks: StateFlow<List<JoinedLinkEntity>> = combine(
        _joinedLinksSearchQuery,
        _joinedLinksShowOnlyStarred,
        _joinedLinksSortOption,
        _joinedLinksSortDirection,
        _joinedLinksEntityTypeFilter
    ) { query, showStarred, sortOption, sortDirection, typeFilter ->
        JoinedDataState(query, showStarred, sortOption, sortDirection, typeFilter)
    }.flatMapLatest { state ->
        val baseFlow = if (state.showStarred) {
            linkBoxRepository.getStarredJoinedLinks()
        } else if (state.query.isBlank()) {
            linkBoxRepository.getAllJoinedLinks()
        } else {
            linkBoxRepository.searchJoinedLinks(state.query)
        }
        baseFlow.map { items ->
            var filtered = items
            
            // Apply Entity Type Filter
            if (state.filterType != null) {
                filtered = filtered.filter { it.type == state.filterType }
            }

            var sorted = filtered
            when (state.sortOption) {
                SortOption.NAME -> {
                    sorted = if (state.sortDirection == SortDirection.ASCENDING) {
                        sorted.sortedBy { it.name.lowercase() }
                    } else {
                        sorted.sortedByDescending { it.name.lowercase() }
                    }
                }
                SortOption.CREATED -> {
                    sorted = if (state.sortDirection == SortDirection.ASCENDING) {
                        sorted.sortedBy { it.firstAccessTime }
                    } else {
                        sorted.sortedByDescending { it.firstAccessTime }
                    }
                }
                SortOption.MODIFIED -> {
                    sorted = if (state.sortDirection == SortDirection.ASCENDING) {
                        sorted.sortedBy { it.lastUpdatedTime }
                    } else {
                        sorted.sortedByDescending { it.lastUpdatedTime }
                    }
                }
                SortOption.OPENED -> {
                    sorted = if (state.sortDirection == SortDirection.ASCENDING) {
                        sorted.sortedBy { it.lastAccessTime }
                    } else {
                        sorted.sortedByDescending { it.lastAccessTime }
                    }
                }
            }
            sorted
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // ==================== USER POINTS STATE ====================
    
    val userPoints = linkBoxRepository.getUserPoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val pointTransactions = linkBoxRepository.getAllPointTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _showPointsShop = MutableStateFlow(false)
    val showPointsShop = _showPointsShop.asStateFlow()

    private val _showInsufficientPointsDialog = MutableStateFlow(false)
    val showInsufficientPointsDialog = _showInsufficientPointsDialog.asStateFlow()

    private val _pointsNeeded = MutableStateFlow(0)
    val pointsNeeded = _pointsNeeded.asStateFlow()

    // ==================== INITIALIZATION ====================
    
    init {
        viewModelScope.launch {
            try {
                // Initialize default owner
                val existingOwner = linkBoxRepository.getFirstOwner()
                if (existingOwner != null) {
                    _currentOwnerId.value = existingOwner.id
                } else {
                    val defaultOwner = OwnerEntity(
                        name = "",
                        email = "",
                        phone = "",
                        password = ""
                    )
                    val id = linkBoxRepository.createOwner(defaultOwner)
                    _currentOwnerId.value = id
                }
                
                // Initialize user points
                linkBoxRepository.initializeUserPoints()
                
                // Initialize settings (database callback handles defaults)
                appSettingsRepository.initializeSettings()
                
                // Initialize user profile
                userProfileRepository.initializeProfile()

                // Check Firebase Auth and sign in anonymously if needed
                if (firebaseAuthRepository.getCurrentUser() == null) {
                    signInAnonymously(
                        onSuccess = { /* Silent success */ },
                        onError = { error -> _errorMessage.value = "Auth failed: $error" }
                    )
                } else {
                     // Ensure local owner exists for current user
                     val user = firebaseAuthRepository.getCurrentUser()
                     if (user != null) {
                         val ownerId = ensureOwnerExists(user)
                         _currentOwnerId.value = ownerId
                     }
                }

                // Add sample data if first time
                // Add sample data if first time - REMOVED
                if (existingOwner == null) {
                    // No sample data to add
                }
                
                _isInitialized.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to initialize app: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }

    // ==================== USER PROFILE OPERATIONS ====================
    
    fun updateUserProfile(name: String, email: String, phone: String? = null) {
        viewModelScope.launch {
            try {
                userProfileRepository.updateProfileInfo(name, email, phone)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update profile: ${e.message}"
            }
        }
    }
    
    fun updateUserName(name: String) {
        viewModelScope.launch {
            userProfileRepository.updateName(name)
        }
    }
    
    fun updateUserEmail(email: String) {
        viewModelScope.launch {
            userProfileRepository.updateEmail(email)
        }
    }
    
    // ==================== SETTINGS OPERATIONS ====================
    
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setDarkMode(enabled)
        }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setNotificationsEnabled(enabled)
        }
    }
    
    fun setThemeColor(color: String) {
        viewModelScope.launch {
            appSettingsRepository.setThemeColor(color)
        }
    }
    
    fun setDefaultLinkPrivacy(privacy: PrivacyType) {
        viewModelScope.launch {
            appSettingsRepository.setDefaultLinkPrivacy(privacy)
        }
    }
    
    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setAutoSyncEnabled(enabled)
        }
    }

    // ==================== ENTITY OPERATIONS ====================

    suspend fun getEntity(id: Long): EntityItem? = linkBoxRepository.getEntity(id)

    fun updateEntityContent(id: Long, name: String, content: String) {
        viewModelScope.launch {
            try {
                val entity = linkBoxRepository.getEntity(id) ?: return@launch
                linkBoxRepository.updateEntity(
                    entity.copy(
                        name = name, 
                        value = content, 
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update entity: ${e.message}"
            }
        }
    }

    fun moveEntity(entityId: Long, newParentId: Long?) {
        viewModelScope.launch {
            try {
                val entity = linkBoxRepository.getEntity(entityId) ?: return@launch
                linkBoxRepository.updateEntity(
                    entity.copy(
                        parentId = newParentId, 
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to move entity: ${e.message}"
            }
        }
    }

    fun addEntity(name: String, type: EntityType, value: String? = null, parentId: Long? = null) {
        viewModelScope.launch {
            try {
                _isInitialized.first { it }
                val ownerId = _currentOwnerId.value ?: return@launch
                
                val entityId = linkBoxRepository.createEntity(
                    EntityItem(
                        ownerId = ownerId,
                        type = type,
                        name = name,
                        parentId = parentId,
                        value = value
                    )
                )
                
                // Automatically create sharing for links
                if (type == EntityType.LINK) {
                    val name = userName.value
                    linkBoxRepository.createSharing(
                        SharingEntity(
                            entityId = entityId,
                            privacy = PrivacyType.PUBLIC,
                            token = java.util.UUID.randomUUID().toString().take(8)
                        ),
                        name
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create entity: ${e.message}"
            }
        }
    }

    fun deleteEntity(entity: EntityItem) {
        viewModelScope.launch {
            try {
                linkBoxRepository.deleteEntity(entity)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete entity: ${e.message}"
            }
        }
    }

    fun navigateToFolder(folder: EntityItem?) {
        _currentFolderId.value = folder?.id
        _currentFolderName.value = folder?.name ?: "My Workspace"
    }

    fun toggleEntityPin(entityId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                val entity = linkBoxRepository.getEntity(entityId) ?: return@launch
                linkBoxRepository.updateEntity(
                    entity.copy(
                        isPinned = !isPinned, 
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update star: ${e.message}"
            }
        }
    }
    
    fun renameEntity(entityId: Long, newName: String) {
        viewModelScope.launch {
            try {
                val entity = linkBoxRepository.getEntity(entityId) ?: return@launch
                linkBoxRepository.updateEntity(
                    entity.copy(
                        name = newName, 
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to rename entity: ${e.message}"
            }
        }
    }
    
    /**
     * Duplicates an entity (from shared content) to the current user's workspace.
     * Creates a new entity with the same content but owned by the current user.
     */
    fun duplicateEntity(sourceEntity: EntityItem) {
        viewModelScope.launch {
            try {
                _isInitialized.first { it }
                val ownerId = _currentOwnerId.value ?: return@launch
                
                // Create a copy with new ownership and ID
                val newEntity = sourceEntity.copy(
                    id = 0, // Auto-generate new ID
                    ownerId = ownerId,
                    parentId = null, // Save to root folder
                    name = "${sourceEntity.name} (Copy)",
                    isPinned = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                linkBoxRepository.createEntity(newEntity)
                _errorMessage.value = "Content saved to your workspace!"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save copy: ${e.message}"
            }
        }
    }
    
    fun navigateUp() {
        val currentId = _currentFolderId.value ?: return
        viewModelScope.launch {
            val folder = linkBoxRepository.getEntity(currentId)
            val parent = folder?.parentId?.let { linkBoxRepository.getEntity(it) }
            navigateToFolder(parent)
        }
    }

    // ==================== SHARING OPERATIONS ====================

    fun getSharingForEntity(entityId: Long): Flow<SharingEntity?> = linkBoxRepository.getSharing(entityId)
    
    fun getAllSharingsByEntity(entityId: Long): Flow<List<SharingEntity>> = linkBoxRepository.getAllSharingsByEntity(entityId)

    fun createNewSharingLink(entityId: Long, baseName: String? = null) {
        viewModelScope.launch {
            try {
                val existingCount = linkBoxRepository.getAllSharingsByEntity(entityId).first().size
                
                val linkName = if (baseName != null) {
                    "$baseName - ${existingCount + 1}"
                } else {
                    val entity = linkBoxRepository.getEntity(entityId)
                    "${entity?.name ?: "Link"} - ${existingCount + 1}"
                }
                
                linkBoxRepository.createSharing(
                    SharingEntity(
                        entityId = entityId,
                        name = linkName,
                        privacy = PrivacyType.PUBLIC,
                        token = java.util.UUID.randomUUID().toString().take(8)
                    ),
                    userName.value
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create sharing link: ${e.message}"
            }
        }
    }

    fun updateSharingSettings(sharingId: Long, newName: String, pointsRequired: Int) {
        viewModelScope.launch {
            try {
                val sharing = linkBoxRepository.getSharingById(sharingId)
                if (sharing != null) {
                    linkBoxRepository.updateSharing(
                        sharing.copy(
                            name = newName, 
                            pointsRequired = pointsRequired,
                            updatedAt = System.currentTimeMillis()
                        ),
                        userName.value
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update link settings: ${e.message}"
            }
        }
    }

    fun deleteSharingLink(sharingId: Long) {
        viewModelScope.launch {
            try {
                linkBoxRepository.deleteSharingById(sharingId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete link: ${e.message}"
            }
        }
    }

    fun toggleSharingAccess(sharingId: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val newPrivacy = if (enabled) PrivacyType.PUBLIC else PrivacyType.PRIVATE
                linkBoxRepository.updateSharingPrivacy(sharingId, newPrivacy, userName.value)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update access: ${e.message}"
            }
        }
    }

    fun updateSharingPermissions(
        sharingId: Long,
        syncChanges: Boolean? = null,
        allowDuplicate: Boolean? = null,
        allowExternalSharing: Boolean? = null
    ) {
        viewModelScope.launch {
            try {
                val sharing = linkBoxRepository.getSharingById(sharingId)
                if (sharing != null) {
                    linkBoxRepository.updateSharing(
                        sharing.copy(
                            syncChanges = syncChanges ?: sharing.syncChanges,
                            allowDuplicate = allowDuplicate ?: sharing.allowDuplicate,
                            allowExternalSharing = allowExternalSharing ?: sharing.allowExternalSharing,
                            updatedAt = System.currentTimeMillis()
                        ),
                        userName.value
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update sharing permissions: ${e.message}"
            }
        }
    }

    fun resetSharingToken(entityId: Long) {
        viewModelScope.launch {
            try {
                val sharing = linkBoxRepository.getSharingSync(entityId)
                if (sharing != null) {
                    linkBoxRepository.resetSharingToken(
                        sharing.id,
                        java.util.UUID.randomUUID().toString().take(8)
                    )
                } else {
                    linkBoxRepository.createSharing(
                        SharingEntity(
                            entityId = entityId,
                            privacy = PrivacyType.PUBLIC,
                            token = java.util.UUID.randomUUID().toString().take(8)
                        ),
                        userName.value
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reset token: ${e.message}"
            }
        }
    }
    
    suspend fun getOwnerName(ownerId: Long): String {
        return try {
            linkBoxRepository.getOwner(ownerId)?.name ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Looks up sharing and entity by token for deep link handling.
     * Returns a Pair of (EntityItem?, SharingEntity?)
     * Checks local database first, then Firestore for latest data.
     */
    suspend fun lookupByToken(token: String): Pair<EntityItem?, SharingEntity?> {
        // 1. Try local first (best for owner)
        val localSharing = try { linkBoxRepository.getSharingByToken(token) } catch (e: Exception) { null }
        val localEntity = localSharing?.let { try { linkBoxRepository.getEntity(it.entityId) } catch (e: Exception) { null } }
        
        // If we found local data and it's THE owner, we can return early or keep looking for updates
        // Let's keep looking to get the latest stats/privacy from cloud if available
        
        return try {
            // 2. Try Cloud for visitors/updates
            val cloudSharing = firestoreRepository.getSharing(token)
            if (cloudSharing != null) {
                val cloudEntity = firestoreRepository.getEntity(cloudSharing.ownerId, cloudSharing.entityId)
                
                if (cloudEntity != null) {
                    // Map cloud data to temporary entities for UI
                    // If this is the current user's own content, use their local ownerId
                    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    val isOwner = currentUid != null && cloudSharing.ownerId == currentUid
                    
                    val ownerId = if (isOwner) _currentOwnerId.value ?: 0L else 0L
                    
                    val sharedEntity = cloudEntity.toEntityItem(ownerId = ownerId)
                    val sharedSharing = cloudSharing.toSharingEntity()
                    return Pair(sharedEntity, sharedSharing)
                }
            }
            
            // Fallback to local if cloud fails or returns null
            Pair(localEntity, localSharing)
        } catch (e: Exception) {
            e.printStackTrace()
            // Important: return local data if cloud fetch fails due to network/rules
            Pair(localEntity, localSharing)
        }
    }

    // ==================== JOINED LINKS OPERATIONS ====================
    
    fun searchMyEntities(query: String) {
        _myEntitiesSearchQuery.value = query
    }

    fun setMyEntitiesSort(option: SortOption, direction: SortDirection) {
        _myEntitiesSortOption.value = option
        _myEntitiesSortDirection.value = direction
    }

    fun setMyEntitiesFilter(type: EntityType?) {
        _myEntitiesEntityTypeFilter.value = type
    }

    fun searchJoinedLinks(query: String) {
        _joinedLinksSearchQuery.value = query
    }

    fun setJoinedLinksSort(option: SortOption, direction: SortDirection) {
        _joinedLinksSortOption.value = option
        _joinedLinksSortDirection.value = direction
    }
    
    fun joinLink(
        token: String, 
        name: String, 
        type: EntityType,
        url: String?, 
        authorName: String?, 
        pointsRequired: Int = 0
    ) {
        viewModelScope.launch {
            try {
                linkBoxRepository.joinLink(token, name, url, authorName, pointsRequired, type)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to join link: ${e.message}"
            }
        }
    }
    
    fun deleteJoinedLink(id: Long) {
        viewModelScope.launch {
            try {
                linkBoxRepository.deleteJoinedLinkById(id)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete joined link: ${e.message}"
            }
        }
    }
    
    fun toggleMyEntitiesStarred() {
        _myEntitiesShowOnlyStarred.value = !_myEntitiesShowOnlyStarred.value
    }

    fun toggleJoinedLinksStarred() {
        _joinedLinksShowOnlyStarred.value = !_joinedLinksShowOnlyStarred.value
    }
    
    fun toggleJoinedLinkStar(id: Long, isCurrentlyStarred: Boolean) {
        viewModelScope.launch {
            try {
                linkBoxRepository.toggleJoinedLinkStar(id, !isCurrentlyStarred)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update star: ${e.message}"
            }
        }
    }
    
    // ==================== USER POINTS OPERATIONS ====================
    
    fun openPointsShop() {
        _showPointsShop.value = true
    }
    
    fun closePointsShop() {
        _showPointsShop.value = false
    }
    
    fun earnPointsFromAd(amount: Int = 1) {
        viewModelScope.launch {
            try {
                linkBoxRepository.addPoints(
                    amount = amount,
                    type = PointTransactionType.EARNED_AD,
                    description = "Earned from watching ad"
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add points: ${e.message}"
            }
        }
    }
    
    fun purchasePoints(packageId: String, points: Int) {
        viewModelScope.launch {
            try {
                linkBoxRepository.addPoints(
                    amount = points,
                    type = PointTransactionType.PURCHASED,
                    description = "Purchased points package",
                    referenceId = packageId
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to purchase points: ${e.message}"
            }
        }
    }
    
    suspend fun spendPointsToOpenLink(amount: Int, linkName: String): Boolean {
        return try {
            linkBoxRepository.spendPoints(
                amount = amount,
                description = "Opened link: $linkName"
            )
        } catch (e: Exception) {
            _errorMessage.value = "Failed to spend points: ${e.message}"
            false
        }
    }

    fun openJoinedLink(joinedLink: JoinedLinkEntity, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            // Lookup the original entity to check status
            val (entity, sharing) = lookupByToken(joinedLink.token)
            
            // Check if link is deleted
            if (entity == null || sharing == null) {
                linkBoxRepository.updateJoinedLink(joinedLink.copy(isActive = false))
                _errorMessage.value = "This link has been deleted by the owner"
                onResult(null)
                return@launch
            }
            
            // Check if link is now private
            if (sharing.privacy == PrivacyType.PRIVATE) {
                linkBoxRepository.updateJoinedLink(joinedLink.copy(isActive = false))
                _errorMessage.value = "This link is now private"
                onResult(null)
                return@launch
            }
            
            // Check if link has expired
            if (sharing.publicUpto != null && sharing.publicUpto < System.currentTimeMillis()) {
                linkBoxRepository.updateJoinedLink(joinedLink.copy(isActive = false))
                _errorMessage.value = "This link has expired"
                onResult(null)
                return@launch
            }
            
            // Link is accessible - charge points
            val isOwner = entity.ownerId != 0L && entity.ownerId == _currentOwnerId.value
            
            if (!isOwner) {
                val pointsCost = sharing.pointsRequired.coerceAtLeast(1)
                val success = spendPointsToOpenLink(pointsCost, entity.name)
                if (!success) {
                    _pointsNeeded.value = pointsCost
                    _showInsufficientPointsDialog.value = true
                    onResult(null)
                    return@launch
                }
            }
            
            // Update cached data if anything changed
            val needsUpdate = entity.name != joinedLink.name || 
                              entity.getUrl() != joinedLink.url ||
                              sharing.pointsRequired != joinedLink.pointsRequired ||
                              !joinedLink.isActive
            if (needsUpdate) {
                linkBoxRepository.updateJoinedLink(joinedLink.copy(
                    name = entity.name,
                    url = entity.getUrl(),
                    pointsRequired = sharing.pointsRequired,
                    lastUpdatedTime = entity.updatedAt,
                    isActive = true
                ))
            }
            
            // Update access info
            linkBoxRepository.incrementJoinedLinkAccess(joinedLink.id)
            onResult(entity.getUrl())
        }
    }

    /**
     * Opens a joined link that is a PAGE type.
     * Looks up the entity by token and returns the entity ID for navigation.
     */
    fun openJoinedPageLink(joinedLink: JoinedLinkEntity, onResult: (Long?) -> Unit) {
        viewModelScope.launch {
            // Lookup the original entity to check status
            val (entity, sharing) = lookupByToken(joinedLink.token)
            
            // Check if page is deleted
            if (entity == null || sharing == null) {
                linkBoxRepository.updateJoinedLink(joinedLink.copy(isActive = false))
                _errorMessage.value = "This page has been deleted by the owner"
                onResult(null)
                return@launch
            }
            
            // Check if page is now private
            if (sharing.privacy == PrivacyType.PRIVATE) {
                linkBoxRepository.updateJoinedLink(joinedLink.copy(isActive = false))
                _errorMessage.value = "This page is now private"
                onResult(null)
                return@launch
            }
            
            // Check if page has expired
            if (sharing.publicUpto != null && sharing.publicUpto < System.currentTimeMillis()) {
                linkBoxRepository.updateJoinedLink(joinedLink.copy(isActive = false))
                _errorMessage.value = "This page has expired"
                onResult(null)
                return@launch
            }
            
            // Page is accessible - charge points
            val isOwner = entity.ownerId != 0L && entity.ownerId == _currentOwnerId.value
            
            if (!isOwner) {
                val pointsCost = sharing.pointsRequired.coerceAtLeast(1)
                val success = spendPointsToOpenLink(pointsCost, entity.name)
                if (!success) {
                    _pointsNeeded.value = pointsCost
                    _showInsufficientPointsDialog.value = true
                    onResult(null)
                    return@launch
                }
            }
            
            // Update cached data if anything changed
            val needsUpdate = entity.name != joinedLink.name || 
                              sharing.pointsRequired != joinedLink.pointsRequired ||
                              !joinedLink.isActive
            if (needsUpdate) {
                linkBoxRepository.updateJoinedLink(joinedLink.copy(
                    name = entity.name,
                    pointsRequired = sharing.pointsRequired,
                    lastUpdatedTime = entity.updatedAt,
                    isActive = true
                ))
            }
            
            // Update access info
            linkBoxRepository.incrementJoinedLinkAccess(joinedLink.id)
            onResult(entity.id)
        }
    }

    fun closeInsufficientPointsDialog() {
        _showInsufficientPointsDialog.value = false
    }
    
    fun initializePoints() {
        viewModelScope.launch {
            linkBoxRepository.initializeUserPoints()
        }
    }
    
    // ==================== SUBSCRIPTION OPERATIONS ====================
    
    fun openSubscriptionDialog() {
        _showSubscriptionDialog.value = true
    }
    
    fun closeSubscriptionDialog() {
        _showSubscriptionDialog.value = false
    }
    
    fun purchaseSubscription(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                appSettingsRepository.activateSubscription()
                _showSubscriptionDialog.value = false
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to activate subscription")
            }
        }
    }
    
    fun extendSubscription(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                appSettingsRepository.extendSubscription()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to extend subscription")
            }
        }
    }
    
    fun shouldShowAds(): Boolean = !isSubscriptionActive.value
    
    // ==================== ANALYTICS OPERATIONS ====================
    
    fun recordLinkClick(sharingId: Long, token: String? = null) {
        viewModelScope.launch {
            try {
                linkBoxRepository.recordClick(sharingId, token)
                analyticsRepository.logClick(sharingId)
            } catch (e: Exception) {
                // Silent failure for analytics
            }
        }
    }
    
    fun recordLinkView(sharingId: Long, token: String? = null) {
        viewModelScope.launch {
            try {
                linkBoxRepository.recordView(sharingId, token)
                analyticsRepository.logView(sharingId)
            } catch (e: Exception) {
                // Silent failure for analytics
            }
        }
    }
    
    fun recordLinkDownload(sharingId: Long) {
        viewModelScope.launch {
            try {
                linkBoxRepository.recordDownload(sharingId)
                analyticsRepository.logDownload(sharingId)
            } catch (e: Exception) {
                // Silent failure for analytics
            }
        }
    }
    
    // ==================== DATA MANAGEMENT ====================
    
    fun resetAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                AppDatabase.clearAllData(getApplication())
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reset data: ${e.message}"
            }
        }
    }

    // ==================== AUTHENTICATION OPERATIONS ====================

    val authState = firebaseAuthRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), firebaseAuthRepository.getCurrentUser())

    private suspend fun syncProfileWithFirebase(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        userProfileRepository.updateProfileInfo(
            name = firebaseUser.displayName ?: "User",
            email = firebaseUser.email ?: "",
            phone = firebaseUser.phoneNumber
        )
    }

    private suspend fun ensureOwnerExists(firebaseUser: com.google.firebase.auth.FirebaseUser): Long {
        val email = firebaseUser.email ?: ""
        val existingOwner = linkBoxRepository.getOwnerByEmail(email)
        
        if (existingOwner != null) {
            return existingOwner.id
        }
        
        // Create new owner
        val newOwner = com.clicktoearn.linkbox.data.entity.OwnerEntity(
            name = firebaseUser.displayName ?: "User",
            email = email,
            phone = firebaseUser.phoneNumber,
            password = "firebase_managed" // Password is managed by Firebase
        )
        return linkBoxRepository.createOwner(newOwner)
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = firebaseAuthRepository.signIn(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    syncProfileWithFirebase(user)
                    
                    // Ensure local owner exists and update current owner ID
                    val ownerId = ensureOwnerExists(user)
                    _currentOwnerId.value = ownerId
                    
                    // Download all data from cloud to local (Restore)
                    dataMigrationManager.downloadAllCloudData(ownerId)
                }
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }

    fun signUp(email: String, password: String, name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = firebaseAuthRepository.signUp(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    userProfileRepository.updateProfileInfo(name, email)
                    
                    // Ensure local owner exists
                    val ownerId = ensureOwnerExists(user)
                    _currentOwnerId.value = ownerId
                    
                     // Sync any initial local state to cloud
                    syncNow()
                }
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }
    fun signInAnonymously(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = firebaseAuthRepository.signInAnonymously()
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    // Update for anonymous user
                    userProfileRepository.updateProfileInfo("Guest", "guest@linkbox.cc")
                    
                    // Create local owner record
                    val ownerId = ensureOwnerExists(user)
                    _currentOwnerId.value = ownerId
                    
                    // Attempt restore (though likely empty for new guest)
                    dataMigrationManager.downloadAllCloudData(ownerId)
                }
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Anonymous sign in failed")
            }
        }
    }
    fun signOut() {
        viewModelScope.launch {
            firebaseAuthRepository.signOut()
            userProfileRepository.clearProfile() // Or reset to default
//            _currentOwnerId.value = null // Reset owner - careful with NPEs in UI
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val ownerId = _currentOwnerId.value
            if (ownerId != null) {
                try {
                    dataMigrationManager.uploadAllLocalData(ownerId, userName.value)
                    // Could show a success message via snackbar
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    // ==================== REFRESH OPERATIONS ====================
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    /**
     * Refreshes all data by syncing with cloud and reloading local data.
     * Used for pull-to-refresh functionality.
     */
    fun refreshData() {
        if (_isRefreshing.value) return // Prevent multiple simultaneous refreshes
        
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 1. Refresh owned data
                val ownerId = _currentOwnerId.value
                if (ownerId != null) {
                    // Download latest from cloud
                    dataMigrationManager.downloadAllCloudData(ownerId)
                }
                
                // 2. Refresh joined content (History)
                refreshHistory()
                
                // Small delay for smoother UX
                kotlinx.coroutines.delay(300)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Iterates through all joined links and refreshes their metadata from the cloud.
     */
    private suspend fun refreshHistory() {
        val links = linkBoxRepository.getAllJoinedLinks().first()
        links.forEach { joinedLink ->
            try {
                val (entity, sharing) = lookupByToken(joinedLink.token)
                if (entity != null && sharing != null) {
                    val needsUpdate = entity.name != joinedLink.name || 
                                     entity.getUrl() != joinedLink.url ||
                                     sharing.pointsRequired != joinedLink.pointsRequired ||
                                     !joinedLink.isActive
                    
                    if (needsUpdate) {
                        linkBoxRepository.updateJoinedLink(joinedLink.copy(
                            name = entity.name,
                            url = entity.getUrl(),
                            pointsRequired = sharing.pointsRequired,
                            lastUpdatedTime = entity.updatedAt,
                            isActive = true
                        ))
                    }
                } else if (joinedLink.isActive) {
                    // Mark as inactive if not found anymore
                    linkBoxRepository.updateJoinedLink(joinedLink.copy(isActive = false))
                }
            } catch (e: Exception) {
                // Skip failed individual updates
            }
        }
    }
}

enum class SortOption {
    NAME, CREATED, MODIFIED, OPENED
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

private data class DataState(
    val ownerId: Long?,
    val folderId: Long?,
    val showStarred: Boolean,
    val query: String,
    val sortOption: SortOption,
    val sortDirection: SortDirection,
    val filterType: EntityType?
)

private data class JoinedDataState(
    val query: String,
    val showStarred: Boolean,
    val sortOption: SortOption,
    val sortDirection: SortDirection,
    val filterType: EntityType?
)
