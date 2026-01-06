package com.example.smsblaster.data.model

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey

private const val TAG = "Template"

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Extract placeholders like {name}, {phone}, {custom_key}
    fun getPlaceholders(): List<String> {
        val regex = Regex("\\{([^}]+)\\}")
        return regex.findAll(content).map { it.groupValues[1] }.toList()
    }
    
    // Replace placeholders with actual values - case insensitive
    fun formatMessage(contact: Contact): String {
        var message = content
        
        Log.d(TAG, "Formatting message for contact: ${contact.name}")
        Log.d(TAG, "Template content: $content")
        Log.d(TAG, "Contact customKeys: ${contact.customKeys}")
        
        // Replace built-in placeholders (case insensitive)
        val nameRegex = Regex("\\{name\\}", RegexOption.IGNORE_CASE)
        val phoneRegex = Regex("\\{phone\\}", RegexOption.IGNORE_CASE)
        
        if (nameRegex.containsMatchIn(message)) {
            Log.d(TAG, "Found {name} placeholder, replacing with: ${contact.name}")
            message = message.replace(nameRegex, contact.name)
        }
        
        if (phoneRegex.containsMatchIn(message)) {
            Log.d(TAG, "Found {phone} placeholder, replacing with: ${contact.phone}")
            message = message.replace(phoneRegex, contact.phone)
        }
        
        // Replace custom keys (case insensitive)
        contact.customKeys.forEach { (key, value) ->
            val keyRegex = Regex("\\{${Regex.escape(key)}\\}", RegexOption.IGNORE_CASE)
            if (keyRegex.containsMatchIn(message)) {
                Log.d(TAG, "Found {$key} placeholder, replacing with: $value")
                message = message.replace(keyRegex, value)
            }
        }
        
        Log.d(TAG, "Final message: $message")
        return message
    }
    
    // Preview message for a specific contact
    fun previewMessage(contact: Contact): String {
        return formatMessage(contact)
    }
}
