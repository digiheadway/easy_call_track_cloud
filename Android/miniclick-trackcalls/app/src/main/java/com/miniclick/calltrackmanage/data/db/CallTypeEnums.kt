package com.miniclick.calltrackmanage.data.db

/**
 * Standardized Call Type enum to replace magic numbers from CallLog.Calls
 * This eliminates scattered hardcoded values like `callType == 6` or `callType == 5`
 */
enum class CallType(val value: Int) {
    INCOMING(1),    // CallLog.Calls.INCOMING_TYPE
    OUTGOING(2),    // CallLog.Calls.OUTGOING_TYPE
    MISSED(3),      // CallLog.Calls.MISSED_TYPE
    VOICEMAIL(4),   // CallLog.Calls.VOICEMAIL_TYPE
    REJECTED(5),    // CallLog.Calls.REJECTED_TYPE
    BLOCKED(6),     // CallLog.Calls.BLOCKED_TYPE
    UNKNOWN(0);     // Catch-all for unknown types

    companion object {
        fun fromValue(value: Int): CallType {
            return entries.find { it.value == value } ?: UNKNOWN
        }

        /**
         * Check if the call was answered/connected (has >0 duration)
         */
        fun isAnswerable(type: CallType): Boolean {
            return type == INCOMING || type == OUTGOING
        }

        /**
         * Check if the call was explicitly declined by user or system
         */
        fun isDeclined(type: CallType): Boolean {
            return type == REJECTED || type == BLOCKED
        }

        /**
         * Check if the call was not attended by the user (incoming)
         */
        fun isNotAttended(type: CallType): Boolean {
            return type == MISSED || type == REJECTED
        }
    }
}

/**
 * Standardized SIM Selection enum to replace magic strings like "Sim1", "Both", "Off"
 */
enum class SimSelection(val displayName: String) {
    OFF("Off"),
    SIM1("Sim1"),
    SIM2("Sim2"),
    BOTH("Both");

    companion object {
        fun fromString(value: String): SimSelection {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: OFF
        }
    }
}
