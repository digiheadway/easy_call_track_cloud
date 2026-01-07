# 📊 Data Filtering & Categorization Guide
*Technical Reference for Calls & Persons Filtering Logic*

---

## 📚 Table of Contents
1. [Overview](#-1-overview)
2. [Data Entities](#-2-data-entities)
3. [Calls View Tabs](#-3-calls-view-tabs)
4. [Persons View Tabs](#-4-persons-view-tabs)
5. [Secondary Filters](#-5-secondary-filters)
6. [Date Range Filters](#-6-date-range-filters)
7. [Exclusion Types](#-7-exclusion-types)
8. [Sorting Options](#-8-sorting-options)
9. [Filter Flow Diagram](#-9-filter-flow-diagram)

---

## 🎯 1. Overview

The app provides two main views for displaying call data:
- **Calls View**: Individual call log entries (each call is a separate row)
- **Persons View**: Aggregated data by phone number (one row per unique caller/callee)

Each view has its own set of **primary tabs** (mutually exclusive) and **secondary filters** (stackable) that can be combined to narrow down results.

### Key Files
| File | Purpose |
|------|---------|
| `HomeViewModel.kt` | Contains all filter enums and filtering logic |
| `FilterComponents.kt` | UI components for tabs and filter modal |
| `CallDataEntity.kt` | Database schema for call records |
| `PersonDataEntity.kt` | Database schema for person records |

---

## 📋 2. Data Entities

### CallDataEntity (Each Call Record)
```kotlin
data class CallDataEntity(
    val compositeId: String,        // Unique ID (systemId + deviceId)
    val phoneNumber: String,        // Phone number
    val contactName: String?,       // Name from device contacts
    val callType: Int,              // 1=INCOMING, 2=OUTGOING, 3=MISSED, 5=REJECTED, 6=BLOCKED
    val callDate: Long,             // Timestamp of call
    val duration: Long,             // Duration in seconds (0 = no connection)
    val callNote: String?,          // User-added note for this call
    val reviewed: Boolean,          // Whether call has been reviewed
    val subscriptionId: Int?,       // SIM card ID
    val localRecordingPath: String? // Path to recording file
)
```

### PersonDataEntity (Aggregated per Phone Number)
```kotlin
data class PersonDataEntity(
    val phoneNumber: String,        // Normalized phone number (PK)
    val contactName: String?,       // Display name
    val personNote: String?,        // Person-level note
    val label: String?,             // Labels (comma-separated: "Lead,VIP")
    
    // Stats
    val totalCalls: Int,            // Total call count
    val totalIncoming: Int,         // Incoming call count
    val totalOutgoing: Int,         // Outgoing call count
    val totalMissed: Int,           // Missed call count
    val totalDuration: Long,        // Sum of all call durations
    
    // Last Call Info
    val lastCallDate: Long?,        // Most recent call timestamp
    val lastCallType: Int?,         // Type of most recent call
    
    // Exclusion Flags
    val excludeFromSync: Boolean,   // "No Tracking" - stop tracking entirely
    val excludeFromList: Boolean    // "Ignored" - track but hide from lists
)

// Computed Properties:
val isNoTracking: Boolean = excludeFromSync && excludeFromList
val isExcludedFromListOnly: Boolean = !excludeFromSync && excludeFromList
val hasAnyExclusion: Boolean = excludeFromSync || excludeFromList
```

---

## 📞 3. Calls View Tabs

The Calls view uses `CallTabFilter` enum for primary categorization.

### Tab Definitions

| Tab | Enum Value | Query Logic | Description |
|-----|------------|-------------|-------------|
| **All** | `ALL` | `!isIgnored` | All calls except ignored/blocked |
| **Incoming** | `INCOMING` | `callType == 1 && duration > 0` | Connected incoming calls only |
| **Not Attended** | `NOT_ATTENDED` | `callType IN (3,5,6) OR (callType == 1 && duration <= 0)` | Missed/Rejected/Blocked OR Incoming with 0 duration |
| **Outgoing** | `OUTGOING` | `callType == 2 && duration > 0` | Connected outgoing calls only |
| **Not Responded** | `NOT_RESPONDED` | `callType == 2 && duration <= 0` | Outgoing calls that weren't picked up |
| **Ignored** | `IGNORED` | `person.hasAnyExclusion == true OR callType == 6` | Excluded numbers + system-blocked calls |

### Call Type Values (Android System)
```kotlin
android.provider.CallLog.Calls.INCOMING_TYPE = 1
android.provider.CallLog.Calls.OUTGOING_TYPE = 2
android.provider.CallLog.Calls.MISSED_TYPE = 3
android.provider.CallLog.Calls.REJECTED_TYPE = 5
android.provider.CallLog.Calls.BLOCKED_TYPE = 6
```

### Detailed Query Logic (from `applyFiltersInternal`)
```kotlin
// 1. Always hide "No Tracking" persons (complete exclusion)
if (person?.isNoTracking == true) return false

// 2. Determine if this call should be in "Ignored" tab
val isIgnored = person?.hasAnyExclusion == true || call.callType == 6

// 3. Apply tab-specific filtering
when (typeFilter) {
    CallTabFilter.ALL -> !isIgnored  // Show all non-ignored
    
    CallTabFilter.INCOMING -> 
        !isIgnored && call.callType == 1 && call.duration > 0
    
    CallTabFilter.OUTGOING -> 
        !isIgnored && call.callType == 2 && call.duration > 0
    
    CallTabFilter.NOT_ATTENDED -> 
        !isIgnored && (
            call.callType == 3 ||  // MISSED
            call.callType == 5 ||  // REJECTED
            call.callType == 6 ||  // BLOCKED
            (call.callType == 1 && call.duration <= 0)  // Incoming no answer
        )
    
    CallTabFilter.NOT_RESPONDED -> 
        !isIgnored && call.callType == 2 && call.duration <= 0
    
    CallTabFilter.IGNORED -> isIgnored  // Only ignored calls
}
```

---

## 👥 4. Persons View Tabs

The Persons view uses `PersonTabFilter` enum for primary categorization.

### Tab Definitions

| Tab | Enum Value | Query Logic | Description |
|-----|------------|-------------|-------------|
| **All** | `ALL` | `!isIgnored` | All persons except excluded |
| **Connected** | `CONNECTED` | `totalDuration > 0` | Had at least one connected call |
| **Attended** | `ATTENDED` | `totalDuration > 0` | Same as Connected (had any call > 0 duration) |
| **Responded** | `RESPONDED` | `any outgoing call with duration > 0` | Person picked up our outgoing calls |
| **Never Connected** | `NEVER_CONNECTED` | `totalDuration == 0` | All calls had 0 duration |
| **Not Attended** | `NOT_ATTENDED` | `any incoming/missed call with duration <= 0` | Has at least one unanswered incoming |
| **Never Attended** | `NEVER_ATTENDED` | `all incoming calls have duration <= 0` | Never picked up any incoming call |
| **Not Responded** | `NOT_RESPONDED` | `any outgoing call with duration <= 0` | Has at least one unanswered outgoing |
| **Never Responded** | `NEVER_RESPONDED` | `all outgoing calls have duration <= 0` | Never answered any of our calls |
| **Ignored** | `IGNORED` | `hasAnyExclusion == true` | Excluded persons only |

### Detailed Query Logic (from `applyPersonFiltersInternal`)
```kotlin
// Get all calls for this person
val pCalls = logsByPhone[normPhone] ?: emptyList()

// 1. Always hide "No Tracking" persons
if (person.isNoTracking) return false

val isIgnored = person.hasAnyExclusion

when (tabFilter) {
    PersonTabFilter.ALL -> !isIgnored
    
    PersonTabFilter.CONNECTED -> 
        !isIgnored && person.totalDuration > 0
    
    PersonTabFilter.ATTENDED -> 
        !isIgnored && person.totalDuration > 0
    
    PersonTabFilter.RESPONDED -> 
        !isIgnored && pCalls.any { 
            it.callType == 2 && it.duration > 0  // Outgoing + connected
        }
    
    PersonTabFilter.NEVER_CONNECTED -> 
        !isIgnored && person.totalDuration == 0
    
    PersonTabFilter.NOT_ATTENDED -> 
        !isIgnored && pCalls.any { 
            (it.callType == 1 || it.callType == 3 || it.callType == 5) 
            && it.duration <= 0 
        }
    
    PersonTabFilter.NEVER_ATTENDED -> 
        !isIgnored && pCalls.all { 
            it.callType != 1 || it.duration <= 0  // No connected incoming
        }
    
    PersonTabFilter.NOT_RESPONDED -> 
        !isIgnored && pCalls.any { 
            it.callType == 2 && it.duration <= 0  // Unanswered outgoing
        }
    
    PersonTabFilter.NEVER_RESPONDED -> 
        !isIgnored && pCalls.all { 
            it.callType != 2 || it.duration <= 0  // No connected outgoing
        }
    
    PersonTabFilter.IGNORED -> isIgnored
}
```

---

## 🔧 5. Secondary Filters

Secondary filters stack with tabs and each other. They are available in the Filter Modal.

### Available Filters

| Filter | Enum | Options | Query Logic |
|--------|------|---------|-------------|
| **Connection** | `ConnectedFilter` | ALL, CONNECTED, NOT_CONNECTED | `duration > 0` or `duration <= 0` |
| **Call Note** | `NotesFilter` | ALL, WITH_NOTE, WITHOUT_NOTE | `callNote != null && !callNote.isEmpty()` |
| **Person Note** | `PersonNotesFilter` | ALL, WITH_NOTE, WITHOUT_NOTE | `personNote != null && !personNote.isEmpty()` |
| **Contacts** | `ContactsFilter` | ALL, IN_CONTACTS, NOT_IN_CONTACTS | `call.contactName != null` |
| **Reviewed** | `ReviewedFilter` | ALL, REVIEWED, NOT_REVIEWED | `call.reviewed == true/false` |
| **Custom Name** | `CustomNameFilter` | ALL, WITH_NAME, WITHOUT_NAME | `person.contactName != null` (app-set name) |
| **Label** | String | Any label | `person.label.contains(labelFilter)` |
| **Min Calls** | Int | 0, 5+, 10+, 20+, 50+ | `person.totalCalls >= minCount` |

### Filter Query Logic
```kotlin
// Connected/Attended Filter
when {
    connFilter == CONNECTED || aFilter == ATTENDED -> call.duration > 0
    connFilter == NOT_CONNECTED || aFilter == NEVER_ATTENDED -> call.duration <= 0
    else -> true
}

// Notes Filter (Call-level)
if (nFilter == WITH_NOTE && call.callNote.isNullOrEmpty()) return false
if (nFilter == WITHOUT_NOTE && !call.callNote.isNullOrEmpty()) return false

// Person Notes Filter
if (pnFilter == WITH_NOTE && person?.personNote.isNullOrEmpty()) return false
if (pnFilter == WITHOUT_NOTE && !person?.personNote.isNullOrEmpty()) return false

// Contacts Filter
if (cFilter == IN_CONTACTS && call.contactName.isNullOrEmpty()) return false
if (cFilter == NOT_IN_CONTACTS && !call.contactName.isNullOrEmpty()) return false

// Reviewed Filter
if (rFilter == REVIEWED && !call.reviewed) return false
if (rFilter == NOT_REVIEWED && call.reviewed) return false

// Custom Name Filter
if (cnFilter == WITH_NAME && person?.contactName.isNullOrEmpty()) return false
if (cnFilter == WITHOUT_NAME && !person?.contactName.isNullOrEmpty()) return false

// Label Filter
if (labelFilter.isNotEmpty()) {
    val personLabels = person?.label?.split(",")?.map { it.trim() } ?: emptyList()
    if (!personLabels.contains(labelFilter)) return false
}

// Min Calls Filter (Persons View only)
if (minCount > 0 && person.totalCalls < minCount) return false
```

---

## 📅 6. Date Range Filters

Date filtering applies to both Calls and Persons views.

### Date Range Options

| Option | Enum Value | Query Logic |
|--------|------------|-------------|
| **Today** | `TODAY` | `callDate >= startOfToday` |
| **Last 3 Days** | `LAST_3_DAYS` | `callDate >= startOfDay(2 days ago)` |
| **Last 7 Days** | `LAST_7_DAYS` | `callDate >= startOfDay(6 days ago)` |
| **Last 14 Days** | `LAST_14_DAYS` | `callDate >= startOfDay(13 days ago)` |
| **Last 30 Days** | `LAST_30_DAYS` | `callDate >= startOfDay(29 days ago)` |
| **This Month** | `THIS_MONTH` | `callDate >= firstDayOfCurrentMonth` |
| **Previous Month** | `PREVIOUS_MONTH` | `callDate in firstDayOfPrevMonth..lastDayOfPrevMonth` |
| **Custom** | `CUSTOM` | `callDate in customStartDate..customEndDate` |
| **All Time** | `ALL` | No date filtering |

### Date Calculation Functions
```kotlin
fun getStartOfDay(daysAgo: Int): Long {
    return Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun getStartOfThisMonth(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
```

---

## 🚫 7. Exclusion Types

The app supports granular exclusion with two independent flags:

### Exclusion Flags

| Flag | Field Name | Effect |
|------|------------|--------|
| **Ignore from Lists** | `excludeFromList = true` | Calls appear ONLY in "Ignored" tab. Hidden from All, Incoming, Outgoing, etc. |
| **No Tracking** | `excludeFromSync = true` | Completely hidden from ALL tabs including "Ignored". Calls are not synced to server. |

### Combined States
```kotlin
// "Ignore from Lists" only
excludeFromSync = false
excludeFromList = true
→ Shows in "Ignored" tab only

// "No Tracking" (completely hidden)
excludeFromSync = true
excludeFromList = true
→ Hidden from ALL tabs, no server sync

// Normal (no exclusion)
excludeFromSync = false
excludeFromList = false
→ Shows in All, Incoming, Outgoing, etc. tabs
```

### Detection Logic
```kotlin
// In PersonDataEntity:
val isNoTracking: Boolean get() = excludeFromSync && excludeFromList
val isExcludedFromListOnly: Boolean get() = !excludeFromSync && excludeFromList
val hasAnyExclusion: Boolean get() = excludeFromSync || excludeFromList
```

---

## 🔀 8. Sorting Options

### Persons View Sorting

| Sort By | Enum Value | Description |
|---------|------------|-------------|
| **Last Call** | `LAST_CALL` | By most recent call timestamp |
| **Most Calls** | `MOST_CALLS` | By total call count |

### Sort Directions
- `ASCENDING` - Oldest first / Fewest calls first
- `DESCENDING` - Newest first / Most calls first (default)

```kotlin
val sorted = when (sortBy) {
    PersonSortBy.LAST_CALL -> {
        if (sortDirection == DESCENDING) 
            filtered.sortedByDescending { it.lastCallDate ?: 0L }
        else 
            filtered.sortedBy { it.lastCallDate ?: 0L }
    }
    PersonSortBy.MOST_CALLS -> {
        if (sortDirection == DESCENDING) 
            filtered.sortedByDescending { it.totalCalls }
        else 
            filtered.sortedBy { it.totalCalls }
    }
}
```

---

## 📊 9. Filter Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        RAW DATA (Room DB)                        │
│   callDataRepository.getAllCallsFlow()                          │
│   callDataRepository.getAllPersonsIncludingExcludedFlow()       │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     TRACK START DATE FILTER                      │
│   if (trackStartDate > 0 && callDate < trackStartDate) EXCLUDE  │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       DATE RANGE FILTER                          │
│   Apply: TODAY / LAST_7_DAYS / THIS_MONTH / CUSTOM / ALL        │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        SIM FILTER                                │
│   if (simSelection != "Both") filter by subscriptionId          │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                 NO TRACKING EXCLUSION                            │
│   if (person.isNoTracking) EXCLUDE completely                   │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PRIMARY TAB FILTER                            │
│                                                                  │
│   CALLS VIEW:                    PERSONS VIEW:                   │
│   ├── ALL                        ├── ALL                         │
│   ├── INCOMING                   ├── CONNECTED                   │
│   ├── NOT_ATTENDED               ├── ATTENDED                    │
│   ├── OUTGOING                   ├── RESPONDED                   │
│   ├── NOT_RESPONDED              ├── NEVER_CONNECTED             │
│   └── IGNORED                    ├── NOT_ATTENDED                │
│                                  ├── NEVER_ATTENDED              │
│                                  ├── NOT_RESPONDED               │
│                                  ├── NEVER_RESPONDED             │
│                                  └── IGNORED                     │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SECONDARY FILTERS (Stackable)                  │
│   ├── Label Filter (string match)                                │
│   ├── Connection Filter (duration > 0)                           │
│   ├── Notes Filter (has call note)                               │
│   ├── Person Notes Filter (has person note)                      │
│   ├── Contacts Filter (is in device contacts)                    │
│   ├── Reviewed Filter (is reviewed)                              │
│   ├── Custom Name Filter (has app-set name)                      │
│   └── Min Calls Filter (persons only)                            │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SEARCH FILTER                               │
│   Matches: phoneNumber, contactName, callNote, personNote       │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       SORTING                                    │
│   Persons: by lastCallDate or totalCalls                        │
│   Direction: ASCENDING or DESCENDING                            │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      FINAL OUTPUT                                │
│   uiState.filteredLogs (Calls View)                             │
│   uiState.filteredPersons (Persons View)                        │
│   uiState.callTypeCounts (count per tab)                        │
│   uiState.personTypeCounts (count per tab)                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📝 Filter State Persistence

All filter states are persisted using `SettingsRepository` and restored on app launch:

| Filter | Storage Key | Default Value |
|--------|-------------|---------------|
| Search Query | `search_query` | `""` |
| Search Visible | `search_visible` | `false` |
| Filters Visible | `filters_visible` | `false` |
| Call Type Filter | `call_type_filter` | `"ALL"` |
| Connected Filter | `connected_filter` | `"ALL"` |
| Notes Filter | `notes_filter` | `"ALL"` |
| Person Notes Filter | `person_notes_filter` | `"ALL"` |
| Contacts Filter | `contacts_filter` | `"ALL"` |
| Attended Filter | `attended_filter` | `"ALL"` |
| Reviewed Filter | `reviewed_filter` | `"ALL"` |
| Custom Name Filter | `custom_name_filter` | `"ALL"` |
| Label Filter | `label_filter` | `""` |
| Date Range | `date_range_filter` | `"LAST_7_DAYS"` |
| Custom Start Date | `custom_start_date` | `0L` |
| Custom End Date | `custom_end_date` | `0L` |

---

*© 2026 MiniClick Calls. Technical Reference Document.*
