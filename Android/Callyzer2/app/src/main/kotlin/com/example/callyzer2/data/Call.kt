package com.example.callyzer2.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "calls",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Call(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val phoneNumber: String,
    val callType: CallType,
    val duration: Long = 0, // in seconds
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val followUpRequired: Boolean = false,
    val followUpDate: Long? = null
)

enum class CallType {
    INCOMING, OUTGOING, MISSED
}
