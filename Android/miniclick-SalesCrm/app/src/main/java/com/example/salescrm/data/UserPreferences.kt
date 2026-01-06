package com.example.salescrm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

// DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Theme modes
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM  // Follow system setting
}

// Preference keys
object PreferencesKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    val DEFAULT_COUNTRY = stringPreferencesKey("default_country") // ISO code e.g. "US", "IN"
    val BUDGET_MULTIPLIER = intPreferencesKey("budget_multiplier") // e.g., 1000 for "K", 100000 for "L"
    val PIPELINE_VISIBLE_FIELDS = stringPreferencesKey("pipeline_visible_fields") // Comma-separated field IDs
    val CONTACT_VISIBLE_FIELDS = stringPreferencesKey("contact_visible_fields") // Comma-separated field IDs
    val TASK_VIEW_MODE = stringPreferencesKey("task_view_mode") // LIST or DATE_WISE
    
    // Login state
    val IS_LOGGED_IN = stringPreferencesKey("is_logged_in")
    val LOGGED_IN_PHONE = stringPreferencesKey("logged_in_phone")
    
    // Custom items storage (as JSON)
    val CUSTOM_STAGES = stringPreferencesKey("custom_stages")
    val CUSTOM_PRIORITIES = stringPreferencesKey("custom_priorities")
    val CUSTOM_SEGMENTS = stringPreferencesKey("custom_segments")
    val CUSTOM_SOURCES = stringPreferencesKey("custom_sources")
    
    // CRM Data storage
    val PEOPLE = stringPreferencesKey("people_data")
    val TASKS = stringPreferencesKey("tasks_data")
    val ACTIVITIES = stringPreferencesKey("activities_data")
    val CALL_SETTINGS = stringPreferencesKey("call_settings_data")
    val HAS_SEEDED_DATA = stringPreferencesKey("has_seeded_data")
    val LAST_SCREEN = stringPreferencesKey("last_screen")
    val LAST_CLOUD_SYNC_TIMESTAMP = stringPreferencesKey("last_cloud_sync_timestamp")
    val CALLER_ID_ENABLED = stringPreferencesKey("caller_id_enabled")
    val DEFAULT_WHATSAPP_PACKAGE = stringPreferencesKey("default_whatsapp_package")
}

// JSON serialization helpers for All Models
private fun CustomItem.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("label", label); put("color", color); put("order", order)
}

private fun JSONObject.toCustomItem(): CustomItem = CustomItem(
    id = getString("id"), label = getString("label"), color = getLong("color"), order = optInt("order", 0)
)

private fun Person.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("name", name); put("phone", phone); put("alternativePhone", alternativePhone)
    put("address", address); put("about", about); put("note", note)
    put("labels", JSONArray(labels)); put("segmentId", segmentId); put("sourceId", sourceId)
    put("budget", budget); put("priorityId", priorityId); put("isInPipeline", isInPipeline)
    put("stageId", stageId); put("pipelinePriorityId", pipelinePriorityId)
    put("createdAt", createdAt.toString()); put("updatedAt", updatedAt.toString())
    put("lastOpenedAt", lastOpenedAt?.toString()); put("lastActivityAt", lastActivityAt?.toString())
    put("activityCount", activityCount)
}

private fun JSONObject.toPerson(): Person = Person(
    id = getInt("id"), name = getString("name"), phone = getString("phone"),
    alternativePhone = optString("alternativePhone"), address = optString("address"),
    about = optString("about"), note = optString("note").ifBlank { optString("mainNote") },
    labels = optJSONArray("labels")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
    // Handle both old "segment" (enum name) and new "segmentId"
    segmentId = optString("segmentId", optString("segment", "new")).lowercase(),
    // Handle both old "source" (enum name) and new "sourceId"
    sourceId = optString("sourceId", optString("source", "other")).lowercase(),
    budget = optString("budget"), 
    // Handle both old "priority" (enum name) and new "priorityId"
    priorityId = optString("priorityId", optString("priority", "none")).lowercase(),
    isInPipeline = optBoolean("isInPipeline"), 
    // Handle both old "pipelineStage" (enum name) and new "stageId"
    stageId = optString("stageId", optString("pipelineStage", "fresh")).lowercase(),
    // Handle both old "pipelinePriority" (enum name) and new "pipelinePriorityId"
    pipelinePriorityId = optString("pipelinePriorityId", optString("pipelinePriority", "medium")).lowercase(),
    createdAt = LocalDateTime.parse(optString("createdAt", LocalDateTime.now().toString())),
    updatedAt = LocalDateTime.parse(optString("updatedAt", LocalDateTime.now().toString())),
    lastOpenedAt = optString("lastOpenedAt", "").takeIf { it.isNotBlank() }?.let { LocalDateTime.parse(it) },
    lastActivityAt = optString("lastActivityAt", "").takeIf { it.isNotBlank() }?.let { LocalDateTime.parse(it) },
    activityCount = optInt("activityCount", 0)
)

private fun Task.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("linkedPersonId", linkedPersonId ?: -1); put("type", type.name)
    put("description", description); put("dueDate", dueDate.toString()); put("dueTime", dueTime?.toString())
    put("priorityId", priorityId); put("status", status.name); put("showReminder", showReminder)
    put("reminderMinutesBefore", reminderMinutesBefore); put("response", response)
    put("createdAt", createdAt.toString()); put("completedAt", completedAt?.toString())
}

private fun JSONObject.toTask(): Task = Task(
    id = getInt("id"), linkedPersonId = getInt("linkedPersonId").takeIf { it != -1 },
    type = TaskType.valueOf(getString("type")), description = optString("description"),
    dueDate = LocalDate.parse(getString("dueDate")), 
    dueTime = optString("dueTime", "").takeIf { it.isNotBlank() }?.let { java.time.LocalTime.parse(it) },
    // Handle both old "priority" (enum name) and new "priorityId"
    priorityId = optString("priorityId", optString("priority", "medium")).lowercase(),
    status = TaskStatus.valueOf(optString("status", TaskStatus.PENDING.name)),
    showReminder = optBoolean("showReminder"), reminderMinutesBefore = optInt("reminderMinutesBefore", 30),
    response = optString("response"), createdAt = LocalDateTime.parse(optString("createdAt", LocalDateTime.now().toString())),
    completedAt = optString("completedAt", "").takeIf { it.isNotBlank() }?.let { LocalDateTime.parse(it) }
)

private fun Activity.toJson(): JSONObject = JSONObject().apply {
    put("id", id); put("personId", personId); put("type", type.name)
    put("title", title ?: ""); put("description", description)
    put("timestamp", timestamp.toString())
    put("recordingPath", recordingPath ?: "")
    val metaJson = JSONObject()
    metadata.forEach { (k, v) -> metaJson.put(k, v) }
    put("metadata", metaJson)
}

private fun JSONObject.toActivity(): Activity {
    val metaObj = optJSONObject("metadata")
    val metaMap = mutableMapOf<String, String>()
    metaObj?.keys()?.forEach { key ->
        metaMap[key] = metaObj.getString(key)
    }
    
    return Activity(
        id = getInt("id"), 
        personId = getInt("personId"), 
        type = try { ActivityType.valueOf(optString("type", ActivityType.SYSTEM.name)) } catch(e: Exception) { ActivityType.SYSTEM },
        title = optString("title", "").takeIf { it.isNotBlank() },
        description = optString("description", ""),
        timestamp = LocalDateTime.parse(optString("timestamp", LocalDateTime.now().toString())),
        recordingPath = optString("recordingPath", "").takeIf { it.isNotBlank() },
        metadata = metaMap
    )
}

private fun CallSettings.toJson(): JSONObject = JSONObject().apply {
    put("simSelection", simSelection.name)
    put("sim1Name", sim1Name)
    put("sim2Name", sim2Name)
    put("trackingStartDate", trackingStartDate.toString())
    put("autoSyncToActivities", autoSyncToActivities)
    put("startContactsWithCallHistory", startContactsWithCallHistory)
    put("recordingPath", recordingPath)
}

private fun JSONObject.toCallSettings(): CallSettings = CallSettings(
    simSelection = optString("simSelection").let { try { SimSelection.valueOf(it) } catch(e: Exception) { SimSelection.BOTH } },
    sim1Name = optString("sim1Name", "SIM 1"),
    sim2Name = optString("sim2Name", "SIM 2"),
    trackingStartDate = optString("trackingStartDate").let { try { LocalDate.parse(it) } catch(e: Exception) { LocalDate.now().minusDays(1) } },
    autoSyncToActivities = optBoolean("autoSyncToActivities", true),
    startContactsWithCallHistory = optBoolean("startContactsWithCallHistory", false),
    recordingPath = optString("recordingPath", "")
)


private fun List<CustomItem>.toJsonString(): String = JSONArray().apply { this@toJsonString.forEach { put(it.toJson()) } }.toString()
private fun String.toCustomItemList(): List<CustomItem> = try {
    val jsonArray = JSONArray(this)
    (0 until jsonArray.length()).map { jsonArray.getJSONObject(it).toCustomItem() }
} catch (e: Exception) { emptyList() }

private fun List<Person>.toPeopleJson(): String = JSONArray().apply { this@toPeopleJson.forEach { put(it.toJson()) } }.toString()
private fun String.toPeopleList(): List<Person> = try {
    val jsonArray = JSONArray(this)
    (0 until jsonArray.length()).map { jsonArray.getJSONObject(it).toPerson() }
} catch (e: Exception) { emptyList() }

private fun List<Task>.toTasksJson(): String = JSONArray().apply { this@toTasksJson.forEach { put(it.toJson()) } }.toString()
private fun String.toTasksList(): List<Task> = try {
    val jsonArray = JSONArray(this)
    (0 until jsonArray.length()).map { jsonArray.getJSONObject(it).toTask() }
} catch (e: Exception) { emptyList() }

private fun List<Activity>.toActivitiesJson(): String = JSONArray().apply { this@toActivitiesJson.forEach { put(it.toJson()) } }.toString()
private fun String.toActivitiesList(): List<Activity> = try {
    val jsonArray = JSONArray(this)
    (0 until jsonArray.length()).map { jsonArray.getJSONObject(it).toActivity() }
} catch (e: Exception) { emptyList() }

// User preference repository
class UserPreferencesRepository(private val context: Context) {
    
    companion object {
        // Default visible fields
        val DEFAULT_PIPELINE_FIELDS = listOf("name", "budget", "stage", "priority", "labels")
        val DEFAULT_CONTACT_FIELDS = listOf("name", "phone", "segment", "labels")
    }
    
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        try {
            ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.DARK.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.DARK
        }
    }

    val currencySymbol: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: "$"
    }

    val defaultCountry: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_COUNTRY] ?: "US"
    }

    val budgetMultiplier: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BUDGET_MULTIPLIER] ?: 1  // Default: no multiplier
    }

    val pipelineVisibleFields: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PIPELINE_VISIBLE_FIELDS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: DEFAULT_PIPELINE_FIELDS
    }

    val contactVisibleFields: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CONTACT_VISIBLE_FIELDS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: DEFAULT_CONTACT_FIELDS
    }

    val taskViewMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TASK_VIEW_MODE] ?: "DATE_WISE" // Default to DATE_WISE (calendar view)
    }
    
    // Login state flows
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_LOGGED_IN] == "true"
    }
    
    val loggedInPhone: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LOGGED_IN_PHONE] ?: ""
    }
    
    // Custom items flows
    val customStages: Flow<List<CustomItem>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_STAGES]?.toCustomItemList()?.takeIf { it.isNotEmpty() }
            ?: defaultCustomStages
    }
    
    val customPriorities: Flow<List<CustomItem>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_PRIORITIES]?.toCustomItemList()?.takeIf { it.isNotEmpty() }
            ?: defaultCustomPriorities
    }
    
    val customSegments: Flow<List<CustomItem>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_SEGMENTS]?.toCustomItemList()?.takeIf { it.isNotEmpty() }
            ?: defaultCustomSegments
    }
    
    val customSources: Flow<List<CustomItem>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_SOURCES]?.toCustomItemList()?.takeIf { it.isNotEmpty() }
            ?: defaultCustomSources
    }
    
    // CRM data flows
    val people: Flow<List<Person>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PEOPLE]?.toPeopleList()?.takeIf { it.isNotEmpty() }
            ?: SampleData.people
    }
    
    val tasks: Flow<List<Task>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TASKS]?.toTasksList()?.takeIf { it.isNotEmpty() }
            ?: SampleData.tasks
    }
    
    val activities: Flow<List<Activity>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVITIES]?.toActivitiesList()?.takeIf { it.isNotEmpty() }
            ?: SampleData.activities
    }

    val callSettings: Flow<CallSettings> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CALL_SETTINGS]?.let {
            try { JSONObject(it).toCallSettings() } catch (e: Exception) { CallSettings() }
        } ?: CallSettings()
    }
    
    // Has seeded data flag
    val hasSeededData: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_SEEDED_DATA] == "true"
    }
    
    suspend fun setHasSeededData(hasSeeded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEDED_DATA] = if (hasSeeded) "true" else "false"
        }
    }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun setDefaultCountry(countryCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_COUNTRY] = countryCode
        }
    }

    suspend fun setBudgetMultiplier(multiplier: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BUDGET_MULTIPLIER] = multiplier
        }
    }

    suspend fun setPipelineVisibleFields(fields: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIPELINE_VISIBLE_FIELDS] = fields.joinToString(",")
        }
    }

    suspend fun setContactVisibleFields(fields: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTACT_VISIBLE_FIELDS] = fields.joinToString(",")
        }
    }

    suspend fun resetPipelineFieldsToDefault() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.PIPELINE_VISIBLE_FIELDS)
        }
    }

    suspend fun resetContactFieldsToDefault() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CONTACT_VISIBLE_FIELDS)
        }
    }

    suspend fun setTaskViewMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASK_VIEW_MODE] = mode
        }
    }
    
    // Custom items setters
    suspend fun setCustomStages(stages: List<CustomItem>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_STAGES] = stages.toJsonString()
        }
    }
    
    suspend fun setCustomPriorities(priorities: List<CustomItem>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_PRIORITIES] = priorities.toJsonString()
        }
    }
    
    suspend fun setCustomSegments(segments: List<CustomItem>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_SEGMENTS] = segments.toJsonString()
        }
    }
    
    suspend fun setCustomSources(sources: List<CustomItem>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_SOURCES] = sources.toJsonString()
        }
    }
    
    suspend fun resetCustomStages() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CUSTOM_STAGES)
        }
    }
    
    suspend fun resetCustomPriorities() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CUSTOM_PRIORITIES)
        }
    }
    
    suspend fun resetCustomSegments() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CUSTOM_SEGMENTS)
        }
    }
    
    suspend fun resetCustomSources() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CUSTOM_SOURCES)
        }
    }
    
    // CRM Data setters
    suspend fun savePeople(people: List<Person>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PEOPLE] = people.toPeopleJson()
        }
    }
    
    suspend fun saveTasks(tasks: List<Task>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASKS] = tasks.toTasksJson()
        }
    }
    
    suspend fun saveActivities(activities: List<Activity>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVITIES] = activities.toActivitiesJson()
        }
    }

    suspend fun saveCallSettings(settings: CallSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALL_SETTINGS] = settings.toJson().toString()
        }
    }
    
    // Login/Logout methods
    suspend fun login(phone: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_LOGGED_IN] = "true"
            preferences[PreferencesKeys.LOGGED_IN_PHONE] = phone
        }
    }
    
    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_LOGGED_IN] = "false"
            preferences[PreferencesKeys.LOGGED_IN_PHONE] = ""
        }
    }
    
    val lastScreen: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_SCREEN] ?: "People"
    }

    suspend fun saveLastScreen(screenName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SCREEN] = screenName
        }
    }
    
    // Cloud sync timestamp
    val lastCloudSyncTimestamp: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_CLOUD_SYNC_TIMESTAMP]
    }
    
    suspend fun getLastCloudSyncTimestampOnce(): String? {
        return context.dataStore.data.map { it[PreferencesKeys.LAST_CLOUD_SYNC_TIMESTAMP] }.first()
    }
    
    suspend fun saveLastCloudSyncTimestamp(timestamp: String?) {
        context.dataStore.edit { preferences ->
            if (timestamp != null) {
                preferences[PreferencesKeys.LAST_CLOUD_SYNC_TIMESTAMP] = timestamp
            } else {
                preferences.remove(PreferencesKeys.LAST_CLOUD_SYNC_TIMESTAMP)
            }
        }
    }
    
    // Config save methods for cloud sync
    suspend fun saveThemeMode(mode: ThemeMode) = setThemeMode(mode)
    suspend fun saveCurrency(symbol: String) = setCurrencySymbol(symbol)
    suspend fun saveDefaultCountry(countryCode: String) = setDefaultCountry(countryCode)
    suspend fun saveBudgetMultiplier(multiplier: Int) = setBudgetMultiplier(multiplier)
    suspend fun saveTaskViewMode(mode: TaskViewMode) = setTaskViewMode(mode.name)
    
    // Caller ID feature toggle
    val callerIdEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CALLER_ID_ENABLED] == "true"
    }
    
    suspend fun setCallerIdEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CALLER_ID_ENABLED] = if (enabled) "true" else "false"
        }
    }

    val defaultWhatsAppPackage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_WHATSAPP_PACKAGE] ?: "always_ask"
    }
    
    suspend fun setDefaultWhatsAppPackage(packageName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_WHATSAPP_PACKAGE] = packageName
        }
    }
}
