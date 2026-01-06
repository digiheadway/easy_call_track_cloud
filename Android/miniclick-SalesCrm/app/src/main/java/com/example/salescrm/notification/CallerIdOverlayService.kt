package com.example.salescrm.notification

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.salescrm.MainActivity
import com.example.salescrm.R
import com.example.salescrm.data.CallLogRepository

class CallerIdOverlayService : Service() {

    companion object {
        private const val TAG = "CallerIdOverlay"
        private const val CHANNEL_ID = "caller_id_overlay_channel"
        private const val NOTIFICATION_ID = 60001
        
        const val EXTRA_PERSON_ID = "extra_person_id"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_DETAILS = "extra_details"
        const val EXTRA_BUDGET = "extra_budget"
        const val EXTRA_STAGE = "extra_stage"
        const val EXTRA_PRIORITY_LABEL = "extra_priority_label"
        const val EXTRA_SEGMENT = "extra_segment"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_DEFAULT_COUNTRY = "extra_default_country"
        const val ACTION_SHOW = "action_show"
        const val ACTION_HIDE = "action_hide"
        
        fun canShowOverlay(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> {
                hideOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SHOW -> {
                val personId = intent.getIntExtra(EXTRA_PERSON_ID, -1)
                val name = intent.getStringExtra(EXTRA_NAME) ?: "Unknown"
                val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
                val type = intent.getStringExtra(EXTRA_TYPE) ?: "Call"
                val details = intent.getStringExtra(EXTRA_DETAILS) ?: ""
                val budget = intent.getStringExtra(EXTRA_BUDGET) ?: ""
                val stage = intent.getStringExtra(EXTRA_STAGE) ?: ""
                val priorityLabel = intent.getStringExtra(EXTRA_PRIORITY_LABEL) ?: ""
                val segment = intent.getStringExtra(EXTRA_SEGMENT) ?: ""
                val note = intent.getStringExtra(EXTRA_NOTE) ?: ""
                val defaultCountry = intent.getStringExtra(EXTRA_DEFAULT_COUNTRY) ?: "US"
                
                startForeground(NOTIFICATION_ID, createNotification(name, phone))
                showOverlay(personId, name, phone, type, details, budget, stage, priorityLabel, segment, note, defaultCountry)
            }
            else -> {
                // Default action
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Caller ID Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows caller ID banner during calls"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(name: String, phone: String): Notification {
        val hideIntent = Intent(this, CallerIdOverlayService::class.java).apply {
            action = ACTION_HIDE
        }
        val hidePendingIntent = PendingIntent.getService(
            this, 0, hideIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Caller ID Active")
            .setContentText("$name - $phone")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.drawable.ic_notification, "Hide", hidePendingIntent)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun showOverlay(
        personId: Int,
        name: String,
        phone: String,
        type: String,
        details: String,
        budget: String,
        stage: String,
        priorityLabel: String,
        segment: String,
        note: String,
        defaultCountry: String
    ) {
        if (!canShowOverlay(this)) {
            Log.e(TAG, "Cannot show overlay - permission not granted")
            stopSelf()
            return
        }

        hideOverlay() // Remove any existing overlay

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            val displayMetrics = resources.displayMetrics
            y = (displayMetrics.heightPixels * 0.3).toInt() 
        }

        overlayView = createOverlayView(personId, name, phone, type, details, budget, stage, priorityLabel, segment, note, defaultCountry)

        try {
            windowManager?.addView(overlayView, layoutParams)
            animateIn()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
            stopSelf()
        }
    }

    private fun createOverlayView(
        personId: Int,
        name: String,
        phone: String,
        type: String,
        details: String,
        budget: String,
        stage: String,
        priorityLabel: String,
        segment: String,
        note: String,
        defaultCountry: String
    ): View {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.caller_id_overlay, null)

        // Find views
        val cardContainer = view.findViewById<View>(R.id.cardContainer)
        val nameText = view.findViewById<TextView>(R.id.nameText)
        val budgetText = view.findViewById<TextView>(R.id.budgetText)
        val stageText = view.findViewById<TextView>(R.id.stageText)
        val priorityText = view.findViewById<TextView>(R.id.priorityText)
        val segmentText = view.findViewById<TextView>(R.id.segmentText)
        val noteText = view.findViewById<TextView>(R.id.noteText)
        
        val stageContainer = view.findViewById<View>(R.id.stageContainer)
        val priorityContainer = view.findViewById<View>(R.id.priorityContainer)
        val segmentContainer = view.findViewById<View>(R.id.segmentContainer)
        val noteContainer = view.findViewById<View>(R.id.noteContainer)
        val closeButton = view.findViewById<View>(R.id.closeButton)

        // Set data
        nameText.text = name
        
        if (budget.isNotBlank()) {
            budgetText.text = budget
            budgetText.visibility = View.VISIBLE
        } else {
            budgetText.visibility = View.GONE
        }

        if (stage.isNotBlank()) {
            stageText.text = stage
            stageContainer.visibility = View.VISIBLE
        } else {
            stageContainer.visibility = View.GONE
        }

        if (priorityLabel.isNotBlank()) {
            priorityText.text = priorityLabel
            priorityContainer.visibility = View.VISIBLE
        } else {
            priorityContainer.visibility = View.GONE
        }

        if (segment.isNotBlank()) {
            segmentText.text = segment
            segmentContainer.visibility = View.VISIBLE
        } else {
            segmentContainer.visibility = View.GONE
        }

        if (note.isNotBlank()) {
            noteText.text = note
            noteContainer.visibility = View.VISIBLE
        } else {
            noteContainer.visibility = View.GONE
        }

        // Close button logic
        closeButton.setOnClickListener {
            hideOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        // Card click logic: Open profile
        cardContainer.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_person_id", personId)
                putExtra("action", "open_profile")
            }
            startActivity(intent)
            
            // Dismiss on click
            hideOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        // Make draggable while respecting clicks
        var initialY = 0
        var initialTouchY = 0f
        var isMoving = false
        val touchSlop = 10 // pixels to distinguish click from drag
        
        cardContainer.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val params = overlayView?.layoutParams as? WindowManager.LayoutParams
                    initialY = params?.y ?: 0
                    initialTouchY = event.rawY
                    isMoving = false
                    false // Return false to allow ClickListener to receive event
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(event.rawY - initialTouchY) > touchSlop) {
                        isMoving = true
                        val params = overlayView?.layoutParams as? WindowManager.LayoutParams
                        if (params != null) {
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(overlayView, params)
                        }
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isMoving) {
                        true // Consume if it was a drag
                    } else {
                        v.performClick() // Trigger the click listener manually if not moved
                        true
                    }
                }
                else -> false
            }
        }

        return view
    }

    private fun animateIn() {
        overlayView?.let { view ->
            view.alpha = 0f
            view.translationY = -100f
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            ObjectAnimator.ofFloat(view, "translationY", -100f, 0f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay view", e)
            }
        }
        overlayView = null
    }
}
