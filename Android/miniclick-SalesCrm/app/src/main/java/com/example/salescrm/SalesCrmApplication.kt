package com.example.salescrm

import android.app.Application
import com.google.firebase.FirebaseApp

class SalesCrmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        
        // Start the call monitor service for persistent caller ID
        com.example.salescrm.notification.CallMonitorService.startService(this)
    }
}
