package com.miniclick.calltrackmanage.ui.home

import com.miniclick.calltrackmanage.data.db.CallDataEntity

data class PersonGroup(
    val number: String,
    val name: String?,
    val photoUri: String?,
    val calls: List<CallDataEntity>,
    val lastCallDate: Long,
    val totalDuration: Long,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int,
    val personNote: String? = null,
    val label: String? = null
)
