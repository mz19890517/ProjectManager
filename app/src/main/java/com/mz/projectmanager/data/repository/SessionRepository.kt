package com.mz.projectmanager.data.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.mz.projectmanager.data.model.ProjectItem
import com.mz.projectmanager.data.model.SessionItem
import com.mz.projectmanager.util.IdGenerator
import com.mz.projectmanager.util.RootCommand
import kotlinx.coroutines.runBlocking

class SessionRepository(private val dbPath: String) {

    private var db: SQLiteDatabase? = null

    private val localDbPath = "/data/local/tmp/opencode_pm.db"

    private fun getDb(): SQLiteDatabase {
        if (db == null || !db!!.isOpen) {
            val r = runBlocking { RootCommand.execute("cp '$dbPath' '$localDbPath' && chmod 666 '$localDbPath'") }
            if (r.isFailure) {
                throw Exception("复制数据库失败: ${r.exceptionOrNull()?.message}")
            }
            db = SQLiteDatabase.openDatabase(localDbPath, null, SQLiteDatabase.OPEN_READWRITE)
        }
        return db!!
    }

    private fun syncBack() {
        runBlocking { RootCommand.execute("cp '$localDbPath' '$dbPath'") }
    }

    fun getAllProjects(): List<ProjectItem> {
        val db = getDb()
        val projects = mutableListOf<ProjectItem>()

        val cursor = db.rawQuery(
            """SELECT p.id, p.worktree, p.name, p.time_created, p.time_updated,
               (SELECT COUNT(*) FROM session WHERE project_id = p.id) as session_count
               FROM project p WHERE p.id != 'global' ORDER BY p.time_updated DESC""",
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                projects.add(
                    ProjectItem(
                        id = it.getString(0),
                        worktree = it.getString(1),
                        name = it.getString(2) ?: it.getString(1).substringAfterLast('/'),
                        timeCreated = it.getLong(3),
                        timeUpdated = it.getLong(4),
                        sessionCount = it.getInt(5)
                    )
                )
            }
        }
        return projects
    }

    fun getProjectById(projectId: String): ProjectItem? {
        val db = getDb()
        val cursor = db.rawQuery(
            """SELECT p.id, p.worktree, p.name, p.time_created, p.time_updated,
               (SELECT COUNT(*) FROM session WHERE project_id = p.id) as session_count
               FROM project p WHERE p.id = ?""",
            arrayOf(projectId)
        )

        return cursor.use {
            if (it.moveToFirst()) {
                ProjectItem(
                    id = it.getString(0),
                    worktree = it.getString(1),
                    name = it.getString(2) ?: it.getString(1).substringAfterLast('/'),
                    timeCreated = it.getLong(3),
                    timeUpdated = it.getLong(4),
                    sessionCount = it.getInt(5)
                )
            } else null
        }
    }

    fun getSessionsForProject(projectId: String): List<SessionItem> {
        val db = getDb()
        val sessions = mutableListOf<SessionItem>()

        val cursor = db.rawQuery(
            """SELECT s.id, s.project_id, s.directory, s.title, s.time_created, s.time_updated,
               (SELECT COUNT(*) FROM message WHERE session_id = s.id) as msg_count
               FROM session s WHERE s.project_id = ? ORDER BY s.time_created DESC""",
            arrayOf(projectId)
        )

        cursor.use {
            while (it.moveToNext()) {
                sessions.add(
                    SessionItem(
                        id = it.getString(0),
                        projectId = it.getString(1),
                        directory = it.getString(2),
                        title = it.getString(3),
                        timeCreated = it.getLong(4),
                        timeUpdated = it.getLong(5),
                        messageCount = it.getInt(6)
                    )
                )
            }
        }
        return sessions
    }

    fun getAllSessions(): List<SessionItem> {
        val db = getDb()
        val sessions = mutableListOf<SessionItem>()

        val cursor = db.rawQuery(
            """SELECT s.id, s.project_id, s.directory, s.title, s.time_created, s.time_updated,
               (SELECT COUNT(*) FROM message WHERE session_id = s.id) as msg_count
               FROM session s ORDER BY s.time_created DESC""",
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                sessions.add(
                    SessionItem(
                        id = it.getString(0),
                        projectId = it.getString(1),
                        directory = it.getString(2),
                        title = it.getString(3),
                        timeCreated = it.getLong(4),
                        timeUpdated = it.getLong(5),
                        messageCount = it.getInt(6)
                    )
                )
            }
        }
        return sessions
    }

    fun createProject(worktree: String, name: String? = null): ProjectItem {
        val db = getDb()
        val projectId = IdGenerator.generateProjectId()
        val now = System.currentTimeMillis()
        val displayName = name ?: worktree.substringAfterLast('/')

        val values = ContentValues().apply {
            put("id", projectId)
            put("worktree", worktree)
            put("name", displayName)
            put("time_created", now)
            put("time_updated", now)
            put("sandboxes", "[]")
        }
        db.insert("project", null, values)
        syncBack()

        return ProjectItem(
            id = projectId,
            worktree = worktree,
            name = displayName,
            timeCreated = now,
            timeUpdated = now
        )
    }

    fun updateProject(projectId: String, name: String?, worktree: String?): Boolean {
        val db = getDb()
        val values = ContentValues()
        if (name != null) values.put("name", name)
        if (worktree != null) values.put("worktree", worktree)
        values.put("time_updated", System.currentTimeMillis())

        val result = db.update("project", values, "id = ?", arrayOf(projectId)) > 0
        if (result) syncBack()
        return result
    }

    fun deleteProject(projectId: String): Boolean {
        val db = getDb()
        val result = db.delete("project", "id = ?", arrayOf(projectId)) > 0
        if (result) syncBack()
        return result
    }

    fun createSession(projectId: String, directory: String, title: String): SessionItem {
        val db = getDb()
        val sessionId = IdGenerator.generateSessionId()
        val now = System.currentTimeMillis()

        val values = ContentValues().apply {
            put("id", sessionId)
            put("project_id", projectId)
            put("directory", directory)
            put("title", title)
            put("time_created", now)
            put("time_updated", now)
            put("version", "")
            put("slug", "")
            put("agent", "agent")
            put("model", "big-pickle")
        }
        db.insert("session", null, values)
        syncBack()

        return SessionItem(
            id = sessionId,
            projectId = projectId,
            directory = directory,
            title = title,
            timeCreated = now,
            timeUpdated = now
        )
    }

    fun rebindSession(sessionId: String, newProjectId: String, newDirectory: String): Boolean {
        val db = getDb()
        val values = ContentValues().apply {
            put("project_id", newProjectId)
            put("directory", newDirectory)
            put("time_updated", System.currentTimeMillis())
        }
        val ok = db.update("session", values, "id = ?", arrayOf(sessionId)) > 0
        if (ok) syncBack()
        return ok
    }

    fun deleteSession(sessionId: String): Boolean {
        val db = getDb()
        val ok = db.delete("session", "id = ?", arrayOf(sessionId)) > 0
        if (ok) syncBack()
        return ok
    }

    fun close() {
        syncBack()
        db?.close()
        db = null
    }
}
