package com.miniclickcrm.deviceadmin3.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat

class DeviceInfoManager(private val context: Context) {

    fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun getDeviceModel(): String {
        return Build.MODEL
    }

    @SuppressLint("HardwareIds")
    fun getImeis(): Pair<String?, String?> {
        // Note: On Android 10+, this returns null/exception unless app is Device Owner
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return Pair(null, null)
        }

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var imei1: String? = null
        var imei2: String? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                imei1 = telephonyManager.getImei(0)
                imei2 = telephonyManager.getImei(1)
            } else {
                @Suppress("DEPRECATION")
                imei1 = telephonyManager.deviceId
            }
        } catch (e: Exception) {
            // Log error or handle gracefully
            e.printStackTrace()
        }

        return Pair(imei1, imei2)
    }
}
