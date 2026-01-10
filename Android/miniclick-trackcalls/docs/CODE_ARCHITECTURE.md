# 📱 Call Track Manager - Complete Code Architecture

> **Last Updated:** January 9, 2026  
> **Package:** `com.miniclick.calltrackmanage`  
> **Min SDK:** 22 | **Target SDK:** 35

---

## 📋 Table of Contents

1. [Executive Summary](#-executive-summary)
2. [Project Structure](#-project-structure)
3. [Architecture Pattern](#-architecture-pattern)
4. [Core Modules](#-core-modules)
5. [Data Layer](#-data-layer)
6. [Network Layer](#-network-layer)
7. [UI Layer](#-ui-layer)
8. [Background Processing](#-background-processing)
9. [Dependency Graph](#-dependency-graph)
10. [Areas of Improvement](#-areas-of-improvement)
11. [Performance Optimization Recommendations](#-performance-optimization-recommendations)
12. [Modern Architecture Recommendations](#-modern-architecture-recommendations)
13. [Code Quality & Bug Prevention](#-code-quality--bug-prevention)
14. [APK Size Optimization](#-apk-size-optimization)
15. [Memory & RAM Optimization](#-memory--ram-optimization)
16. [Implementation Priority Roadmap](#-implementation-priority-roadmap)

---

## 🎯 Executive Summary

Call Track Manager is a **Jetpack Compose-based Android application** for tracking, syncing, and managing call logs with cloud backup functionality. The app uses a **single-activity architecture** with ViewModels managing UI state.

### Current Tech Stack

| Category | Technology |
|----------|------------|
| UI Framework | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + Manager + Repository) |
| Database | Room (SQLite) |
| Networking | Retrofit + OkHttp + Gson |
| Background | WorkManager + Foreground Service |
| DI Pattern | Hilt (for Settings) / Manual Singleton (Legacy) |
| Build System | Gradle Kotlin DSL with Version Catalogs |
| Analytics | Firebase Analytics + Crashlytics |

### Key Metrics (Current State)

| Metric | Value | Status |
|--------|-------|--------|
| Total Kotlin Files | 110+ | 🟢 Growing |
| Largest File | `SettingsViewModel.kt` (~1372 lines) | 🔴 Needs Refactoring |
| Second Largest | `HomeViewModel.kt` (~932 lines) | 🟡 Refactored |
| Database Version | 10 (8 migrations) | 🟢 Good |
| ProGuard Rules | Comprehensive | 🟢 Good |

---

## 📁 Project Structure

```
app/src/main/java/com/miniclick/calltrackmanage/
│
├── 📄 CallTrackerApplication.kt      # Application class (workers + service init)
├── 📄 MainActivity.kt                 # Single Activity (722 lines)
├── 📄 MainViewModel.kt                # Main screen state management
│
├── 📂 data/                           # Data Layer
│   ├── 📄 CallDataRepository.kt       # Call data operations (1256 lines)
│   ├── 📄 RecordingRepository.kt      # Recording file management (1000+ lines)
│   ├── 📄 SettingsRepository.kt       # SharedPreferences wrapper (500+ lines)
│   ├── 📄 ProcessMonitor.kt           # Sync progress monitoring
│   │
│   └── 📂 db/                         # Room Database
│       ├── 📄 AppDatabase.kt          # Database + Migrations (200 lines)
│       ├── 📄 CallDataDao.kt          # Call queries (DAO)
│       ├── 📄 CallDataEntity.kt       # Call entity model
│       ├── 📄 PersonDataDao.kt        # Person queries (DAO)
│       ├── 📄 PersonDataEntity.kt     # Person entity model
│       ├── 📄 CallLogStatus.kt        # Sync status enums
│       ├── 📄 CallTypeEnums.kt        # Call type enums
│       └── 📄 Converters.kt           # Type converters
│
├── 📂 network/                        # Network Layer
│   ├── 📄 NetworkClient.kt            # Retrofit client singleton (71 lines)
│   ├── 📄 CallCloudApi.kt             # API interface (145 lines)
│   └── 📄 DataObjects.kt              # DTOs/Response models (87 lines)
│
├── 📂 receiver/                       # Broadcast Receivers
│   ├── 📄 CallReceiver.kt             # Phone state receiver (181 lines)
│   └── 📄 BootReceiver.kt             # Boot completed receiver
│
├── 📂 service/                        # Background Services
│   ├── 📄 SyncService.kt              # Foreground sync service (317 lines)
│   ├── 📄 CallerIdManager.kt          # Caller ID overlay manager
│   └── 📄 CallTrackInCallService.kt   # InCall service (default dialer)
│
├── 📂 worker/                         # WorkManager Workers
│   ├── 📄 CallSyncWorker.kt           # Metadata sync worker (681 lines)
│   ├── 📄 RecordingUploadWorker.kt    # Recording upload worker
│   └── 📄 ReattachRecordingsWorker.kt # Recording reattachment
│
├── 📂 util/                           # Utilities (Legacy)
│   ├── 📄 LogExporter.kt              # Debug log export
│   └── 📄 NetworkConnectivityObserver.kt
│
├── 📂 utils/                          # Utilities (Current)
│   ├── 📄 AudioCompressor.kt          # MediaCodec compression
│   └── 📄 DevicePermissionGuide.kt    # Permission guidance
│
└── 📂 ui/                             # UI Layer
    ├── 📂 call/
    │   └── 📄 InCallActivity.kt       # In-call screen
    │
    ├── 📂 common/                     # Shared UI Components
    │   ├── 📄 AudioComponents.kt
    │   ├── 📄 DevicePermissionGuideSheet.kt
    │   ├── 📄 Dialogs.kt              # Common dialogs
    │   ├── 📄 EmptyState.kt           # Empty state UI
    │   ├── 📄 FilterComponents.kt     # Filters & chips (32KB)
    │   ├── 📄 Labels.kt               # Label chips
    │   ├── 📄 PhoneLookupModal.kt
    │   ├── 📄 ScrollbarComponents.kt
    │   ├── 📄 ShimmerComponents.kt    # Loading skeletons
    │   └── 📄 SyncComponents.kt       # Sync status UI (42KB)
    │
    ├── 📂 home/                       # Home Screen
    │   ├── 📄 HomeScreen.kt           # Main home screen
    │   ├── 📄 HomeViewModel.kt        # Refactored (~932 lines)
    │   ├── 📄 HomeScreenComponents.kt
    │   ├── 📄 CallLogComponents.kt    # Call list (1315 lines)
    │   ├── 📄 PersonsComponents.kt    # Persons list
    │   ├── 📄 PersonGroup.kt          # Data class
    │   ├── 📄 PersonInteractionBottomSheet.kt
    │   ├── 📄 DateRangeHeaderAction.kt
    │   ├── 📄 DialerScreen.kt         # Dialer UI
    │   ├── 📄 ReportsScreen.kt        # Reports/analytics
    │   └── 📄 SetupGuide.kt           # Onboarding steps
    │
    ├── 📂 onboarding/
    │   ├── 📄 AgreementScreen.kt
    │   └── 📄 OnboardingScreen.kt
    │
    ├── 📂 settings/                   # Settings Screens
    │   ├── 📄 SettingsScreen.kt       # Main settings (73KB)
    │   ├── 📄 SettingsViewModel.kt    # Delegating ViewModel (now Hilt-enabled)
    │   ├── 📄 SettingsComponents.kt   # Reusable setting cards
    │   ├── 📄 TrackingSettingsScreen.kt
    │   ├── 📄 ExtrasScreen.kt
    │   ├── 📄 DataManagementScreen.kt
    │   ├── 📄 DataManagementBottomSheet.kt
    │   ├── 📄 SimSettingsModals.kt
    │   │
    │   └── 📂 viewmodel/              # Feature Managers & ViewModels
    │       ├── 📄 PermissionManager.kt # Extract Logic
    │       ├── 📄 SimManager.kt        # SIM/Calibration Logic
    │       ├── 📄 SyncManager.kt       # Pairing/Cloud Logic
    │       ├── 📄 DataManager.kt       # Export/Import Logic
    │       ├── 📄 TrackingManager.kt   # Tracking/Sync Logic
    │       ├── 📄 LookupManager.kt     # Custom Lookup Logic
    │       ├── 📄 GeneralSettingsManager.kt
    │       ├── 📄 PermissionsViewModel.kt
    │       ├── 📄 SimViewModel.kt
    │       ├── 📄 AccountViewModel.kt
    │       ├── 📄 DataManagementViewModel.kt
    │       ├── 📄 TrackingViewModel.kt
    │       ├── 📄 LookupViewModel.kt
    │       └── 📄 GeneralSettingsViewModel.kt
    │
    ├── 📂 theme/                      # Material 3 Theme
    │   ├── 📄 Color.kt
    │   ├── 📄 Theme.kt
    │   └── 📄 Type.kt
    │
    └── 📂 utils/                      # UI Utilities
        ├── 📄 AudioPlayer.kt          # ExoPlayer wrapper
        ├── 📄 CallUtils.kt            # Call type helpers
        ├── 📄 FormatUtils.kt          # Date/duration formatting
        └── 📄 WhatsAppUtils.kt
```

---

## 🏗️ Architecture Pattern

### Current: **MVVM with Repository Pattern**

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI LAYER                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │ MainActivity│  │ HomeScreen  │  │ SettingsScreen          │ │
│  │             │  │ (Compose)   │  │ (Compose)               │ │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘ │
│         │                │                      │               │
│         ▼                ▼                      ▼               │
│  ┌─────────────┐  ┌─────────────┐  ┌───────────┴─────────────┐ │
│  │MainViewModel│  │HomeViewModel│  │SettingsViewModel        │ │
│  │             │  │             │  │ (Delegates to Managers) │ │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘ │
│         │                │                      │               │
│         │                │                      ▼               │
│         │                │          ┌─────────────────────────┐ │
│         │                │          │        Managers         │ │
│         │                │          │ (Sim, Sync, Data, etc.) │ │
│         │                │          └───────────┬─────────────┘ │
└─────────┼────────────────┼──────────────────────┼───────────────┘
          │                │                      │
          ▼                ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DATA LAYER                                  │
│  ┌───────────────────┐  ┌─────────────────┐  ┌───────────────┐ │
│  │ CallDataRepository│  │RecordingRepo    │  │SettingsRepo   │ │
│  │ (Singleton)       │  │(Singleton)      │  │(Singleton)    │ │
│  └─────────┬─────────┘  └────────┬────────┘  └───────┬───────┘ │
│            │                     │                    │         │
│            ▼                     ▼                    ▼         │
│  ┌───────────────────┐  ┌─────────────────┐  ┌───────────────┐ │
│  │ Room Database     │  │ File System     │  │SharedPrefs    │ │
│  │ (AppDatabase)     │  │ (Audio Files)   │  │               │ │
│  └───────────────────┘  └─────────────────┘  └───────────────┘ │
└─────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     NETWORK LAYER                                │
│  ┌───────────────────┐  ┌─────────────────┐                     │
│  │ NetworkClient     │  │ CallCloudApi    │                     │
│  │ (Retrofit)        │  │ (Interface)     │                     │
│  └───────────────────┘  └─────────────────┘                     │
└─────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│                  BACKGROUND PROCESSING                           │
│  ┌───────────────────┐  ┌─────────────────┐  ┌───────────────┐ │
│  │ CallSyncWorker    │  │RecordingUpload  │  │ SyncService   │ │
│  │ (WorkManager)     │  │Worker           │  │ (Foreground)  │ │
│  └───────────────────┘  └─────────────────┘  └───────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Core Modules

### 1. Application Class (`CallTrackerApplication.kt`)

**Purpose:** Initialize background workers and foreground service on app start.

```kotlin
class CallTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleWorkers()     // Periodic sync workers
        startSyncService()    // Foreground service
    }
}
```

**Issues:**
- ⚠️ No dependency injection framework
- ⚠️ All initialization happens synchronously on main thread

---

### 2. Main Activity (`MainActivity.kt`) - 689 lines

**Responsibilities:**
- Single activity hosting all Compose screens
- Intent handling (dial, share recording)
- Theme management
- Navigation state

**Issues:**
- 🔴 Too many responsibilities
- 🔴 Contains business logic (recording processing)
- 🔴 Manual ViewModel creation

---

## 💾 Data Layer

### Database Schema

#### `call_data` Table

| Column | Type | Description |
|--------|------|-------------|
| `compositeId` | TEXT (PK) | Unique call identifier |
| `systemId` | TEXT | System call log ID |
| `phoneNumber` | TEXT (Indexed) | Phone number |
| `contactName` | TEXT | Contact name from system |
| `callType` | INT | INCOMING(1), OUTGOING(2), MISSED(3), REJECTED(5) |
| `callDate` | LONG (Indexed) | Timestamp |
| `duration` | LONG | Duration in seconds |
| `subscriptionId` | INT | SIM slot |
| `callNote` | TEXT | User-added note |
| `localRecordingPath` | TEXT | Local file path |
| `reviewed` | BOOL | Reviewed status |
| `metadataSyncStatus` | ENUM | PENDING, SYNCED, FAILED, NEEDS_PUSH |
| `recordingSyncStatus` | ENUM | NOT_APPLICABLE, PENDING, SYNCED, etc. |
| `serverUpdatedAt` | LONG | Conflict resolution timestamp |

#### `person_data` Table

| Column | Type | Description |
|--------|------|-------------|
| `phoneNumber` | TEXT (PK) | Normalized phone |
| `contactName` | TEXT | Contact name |
| `personNote` | TEXT | Person-level note |
| `label` | TEXT | Category label |
| `totalCalls` | INT | Call count |
| `totalDuration` | LONG | Total duration |
| `excludeFromSync` | BOOL | "No Tracking" |
| `excludeFromList` | BOOL | "Hide from UI" |
| `needsSync` | BOOL | Pending server push |

### Repository Pattern

```kotlin
// Singleton pattern - manual DI
class CallDataRepository private constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: CallDataRepository? = null
        
        fun getInstance(context: Context): CallDataRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallDataRepository(context).also { INSTANCE = it }
            }
        }
    }
}
```

**Issues:**
- 🟡 Manual singleton (should use Hilt)
- 🟡 Context leakage potential
- 🟡 No interface abstraction for testing

---

## 🌐 Network Layer

### API Endpoints (Single PHP File)

All API calls go to `sync_app.php` with different `action` parameters:

| Action | Purpose |
|--------|---------|
| `verify_pairing` | Device pairing |
| `start_call` | Register new call |
| `batch_sync` | Batch upload calls |
| `upload_chunk` | Chunked recording upload |
| `finalize_upload` | Complete recording upload |
| `update_call` | Update call metadata |
| `update_person` | Update person metadata |
| `fetch_updates` | Delta sync (pull updates) |
| `fetch_config` | Get org settings |

### Network Configuration

```kotlin
object NetworkClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)           // Debug only
        .addInterceptor(retryInterceptor)  // 3 retries for 5xx
        .connectionPool(connectionPool)    // Connection reuse
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
```

**Issues:**
- 🔴 Single endpoint (no proper REST structure)
- 🟡 No certificate pinning
- 🟡 No offline caching strategy

---

## 🎨 UI Layer

### Screen Hierarchy

```
MainActivity
├── OnboardingScreen (if !completed)
├── AgreementScreen (if !accepted)
└── MainScreen
    ├── HomeScreen (Tab 0) ─────────┐
    │   ├── CallLogList             │
    │   ├── PersonsList             │
    │   └── ReportsScreen           ├── HomeViewModel
    ├── DialerScreen (Tab 1)        │
    └── SettingsScreen (Tab 2) ─────┴── SettingsViewModel
        ├── TrackingSettingsScreen
        ├── ExtrasScreen
        └── DataManagementScreen
```

### State Management

```kotlin
// HomeUiState - 90+ fields!
data class HomeUiState(
    val isLoading: Boolean = true,
    val callLogs: List<CallDataEntity> = emptyList(),
    val personList: List<PersonDataEntity> = emptyList(),
    val filteredLogs: List<CallDataEntity> = emptyList(),
    val recordings: Map<String, String> = emptyMap(),
    val searchQuery: String = "",
    val selectedTabIndex: Int = 0,
    // ... 85+ more fields
)
```

**Issues:**
- 🔴 Massive state class (90+ fields)
- 🔴 No sealed class for screen states
- 🔴 State not split by feature

---

## ⚙️ Background Processing

### Worker Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                 BOOT_COMPLETED                               │
│                      │                                       │
│                      ▼                                       │
│              BootReceiver                                    │
│                      │                                       │
│       ┌──────────────┼──────────────┐                       │
│       ▼              ▼              ▼                       │
│  SyncService   CallSyncWorker   RecordingUploadWorker       │
│  (Foreground)    (Periodic)         (Periodic)              │
│       │          30 min             1 hour                  │
│       │              │                                       │
│       ▼              ▼                                       │
│  Phone State   Import → Sync → Pull                         │
│   Monitoring      Metadata                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                 PHONE_STATE_CHANGED                          │
│                      │                                       │
│                      ▼                                       │
│              CallReceiver                                    │
│                      │                                       │
│       ┌──────────────┼──────────────┐                       │
│       ▼              ▼              ▼                       │
│  Show CallerID  Trigger Sync   Show Notifications           │
│   Overlay        (on IDLE)      (Recording reminder)        │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Dependency Graph

```
                    ┌─────────────────┐
                    │ Firebase BOM    │
                    │ (Analytics,     │
                    │  Crashlytics)   │
                    └────────┬────────┘
                             │
┌──────────────┐    ┌───────┴────────┐    ┌───────────────┐
│ Material 3   │    │   Compose BOM  │    │  Navigation   │
│ + Icons Ext  │◄───┤   2024.10.01   │───►│   Compose     │
└──────────────┘    └────────────────┘    └───────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
┌──────────────┐    ┌───────────────┐    ┌───────────────┐
│  Room 2.6.1  │    │ Retrofit 2.9  │    │WorkManager 2.9│
│  + KSP       │    │ + OkHttp 4.12 │    │               │
└──────────────┘    │ + Gson 2.10.1 │    └───────────────┘
                    └───────────────┘
                             │
                    ┌────────┴────────┐
                    │     Coil 2.7    │
                    │ (Image Loading) │
                    └─────────────────┘
```

---

## 🚨 Areas of Improvement

### 🔴 Critical Issues

#### 1. **God ViewModels**

| ViewModel | Lines | Issue |
|-----------|-------|-------|
| `SettingsViewModel.kt` | ~1,372 | Mixed concerns, needs further split |
| `HomeViewModel.kt` | ~932 | Refactored, logic extracted to managers |

**Fix:** Split into feature-specific ViewModels:
- `CallListViewModel`
- `PersonListViewModel`  
- `ReportsViewModel`
- `FilterViewModel`
- `RecordingPlaybackViewModel`

**Fix (In Progress):** Hilt implemented for Settings ViewModels and Managers.
```kotlin
@HiltViewModel
class DataManagementViewModel @Inject constructor(
    application: Application,
    callDataRepository: CallDataRepository,
    // ...
) : AndroidViewModel(application)
```

**Fix:** Implement Hilt:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)
}
```

#### 3. **Massive UI State Class**

`HomeUiState` has 90+ fields - impossible to maintain.

**Fix:** Split into feature states:
```kotlin
data class CallListState(
    val calls: List<CallDataEntity>,
    val isLoading: Boolean,
    val error: String?
)

data class FilterState(
    val dateRange: DateRange,
    val callType: CallTabFilter,
    val searchQuery: String
)

data class HomeUiState(
    val callListState: CallListState,
    val filterState: FilterState,
    val reportState: ReportState
)
```

#### 4. **No Error Handling Strategy**

Current: Random try-catch blocks.

**Fix:** Implement Result wrapper:
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

---

### 🟡 Medium Priority Issues

#### 5. **Duplicate Utility Folders**

```
├── util/        # LogExporter, NetworkConnectivityObserver
├── utils/       # AudioCompressor, DevicePermissionGuide
└── ui/utils/    # AudioPlayer, CallUtils, FormatUtils
```

**Fix:** Consolidate into single `util` package:
```
├── util/
│   ├── audio/
│   ├── formatting/
│   ├── network/
│   └── permissions/
```

#### 6. **Missing Interface Abstractions**

Repositories are concrete classes - can't mock for testing.

**Fix:**
```kotlin
interface CallRepository {
    suspend fun getAllCalls(): Flow<List<CallDataEntity>>
    suspend fun syncFromSystem()
}

class CallDataRepositoryImpl @Inject constructor(
    private val dao: CallDataDao
) : CallRepository
```

#### 7. **Blocking I/O on Main Thread**

SharedPreferences accessed synchronously.

**Fix:** Use DataStore:
```kotlin
val Context.settingsDataStore by preferencesDataStore(name = "settings")
```

---

### 🟢 Low Priority (Nice to Have)

#### 8. **No Modularization**

Single `app` module contains everything.

**Fix:** Create feature modules:
```
:core:data        # Data layer
:core:network     # Network layer
:core:ui          # Common UI components
:feature:home     # Home feature
:feature:settings # Settings feature
```

#### 9. **No Unit Tests**

Only 1 test file: `ExampleInstrumentedTest.kt`

**Fix:** Add test coverage for:
- Repositories (use in-memory Room)
- ViewModels (use `runTest`)
- Use Cases (if introduced)

---

## ⚡ Performance Optimization Recommendations

### Memory & RAM Optimization

| Issue | Current | Recommended |
|-------|---------|-------------|
| **Image Loading** | Coil default | Add memory cache limits |
| **List Rendering** | Load all calls | Implement paging (Paging 3) |
| **State Updates** | 90+ field copy | Split state, use `derivedStateOf` |
| **Recording Files** | Keep in memory | Stream directly |
| **Log Statements** | Present in release | Use ProGuard to strip |

```kotlin
// Implement Paging 3 for call list
@Query("SELECT * FROM call_data ORDER BY callDate DESC")
fun getCallsPaged(): PagingSource<Int, CallDataEntity>
```

### APK Size Optimization

| Optimization | Estimated Savings |
|--------------|-------------------|
| Enable R8 full mode | 10-15% |
| Remove unused Material Icons | 2-3 MB |
| Use WebP for images | 30-50% per image |
| Split APK by ABI | 40% per variant |
| Remove unused Compose features | 500KB-1MB |

```kotlin
// build.gradle.kts
android {
    bundle {
        language { enableSplit = true }
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}
```

### Database Optimization

```kotlin
// Add compound indices for common queries
@Entity(
    indices = [
        Index("phoneNumber", "callDate"),  // GroupBy phone + sort by date
        Index("metadataSyncStatus"),        // Find pending syncs fast
        Index("callType", "callDate")       // Filter by type
    ]
)
```

### Network Optimization

1. **Implement caching:**
```kotlin
@GET("sync_app.php")
@Headers("Cache-Control: max-age=300")
suspend fun fetchConfig(...)
```

2. **Use compression:**
```kotlin
.addInterceptor { chain ->
    chain.proceed(
        chain.request().newBuilder()
            .header("Accept-Encoding", "gzip")
            .build()
    )
}
```

---

## 🏛️ Modern Architecture Recommendations

### Recommended: Clean Architecture with MVI

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌─────────────────┐  ┌─────────────────────────────────┐  │
│  │    UI State     │◄─┤        ViewModel                │  │
│  │ (Compose State) │  │ - Handles UI Events             │  │
│  └─────────────────┘  │ - Executes Use Cases            │  │
│          │            │ - Emits UI State                │  │
│          ▼            └─────────────────────────────────┘  │
│  ┌─────────────────┐                                        │
│  │   Compose UI    │                                        │
│  │   (Screens)     │                                        │
│  └─────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                     DOMAIN LAYER                             │
│  ┌─────────────────┐  ┌─────────────────────────────────┐  │
│  │   Use Cases     │  │        Entities                 │  │
│  │ - SyncCallsUC   │  │ - Call                          │  │
│  │ - GetCallsUC    │  │ - Person                        │  │
│  │ - FilterCallsUC │  │ - Recording                     │  │
│  └─────────────────┘  └─────────────────────────────────┘  │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Repository Interfaces                   │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                              │
│  ┌─────────────────┐  ┌─────────────────────────────────┐  │
│  │  Repository     │  │       Data Sources              │  │
│  │  Implementations│  │ - LocalDataSource (Room)        │  │
│  │                 │  │ - RemoteDataSource (Retrofit)   │  │
│  └─────────────────┘  └─────────────────────────────────┘  │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                     Mappers                          │   │
│  │              (Entity ↔ Domain ↔ DTO)                 │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### MVI Pattern for State Management

```kotlin
// Intent (User Actions)
sealed class HomeIntent {
    object LoadCalls : HomeIntent()
    data class Search(val query: String) : HomeIntent()
    data class FilterByType(val type: CallTabFilter) : HomeIntent()
    object Refresh : HomeIntent()
}

// State
data class HomeViewState(
    val calls: List<Call> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// ViewModel
class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeViewState())
    val state: StateFlow<HomeViewState> = _state.asStateFlow()
    
    fun processIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadCalls -> loadCalls()
            is HomeIntent.Search -> search(intent.query)
            // ...
        }
    }
}
```

---

## 🛡️ Code Quality & Bug Prevention

### 1. **Enable Strict Kotlin Compiler Options**

```kotlin
// build.gradle.kts
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf(
            "-Xexplicit-api=strict",     // Force visibility modifiers
            "-Werror",                    // Treat warnings as errors
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}
```

### 2. **Add Static Analysis**

```kotlin
// build.gradle.kts
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.0"
}

detekt {
    config.setFrom("$rootDir/config/detekt.yml")
    buildUponDefaultConfig = true
}
```

### 3. **Implement Sealed Results**

```kotlin
sealed class SyncResult {
    data class Success(val syncedCount: Int) : SyncResult()
    data class PartialSuccess(
        val synced: Int,
        val failed: Int,
        val errors: List<String>
    ) : SyncResult()
    data class Failure(val error: Throwable) : SyncResult()
}
```

### 4. **Add Logging Abstraction**

```kotlin
interface Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

class DebugLogger : Logger {
    override fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }
}
```

### 5. **Null Safety Improvements**

```kotlin
// Instead of
val name = call.contactName ?: ""

// Use extension
fun String?.orEmpty(): String = this ?: ""
```

---

## 📉 APK Size Optimization

### Current Dependencies Analysis

| Dependency | Estimated Size | Necessity |
|------------|----------------|-----------|
| Material Icons Extended | ~5 MB | 🔴 Too large |
| Firebase BOM | ~800 KB | 🟢 Required |
| Retrofit + OkHttp | ~1.2 MB | 🟢 Required |
| Coil | ~500 KB | 🟢 Required |
| Room | ~400 KB | 🟢 Required |

### Optimizations

#### 1. Replace Material Icons Extended

```kotlin
// Instead of importing entire library
implementation("androidx.compose.material:material-icons-extended")

// Import only needed icons
implementation("androidx.compose.material:material-icons-core")
// Then add specific icons as drawable resources
```

#### 2. Enable R8 Full Mode

```kotlin
// gradle.properties
android.enableR8.fullMode=true
```

#### 3. Remove Unused Resources

```kotlin
android {
    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            
            // Add resource optimization
            optimization {
                keepRules {
                    // Keep only English resources
                    ignoreFrom("res/values-*")
                }
            }
        }
    }
}
```

---

## 💾 Memory & RAM Optimization

### 1. **Lazy State Reading**

```kotlin
// Bad - reads entire list on every recomposition
val calls by viewModel.calls.collectAsState()

// Good - only reads visible items
val callsPaged = viewModel.callsPager.collectAsLazyPagingItems()
```

### 2. **Derived State for Computations**

```kotlin
// Bad - recomputes on every recomposition
val filteredCalls = calls.filter { it.type == selectedType }

// Good - only recomputes when dependencies change
val filteredCalls by remember(calls, selectedType) {
    derivedStateOf { calls.filter { it.type == selectedType } }
}
```

### 3. **Remember with Keys**

```kotlin
// Bad - recreates on every recomposition
val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

// Good - stable between recompositions
val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
```

### 4. **Stable Collections**

```kotlin
// Add stability annotation for immutable data
@Immutable
data class CallDisplayItem(
    val id: String,
    val number: String,
    val date: Long
)
```

### 5. **Image Memory Cache**

```kotlin
// Configure Coil with memory limits
ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.15)  // Use 15% of available memory
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .maxSizeBytes(50 * 1024 * 1024)  // 50 MB
            .build()
    }
    .build()
```

---

## 📋 Implementation Priority Roadmap

| Task | Status | Impact | Effort |
|------|--------|--------|--------|
| Split SettingsViewModel into Managers | ✅ Done | 🟢 High | 🟡 Medium |
| Split HomeViewModel into 4+ smaller VMs | ⏳ TBD | 🟢 High | 🟡 Medium |
| Consolidate util folders | ⏳ TBD | 🟢 High | 🟢 Low |
| Add Result wrapper class | ⏳ TBD | 🟢 High | 🟢 Low |

### Phase 2: Architecture (In Progress)

| Task | Status | Impact | Effort |
|------|--------|--------|--------|
| Add Hilt DI | 🟠 Partial | 🟢 High | 🟡 Medium |
| Extract Repository interfaces | ⏳ TBD | 🟡 Medium | 🟢 Low |
| Replace SharedPreferences with DataStore | ⏳ TBD | 🟡 Medium | 🟡 Medium |
| Implement Paging 3 for call list | ⏳ TBD | 🟢 High | 🟡 Medium |

### Phase 3: Testing & Quality (2-3 weeks)

| Task | Impact | Effort |
|------|--------|--------|
| Add unit tests for repositories | 🟢 High | 🟡 Medium |
| Add ViewModel tests | 🟢 High | 🟡 Medium |
| Set up Detekt/ktlint | 🟡 Medium | 🟢 Low |
| Add UI tests with Compose testing | 🟡 Medium | 🔴 High |

### Phase 4: Modularization (4-6 weeks)

| Task | Impact | Effort |
|------|--------|--------|
| Create `:core:data` module | 🟡 Medium | 🟡 Medium |
| Create `:core:network` module | 🟡 Medium | 🟡 Medium |
| Create `:feature:home` module | 🟡 Medium | 🔴 High |
| Create `:feature:settings` module | 🟡 Medium | 🔴 High |

---

## 📝 Quick Reference Commands

```bash
# Build release APK
./gradlew assembleRelease

# Analyze APK size
./gradlew app:analyzeReleaseBundle

# Run tests
./gradlew test

# Check dependencies
./gradlew :app:dependencies

# Generate lint report
./gradlew lint
```

---

## 📚 Related Documentation

- [DATABASE_SYSTEM_REPORT.md](./DATABASE_SYSTEM_REPORT.md) - Database architecture details
- [APP_STARTUP_FLOWS.md](./APP_STARTUP_FLOWS.md) - Startup sequence documentation
- [CALLYZER_VS_OUR_COMPARISON.md](./CALLYZER_VS_OUR_COMPARISON.md) - Feature comparison
- [DATA_FILTERING_GUIDE.md](./DATA_FILTERING_GUIDE.md) - Filter implementation guide

---

> **Note:** This document should be updated as the architecture evolves. Run the analysis periodically to track improvements.
