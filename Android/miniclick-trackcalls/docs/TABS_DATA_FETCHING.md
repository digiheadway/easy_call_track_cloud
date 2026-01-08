# Tabs Data Fetching Guide

This document describes the tabs available in the application for both Calls and Persons views, and how the data is filtered and fetched for each.

## Calls Screen Tabs

The `CallsScreen` uses the `CallTabFilter` enum to define its tabs. Data is pre-calculated in `HomeViewModel` using a single-pass filter for efficiency.

| Tab | Filter Logic | Description |
| :--- | :--- | :--- |
| **All** | All calls excluding those with `isNoTracking` or from numbers marked with `hasAnyExclusion`. | The default view showing all tracked call activity. |
| **Incoming** | `callType == INCOMING_TYPE` AND `duration > 0`. | Successfully answered incoming calls. |
| **Not Attended** | (`callType == INCOMING_TYPE` OR `MISSED_TYPE` OR `REJECTED_TYPE`) AND `duration <= 0`. | Incoming calls that were not answered or were rejected. |
| **Outgoing** | `callType == OUTGOING_TYPE` AND `duration > 0`. | Successfully connected outgoing calls. |
| **Not Responded**| `callType == OUTGOING_TYPE` AND `duration <= 0`. | Outgoing calls that were not answered by the recipient. |
| **Ignored** | `hasAnyExclusion == true` OR `callType == 6` (Blocked). | Calls from numbers explicitly marked for exclusion or system-blocked calls. |

---

## Persons Screen Tabs

The `PersonsScreen` uses the `PersonTabFilter` enum. Persons are grouped by their phone number, and stats are aggregated across all their calls.

| Tab | Filter Logic | Description |
| :--- | :--- | :--- |
| **All** | All persons excluding those marked with `isNoTracking` or `hasAnyExclusion`. | List of all unique contacts/numbers tracked. |
| **Connected** | `totalDuration > 0`. | Persons who have had at least one successful call (incoming or outgoing). |
| **Attended** | `pCalls.any { INCOMING_TYPE && duration > 0 }`. | Persons who have answered at least one of your incoming calls. |
| **Responded** | `pCalls.any { OUTGOING_TYPE && duration > 0 }`. | Persons who have answered at least one of your outgoing calls. |
| **Never Connected**| `totalDuration == 0`. | Persons with whom no successful call has ever been established. |
| **Not Attended** | `pCalls.any { (INCOMING/MISSED/REJECTED) && duration <= 0 }`. | Persons who have at least one unanswered incoming call. |
| **Never Attended**| `pCalls.all { INCOMING_TYPE -> duration <= 0 }`. | Persons who have NEVER answered an incoming call. |
| **Not Responded** | `pCalls.any { OUTGOING_TYPE && duration <= 0 }`. | Persons who have at least one unanswered outgoing call. |
| **Never Responded**| `pCalls.all { OUTGOING_TYPE -> duration <= 0 }`. | Persons who have NEVER answered an outgoing call. |
| **Ignored** | `hasAnyExclusion == true`. | Persons explicitly marked as ignored. |

---

## Data Fetching Architecture

1. **Reactive Stream**: `HomeViewModel` observes `callDataRepository.getAllCallsFlow()` and `getAllPersonsIncludingExcludedFlow()`.
2. **Single-Pass Processing**: Whenever raw data changes or filters are updated, `triggerFilter()` is called.
3. **Efficiency**: 
    - Filtering is performed using `processCallFilters` and `processPersonFilters` on a `Dispatchers.Default` background thread.
    - It generates a `Map<TabFilter, List<Data>>` containing data for ALL tabs simultaneously.
    - The UI (Pager) then simply reads from these pre-computed lists, ensuring zero-lag swipes.
4. **Global Filters**: Date range, SIM selection, Label, and other active filters are applied *before* distributing calls/persons into their respective tabs.
