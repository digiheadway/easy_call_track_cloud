package com.example.callyzer4.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteStorage(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("call_notes", Context.MODE_PRIVATE)
    }
    
    suspend fun saveNote(phoneNumber: String, note: String) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString("note_$phoneNumber", note)
            .apply()
    }
    
    suspend fun getNote(phoneNumber: String): String = withContext(Dispatchers.IO) {
        prefs.getString("note_$phoneNumber", "") ?: ""
    }
    
    suspend fun getAllNotes(): Map<String, String> = withContext(Dispatchers.IO) {
        val allEntries = prefs.all
        allEntries.filterKeys { it.startsWith("note_") }
            .mapKeys { it.key.removePrefix("note_") }
            .mapValues { it.value as? String ?: "" }
    }
    
    suspend fun deleteNote(phoneNumber: String) = withContext(Dispatchers.IO) {
        prefs.edit()
            .remove("note_$phoneNumber")
            .apply()
    }
}
