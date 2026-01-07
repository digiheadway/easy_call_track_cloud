package com.clicktoearn.linkbox.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.clicktoearn.linkbox.data.local.AssetType

object AnalyticsManager {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }

    // Generic log
    fun logEvent(eventName: String, params: Bundle? = null) {
        firebaseAnalytics?.logEvent(eventName, params)
    }

    // ==================== Lifecycle & Navigation ====================

    fun logAppOpen() {
        logEvent(FirebaseAnalytics.Event.APP_OPEN)
    }

    fun logScreenView(screenName: String) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName) // Usually class name, but we use route
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }

    // ==================== Home / Assets ====================

    fun logContentCardClick(cardName: String, url: String? = null) {
        val params = Bundle().apply {
            putString("card_name", cardName)
            if (url != null) putString("url", url)
        }
        logEvent("home_card_click", params)
    }

    fun logAssetCreated(type: AssetType, name: String) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, type.name)
            putString(FirebaseAnalytics.Param.ITEM_NAME, name)
        }
        logEvent("asset_created", params)
    }
    
    fun logAssetMoved(assetId: String, fromParent: String?, toParent: String?) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, assetId)
            putString("from_parent", fromParent ?: "root")
            putString("to_parent", toParent ?: "root")
        }
        logEvent("asset_moved", params)
    }
    
    fun logAssetRenamed(assetId: String, newName: String) {
         val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, assetId)
            putString("new_name", newName)
        }
        logEvent("asset_renamed", params)
    }

    // ==================== Links & Sharing ====================
    
    fun logLinkRenamed(token: String, newName: String) {
         val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, token)
            putString("new_name", newName)
        }
        logEvent("link_renamed", params)
    }

    fun logSharedContentOpened(token: String, isNewInstall: Boolean) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, token)
            putBoolean("is_new_install", isNewInstall)
        }
        logEvent("shared_content_opened", params)
    }

    // ==================== History ====================

    fun logHistoryCleared() {
        logEvent("history_cleared")
    }

    fun logHistoryItemDeleted(token: String) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, token)
        }
        logEvent("history_item_deleted", params)
    }

    // ==================== Reports ====================

    fun logReportSubmitted(reason: String, assetId: String) {
        val params = Bundle().apply {
            putString("reason", reason)
            putString("asset_id", assetId)
        }
        logEvent("report_submitted", params)
    }

    // ==================== Points / Wallet ====================

    fun logPointsSpent(amount: Int, itemName: String, balanceAfter: Int) {
        val params = Bundle().apply {
            putInt(FirebaseAnalytics.Param.VALUE, amount)
            putString(FirebaseAnalytics.Param.CURRENCY, "POINTS") // Virtual currency
            putString(FirebaseAnalytics.Param.ITEM_NAME, itemName)
            putInt("balance_after", balanceAfter)
        }
        logEvent(FirebaseAnalytics.Event.SPEND_VIRTUAL_CURRENCY, params)
    }
    
    fun logPointsEarned(amount: Int, source: String, balanceAfter: Int) {
         val params = Bundle().apply {
            putInt(FirebaseAnalytics.Param.VALUE, amount)
            putString(FirebaseAnalytics.Param.CURRENCY, "POINTS")
            putString("source", source)
             putInt("balance_after", balanceAfter)
        }
        logEvent(FirebaseAnalytics.Event.EARN_VIRTUAL_CURRENCY, params)
    }
}
