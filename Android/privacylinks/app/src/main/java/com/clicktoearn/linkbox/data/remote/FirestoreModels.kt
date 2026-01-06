package com.clicktoearn.linkbox.data.remote

import com.clicktoearn.linkbox.data.local.AssetType

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import androidx.annotation.Keep

@Keep
@IgnoreExtraProperties

data class FirestoreUser(
    val userId: String = "",
    @get:PropertyName("username") @set:PropertyName("username")
    var username: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val points: Int = 10,
    val accessToken: String? = null,
    val totalEarned: Float = 0f,
    @get:PropertyName("isPremium") @set:PropertyName("isPremium")
    var isPremium: Boolean = false,
    @get:PropertyName("premiumExpiry") @set:PropertyName("premiumExpiry")
    var premiumExpiry: Long? = null,
    val isGuest: Boolean = false,
    // Referral tracking
    val firstReferredBy: String? = null,      // User ID who referred this user (NEW INSTALL only)
    val lastReferredBy: String? = null,       // User ID who last shared content with this user
    val referralCount: Int = 0,               // Total number of users referred (new installs)
    val shareCount: Int = 0,                   // Total shares to existing users
    // Security & Tracking
    val firstIp: String? = null,
    val lastIp: String? = null,
    val deviceId: String? = null,
    val advertisingId: String? = null
) {
    @get:PropertyName("name")
    @set:PropertyName("name")
    var legacyName: String 
        get() = username
        set(value) { if (username.isEmpty()) username = value }

    @get:PropertyName("displayName")
    @set:PropertyName("displayName")
    var legacyDisplayName: String 
        get() = username
        set(value) { if (username.isEmpty()) username = value }
}

@Keep
@IgnoreExtraProperties
data class FirestoreLink(

    val id: String = "",
    val assetId: String = "",
    val token: String = "",
    val name: String = "",
    @get:PropertyName("expiryDate") @set:PropertyName("expiryDate")
    var expiryDate: Long? = null,
    val status: String = "ACTIVE",
    val newUsers: Int = 0,
    val users: Int = 0,
    val views: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    // Referral analytics
    val installs: Int = 0,                    // Count of new app installs via this link
    val uniqueUsers: Int = 0                  // Count of unique users who accessed
)

@Keep
@IgnoreExtraProperties
data class FirestoreAsset(

    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val content: String = "",
    val parentId: String? = null,
    val ownerId: String = "",
    @get:PropertyName("pointCost") @set:PropertyName("pointCost")
    var pointCost: Int = 0,
    val allowSaveCopy: Boolean = false,
    val allowFurtherSharing: Boolean = true,
    val allowScreenCapture: Boolean = true,
    val exposeUrl: Boolean = false,
    @get:PropertyName("chargeEveryTime") @set:PropertyName("chargeEveryTime")
    var chargeEveryTime: Boolean = true,
    val sharingEnabled: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Keep
@IgnoreExtraProperties
data class FirestoreHistory(

    @get:PropertyName("token") @set:PropertyName("token")
    var token: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",
    @get:PropertyName("accessedAt") @set:PropertyName("accessedAt")
    var accessedAt: Long = 0,
    @get:PropertyName("isStarred") @set:PropertyName("isStarred")
    var isStarred: Boolean = false,
    @get:PropertyName("isPaid") @set:PropertyName("isPaid")
    var isPaid: Boolean = false
)

@Keep
data class FirestorePointsTransaction(

    val id: String = "",
    val userId: String = "",
    val points: Int = 0,
    val type: String = "",
    val remark: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Keep
@IgnoreExtraProperties
data class FirestoreReferral(

    val id: String = "",
    val referrerId: String = "",              // User who shared the link
    val referredUserId: String = "",          // User who accessed/installed
    val token: String = "",                   // Link token used
    val isNewInstall: Boolean = false,        // True if this was a new app install
    val rewardPoints: Int = 0,                // Points awarded to referrer
    val createdAt: Long = 0
)
