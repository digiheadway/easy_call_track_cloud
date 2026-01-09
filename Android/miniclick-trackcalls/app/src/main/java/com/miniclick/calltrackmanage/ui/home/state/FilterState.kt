package com.miniclick.calltrackmanage.ui.home.state

import com.miniclick.calltrackmanage.ui.home.viewmodel.*

/**
 * Complete filter state for calls and persons.
 * All filter-related state is consolidated here for clarity.
 */
data class FilterState(
    // View Mode
    val viewMode: ViewMode = ViewMode.CALLS,
    val isSearchVisible: Boolean = false,
    val isFiltersVisible: Boolean = false,
    val searchQuery: String = "",
    
    // Tab Filters (determine which tab is selected)
    val callTypeFilter: CallTabFilter = CallTabFilter.ALL,
    val personTabFilter: PersonTabFilter = PersonTabFilter.ALL,
    
    // Secondary Filters (applied on top of tab filter)
    val connectedFilter: ConnectedFilter = ConnectedFilter.ALL,
    val notesFilter: NotesFilter = NotesFilter.ALL,
    val personNotesFilter: PersonNotesFilter = PersonNotesFilter.ALL,
    val contactsFilter: ContactsFilter = ContactsFilter.ALL,
    val attendedFilter: AttendedFilter = AttendedFilter.ALL,
    val reviewedFilter: ReviewedFilter = ReviewedFilter.ALL,
    val customNameFilter: CustomNameFilter = CustomNameFilter.ALL,
    val labelFilter: String = "",
    val minCallCount: Int = 0,
    
    // Date Range
    val dateRange: DateRange = DateRange.LAST_7_DAYS,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    
    // Sorting
    val personSortBy: PersonSortBy = PersonSortBy.LAST_CALL,
    val personSortDirection: SortDirection = SortDirection.DESCENDING,
    val callSortBy: CallSortBy = CallSortBy.DATE,
    val callSortDirection: SortDirection = SortDirection.DESCENDING,
    
    // Special Toggles
    val showIgnoredOnly: Boolean = false,
    val showComparisons: Boolean = false,
    
    // Active Filter Count (for badge display)
    val activeFilterCount: Int = 0
) {
    /**
     * Calculate the number of active filters (excluding defaults).
     */
    fun calculateActiveFilterCount(): Int {
        var count = 0
        if (connectedFilter != ConnectedFilter.ALL) count++
        if (notesFilter != NotesFilter.ALL) count++
        if (personNotesFilter != PersonNotesFilter.ALL) count++
        if (contactsFilter != ContactsFilter.ALL) count++
        if (reviewedFilter != ReviewedFilter.ALL) count++
        if (customNameFilter != CustomNameFilter.ALL) count++
        if (minCallCount > 0) count++
        if (labelFilter.isNotEmpty()) count++
        return count
    }
}

/**
 * Reports-related state.
 */
data class ReportsState(
    val reportCategory: ReportCategory = ReportCategory.OVERVIEW,
    val reportStats: ReportStats = ReportStats()
)

/**
 * Loading/Error state for the screen.
 */
data class LoadingState(
    val isLoading: Boolean = true,
    val selectedTab: Int = 0, // Bottom navigation tab
    val error: String? = null
)

// Re-export the enum from HomeViewModel for convenience
// (These are used in FilterState but defined in HomeViewModel)
typealias ReviewedFilter = com.miniclick.calltrackmanage.ui.home.viewmodel.ReviewedFilter
