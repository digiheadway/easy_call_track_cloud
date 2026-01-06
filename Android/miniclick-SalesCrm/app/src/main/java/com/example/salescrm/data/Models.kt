package com.example.salescrm.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Task status
enum class TaskStatus { PENDING, COMPLETED, CANCELLED }

// View modes for tasks
enum class TaskViewMode { LIST, DATE_WISE, CALENDAR }

// Person type (for linking tasks/notes)
enum class PersonType { PERSON }

// Task types
enum class TaskType(val label: String) {
    FOLLOW_UP("Follow up"),
    MEETING("Meeting"),
    TO_DO("To do")
}

// Activity types for logging
enum class ActivityType(val label: String, val icon: String) {
    COMMENT("Comment", "comment"), // Default if no other type
    SITE_VISIT("Site Visit", "location"),
    WHATSAPP("WhatsApp", "chat"),
    DETAILS_SENT("Details Sent", "send"),
    SYSTEM("System", "settings"), // For automatic actions
    CUSTOM("Custom", "more")
}

// ==================== DATA MODELS ====================

// ==================== VIEW MODE ====================

enum class ViewMode {
    CARD, LIST, TABLE
}

// ==================== ENUMS ====================

// Pipeline Stages - shown when a person is in the pipeline
enum class PipelineStage(val label: String, val color: Long) {
    FRESH("Fresh", 0xFF10B981),           // Green
    NOT_CONTACTED("Not Contacted", 0xFF6B7280),  // Gray
    FOLLOW_UP("Follow Up", 0xFFF59E0B),    // Yellow/Orange
    IN_CLOSURE("In Closure", 0xFF8B5CF6),  // Purple
    CLOSED("Closed", 0xFF3B82F6)           // Blue
}

// Priority levels - Fixed values (not customizable)
enum class Priority(val id: String, val label: String, val color: Long) {
    NORMAL("normal", "Normal", 0xFF6B7280),           // Neutral Gray
    HIGH("high", "High", 0xFFF59E0B),                  // Orange
    SUPER_HIGH("super_high", "Super High", 0xFFEF4444); // Red

    companion object {
        fun fromId(id: String): Priority {
            return entries.find { it.id == id } ?: NORMAL
        }
    }
}

// Segment for organizing people
enum class Segment(val label: String, val color: Long) {
    VIP("VIP", 0xFFEF4444),
    HOT("Hot", 0xFFEF4444),
    WARM("Warm", 0xFFF59E0B),
    COLD("Cold", 0xFF3B82F6),
    NEW("New", 0xFF10B981),
    QUALIFIED("Qualified", 0xFF8B5CF6),
    VISIT_DONE("Visit Done", 0xFF6366F1)
}

// ==================== CUSTOM ITEMS ====================

/**
 * CustomItem - For customizable pipeline stages, priorities, and segments
 * Allows users to define their own labels and colors
 */
data class CustomItem(
    val id: String,           // Unique identifier (e.g., "stage_1", "priority_high")
    val label: String,        // Display label
    val color: Long,          // Color as Long (ARGB)
    val order: Int = 0        // Sort order
)

// Default Pipeline Stages (converted from enum)
val defaultCustomStages = listOf(
    CustomItem("fresh", "Fresh", 0xFF10B981, 0),
    CustomItem("not_contacted", "Not Contacted", 0xFF6B7280, 1),
    CustomItem("follow_up", "Follow Up", 0xFFF59E0B, 2),
    CustomItem("in_closure", "In Closure", 0xFF8B5CF6, 3),
    CustomItem("closed", "Closed", 0xFF3B82F6, 4)
)

// Fixed Priorities (not customizable)
val fixedPriorities = listOf(
    CustomItem("normal", "Normal", 0xFF6B7280, 0),    // Neutral Gray
    CustomItem("high", "High", 0xFFF59E0B, 1),        // Orange
    CustomItem("super_high", "Super High", 0xFFEF4444, 2)  // Red
)

// For backward compatibility
val defaultCustomPriorities = fixedPriorities

// Default Segments (converted from enum)
val defaultCustomSegments = listOf(
    CustomItem("vip", "VIP", 0xFFEF4444, 0),
    CustomItem("hot", "Hot", 0xFFEF4444, 1),
    CustomItem("warm", "Warm", 0xFFF59E0B, 2),
    CustomItem("cold", "Cold", 0xFF3B82F6, 3),
    CustomItem("new", "New", 0xFF10B981, 4),
    CustomItem("qualified", "Qualified", 0xFF8B5CF6, 5),
    CustomItem("visit_done", "Visit Done", 0xFF6366F1, 6)
)

// Default Sources (converted from enum)
val defaultCustomSources = listOf(
    CustomItem("website", "Website", 0xFF3B82F6, 0),
    CustomItem("referral", "Referral", 0xFF10B981, 1),
    CustomItem("cold_call", "Cold Call", 0xFF6B7280, 2),
    CustomItem("social_media", "Social Media", 0xFF8B5CF6, 3),
    CustomItem("advertisement", "Advertisement", 0xFFF59E0B, 4),
    CustomItem("walk_in", "Walk-in", 0xFF06B6D4, 5),
    CustomItem("whatsapp", "WhatsApp", 0xFF22C55E, 6),
    CustomItem("other", "Other", 0xFF71717A, 7)
)

// Available color palette for customization
val colorPalette = listOf(
    0xFFEF4444, // Red
    0xFFF97316, // Orange
    0xFFF59E0B, // Amber
    0xFFEAB308, // Yellow
    0xFF84CC16, // Lime
    0xFF22C55E, // Green
    0xFF10B981, // Emerald
    0xFF14B8A6, // Teal
    0xFF06B6D4, // Cyan
    0xFF0EA5E9, // Light Blue
    0xFF3B82F6, // Blue
    0xFF6366F1, // Indigo
    0xFF8B5CF6, // Violet
    0xFFA855F7, // Purple
    0xFFD946EF, // Fuchsia
    0xFFEC4899, // Pink
    0xFFF43F5E, // Rose
    0xFF6B7280, // Gray
    0xFF78716C, // Stone
    0xFF71717A  // Zinc
)

// Helper functions to map between enums and custom items (used for defaults)
fun PipelineStage.toCustomItem(): CustomItem = CustomItem(
    id = this.name.lowercase(), 
    label = this.label, 
    color = this.color,
    order = this.ordinal
)

fun Priority.toCustomItem(): CustomItem = CustomItem(
    id = this.id, 
    label = this.label, 
    color = this.color,
    order = this.ordinal
)

fun Segment.toCustomItem(): CustomItem = CustomItem(
    id = this.name.lowercase(), 
    label = this.label, 
    color = this.color,
    order = this.ordinal
)

// Helper to find a custom item by ID or return a fallback
fun List<CustomItem>.findById(id: String, fallback: CustomItem? = null): CustomItem? = 
    this.find { it.id == id } ?: fallback

/**
 * Person - Unified model for contacts and pipeline leads
 * All people are stored in this single model.
 * Pipeline-related fields are only relevant when isInPipeline = true
 */
data class Person(
    val id: Int = 0,                   // Default 0 for Room auto-generate
    // Basic Info
    val name: String,
    val phone: String,
    val alternativePhone: String = "",
    val address: String = "",
    val about: String = "",           // About the person
    val note: String = "",            // Main note/description (was mainNote)
    
    // Organization & Categorization
    val labels: List<String> = emptyList(),  // Tags/Labels for categorization
    val segmentId: String = "new",           // CustomItem ID
    val sourceId: String = "other",          // CustomItem ID (was enum Source)
    val budget: String = "",          // Budget as raw numeric string
    val priorityId: String = "normal",         // Fixed Priority ID
    
    // Pipeline fields (only used when isInPipeline = true)
    val isInPipeline: Boolean = false,
    val stageId: String = "fresh",           // CustomItem ID
    val pipelinePriorityId: String = "normal", // Fixed Priority ID
    
    // Metadata
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    // Tracking for sort options
    val lastOpenedAt: LocalDateTime? = null,       // Last time profile was viewed
    val lastActivityAt: LocalDateTime? = null,     // Last activity/interaction
    val activityCount: Int = 0                 // Number of activities
)

/**
 * Task - Linked to a person with time, reminder support
 */
data class Task(
    val id: Int = 0,                   // Default 0 for Room auto-generate
    val linkedPersonId: Int?,         // Nullable - can be a standalone task
    val type: TaskType,
    val description: String = "",
    
    // Time & Date
    val dueDate: LocalDate,
    val dueTime: LocalTime? = null,   // Optional specific time
    
    // Priority & Status
    val priorityId: String = "normal",       // Fixed Priority ID
    val status: TaskStatus = TaskStatus.PENDING,
    
    // Reminder
    val showReminder: Boolean = false,
    val reminderMinutesBefore: Int = 30,  // Minutes before due time
    
    // Response/Notes
    val response: String = "",        // Task response/notes
    
    // Metadata
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null
)

/**
 * Activity - History of actions on a person (Includes what were previously Notes/Comments)
 */
data class Activity(
    val id: Int = 0,                   // Default 0 for Room auto-generate
    val personId: Int,
    val type: ActivityType = ActivityType.COMMENT,
    val title: String? = null,
    val description: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val recordingPath: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

// ==================== CALL LOG MODELS ====================

/**
 * CallType - Types of calls in the device call log
 */
enum class CallType(val label: String, val color: Long) {
    INCOMING("Incoming", 0xFF10B981),    // Green
    OUTGOING("Outgoing", 0xFF3B82F6),    // Blue
    MISSED("Missed", 0xFFEF4444),         // Red
    REJECTED("Rejected", 0xFFF59E0B),     // Orange
    BLOCKED("Blocked", 0xFF6B7280),       // Gray
    VOICEMAIL("Voicemail", 0xFF8B5CF6),   // Purple
    UNKNOWN("Unknown", 0xFF6B7280)        // Gray
}

/**
 * CallLogEntry - Represents a single call from device call log
 */
data class CallLogEntry(
    val id: Long,
    val phoneNumber: String,
    val normalizedNumber: String,       // For matching with CRM contacts
    val contactName: String?,           // From device contacts
    val callType: CallType,
    val duration: Long,                 // In seconds
    val timestamp: LocalDateTime,
    val isNew: Boolean = false,         // Unseen call
    
    // CRM-specific fields (populated when matched)
    val linkedPersonId: Int? = null,    // Linked Person in CRM
    val linkedPersonName: String? = null,
    val note: String? = null,           // Note attached to this phone number
    val subscriptionId: String? = null, // Subscription ID for dual SIM support
    val recordingPath: String? = null   // Path to recording file if found
)

/**
 * CallLogGroup - Grouped calls by phone number
 */
data class CallLogGroup(
    val phoneNumber: String,
    val normalizedNumber: String,
    val displayName: String,            // From contacts or CRM person
    val calls: List<CallLogEntry>,
    val linkedPersonId: Int? = null,
    val hasNote: Boolean = false
) {
    val totalCalls: Int get() = calls.size
    val lastCall: CallLogEntry? get() = calls.maxByOrNull { it.timestamp }
    val totalDuration: Long get() = calls.sumOf { it.duration }
    val missedCount: Int get() = calls.count { it.callType == CallType.MISSED }
    val incomingCount: Int get() = calls.count { it.callType == CallType.INCOMING }
    val outgoingCount: Int get() = calls.count { it.callType == CallType.OUTGOING }
}

/**
 * DateRange presets for call log filtering
 */
enum class DateRangePreset(val label: String, val days: Int) {
    TODAY("Today", 0),
    LAST_3_DAYS("Last 3 Days", 3),
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_15_DAYS("Last 15 Days", 15),
    LAST_30_DAYS("Last 30 Days", 30),
    CUSTOM("Custom", -1)
}

/**
 * SIM selection for call tracking
 */
enum class CrmFilter(val label: String) {
    ALL("All"),
    IN_CRM("In CRM"),
    NOT_IN_CRM("Not in CRM")
}

/**
 * Filter for device contact status
 */
enum class ContactFilter(val label: String) {
    ALL("All"),
    SAVED("Saved"),
    NOT_SAVED("Not Saved")
}

/**
 * Filter for note status
 */
enum class NoteFilter(val label: String) {
    ALL("All"),
    HAS_NOTE("Has Note"),
    NO_NOTE("No Note")
}

/**
 * SIM selection for call tracking
 */
enum class SimSelection(val label: String) {
    BOTH("Both SIMs"),
    SIM_1("SIM 1"),
    SIM_2("SIM 2")
}

/**
 * Call Settings - Configuration for call tracking
 */
data class CallSettings(
    val simSelection: SimSelection = SimSelection.BOTH,
    val sim1Name: String = "SIM 1",
    val sim2Name: String = "SIM 2",
    val trackingStartDate: LocalDate = LocalDate.now().minusDays(1),
    val autoSyncToActivities: Boolean = true,
    val startContactsWithCallHistory: Boolean = false,
    val recordingPath: String = ""  // Empty means auto-detect
)

// ==================== COLUMN CONFIGURATION ====================

data class ColumnConfig(
    val id: String,
    val label: String,
    val visible: Boolean = true
)

// All available fields for person display
val allPersonFields = listOf(
    ColumnConfig("name", "Name", true),
    ColumnConfig("budget", "Budget", true),
    ColumnConfig("stage", "Stage", true),
    ColumnConfig("priority", "Priority", true),
    ColumnConfig("labels", "Labels", true),
    ColumnConfig("note", "Note", false),
    ColumnConfig("phone", "Phone", false),
    ColumnConfig("segment", "Segment", false),
    ColumnConfig("address", "Address", false),
    ColumnConfig("source", "Source", false),
    ColumnConfig("created", "Created Date", false),
    ColumnConfig("modified", "Modified Date", false),
    ColumnConfig("lastActivity", "Last Activity", false)
)

// Default visible fields for pipeline
val defaultPipelineFields = listOf("name", "budget", "stage", "priority", "labels")

// Default visible fields for contacts
val defaultContactFields = listOf("name", "phone", "segment", "labels")

val defaultPipelineColumns = listOf(
    ColumnConfig("name", "Name", true),
    ColumnConfig("budget", "Budget", true),
    ColumnConfig("stage", "Stage", true),
    ColumnConfig("priority", "Priority", true),
    ColumnConfig("labels", "Labels", true),
    ColumnConfig("note", "Note", false),
    ColumnConfig("phone", "Phone", false),
    ColumnConfig("segment", "Segment", false),
    ColumnConfig("address", "Address", false),
    ColumnConfig("source", "Source", false),
    ColumnConfig("created", "Created Date", false),
    ColumnConfig("modified", "Modified Date", false),
    ColumnConfig("lastActivity", "Last Activity", false)
)

val defaultContactColumns = listOf(
    ColumnConfig("name", "Name", true),
    ColumnConfig("phone", "Phone", true),
    ColumnConfig("segment", "Segment", true),
    ColumnConfig("labels", "Labels", true),
    ColumnConfig("note", "Note", false),
    ColumnConfig("budget", "Budget", false),
    ColumnConfig("address", "Address", false),
    ColumnConfig("source", "Source", false),
    ColumnConfig("created", "Created Date", false),
    ColumnConfig("modified", "Modified Date", false),
    ColumnConfig("lastActivity", "Last Activity", false)
)

// ==================== HELPER FUNCTIONS ====================

fun LocalDate.toDisplayString(): String {
    val today = LocalDate.now()
    return when {
        this == today -> "Today"
        this == today.minusDays(1) -> "Yesterday"
        this == today.plusDays(1) -> "Tomorrow"
        this.year == today.year -> format(DateTimeFormatter.ofPattern("MMM dd"))
        else -> format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
}

fun LocalDate.toFullDisplayString(): String = format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))

fun LocalTime.toDisplayString(): String = format(DateTimeFormatter.ofPattern("hh:mm a"))
fun LocalTime.toFullDisplayString(): String = format(DateTimeFormatter.ofPattern("hh:mm:ss a"))

fun LocalDateTime.toDisplayString(): String {
    val now = LocalDateTime.now()
    val today = LocalDate.now()
    val date = toLocalDate()
    val duration = Duration.between(this, now)
    val seconds = duration.seconds
    val absoluteSeconds = Math.abs(seconds)
    
    // Future handling (simple)
    if (seconds < 0) {
        return when {
            date == today -> "Today, ${toLocalTime().toDisplayString()}"
            date == today.plusDays(1) -> "Tomorrow, ${toLocalTime().toDisplayString()}"
            date.year == today.year -> format(DateTimeFormatter.ofPattern("MMM dd"))
            else -> format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        }
    }

    // Past handling (Relative)
    return when {
        absoluteSeconds < 10 -> "now"
        absoluteSeconds < 60 -> "$absoluteSeconds sec ago"
        absoluteSeconds < 3600 -> {
            val mins = absoluteSeconds / 60
            "$mins min${if (mins > 1) "s" else ""} ago"
        }
        absoluteSeconds < 6 * 3600 -> {
            val hrs = absoluteSeconds / 3600
            "$hrs hr${if (hrs > 1) "s" else ""} ago"
        }
        date == today -> "${toLocalTime().toDisplayString()} Today"
        date == today.minusDays(1) -> "${toLocalTime().toDisplayString()} Yesterday"
        date.year == today.year -> format(DateTimeFormatter.ofPattern("MMM dd"))
        else -> format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }
}

fun LocalDateTime.toFullDateTimeString(): String {
    return "${toLocalDate().toFullDisplayString()} at ${toLocalTime().toFullDisplayString()}"
}

fun LocalDate.toFullDateTimeString(time: LocalTime? = null): String {
    val datePart = toFullDisplayString()
    return if (time != null) "$datePart at ${time.toFullDisplayString()}" else datePart
}

// ==================== HELPER FUNCTIONS ====================

fun LocalDate.isOverdue(): Boolean = this.isBefore(LocalDate.now())
fun LocalDate.isDueSoon(): Boolean {
    val today = LocalDate.now()
    return this == today || this == today.plusDays(1)
}

/**
 * Format budget value with multiplier
 * @param budgetStr The numeric budget string (e.g., "150", "85")
 * @param currencySymbol Currency symbol (e.g., "₹", "$")
 * @param multiplier Budget multiplier (1 = as-is, 1000 = K, 100000 = Lakh)
 * @return Formatted budget string (e.g., "₹1.5 Cr", "₹85 L")
 */
fun formatBudget(budgetStr: String, currencySymbol: String = "₹", multiplier: Int = 100000): String {
    if (budgetStr.isBlank()) return ""
    
    val numericValue = budgetStr.toDoubleOrNull() ?: return budgetStr
    
    // Calculate actual value (budget * multiplier)
    val actualValue = numericValue * multiplier
    
    return when {
        // 1 Crore = 100 Lakhs = 10,000,000
        actualValue >= 10_000_000 -> {
            val crores = actualValue / 10_000_000
            if (crores == crores.toLong().toDouble()) {
                "$currencySymbol${crores.toLong()} Cr"
            } else {
                "$currencySymbol${"%.1f".format(crores).trimEnd('0').trimEnd('.')} Cr"
            }
        }
        // 1 Lakh = 100,000
        actualValue >= 100_000 -> {
            val lakhs = actualValue / 100_000
            if (lakhs == lakhs.toLong().toDouble()) {
                "$currencySymbol${lakhs.toLong()} L"
            } else {
                "$currencySymbol${"%.1f".format(lakhs).trimEnd('0').trimEnd('.')} L"
            }
        }
        // 1 Thousand = 1,000
        actualValue >= 1_000 -> {
            val thousands = actualValue / 1_000
            if (thousands == thousands.toLong().toDouble()) {
                "$currencySymbol${thousands.toLong()}K"
            } else {
                "$currencySymbol${"%.1f".format(thousands).trimEnd('0').trimEnd('.')}K"
            }
        }
        else -> "$currencySymbol${actualValue.toLong()}"
    }
}

// ==================== SAMPLE DATA ====================

object SampleData {
    val availableLabels = emptyList<String>()
    val people = mutableListOf<Person>()
    val tasks = mutableListOf<Task>()
    val activities = mutableListOf<Activity>()
}
