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
    // Default is "Both" so app works out-of-box without calibration
    fun getSimSelection(): String {
        return prefs.getString(KEY_SIM_SELECTION, "Both") ?: "Both"
    }

    fun setSimSelection(selection: String) {
        prefs.edit().putString(KEY_SIM_SELECTION, selection).apply()
    }

    // Track Start Date
    // Default is Yesterday (Today - 1)
    fun getTrackStartDate(): Long {
        val defaultDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return prefs.getLong(KEY_TRACK_START_DATE, defaultDate)
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
        prefs.edit().putLong(KEY_TRACK_START_DATE, date).apply()
    }

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

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

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
    
    fun isCallerIdEnabled(): Boolean = prefs.getBoolean(KEY_CALLER_ID_ENABLED, true)
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
}
