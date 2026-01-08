package com.miniclick.calltrackmanage.data

import android.content.Context
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

class SettingsRepository private constructor(private val context: Context) {

    private val PREFS_NAME = "app_prefs"
    private val KEY_SIM_SELECTION = "sim_selection"
    private val KEY_TRACK_START_DATE = "track_start_date"
    private val KEY_OWN_PHONE_NUMBER = "own_phone_number"
    private val KEY_ORGANISATION_ID = "organisation_id"
    private val KEY_USER_ID = "user_id"
    private val KEY_CALLER_PHONE_SIM1 = "caller_phone_sim1"
    private val KEY_CALLER_PHONE_SIM2 = "caller_phone_sim2"
    private val KEY_WHATSAPP_PREFERENCE = "whatsapp_preference"
    private val KEY_LAST_SYNC_TIME = "last_sync_time"
    private val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private val KEY_SIM1_SUB_ID = "sim1_sub_id"
    private val KEY_SIM2_SUB_ID = "sim2_sub_id"
    private val KEY_SIM1_CALIBRATION_HINT = "sim1_calibration_hint"
    private val KEY_SIM2_CALIBRATION_HINT = "sim2_calibration_hint"
    private val KEY_ALLOW_PERSONAL_EXCLUSION = "allow_personal_exclusion"
    private val KEY_ALLOW_CHANGING_TRACK_START_DATE = "allow_changing_track_start_date"
    private val KEY_ALLOW_UPDATING_TRACK_SIMS = "allow_updating_track_sims"
    private val KEY_DEFAULT_TRACK_START_DATE = "default_track_start_date"
    private val KEY_EXCLUDED_CONTACTS = "excluded_contacts"
    private val KEY_CUSTOM_LOOKUP_URL = "custom_lookup_url"
    private val KEY_CUSTOM_LOOKUP_ENABLED = "custom_lookup_enabled"
    private val KEY_CUSTOM_LOOKUP_CALLER_ID_ENABLED = "custom_lookup_caller_id_enabled"
    private val KEY_CALL_TRACK_ENABLED = "call_track_enabled"
    private val KEY_CALL_RECORD_ENABLED = "call_record_enabled"
    private val KEY_PLAN_EXPIRY_DATE = "plan_expiry_date"
    private val KEY_ALLOWED_STORAGE_GB = "allowed_storage_gb"
    private val KEY_STORAGE_USED_BYTES = "storage_used_bytes"
    private val KEY_ONBOARDING_OFFLINE = "onboarding_offline"

    private val KEY_TRACK_START_DATE_SET = "track_start_date_set"
    private val KEY_USER_DECLINED_RECORDING = "user_declined_recording"
    private val KEY_RECORDING_LAST_ENABLED_TIMESTAMP = "recording_last_enabled_timestamp"
    private val KEY_DIALER_ENABLED = "dialer_enabled"
    private val KEY_SHOW_RECORDING_REMINDER = "show_recording_reminder"
    private val KEY_SHOW_UNKNOWN_NOTE_REMINDER = "show_unknown_note_reminder"
    private val KEY_AGREEMENT_ACCEPTED = "agreement_accepted"
    private val KEY_SKIPPED_STEPS = "skipped_onboarding_steps"
    private val KEY_SHORT_CALL_THRESHOLD_SECONDS = "short_call_threshold_seconds"
    private val KEY_SHOW_DIAL_BUTTON = "show_dial_button"
    private val KEY_CALL_ACTION_BEHAVIOR = "call_action_behavior" // "Direct", "Dialpad"

    // Persistence for Home Screen State
    private val KEY_SEARCH_VISIBLE = "search_visible"
    private val KEY_FILTERS_VISIBLE = "filters_visible"
    private val KEY_SEARCH_QUERY = "search_query"
    private val KEY_FILTER_CALL_TYPE = "filter_call_type"
    private val KEY_FILTER_CONNECTED = "filter_connected"
    private val KEY_FILTER_NOTES = "filter_notes"
    private val KEY_FILTER_CONTACTS = "filter_contacts"
    private val KEY_FILTER_ATTENDED = "filter_attended"
    private val KEY_FILTER_PERSON_NOTES = "filter_person_notes"
    private val KEY_FILTER_REVIEWED = "filter_reviewed"
    private val KEY_FILTER_CUSTOM_NAME = "filter_custom_name"
    private val KEY_FILTER_PERSON_TYPE = "filter_person_type"
    private val KEY_FILTER_LABEL = "filter_label"
    private val KEY_FILTER_DATE_RANGE = "filter_date_range"
    private val KEY_CUSTOM_START_DATE = "filter_custom_start_date"
    private val KEY_CUSTOM_END_DATE = "filter_custom_end_date"
    private val KEY_SEARCH_HISTORY = "search_history"
    private val MAX_SEARCH_HISTORY = 10

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null
        
        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getSim1SubscriptionId(): Int? {
        val id = prefs.getInt(KEY_SIM1_SUB_ID, -1)
        return if (id != -1) id else null
    }

    fun setSim1SubscriptionId(id: Int) {
        prefs.edit().putInt(KEY_SIM1_SUB_ID, id).apply()
    }

    fun getSim2SubscriptionId(): Int? {
        val id = prefs.getInt(KEY_SIM2_SUB_ID, -1)
        return if (id != -1) id else null
    }

    fun setSim2SubscriptionId(id: Int) {
        prefs.edit().putInt(KEY_SIM2_SUB_ID, id).apply()
    }

    // WhatsApp Preference: Package Name or "Always Ask"
    fun getWhatsappPreference(): String {
        return prefs.getString(KEY_WHATSAPP_PREFERENCE, "Always Ask") ?: "Always Ask"
    }

    fun setWhatsappPreference(preference: String) {
        prefs.edit().putString(KEY_WHATSAPP_PREFERENCE, preference).apply()
    }

    // Sim Selection: "Both", "Sim1", "Sim2", "Off"
    // Default is "Off" so user must explicitly enable and configure SIMs
    fun getSimSelection(): String {
        return prefs.getString(KEY_SIM_SELECTION, "Off") ?: "Off"
    }

    fun setSimSelection(selection: String) {
        prefs.edit().putString(KEY_SIM_SELECTION, selection).apply()
    }

    fun getSimSelectionFlow(): Flow<String> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_SIM_SELECTION) {
                trySend(getSimSelection())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getSimSelection())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Track Start Date
    // Default is Yesterday (Today - 1)
    fun getTrackStartDate(): Long {
        val date = prefs.getLong(KEY_TRACK_START_DATE, 0L)
        if (date != 0L) return date
        
        // Default to yesterday at start of day
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getTrackStartDateFlow(): Flow<Long> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_TRACK_START_DATE) {
                trySend(getTrackStartDate())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getTrackStartDate())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setTrackStartDate(date: Long) {
        prefs.edit()
            .putLong(KEY_TRACK_START_DATE, date)
            .putBoolean(KEY_TRACK_START_DATE_SET, true)
            .apply()
    }

    fun isTrackStartDateSet(): Boolean = prefs.getBoolean(KEY_TRACK_START_DATE_SET, false)
    fun setTrackStartDateSet(set: Boolean) = prefs.edit().putBoolean(KEY_TRACK_START_DATE_SET, set).apply()

    fun getOwnPhoneNumber(): String? {
        return prefs.getString(KEY_OWN_PHONE_NUMBER, null)
    }

    fun setOwnPhoneNumber(number: String) {
        prefs.edit().putString(KEY_OWN_PHONE_NUMBER, number).apply()
    }

    fun getOrganisationId(): String {
        return prefs.getString(KEY_ORGANISATION_ID, "") ?: ""
    }

    fun setOrganisationId(id: String) {
        prefs.edit().putString(KEY_ORGANISATION_ID, id).apply()
    }

    fun getOrganisationIdFlow(): Flow<String> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_ORGANISATION_ID) {
                trySend(getOrganisationId())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getOrganisationId())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun getUserId(): String {
        return prefs.getString(KEY_USER_ID, "") ?: ""
    }

    fun setUserId(id: String) {
        prefs.edit().putString(KEY_USER_ID, id).apply()
    }

    fun getCallerPhoneSim1(): String {
        return prefs.getString(KEY_CALLER_PHONE_SIM1, "") ?: ""
    }

    fun setCallerPhoneSim1(phone: String) {
        prefs.edit().putString(KEY_CALLER_PHONE_SIM1, phone).apply()
    }

    fun getCallerPhoneSim2(): String {
        return prefs.getString(KEY_CALLER_PHONE_SIM2, "") ?: ""
    }

    fun setCallerPhoneSim2(phone: String) {
        prefs.edit().putString(KEY_CALLER_PHONE_SIM2, phone).apply()
    }

    private val KEY_THEME_MODE = "theme_mode" // "System", "Light", "Dark"

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "System") ?: "System"
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun setLastSyncTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, time).apply()
    }

    fun getOnboardingCompletedFlow(): Flow<Boolean> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_ONBOARDING_COMPLETED || key == KEY_ONBOARDING_OFFLINE) {
                trySend(isOnboardingCompleted())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isOnboardingCompleted())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false) || prefs.getBoolean(KEY_ONBOARDING_OFFLINE, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isOnboardingOffline(): Boolean = prefs.getBoolean(KEY_ONBOARDING_OFFLINE, false)
    fun setOnboardingOffline(offline: Boolean) = prefs.edit().putBoolean(KEY_ONBOARDING_OFFLINE, offline).apply()

    fun getSim1CalibrationHint(): String? {
        return prefs.getString(KEY_SIM1_CALIBRATION_HINT, null)
    }

    fun setSim1CalibrationHint(hint: String?) {
        prefs.edit().putString(KEY_SIM1_CALIBRATION_HINT, hint).apply()
    }

    fun getSim2CalibrationHint(): String? {
        return prefs.getString(KEY_SIM2_CALIBRATION_HINT, null)
    }

    fun setSim2CalibrationHint(hint: String?) {
        prefs.edit().putString(KEY_SIM2_CALIBRATION_HINT, hint).apply()
    }

    fun isAllowPersonalExclusion(): Boolean = prefs.getBoolean(KEY_ALLOW_PERSONAL_EXCLUSION, false)
    fun setAllowPersonalExclusion(allow: Boolean) = prefs.edit().putBoolean(KEY_ALLOW_PERSONAL_EXCLUSION, allow).apply()

    fun isAllowChangingTrackStartDate(): Boolean = prefs.getBoolean(KEY_ALLOW_CHANGING_TRACK_START_DATE, false)
    fun setAllowChangingTrackStartDate(allow: Boolean) = prefs.edit().putBoolean(KEY_ALLOW_CHANGING_TRACK_START_DATE, allow).apply()

    fun isAllowUpdatingTrackSims(): Boolean = prefs.getBoolean(KEY_ALLOW_UPDATING_TRACK_SIMS, false)
    fun setAllowUpdatingTrackSims(allow: Boolean) = prefs.edit().putBoolean(KEY_ALLOW_UPDATING_TRACK_SIMS, allow).apply()

    fun getDefaultTrackStartDate(): String? = prefs.getString(KEY_DEFAULT_TRACK_START_DATE, null)
    fun setDefaultTrackStartDate(date: String?) = prefs.edit().putString(KEY_DEFAULT_TRACK_START_DATE, date).apply()

    fun getExcludedContacts(): Set<String> = prefs.getStringSet(KEY_EXCLUDED_CONTACTS, emptySet()) ?: emptySet()
    fun setExcludedContacts(contacts: Set<String>) = prefs.edit().putStringSet(KEY_EXCLUDED_CONTACTS, contacts).apply()

    fun isNumberExcluded(number: String): Boolean {
        val excluded = getExcludedContacts()
        // Clean number for comparison
        val cleanNumber = number.replace("[^\\d]".toRegex(), "")
        return excluded.any { it.replace("[^\\d]".toRegex(), "") == cleanNumber }
    }

    // Caller ID Overlay Settings
    private val KEY_CALLER_ID_ENABLED = "caller_id_enabled"
    
    fun isCallerIdEnabled(): Boolean = prefs.getBoolean(KEY_CALLER_ID_ENABLED, false)
    fun setCallerIdEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_CALLER_ID_ENABLED, enabled).apply()

    fun getCustomLookupUrl(): String = prefs.getString(KEY_CUSTOM_LOOKUP_URL, "") ?: ""
    fun setCustomLookupUrl(url: String) = prefs.edit().putString(KEY_CUSTOM_LOOKUP_URL, url).apply()

    fun isCustomLookupEnabled(): Boolean = prefs.getBoolean(KEY_CUSTOM_LOOKUP_ENABLED, false)
    fun setCustomLookupEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_CUSTOM_LOOKUP_ENABLED, enabled).apply()

    fun isCustomLookupCallerIdEnabled(): Boolean = prefs.getBoolean(KEY_CUSTOM_LOOKUP_CALLER_ID_ENABLED, false)
    fun setCustomLookupCallerIdEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_CUSTOM_LOOKUP_CALLER_ID_ENABLED, enabled).apply()

    fun clearAllSettings() {
        prefs.edit().clear().apply()
    }

    fun isCallTrackEnabled(): Boolean = prefs.getBoolean(KEY_CALL_TRACK_ENABLED, true)
    fun setCallTrackEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_CALL_TRACK_ENABLED, enabled).apply()

    fun isCallRecordEnabled(): Boolean = prefs.getBoolean(KEY_CALL_RECORD_ENABLED, true)
    fun setCallRecordEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_CALL_RECORD_ENABLED, enabled).apply()

    fun getPlanExpiryDate(): String? = prefs.getString(KEY_PLAN_EXPIRY_DATE, null)
    fun setPlanExpiryDate(date: String?) = prefs.edit().putString(KEY_PLAN_EXPIRY_DATE, date).apply()

    fun getAllowedStorageGb(): Float = prefs.getFloat(KEY_ALLOWED_STORAGE_GB, 0f)
    fun setAllowedStorageGb(gb: Float) = prefs.edit().putFloat(KEY_ALLOWED_STORAGE_GB, gb).apply()

    fun getStorageUsedBytes(): Long = prefs.getLong(KEY_STORAGE_USED_BYTES, 0L)
    fun setStorageUsedBytes(bytes: Long) = prefs.edit().putLong(KEY_STORAGE_USED_BYTES, bytes).apply()

    fun isUserDeclinedRecording(): Boolean = prefs.getBoolean(KEY_USER_DECLINED_RECORDING, false)
    fun setUserDeclinedRecording(declined: Boolean) = prefs.edit().putBoolean(KEY_USER_DECLINED_RECORDING, declined).apply()

    fun getRecordingLastEnabledTimestamp(): Long = prefs.getLong(KEY_RECORDING_LAST_ENABLED_TIMESTAMP, 0L)
    fun setRecordingLastEnabledTimestamp(timestamp: Long) = prefs.edit().putLong(KEY_RECORDING_LAST_ENABLED_TIMESTAMP, timestamp).apply()

    fun isDialerEnabled(): Boolean = prefs.getBoolean(KEY_DIALER_ENABLED, true)
    fun setDialerEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_DIALER_ENABLED, enabled).apply()

    fun isShowRecordingReminder(): Boolean = prefs.getBoolean(KEY_SHOW_RECORDING_REMINDER, true)
    fun setShowRecordingReminder(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_RECORDING_REMINDER, show).apply()

    fun isShowUnknownNoteReminder(): Boolean = prefs.getBoolean(KEY_SHOW_UNKNOWN_NOTE_REMINDER, true)
    fun setShowUnknownNoteReminder(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_UNKNOWN_NOTE_REMINDER, show).apply()

    fun isShowDialButton(): Boolean = prefs.getBoolean(KEY_SHOW_DIAL_BUTTON, true)
    fun setShowDialButton(show: Boolean) = prefs.edit().putBoolean(KEY_SHOW_DIAL_BUTTON, show).apply()

    fun getCallActionBehavior(): String = prefs.getString(KEY_CALL_ACTION_BEHAVIOR, "Direct") ?: "Direct"
    fun setCallActionBehavior(behavior: String) = prefs.edit().putString(KEY_CALL_ACTION_BEHAVIOR, behavior).apply()

    // Home Screen State Persistence
    fun isSearchVisible(): Boolean = prefs.getBoolean(KEY_SEARCH_VISIBLE, false)
    fun setSearchVisible(visible: Boolean) = prefs.edit().putBoolean(KEY_SEARCH_VISIBLE, visible).apply()

    fun isFiltersVisible(): Boolean = prefs.getBoolean(KEY_FILTERS_VISIBLE, false)
    fun setFiltersVisible(visible: Boolean) = prefs.edit().putBoolean(KEY_FILTERS_VISIBLE, visible).apply()

    fun getSearchQuery(): String = prefs.getString(KEY_SEARCH_QUERY, "") ?: ""
    fun setSearchQuery(query: String) = prefs.edit().putString(KEY_SEARCH_QUERY, query).apply()

    fun getCallTypeFilter(): String = prefs.getString(KEY_FILTER_CALL_TYPE, "ALL") ?: "ALL"
    fun setCallTypeFilter(filter: String) = prefs.edit().putString(KEY_FILTER_CALL_TYPE, filter).apply()

    fun getPersonTabFilter(): String = prefs.getString(KEY_FILTER_PERSON_TYPE, "ALL") ?: "ALL"
    fun setPersonTabFilter(filter: String) = prefs.edit().putString(KEY_FILTER_PERSON_TYPE, filter).apply()

    fun getConnectedFilter(): String = prefs.getString(KEY_FILTER_CONNECTED, "ALL") ?: "ALL"
    fun setConnectedFilter(filter: String) = prefs.edit().putString(KEY_FILTER_CONNECTED, filter).apply()

    fun getNotesFilter(): String = prefs.getString(KEY_FILTER_NOTES, "ALL") ?: "ALL"
    fun setNotesFilter(filter: String) = prefs.edit().putString(KEY_FILTER_NOTES, filter).apply()

    fun getContactsFilter(): String = prefs.getString(KEY_FILTER_CONTACTS, "ALL") ?: "ALL"
    fun setContactsFilter(filter: String) = prefs.edit().putString(KEY_FILTER_CONTACTS, filter).apply()

    fun getAttendedFilter(): String = prefs.getString(KEY_FILTER_ATTENDED, "ALL") ?: "ALL"
    fun setAttendedFilter(filter: String) = prefs.edit().putString(KEY_FILTER_ATTENDED, filter).apply()

    fun getPersonNotesFilter(): String = prefs.getString(KEY_FILTER_PERSON_NOTES, "ALL") ?: "ALL"
    fun setPersonNotesFilter(filter: String) = prefs.edit().putString(KEY_FILTER_PERSON_NOTES, filter).apply()

    fun getReviewedFilter(): String = prefs.getString(KEY_FILTER_REVIEWED, "ALL") ?: "ALL"
    fun setReviewedFilter(filter: String) = prefs.edit().putString(KEY_FILTER_REVIEWED, filter).apply()

    fun getCustomNameFilter(): String = prefs.getString(KEY_FILTER_CUSTOM_NAME, "ALL") ?: "ALL"
    fun setCustomNameFilter(filter: String) = prefs.edit().putString(KEY_FILTER_CUSTOM_NAME, filter).apply()

    fun getLabelFilter(): String = prefs.getString(KEY_FILTER_LABEL, "") ?: ""
    fun setLabelFilter(label: String) = prefs.edit().putString(KEY_FILTER_LABEL, label).apply()

    fun getDateRangeFilter(): String = prefs.getString(KEY_FILTER_DATE_RANGE, "LAST_7_DAYS") ?: "LAST_7_DAYS"
    fun setDateRangeFilter(filter: String) = prefs.edit().putString(KEY_FILTER_DATE_RANGE, filter).apply()

    fun getDateRangeFlow(): Flow<String> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_FILTER_DATE_RANGE) {
                trySend(getDateRangeFilter())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getDateRangeFilter())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun getCustomStartDate(): Long = prefs.getLong(KEY_CUSTOM_START_DATE, 0L)
    fun setCustomStartDate(date: Long) = prefs.edit().putLong(KEY_CUSTOM_START_DATE, date).apply()

    fun getCustomEndDate(): Long = prefs.getLong(KEY_CUSTOM_END_DATE, 0L)
    fun setCustomEndDate(date: Long) = prefs.edit().putLong(KEY_CUSTOM_END_DATE, date).apply()

    fun isAgreementAccepted(): Boolean = prefs.getBoolean(KEY_AGREEMENT_ACCEPTED, false)
    fun setAgreementAccepted(accepted: Boolean) = prefs.edit().putBoolean(KEY_AGREEMENT_ACCEPTED, accepted).apply()

    fun getAgreementAcceptedFlow(): Flow<Boolean> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_AGREEMENT_ACCEPTED) {
                trySend(isAgreementAccepted())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isAgreementAccepted())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun getSkippedSteps(): Set<String> = prefs.getStringSet(KEY_SKIPPED_STEPS, emptySet()) ?: emptySet()
    
    fun setStepSkipped(step: String, skipped: Boolean) {
        val current = getSkippedSteps().toMutableSet()
        if (skipped) current.add(step) else current.remove(step)
        prefs.edit().putStringSet(KEY_SKIPPED_STEPS, current).apply()
    }

    // Search History
    fun getSearchHistory(): List<String> {
        val historySet = prefs.getStringSet(KEY_SEARCH_HISTORY, emptySet()) ?: emptySet()
        return historySet.toList().take(MAX_SEARCH_HISTORY)
    }

    fun addSearchHistory(query: String) {
        if (query.isBlank() || query.length < 2) return
        val current = getSearchHistory().toMutableList()
        current.remove(query) // Remove if exists to avoid duplicates
        current.add(0, query) // Add to front
        val trimmed = current.take(MAX_SEARCH_HISTORY).toSet()
        prefs.edit().putStringSet(KEY_SEARCH_HISTORY, trimmed).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    // Short Call Alert Threshold (default 10 seconds)
    fun getShortCallThresholdSeconds(): Int = prefs.getInt(KEY_SHORT_CALL_THRESHOLD_SECONDS, 10)
    fun setShortCallThresholdSeconds(seconds: Int) = prefs.edit().putInt(KEY_SHORT_CALL_THRESHOLD_SECONDS, seconds).apply()
}
