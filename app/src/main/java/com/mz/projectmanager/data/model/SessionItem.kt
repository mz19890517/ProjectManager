package com.mz.projectmanager.data.model

data class SessionItem(
    val id: String,
    val projectId: String,
    val directory: String,
    val title: String,
    val timeCreated: Long,
    val timeUpdated: Long,
    val messageCount: Int = 0
)
