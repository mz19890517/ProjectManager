package com.mz.projectmanager.data.model

data class ProjectItem(
    val id: String,
    val worktree: String,
    val name: String,
    val timeCreated: Long,
    val timeUpdated: Long,
    val sessionCount: Int = 0,
    val fileCount: Int = 0
)
