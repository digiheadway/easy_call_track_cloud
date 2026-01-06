package com.clicktoearn.linkbox

import android.app.Application
import com.google.firebase.FirebaseApp

class LinkBoxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}
