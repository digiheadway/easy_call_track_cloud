package com.miniclickcrm.deviceadmin3.manager

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Typeface
import android.view.ViewGroup
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.miniclickcrm.deviceadmin3.R
import android.content.Intent
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import com.miniclickcrm.deviceadmin3.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LockOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private val securityManager = SecurityManager(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    fun showOverlay() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(context)) {
            Log.w("LockOverlay", "No overlay permission")
            return
        }

        Log.d("LockOverlay", "Showing overlay")
        
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        // Use simple, compatible flags
        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            flags,
            PixelFormat.RGBA_8888  // Use RGBA_8888 for best compatibility
        ).apply {
            gravity = Gravity.CENTER
            // Use this to try and cover the status bar area
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            // Ensure focusable so we can type PIN
            this.flags = this.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        }

        val prefs = context.getSharedPreferences("device_admin_prefs", Context.MODE_PRIVATE)
        
        val layout = object : LinearLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_HOME) {
                    return true 
                }
                return super.dispatchKeyEvent(event)
            }
        }.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FF1a1a2e"))  // Dark blue-black, fully opaque
            val density = context.resources.displayMetrics.density
            setPadding((32 * density).toInt(), (48 * density).toInt(), (32 * density).toInt(), (32 * density).toInt())
            
            // Don't set layer type - let Android use the default for best compatibility
            
            var isPinInputVisible = false
            
            // Reusable UI update function
            fun updateUi(view: LinearLayout) {
                view.removeAllViews()
                
                Log.d("LockOverlay", "Building UI content")
                
                val currentMessage = prefs.getString("message", "Your device has been frozen due to loan non-compliance.") ?: ""
                val callTo = prefs.getString("call_to", "") ?: ""

                // 1. Locked Icon
                val icon = ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_lock_lock)
                    setColorFilter(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams((64 * density).toInt(), (64 * density).toInt()).apply {
                        setMargins(0, 0, 0, (20 * density).toInt())
                    }
                }
                view.addView(icon)
                Log.d("LockOverlay", "Added icon")

                val header = TextView(context).apply {
                    text = "DEVICE FROZEN"
                    setTextColor(Color.WHITE)
                    textSize = 32f
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, (4 * density).toInt())
                    setTypeface(null, Typeface.BOLD)
                }
                view.addView(header)

                // 2.0.1 Pairing Code (Small)
                val pairingCode = prefs.getString("pairing_code", "N/A")
                val pairingCodeTv = TextView(context).apply {
                    text = "Pairing Code: $pairingCode"
                    setTextColor(Color.GRAY)
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, (12 * density).toInt())
                }
                view.addView(pairingCodeTv)

                // 2.1 Pending Amount (if > 0)
                val amount = prefs.getInt("amount", 0)
                if (amount > 0) {
                    val amountTv = TextView(context).apply {
                        text = "Pending Amount: ₹$amount"
                        setTextColor(Color.YELLOW)
                        textSize = 24f
                        gravity = Gravity.CENTER
                        setPadding(0, 0, 0, (16 * density).toInt())
                        setTypeface(null, Typeface.BOLD)
                    }
                    view.addView(amountTv)
                }

                // 3. Message
                val msgTv = TextView(context).apply {
                    text = currentMessage
                    setTextColor(Color.LTGRAY)
                    textSize = 18f
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, (32 * density).toInt())
                }
                view.addView(msgTv)

                // 4. Refresh Status Button
                val btnRefresh = Button(context).apply {
                    text = "REFRESH STATUS"
                    setBackgroundColor(Color.parseColor("#33FFFFFF"))
                    setTextColor(Color.WHITE)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        val pc = prefs.getString("pairing_code", null)
                        if (pc != null) {
                            scope.launch {
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        RetrofitClient.apiService.checkStatus(pc)
                                    }
                                    if (response.success && response.data != null) {
                                        prefs.edit().apply {
                                            putInt("amount", response.data.amount)
                                            putString("message", response.data.message)
                                            putBoolean("is_freezed", response.data.is_freezed)
                                            putString("call_to", response.data.call_to)
                                            apply()
                                        }
                                        if (!response.data.is_freezed) {
                                            val dm = DeviceManager(context)
                                            dm.unfreezeDevice()
                                            hideOverlay()
                                        } else {
                                            updateUi(view)
                                            Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
                                        }
                                        val dm = DeviceManager(context)
                                        dm.setProtected(response.data.is_protected)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (60 * density).toInt()).apply {
                        setMargins(0, 0, 0, (12 * density).toInt())
                    }
                }
                view.addView(btnRefresh)

                // 5. Call Manager Button
                if (callTo.isNotEmpty()) {
                    val btnCall = Button(context).apply {
                        text = "CALL $callTo"
                        setBackgroundColor(Color.parseColor("#FF2ECC71")) // Success Green
                        setTextColor(Color.WHITE)
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            Log.d("LockOverlay", "Call button clicked for: $callTo")
                            Toast.makeText(context, "Opening Phone...", Toast.LENGTH_SHORT).show()
                            try {
                                val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                                val telUri = android.net.Uri.parse("tel:${callTo.trim()}")
                                if (permission == PackageManager.PERMISSION_GRANTED) {
                                    try {
                                        val intent = Intent(Intent.ACTION_CALL, telUri).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Intent.ACTION_DIAL, telUri).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        }
                                        context.startActivity(intent)
                                    }
                                } else {
                                    val intent = Intent(Intent.ACTION_DIAL, telUri).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    }
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                Log.e("LockOverlay", "Call setup failed", e)
                                Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                            }
                        }
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (60 * density).toInt()).apply {
                            setMargins(0, 0, 0, (20 * density).toInt())
                        }
                    }
                    view.addView(btnCall)
                }

                // 6. Unlock Using Code Toggle
                val btnTogglePin = TextView(context).apply {
                    text = "Unlock Using Unlock Code"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        isPinInputVisible = !isPinInputVisible
                        updateUi(view)
                    }
                    setPadding((8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
                }
                view.addView(btnTogglePin)

                // PIN input and Submit button (Conditional)
                if (isPinInputVisible) {
                    val pinInput = EditText(context).apply {
                        hint = "Enter 6-Digit Code"
                        setHintTextColor(Color.LTGRAY)
                        setTextColor(Color.BLACK)
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                        gravity = Gravity.CENTER
                        setBackgroundColor(Color.WHITE)
                        setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (56 * density).toInt()).apply {
                            setMargins(0, (12 * density).toInt(), 0, (12 * density).toInt())
                        }
                    }
                    view.addView(pinInput)

                    val btnUnlock = Button(context).apply {
                        text = "UNLOCK NOW"
                        setBackgroundColor(Color.WHITE)
                        setTextColor(Color.BLACK)
                        setOnClickListener {
                            val pin = pinInput.text.toString()
                            if (securityManager.verifyCode(pin) || pin == securityManager.getRecoveryKey()) {
                                val deviceManager = DeviceManager(context)
                                deviceManager.unfreezeDevice()
                                hideOverlay()
                            } else {
                                Toast.makeText(context, "Invalid code", Toast.LENGTH_SHORT).show()
                            }
                        }
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (56 * density).toInt())
                    }
                    view.addView(btnUnlock)
                }
            }
            
            updateUi(this)
            Log.d("LockOverlay", "updateUi completed, child count: ${this.childCount}")
        }

        overlayView = layout
        Log.d("LockOverlay", "Layout created with ${layout.childCount} children")
        
        try {
            windowManager.addView(overlayView, params)
            Log.d("LockOverlay", "View added to WindowManager")
            
            // Force layout and visibility update
            // Use a small delay to ensure the view is attached
            overlayView?.postDelayed({
                overlayView?.let { view ->
                    view.visibility = View.VISIBLE
                    view.requestLayout()
                    view.invalidate()
                    // Also invalidate all children
                    if (view is ViewGroup) {
                        for (i in 0 until view.childCount) {
                            view.getChildAt(i)?.invalidate()
                        }
                    }
                    Log.d("LockOverlay", "Overlay layout forced, isAttached: ${view.isAttachedToWindow}, visible: ${view.visibility == View.VISIBLE}")
                }
            }, 100)
        } catch (e: Exception) {
            Log.e("LockOverlay", "Failed to add overlay view", e)
            overlayView = null
        }
    }

    fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
            }
            overlayView = null
        }
    }
    
    fun isShowing(): Boolean = overlayView != null
}
