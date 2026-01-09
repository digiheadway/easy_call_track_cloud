package com.miniclick.calltrackmanage.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.miniclick.calltrackmanage.R
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager that displays a floating caller ID overlay during calls.
 * Shows contact info, notes, labels, and call statistics for known contacts.
 */
object CallerIdManager {

    private const val TAG = "CallerIdManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentPhoneNumber: String? = null

    @SuppressLint("ClickableViewAccessibility")
    fun show(context: Context, phoneNumber: String) {
        val appContext = context.applicationContext
        
        // Check if caller ID is enabled in settings first
        val settingsRepository = SettingsRepository.getInstance(appContext)
        if (!settingsRepository.isCallerIdEnabled()) {
            Log.d(TAG, "Caller ID is disabled in settings")
            return
        }
        
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(appContext)) {
                Log.d(TAG, "Overlay permission not granted")
                return
            }
        }
        
        // Normalize phone number using repository for consistent lookup
        val callDataRepository = CallDataRepository.getInstance(appContext)
        val normalizedPhone = callDataRepository.normalizePhoneNumber(phoneNumber)
        
        val customLookupEnabled = settingsRepository.isCustomLookupEnabled()
        val customLookupCallerIdEnabled = settingsRepository.isCustomLookupCallerIdEnabled()
        
        // Avoid duplicate overlays for same number
        if (overlayView != null && currentPhoneNumber == normalizedPhone) {
            Log.d(TAG, "Overlay already showing for $normalizedPhone")
            return
        }
        
        currentPhoneNumber = normalizedPhone
        
        // Fetch data
        scope.launch {
            val personData = withContext(Dispatchers.IO) {
                callDataRepository.getPersonData(normalizedPhone)
            }
            
            var customData: String? = null
            if (customLookupEnabled && customLookupCallerIdEnabled) {
                customData = withContext(Dispatchers.IO) {
                    fetchCustomLookupSnippet(appContext, normalizedPhone)
                }
            }
            
            // Only show overlay if we have meaningful data (note, label, call history, or custom lookup)
            if ((personData != null && hasRelevantData(personData)) || !customData.isNullOrBlank()) {
                createAndShowOverlay(appContext, phoneNumber, personData, customData)
            } else {
                Log.d(TAG, "No relevant data for $normalizedPhone, skipping overlay")
            }
        }
    }

    private suspend fun fetchCustomLookupSnippet(context: Context, phoneNumber: String): String? {
        try {
            val settingsRepository = SettingsRepository.getInstance(context)
            val baseUrl = settingsRepository.getCustomLookupUrl().ifEmpty { 
                "https://prop.digiheadway.in/api/calls/caller_id.php?phone={phone}"
            }
            
            val url = if (baseUrl.contains("{phone}")) {
                baseUrl.replace("{phone}", phoneNumber)
            } else {
                val separator = if (baseUrl.contains("?")) "&" else "?"
                "${baseUrl}${separator}phone=${phoneNumber}"
            }

            val response = com.miniclick.calltrackmanage.network.NetworkClient.api.fetchData(url)
            if (response.isSuccessful) {
                val body = response.body() ?: return null
                // Create a small snippet from the JSON
                val entries = body.entries.take(3).filter { it.value != null && it.value.toString().isNotBlank() }
                return if (entries.isNotEmpty()) {
                    entries.joinToString(", ") { "${it.key}: ${it.value}" }
                } else {
                    "Data found"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch custom lookup for overlay", e)
        }
        return null
    }

    private fun hasRelevantData(person: PersonDataEntity): Boolean {
        // Show overlay if we have any of: note, label, contact name, or call history
        return !person.personNote.isNullOrBlank() ||
               !person.label.isNullOrBlank() ||
               !person.contactName.isNullOrBlank() ||
               person.totalCalls > 0
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createAndShowOverlay(context: Context, phoneNumber: String, person: PersonDataEntity?, customData: String? = null) {
        try {
            // Remove previous if exists
            hide(context)

            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutInflater = LayoutInflater.from(context)
            val view = layoutInflater.inflate(R.layout.caller_id_overlay, null)
            overlayView = view

            // Set up window layout params
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT
            )
            layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

            // Populate the overlay with data
            populateOverlay(view, phoneNumber, person, customData)

            // Set up touch listener for dragging
            setupDragListener(view, layoutParams)

            // Add view to window
            windowManager?.addView(view, layoutParams)
            Log.d(TAG, "Overlay shown for ${person?.contactName ?: phoneNumber}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    private fun populateOverlay(view: View, phoneNumber: String, person: PersonDataEntity?, customData: String? = null) {
        // Name
        view.findViewById<TextView>(R.id.nameText).text = person?.contactName?.takeIf { it.isNotBlank() } ?: phoneNumber

        // Phone number
        view.findViewById<TextView>(R.id.phoneText).text = formatPhoneNumber(phoneNumber)

        // Label
        val labelText = view.findViewById<TextView>(R.id.labelText)
        if (person != null && !person.label.isNullOrBlank()) {
            labelText.text = person.label
            labelText.visibility = View.VISIBLE
        } else {
            labelText.visibility = View.GONE
        }

        // Stats row
        val statsRow = view.findViewById<LinearLayout>(R.id.statsRow)
        if (person != null && person.totalCalls > 0) {
            statsRow.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.callsText).text = "${person.totalCalls} calls"
            view.findViewById<TextView>(R.id.durationText).text = formatDuration(person.totalDuration)
        } else {
            statsRow.visibility = View.GONE
        }

        // Note
        val noteContainer = view.findViewById<LinearLayout>(R.id.noteContainer)
        if (person != null && !person.personNote.isNullOrBlank()) {
            noteContainer.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.noteText).text = person.personNote
        } else {
            noteContainer.visibility = View.GONE
        }

        // Custom Lookup
        val lookupContainer = view.findViewById<LinearLayout>(R.id.lookupContainer)
        if (!customData.isNullOrBlank()) {
            lookupContainer.visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.lookupText).text = customData
        } else {
            lookupContainer.visibility = View.GONE
        }

        // Card click to open app
        view.findViewById<LinearLayout>(R.id.cardContainer).setOnClickListener {
            openAppWithLookup(view.context, phoneNumber)
            hide(view.context)
        }

        // Close button
        view.findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            hide(view.context)
        }
    }

    private fun openAppWithLookup(context: Context, phoneNumber: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.apply {
                putExtra("phone_lookup", phoneNumber)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app with lookup", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        val cardContainer = view.findViewById<LinearLayout>(R.id.cardContainer)
        cardContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    fun hide(context: Context) {
        try {
            overlayView?.let {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(it)
                overlayView = null
            }
            currentPhoneNumber = null
            Log.d(TAG, "Overlay hidden")
        } catch (e: Exception) {
            // Ignore if view was already removed
            overlayView = null
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        val cleaned = phone.replace(Regex("[^0-9+]"), "")
        return when {
            cleaned.startsWith("+91") && cleaned.length == 13 -> {
                "${cleaned.substring(0, 3)} ${cleaned.substring(3, 8)} ${cleaned.substring(8)}"
            }
            cleaned.length == 10 -> {
                "${cleaned.substring(0, 5)} ${cleaned.substring(5)}"
            }
            else -> phone
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}

