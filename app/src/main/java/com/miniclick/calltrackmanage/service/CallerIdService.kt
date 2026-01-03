package com.miniclick.calltrackmanage.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
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
 * Service that displays a floating caller ID overlay during calls.
 * Shows contact info, notes, labels, and call statistics for known contacts.
 */
class CallerIdService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentPhoneNumber: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CallerIdService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                if (phoneNumber != null) {
                    showOverlay(phoneNumber)
                }
            }
            ACTION_HIDE -> {
                hideOverlay()
                stopSelf()
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        Log.d(TAG, "CallerIdService destroyed")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOverlay(phoneNumber: String) {
        // Check if caller ID is enabled in settings first
        val settingsRepository = SettingsRepository.getInstance(applicationContext)
        if (!settingsRepository.isCallerIdEnabled()) {
            Log.d(TAG, "Caller ID is disabled in settings")
            return
        }
        
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(applicationContext)) {
                Log.d(TAG, "Overlay permission not granted")
                return
            }
        }
        
        // Normalize phone number using repository for consistent lookup
        val callDataRepository = CallDataRepository.getInstance(applicationContext)
        val normalizedPhone = callDataRepository.normalizePhoneNumber(phoneNumber)
        
        // Avoid duplicate overlays for same number
        if (overlayView != null && currentPhoneNumber == normalizedPhone) {
            Log.d(TAG, "Overlay already showing for $normalizedPhone")
            return
        }
        
        currentPhoneNumber = normalizedPhone
        
        // Fetch person data from repository
        serviceScope.launch {
            val personData = callDataRepository.getPersonData(normalizedPhone)
            
            // Only show overlay if we have meaningful data (note, label, or call history)
            if (personData != null && hasRelevantData(personData)) {
                withContext(Dispatchers.Main) {
                    createAndShowOverlay(phoneNumber, personData)
                }
            } else {
                Log.d(TAG, "No relevant data for $normalizedPhone, skipping overlay")
            }
        }
    }

    private fun hasRelevantData(person: PersonDataEntity): Boolean {
        // Show overlay if we have any of: note, label, contact name, or call history
        return !person.personNote.isNullOrBlank() ||
               !person.label.isNullOrBlank() ||
               !person.contactName.isNullOrBlank() ||
               person.totalCalls > 0
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createAndShowOverlay(phoneNumber: String, person: PersonDataEntity) {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutInflater = LayoutInflater.from(this)
            overlayView = layoutInflater.inflate(R.layout.caller_id_overlay, null)

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
            populateOverlay(overlayView!!, phoneNumber, person)

            // Set up touch listener for dragging
            setupDragListener(overlayView!!, layoutParams)

            // Add view to window
            windowManager?.addView(overlayView, layoutParams)
            Log.d(TAG, "Overlay shown for ${person.contactName ?: phoneNumber}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    private fun populateOverlay(view: View, phoneNumber: String, person: PersonDataEntity) {
        // Name
        val nameText = view.findViewById<TextView>(R.id.nameText)
        nameText.text = person.contactName ?: phoneNumber

        // Phone number
        val phoneText = view.findViewById<TextView>(R.id.phoneText)
        phoneText.text = formatPhoneNumber(phoneNumber)

        // Label
        val labelText = view.findViewById<TextView>(R.id.labelText)
        if (!person.label.isNullOrBlank()) {
            labelText.text = person.label
            labelText.visibility = View.VISIBLE
        } else {
            labelText.visibility = View.GONE
        }

        // Stats row (call count and duration)
        val statsRow = view.findViewById<LinearLayout>(R.id.statsRow)
        if (person.totalCalls > 0) {
            statsRow.visibility = View.VISIBLE

            val callsText = view.findViewById<TextView>(R.id.callsText)
            callsText.text = "${person.totalCalls} calls"

            val durationText = view.findViewById<TextView>(R.id.durationText)
            durationText.text = formatDuration(person.totalDuration)
        } else {
            statsRow.visibility = View.GONE
        }

        // Note
        val noteContainer = view.findViewById<LinearLayout>(R.id.noteContainer)
        val noteText = view.findViewById<TextView>(R.id.noteText)
        if (!person.personNote.isNullOrBlank()) {
            noteContainer.visibility = View.VISIBLE
            noteText.text = person.personNote
        } else {
            noteContainer.visibility = View.GONE
        }

        // Close button
        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)
        closeButton.setOnClickListener {
            hideOverlay()
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

    private fun hideOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
            currentPhoneNumber = null
            Log.d(TAG, "Overlay hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding overlay", e)
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        // Simple formatting: add spaces for Indian numbers
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

    companion object {
        private const val TAG = "CallerIdService"
        const val ACTION_SHOW = "com.miniclick.calltrackmanage.SHOW_CALLER_ID"
        const val ACTION_HIDE = "com.miniclick.calltrackmanage.HIDE_CALLER_ID"
        const val EXTRA_PHONE_NUMBER = "phone_number"

        fun show(context: Context, phoneNumber: String) {
            val intent = Intent(context, CallerIdService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, CallerIdService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }
    }
}
