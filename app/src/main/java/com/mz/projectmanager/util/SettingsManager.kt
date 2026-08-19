package com.mz.projectmanager.util

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("project_manager_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROJECTS_ROOT = "projects_root"
        private const val KEY_DB_PATH = "db_path"
        private const val DEFAULT_PROJECTS_ROOT = "/root/projects"
        private const val DEFAULT_DB_PATH = "/root/.local/share/opencode/opencode.db"
    }

    var projectsRoot: String
        get() = prefs.getString(KEY_PROJECTS_ROOT, DEFAULT_PROJECTS_ROOT) ?: DEFAULT_PROJECTS_ROOT
        set(value) = prefs.edit().putString(KEY_PROJECTS_ROOT, value).apply()

    var dbPath: String
        get() = prefs.getString(KEY_DB_PATH, DEFAULT_DB_PATH) ?: DEFAULT_DB_PATH
        set(value) = prefs.edit().putString(KEY_DB_PATH, value).apply()

    val isConfigured: Boolean
        get() = prefs.contains(KEY_PROJECTS_ROOT)
}
