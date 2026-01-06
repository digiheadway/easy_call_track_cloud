package com.example.callyzer4.data

import java.util.Date

data class CallFilter(
    val callTypes: Set<CallType> = CallType.values().toSet(),
    val dateRange: DateRange? = null,
    val searchQuery: String = "",
    val minDuration: Long? = null,
    val maxDuration: Long? = null
)

data class DateRange(
    val startDate: Date,
    val endDate: Date
)

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    DURATION_DESC,
    DURATION_ASC,
    NAME_ASC,
    NAME_DESC
}
