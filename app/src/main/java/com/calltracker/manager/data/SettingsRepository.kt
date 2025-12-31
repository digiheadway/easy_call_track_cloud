package com.calltracker.manager.data

import android.content.Context
import java.util.Calendar

class SettingsRepository(private val context: Context) {

    private val PREFS_NAME = "app_prefs"
    private val KEY_SIM_SELECTION = "sim_selection"
    private val KEY_TRACK_START_DATE = "track_start_date"
    private val KEY_OWN_PHONE_NUMBER = "own_phone_number"
    private val KEY_ORGANISATION_ID = "organisation_id"
    private val KEY_USER_ID = "user_id"
    private val KEY_CALLER_PHONE_SIM1 = "caller_phone_sim1"
    private val KEY_CALLER_PHONE_SIM2 = "caller_phone_sim2"
    private val KEY_WHATSAPP_PREFERENCE = "whatsapp_preference"

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // WhatsApp Preference: Package Name or "Always Ask"
    fun getWhatsappPreference(): String {
        return prefs.getString(KEY_WHATSAPP_PREFERENCE, "Always Ask") ?: "Always Ask"
    }

    fun setWhatsappPreference(preference: String) {
        prefs.edit().putString(KEY_WHATSAPP_PREFERENCE, preference).apply()
    }

    // Sim Selection: "Both", "Anyone"
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

    fun clearAllSettings() {
        prefs.edit().clear().apply()
    }
}
