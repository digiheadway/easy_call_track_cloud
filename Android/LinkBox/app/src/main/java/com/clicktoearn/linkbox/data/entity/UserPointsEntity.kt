package com.clicktoearn.linkbox.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the user's points balance and transaction history.
 * Points are required to open/access links.
 * Users can earn points by watching ads or purchasing them.
 * Pricing: 2 Credits per point, packages available in multiples of 50.
 */
@Entity(tableName = "user_points")
data class UserPointsEntity(
    @PrimaryKey
    val id: Long = 1, // Single row for user's current balance
    
    // Current points balance
    @ColumnInfo(name = "current_balance")
    val currentBalance: Int = 0,
    
    // Total points ever earned (from ads, rewards, etc.)
    @ColumnInfo(name = "total_earned")
    val totalEarned: Int = 0,
    
    // Total points ever purchased
    @ColumnInfo(name = "total_purchased")
    val totalPurchased: Int = 0,
    
    // Total points ever spent
    @ColumnInfo(name = "total_spent")
    val totalSpent: Int = 0,
    
    // Last time points were updated
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Point transaction types for history tracking
 */
enum class PointTransactionType {
    EARNED_AD,          // Earned by watching ads
    EARNED_REWARD,      // Earned through rewards/referrals
    PURCHASED,          // Bought with money
    SPENT,              // Used to open links
    REFUND              // Refunded points
}

/**
 * Represents individual point transactions for history
 */
@Entity(tableName = "point_transactions")
data class PointTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Type of transaction
    val type: PointTransactionType,
    
    // Amount of points (positive for earned/purchased, negative for spent)
    val amount: Int,
    
    // Description/reason for the transaction
    val description: String? = null,
    
    // Reference ID (e.g., joined link ID, package ID)
    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,
    
    // Timestamp of transaction
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Predefined point packages for purchase
 * Price: 2 Credits per point, packages in multiples of 50
 */
data class PointPackage(
    val id: String,
    val points: Int,
    val price: Double, // in INR
    val bonusPoints: Int = 0,
    val label: String? = null,
    val isBestValue: Boolean = false
) {
    val totalPoints: Int get() = points + bonusPoints
    
    companion object {
        val packages = listOf(
            PointPackage(
                id = "pkg_50",
                points = 50,
                price = 100.0,
                label = "Starter"
            ),
            PointPackage(
                id = "pkg_100",
                points = 100,
                price = 200.0,
                bonusPoints = 5,
                label = "Basic"
            ),
            PointPackage(
                id = "pkg_250",
                points = 250,
                price = 500.0,
                bonusPoints = 25,
                label = "Popular",
                isBestValue = true
            ),
            PointPackage(
                id = "pkg_500",
                points = 500,
                price = 1000.0,
                bonusPoints = 75,
                label = "Pro"
            ),
            PointPackage(
                id = "pkg_1000",
                points = 1000,
                price = 2000.0,
                bonusPoints = 200,
                label = "Ultimate"
            )
        )
    }
}
