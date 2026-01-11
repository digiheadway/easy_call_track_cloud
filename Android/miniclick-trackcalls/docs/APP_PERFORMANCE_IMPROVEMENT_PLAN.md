# 🚀 App Performance Improvement Plan

**Created:** January 11, 2026  
**Status:** ✅ Phase 1 & 3 Implemented | Phase 2 & 4 Pending  
**Priority:** Critical  

---

## 📋 Executive Summary

This document outlines a comprehensive plan to address performance issues affecting the app's responsiveness, including:
- **App Feels Laggy** – Overall sluggish experience across the app
- **Local Data Loading Slow** – 3-4 seconds to load calls from local database
- **In-Call Display Lag** – Delayed UI updates during active calls
- **Call End Detection Failure** – Not detecting phone call disconnection properly
- **In-Call Screen Persistence** – Screen not going away after call ends
- **Notification Stacking Issue** – Ongoing call notification showing above in-call screen
- **Navigation System Lag** – Exploring app, switching tabs, etc. is slow

---

## 🔍 Issue Analysis

### Problem 1: App Feels Laggy Overall
**Root Causes Identified:**
1. **Heavy ViewModel Init**: `HomeViewModel` performs multiple operations during initialization
2. **Blocking Main Thread**: `loadSettingsSync()` performs synchronous SharedPreferences reads
3. **Excessive Flow Emissions**: Multiple database observers triggering recompositions
4. **Filter Computation on Main Thread**: Complex filtering logic running during recomposition

### Problem 2: Local Data Loading (3-4 seconds)
**Root Causes Identified:**
1. **Cold Database Query**: Initial Room DB query takes time on fresh app launch
2. **Over-fetching Data**: Loading entire call history instead of paginated chunks
3. **Sequential Loading**: Data loads sequentially instead of parallel
4. **PersonGroup Computation**: Heavy object creation in `triggerFilter()` for every call

### Problem 3: In-Call Display Lag
**Root Causes Identified:**
1. **InCallViewModel Loading All Logs**: `loadCallerData()` fetches ALL logs for a phone number
2. **Database Access on Collect**: Repository calls happening on Main thread indirectly
3. **Label Loading Inefficiency**: Loading distinct labels on every person update flow emission

### Problem 4: Call End Detection Failure
**Root Causes Identified:**
1. **Missed State Transitions**: `onStateChanged` callback may not fire for all disconnection scenarios
2. **External Call Handling**: When app is not default dialer, `updateExternalStatus()` relies on TelephonyManager which can be delayed
3. **Race Condition**: `currentCall` being set to null before UI can react

### Problem 5: In-Call Screen Not Dismissing
**Root Causes Identified:**
1. **Delayed `onEndCall()`**: Current code has `delay(1000)` before calling `onEndCall()`
2. **Call Status Flow Issue**: `callStatus` StateFlow may not emit `null` or `DISCONNECTED` promptly
3. **Activity Lifecycle**: `InCallActivity` doesn't have `finish()` tied to call disconnection explicitly

### Problem 6: Notification Above In-Call Screen
**Root Causes Identified:**
1. **Foreground Service Notification**: `startForeground()` creates a persistent notification that overlays the activity
2. **Notification Channel Priority**: Both channels set to `IMPORTANCE_HIGH`, causing visual conflicts
3. **Missing Notification Dismissal**: When activity is visible, notification should be minimized

### Problem 7: Navigation System Lag
**Root Causes Identified:**
1. **LazyColumn/LazyVerticalGrid Item Keys**: Not using stable keys causes full recomposition
2. **State Changes Triggering Full Recomposition**: `uiState` updates cause entire tree to recompose
3. **Heavy Date Formatting**: Formatting dates on every item render
4. **Image Loading**: Contact photos loading synchronously

---

## ✅ Improvement Plan

### Phase 1: Critical Fixes (1-2 Days)

#### 1.1 Fix Call End Detection & Screen Dismissal
**Priority:** 🔴 Critical  
**Files:** `CallTrackInCallService.kt`, `InCallActivity.kt`, `CallReceiver.kt`

**Changes:**
```kotlin
// InCallActivity.kt - Remove the 1-second delay
LaunchedEffect(callStatus) {
    callStatus?.let { call ->
        // ... existing state updates ...
        
        if (call.state == android.telecom.Call.STATE_DISCONNECTED) {
            // Immediate dismiss - no delay
            onEndCall()
        }
    }
    
    // ALSO check for null status (call removed entirely)
    if (callStatus == null && phoneNumber.isNotEmpty()) {
        onEndCall() // Call was removed, close activity
    }
}
```

```kotlin
// CallTrackInCallService.kt - Ensure proper cleanup on call removal
override fun onCallRemoved(call: android.telecom.Call) {
    super.onCallRemoved(call)
    if (currentCall == call) {
        // Cancel notification FIRST to avoid stacking
        val nm = getSystemService(NotificationManager::class.java)
        nm?.cancel(NOTIFICATION_ID)
        
        currentCall = null
        stopForeground(STOPFOREGROUND_REMOVE) // Use explicit remove flag
    }
}
```

**Impact:** Immediate call end detection, no lingering screens

---

#### 1.2 Fix Notification Stacking Issue
**Priority:** 🔴 Critical  
**Files:** `CallTrackInCallService.kt`, `InCallActivity.kt`

**Changes:**
```kotlin
// CallTrackInCallService.kt - Add activity visibility awareness
companion object {
    // ... existing code ...
    
    var isInCallActivityVisible = false
}

private fun updateNotification(call: android.telecom.Call) {
    // ... existing notification builder ...
    
    // When activity is visible and call is active, use lower priority
    if (isInCallActivityVisible && call.state == Call.STATE_ACTIVE) {
        builder.setPriority(NotificationCompat.PRIORITY_LOW)
    }
    
    // ... rest of notification logic ...
}
```

```kotlin
// InCallActivity.kt - Report visibility state
override fun onResume() {
    super.onResume()
    CallTrackInCallService.isInCallActivityVisible = true
}

override fun onPause() {
    super.onPause()
    CallTrackInCallService.isInCallActivityVisible = false
}
```

**Impact:** Notification won't overlap with the in-call screen

---

### Phase 2: Local Data Loading Optimization (2-3 Days)

#### 2.1 Implement Paginated Data Loading
**Priority:** 🟠 High  
**Files:** `CallDataDao.kt`, `CallDataRepository.kt`, `HomeViewModel.kt`

**Changes:**
```kotlin
// CallDataDao.kt - Add paginated query
@Query("SELECT * FROM call_data WHERE callDate >= :minDate ORDER BY callDate DESC LIMIT :limit OFFSET :offset")
suspend fun getCallsPaginated(minDate: Long, limit: Int, offset: Int): List<CallDataEntity>

@Query("SELECT * FROM call_data WHERE callDate >= :minDate ORDER BY callDate DESC LIMIT :limit")
fun getCallsPagedFlow(minDate: Long, limit: Int): Flow<List<CallDataEntity>>
```

```kotlin
// HomeViewModel.kt - Load first page immediately 
// Phase 0: Load only first 50 calls for instant display
viewModelScope.launch(Dispatchers.IO) {
    val minDate = settingsRepository.getTrackStartDate()
    val firstPage = callDataRepository.getCallsPaginated(minDate, limit = 50, offset = 0)
    
    withContext(Dispatchers.Main) {
        _uiState.update { it.copy(callLogs = firstPage, isLoading = false) }
    }
    
    // Then load remaining in background (lazy loading on scroll)
}
```

**Impact:** Initial load under 500ms instead of 3-4 seconds

---

#### 2.2 Cache PersonGroup Computation Aggressively
**Priority:** 🟠 High  
**Files:** `HomeViewModel.kt`

**Changes:**
```kotlin
// Use Identifiable hash for cache invalidation
private var groupCacheHash: Int = 0

private fun computePersonGroupsIfNeeded(calls: List<CallDataEntity>, persons: List<PersonDataEntity>): Map<String, PersonGroup> {
    val newHash = System.identityHashCode(calls) xor System.identityHashCode(persons)
    
    if (newHash == groupCacheHash && lastComputedGroups.isNotEmpty()) {
        return lastComputedGroups // Return cached result
    }
    
    // ... existing heavy computation ...
    
    groupCacheHash = newHash
    return groups
}
```

**Impact:** Avoid redundant O(N) operations on data that hasn't changed

---

#### 2.3 Pre-compute Date Headers
**Priority:** 🟡 Medium  
**Files:** `CallLogManager.kt`, `HomeViewModel.kt`

**Changes:**
```kotlin
// Move date formatting and grouping to background thread
data class FormattedCallGroup(
    val header: String,
    val calls: List<CallDataEntity>,
    val dateKey: String // For stable key
)

// Pre-compute in filter pass
val formattedGroups = withContext(Dispatchers.Default) {
    calls.groupBy { 
        val cal = Calendar.getInstance().apply { timeInMillis = it.callDate }
        "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }.map { (key, groupCalls) ->
        FormattedCallGroup(
            header = formatDateHeader(groupCalls.first().callDate),
            calls = groupCalls,
            dateKey = key
        )
    }
}
```

**Impact:** No date formatting during LazyColumn composition

---

### Phase 3: In-Call Performance (1-2 Days)

#### 3.1 Optimize InCallViewModel Data Loading
**Priority:** 🟠 High  
**Files:** `InCallViewModel.kt`, `CallDataRepository.kt`

**Changes:**
```kotlin
// InCallViewModel.kt - Limit call history loaded
private suspend fun loadCallerData(phoneNumber: String, systemName: String?) = withContext(Dispatchers.IO) {
    val normalized = callDataRepository.normalizePhoneNumber(phoneNumber)
    val person = callDataRepository.getPersonByNumber(normalized)
    
    // OPTIMIZATION: Only fetch last 10 calls for in-call display
    val logs = callDataRepository.getRecentLogsForPhone(normalized, limit = 10)
    
    // ... rest of logic ...
}
```

```kotlin
// CallDataRepository.kt - Add limited query
suspend fun getRecentLogsForPhone(phoneNumber: String, limit: Int): List<CallDataEntity> {
    return dao.getRecentLogsForPhone(phoneNumber, limit)
}

// CallDataDao.kt
@Query("SELECT * FROM call_data WHERE phoneNumber = :number ORDER BY callDate DESC LIMIT :limit")
suspend fun getRecentLogsForPhone(number: String, limit: Int): List<CallDataEntity>
```

**Impact:** In-call screen loads instantly (only 10 records)

---

#### 3.2 Lazy Load Labels
**Priority:** 🟡 Medium  
**Files:** `InCallViewModel.kt`

**Changes:**
```kotlin
// Load labels only when user opens the label picker
private var labelsLoaded = false

fun loadLabelsIfNeeded() {
    if (labelsLoaded) return
    labelsLoaded = true
    
    viewModelScope.launch(Dispatchers.IO) {
        val labels = callDataRepository.getDistinctLabels()
        _availableLabels.value = labels
    }
}

// In InCallActivity.kt - Call when dialog opens
LabelPickerDialog(
    // ...
    onShow = { inCallViewModel.loadLabelsIfNeeded() }
)
```

**Impact:** Faster InCallViewModel initialization

---

### Phase 4: Navigation System Optimization (2-3 Days)

#### 4.1 Use Stable Keys for LazyColumn
**Priority:** 🟠 High  
**Files:** `HomeScreen.kt`, `CallLogComponents.kt`, `PersonsComponents.kt`

**Changes:**
```kotlin
// CallLogComponents.kt
LazyColumn {
    items(
        items = calls,
        key = { call -> call.compositeId } // MUST use stable, unique key
    ) { call ->
        CallLogItem(call = call, ...)
    }
}

// PersonsComponents.kt  
LazyColumn {
    items(
        items = personGroups,
        key = { person -> person.number } // Stable key by phone number
    ) { person ->
        PersonCard(person = person, ...)
    }
}
```

**Impact:** Partial recomposition instead of full list rebuild

---

#### 4.2 Derive State Instead of Full UIState Updates
**Priority:** 🟡 Medium  
**Files:** `HomeViewModel.kt`, `HomeScreen.kt`

**Changes:**
```kotlin
// HomeViewModel.kt - Use derived state for filtered results
val filteredCalls: StateFlow<List<CallDataEntity>> = uiState
    .map { it.callLogs.filter(/* your filter logic */) }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

val filteredPersons: StateFlow<List<PersonGroup>> = uiState
    .map { it.personGroups.filter(/* your filter logic */) }
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```

```kotlin
// HomeScreen.kt - Observe only what's needed
@Composable
fun CallsScreen(viewModel: HomeViewModel) {
    val calls by viewModel.filteredCalls.collectAsState()
    // Now only recomposes when filteredCalls changes, not every uiState change
}
```

**Impact:** Reduced recomposition scope = faster UI updates

---

#### 4.3 Debounce Search Input
**Priority:** 🟡 Medium  
**Files:** `HomeViewModel.kt`

**Changes:**
```kotlin
// Already has 8ms debounce in triggerFilter(), but search should be longer
private var searchJob: Job? = null

fun onSearchQueryChanged(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    settingsRepository.setSearchQuery(query)
    
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
        delay(200) // 200ms debounce for search
        triggerFilter()
    }
}
```

**Impact:** Typing in search won't cause lag

---

### Phase 5: Advanced Optimizations (Future)

#### 5.1 Implement RecyclerView with DiffUtil (Major Refactor)
- Replace Compose LazyColumn with RecyclerView for extremely large lists
- Use DiffUtil for efficient list updates
- Consider only if Phase 4 doesn't achieve acceptable performance

#### 5.2 Database Indexing Review
```sql
-- Ensure these indexes exist
CREATE INDEX idx_calldata_phone_date ON call_data(phoneNumber, callDate DESC);
CREATE INDEX idx_calldata_date ON call_data(callDate DESC);
CREATE INDEX idx_person_phone ON person_data(phoneNumber);
```

#### 5.3 Background Pre-computation
- Use WorkManager to pre-compute reports during idle time
- Cache computed results in Room

---

## 📊 Expected Results

| Metric | Current | Target | Phase |
|--------|---------|--------|-------|
| Initial Load Time | 3-4 sec | < 500ms | Phase 2 |
| In-Call Screen Lag | Noticeable | Instant | Phase 3 |
| Call End Detection | Unreliable | 100% | Phase 1 |
| Screen Dismissal | Delayed 1s+ | Instant | Phase 1 |
| Navigation Smoothness | Laggy | 60 FPS | Phase 4 |
| Notification Issue | Overlapping | Resolved | Phase 1 |

---

## 🔧 Implementation Checklist

### Phase 1 (Critical) - Target: 1-2 Days ✅ IMPLEMENTED
- [x] Remove 1-second delay in call end handling
- [x] Add null callStatus check for activity dismissal
- [x] Fix `onCallRemoved` to cancel notification explicitly
- [x] Add activity visibility tracking for notification priority
- [x] Add `onResume`/`onPause` to InCallActivity

### Phase 2 (Data Loading) - Target: 2-3 Days
- [x] Add paginated query to DAO (`getRecentCallsForNumber`)
- [ ] Implement first-page-first loading in ViewModel
- [ ] Add aggressive PersonGroup caching
- [ ] Pre-compute date headers in background

### Phase 3 (In-Call) - Target: 1-2 Days ✅ IMPLEMENTED
- [x] Limit call history in InCallViewModel to 10 records
- [x] Lazy load labels on dialog open
- [x] Move InCallViewModel init work to background

### Phase 4 (Navigation) - Target: 2-3 Days
- [ ] Add stable keys to all LazyColumn items
- [ ] Implement derived StateFlows for filtered data
- [ ] Add proper search debouncing
- [ ] Profile and eliminate remaining recomposition hotspots

---

## 📝 Testing Requirements

1. **Call End Scenarios:**
   - Test call ended by remote party
   - Test call ended by user (hang up button)
   - Test call ended via notification action
   - Test call rejected/declined

2. **Performance Metrics:**
   - Profile with Android Studio Profiler
   - Check for "Skipped frames" logcat messages  
   - Verify 60 FPS during scroll
   - Measure cold start time

3. **Edge Cases:**
   - App not default dialer (fallback mode)
   - Very long call history (1000+ calls)
   - Rapid navigation between tabs
   - Search while data is loading

---

## 🔗 Related Documents
- [APP_STARTUP_FLOWS.md](./APP_STARTUP_FLOWS.md)
- [PROPOSED_APP_STARTUP_OPTIMIZATIONS.md](./PROPOSED_APP_STARTUP_OPTIMIZATIONS.md)
- [PROJECT_IMPROVEMENT_PROPOSAL.md](./PROJECT_IMPROVEMENT_PROPOSAL.md)
- [CODE_ARCHITECTURE.md](./CODE_ARCHITECTURE.md)
