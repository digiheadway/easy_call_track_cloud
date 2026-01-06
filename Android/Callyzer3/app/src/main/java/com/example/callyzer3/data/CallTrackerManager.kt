package com.example.callyzer3.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class CallTrackerManager(private val context: Context, private val repository: CallHistoryRepository) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val callStateListener = CallStateListener()

    private var currentCallStartTime: Long? = null
    private var currentIncomingNumber: String? = null

    inner class CallStateListener : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)

            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // Call is answered/picked up
                    currentCallStartTime = System.currentTimeMillis()
                    currentIncomingNumber = phoneNumber
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    // Call ended
                    currentCallStartTime?.let { startTime ->
                        val endTime = System.currentTimeMillis()
                        val duration = (endTime - startTime) / 1000 // Convert to seconds

                        currentIncomingNumber?.let { number ->
                            if (number.isNotBlank() && !repository.isNumberExcluded(number)) {
                                val callLog = CallLog(
                                    phoneNumber = number,
                                    callType = CallType.INCOMING,
                                    callStatus = CallStatus.ANSWERED,
                                    duration = duration,
                                    timestamp = LocalDateTime.now()
                                )
                                CoroutineScope(Dispatchers.IO).launch {
                                    repository.addCallLog(callLog)
                                }
                            }
                        }
                    }
                    currentCallStartTime = null
                    currentIncomingNumber = null
                }
                TelephonyManager.CALL_STATE_RINGING -> {
                    // Incoming call ringing
                    currentIncomingNumber = phoneNumber
                    if (phoneNumber?.isNotBlank() == true && !repository.isNumberExcluded(phoneNumber)) {
                        val callLog = CallLog(
                            phoneNumber = phoneNumber,
                            callType = CallType.INCOMING,
                            callStatus = CallStatus.MISSED, // Will be updated if answered
                            duration = 0,
                            timestamp = LocalDateTime.now()
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.addCallLog(callLog)
                        }
                    }
                }
            }
        }
    }

    fun startTracking() {
        if (hasPhonePermission()) {
            telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun stopTracking() {
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE)
    }

    private fun hasPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Method to manually sync call logs from device (for initial setup or refresh)
    fun syncCallLogsFromDevice() {
        if (!hasPhonePermission()) return

        CoroutineScope(Dispatchers.IO).launch {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.CACHED_NAME
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

                while (it.moveToNext()) {
                    val number = it.getString(numberIndex) ?: continue
                    if (repository.isNumberExcluded(number)) continue

                    val type = when (it.getInt(typeIndex)) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.ALL
                        else -> CallType.ALL
                    }

                    val status = when (it.getInt(typeIndex)) {
                        CallLog.Calls.MISSED_TYPE -> CallStatus.MISSED
                        else -> if (it.getLong(durationIndex) > 0) CallStatus.ANSWERED else CallStatus.MISSED
                    }

                    val timestamp = LocalDateTime.ofEpochSecond(
                        it.getLong(dateIndex) / 1000,
                        0,
                        ZoneOffset.systemDefault()
                    )

                    val callLog = CallLog(
                        phoneNumber = number,
                        contactName = it.getString(nameIndex),
                        callType = type,
                        callStatus = status,
                        duration = it.getLong(durationIndex),
                        timestamp = timestamp
                    )

                    repository.addCallLog(callLog)
                }
            }
        }
    }
}
