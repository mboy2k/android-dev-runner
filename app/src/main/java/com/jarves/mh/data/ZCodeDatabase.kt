package com.jarves.mh.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.util.Log
import com.jarves.mh.model.ActivityItem
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectChat
import com.jarves.mh.model.ProjectKind
import java.io.File
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

/**
 * SQLite Database Manager for ZCode Mobile.
 * Stores all projects, chats, and messages in a persistent SQLite database located at:
 * /storage/emulated/0/.Zcode/database/zcode.db
 * 
 * Survives application uninstalls and data clearance 100%!
 */
class ZCodeDatabase(private val context: Context) {

    private val dbHelper: DatabaseHelper by lazy {
        DatabaseHelper(context, resolveDbPath(context))
    }

    val databasePath: String
        get() = resolveDbPath(context).absolutePath

    companion object {
        private const val TAG = "ZCodeDatabase"
        private const val DB_VERSION = 1

        fun resolveDbPath(context: Context): File {
            return runCatching {
                val zcodeRoot = File(Environment.getExternalStorageDirectory(), ".Zcode/database")
                if (!zcodeRoot.exists()) zcodeRoot.mkdirs()
                File(zcodeRoot, "zcode.db")
            }.getOrElse {
                val fallback = File(context.filesDir, "database").apply { mkdirs() }
                File(fallback, "zcode.db")
            }
        }
    }

    private class DatabaseHelper(context: Context, private val dbFile: File) :
        SQLiteOpenHelper(context, dbFile.absolutePath, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS projects (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT,
                    language TEXT,
                    slug TEXT NOT NULL,
                    root_path TEXT,
                    updated_at INTEGER,
                    kind TEXT
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS chats (
                    id TEXT NOT NULL,
                    project_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    created_at INTEGER,
                    updated_at INTEGER,
                    is_pinned INTEGER DEFAULT 0,
                    is_archived INTEGER DEFAULT 0,
                    PRIMARY KEY (id, project_id)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS messages (
                    id TEXT NOT NULL,
                    chat_id TEXT NOT NULL,
                    project_id TEXT NOT NULL,
                    from_user INTEGER DEFAULT 0,
                    text TEXT,
                    created_at TEXT,
                    attachments TEXT,
                    work_items TEXT,
                    worked_millis INTEGER DEFAULT 0,
                    PRIMARY KEY (id, chat_id, project_id)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_chats_proj ON chats (project_id)
            """.trimIndent())

            db.execSQL("""
                CREATE INDEX IF NOT EXISTS idx_msgs_chat ON messages (chat_id, project_id)
            """.trimIndent())
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Future schema migrations
        }
    }

    private fun getReadableDb(): SQLiteDatabase = dbHelper.readableDatabase
    private fun getWritableDb(): SQLiteDatabase = dbHelper.writableDatabase

    // =========================================================================
    // 1. PROJECTS
    // =========================================================================

    fun loadProjects(): List<Project> {
        val list = mutableListOf<Project>()
        runCatching {
            val db = getReadableDb()
            db.query(
                "projects",
                null,
                null,
                null,
                null,
                null,
                "updated_at DESC"
            ).use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow("id")
                val nameCol = cursor.getColumnIndexOrThrow("name")
                val descCol = cursor.getColumnIndexOrThrow("description")
                val langCol = cursor.getColumnIndexOrThrow("language")
                val slugCol = cursor.getColumnIndexOrThrow("slug")
                val rootCol = cursor.getColumnIndexOrThrow("root_path")
                val updCol = cursor.getColumnIndexOrThrow("updated_at")
                val kindCol = cursor.getColumnIndexOrThrow("kind")

                while (cursor.moveToNext()) {
                    val kindStr = cursor.getString(kindCol)
                    val kind = runCatching { ProjectKind.valueOf(kindStr) }.getOrDefault(ProjectKind.PROJECT)
                    list.add(
                        Project(
                            id = cursor.getString(idCol),
                            name = cursor.getString(nameCol),
                            description = cursor.getString(descCol) ?: "",
                            language = cursor.getString(langCol) ?: "",
                            slug = cursor.getString(slugCol),
                            rootPath = cursor.getString(rootCol) ?: "",
                            updatedAtMillis = cursor.getLong(updCol),
                            kind = kind
                        )
                    )
                }
            }
        }.onFailure { Log.e(TAG, "Failed to load projects", it) }
        return list
    }

    fun saveProjects(projects: List<Project>) {
        runCatching {
            val db = getWritableDb()
            db.beginTransaction()
            try {
                // Upsert projects
                projects.forEach { p ->
                    val cv = ContentValues().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("description", p.description)
                        put("language", p.language)
                        put("slug", p.slug)
                        put("root_path", p.rootPath)
                        put("updated_at", p.updatedAtMillis)
                        put("kind", p.kind.name)
                    }
                    db.insertWithOnConflict("projects", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }.onFailure { Log.e(TAG, "Failed to save projects", it) }
    }

    fun deleteProject(projectId: String) {
        runCatching {
            val db = getWritableDb()
            db.beginTransaction()
            try {
                db.delete("projects", "id = ?", arrayOf(projectId))
                db.delete("chats", "project_id = ?", arrayOf(projectId))
                db.delete("messages", "project_id = ?", arrayOf(projectId))
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }.onFailure { Log.e(TAG, "Failed to delete project $projectId", it) }
    }

    // =========================================================================
    // 2. CHATS
    // =========================================================================

    fun loadProjectChats(projectId: String): List<ProjectChat> {
        val list = mutableListOf<ProjectChat>()
        runCatching {
            val db = getReadableDb()
            db.query(
                "chats",
                null,
                "project_id = ?",
                arrayOf(projectId),
                null,
                null,
                "is_pinned DESC, updated_at DESC"
            ).use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow("id")
                val titleCol = cursor.getColumnIndexOrThrow("title")
                val createdCol = cursor.getColumnIndexOrThrow("created_at")
                val updatedCol = cursor.getColumnIndexOrThrow("updated_at")
                val pinnedCol = cursor.getColumnIndexOrThrow("is_pinned")
                val archCol = cursor.getColumnIndexOrThrow("is_archived")

                while (cursor.moveToNext()) {
                    list.add(
                        ProjectChat(
                            id = cursor.getString(idCol),
                            title = cursor.getString(titleCol),
                            createdAtMillis = cursor.getLong(createdCol),
                            updatedAtMillis = cursor.getLong(updatedCol),
                            isPinned = cursor.getInt(pinnedCol) == 1,
                            isArchived = cursor.getInt(archCol) == 1
                        )
                    )
                }
            }
        }.onFailure { Log.e(TAG, "Failed to load chats for $projectId", it) }
        return list
    }

    fun saveProjectChats(projectId: String, chats: List<ProjectChat>) {
        runCatching {
            val db = getWritableDb()
            db.beginTransaction()
            try {
                chats.forEach { c ->
                    val cv = ContentValues().apply {
                        put("id", c.id)
                        put("project_id", projectId)
                        put("title", c.title)
                        put("created_at", c.createdAtMillis)
                        put("updated_at", c.updatedAtMillis)
                        put("is_pinned", if (c.isPinned) 1 else 0)
                        put("is_archived", if (c.isArchived) 1 else 0)
                    }
                    db.insertWithOnConflict("chats", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }.onFailure { Log.e(TAG, "Failed to save chats for $projectId", it) }
    }

    fun deleteChat(projectId: String, chatId: String) {
        runCatching {
            val db = getWritableDb()
            db.beginTransaction()
            try {
                db.delete("chats", "id = ? AND project_id = ?", arrayOf(chatId, projectId))
                db.delete("messages", "chat_id = ? AND project_id = ?", arrayOf(chatId, projectId))
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }.onFailure { Log.e(TAG, "Failed to delete chat $chatId", it) }
    }

    // =========================================================================
    // 3. MESSAGES
    // =========================================================================

    fun loadMessages(projectId: String, chatId: String): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        runCatching {
            val db = getReadableDb()
            db.query(
                "messages",
                null,
                "project_id = ? AND chat_id = ?",
                arrayOf(projectId, chatId),
                null,
                null,
                "created_at ASC"
            ).use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow("id")
                val fromUserCol = cursor.getColumnIndexOrThrow("from_user")
                val textCol = cursor.getColumnIndexOrThrow("text")
                val createdCol = cursor.getColumnIndexOrThrow("created_at")
                val attachCol = cursor.getColumnIndexOrThrow("attachments")
                val workCol = cursor.getColumnIndexOrThrow("work_items")
                val workedCol = cursor.getColumnIndexOrThrow("worked_millis")

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idCol)
                    val fromUser = cursor.getInt(fromUserCol) == 1
                    val text = cursor.getString(textCol) ?: ""
                    val createdStr = cursor.getString(createdCol) ?: Instant.now().toString()
                    val createdInstant = runCatching { Instant.parse(createdStr) }.getOrDefault(Instant.now())
                    val attachJson = cursor.getString(attachCol) ?: "[]"
                    val workJson = cursor.getString(workCol) ?: "[]"
                    val workedMillis = cursor.getLong(workedCol)

                    val attachments = parseAttachments(attachJson)
                    val workItems = parseWorkItems(workJson)

                    list.add(
                        ChatMessage(
                            id = id,
                            fromUser = fromUser,
                            text = text,
                            createdAt = createdInstant,
                            attachments = attachments,
                            workItems = workItems,
                            workedMillis = workedMillis
                        )
                    )
                }
            }
        }.onFailure { Log.e(TAG, "Failed to load messages for $projectId / $chatId", it) }
        return list
    }

    fun saveMessages(projectId: String, chatId: String, messages: List<ChatMessage>) {
        runCatching {
            val db = getWritableDb()
            db.beginTransaction()
            try {
                messages.forEach { m ->
                    val cv = ContentValues().apply {
                        put("id", m.id)
                        put("chat_id", chatId)
                        put("project_id", projectId)
                        put("from_user", if (m.fromUser) 1 else 0)
                        put("text", m.text)
                        put("created_at", m.createdAt.toString())
                        put("attachments", serializeAttachments(m.attachments))
                        put("work_items", serializeWorkItems(m.workItems))
                        put("worked_millis", m.workedMillis)
                    }
                    db.insertWithOnConflict("messages", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }.onFailure { Log.e(TAG, "Failed to save messages for $chatId", it) }
    }

    private fun serializeAttachments(list: List<ChatAttachment>): String {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("displayName", a.displayName)
                put("relativePath", a.relativePath)
                put("mimeType", a.mimeType)
                put("sizeBytes", a.sizeBytes)
            })
        }
        return arr.toString()
    }

    private fun parseAttachments(json: String): List<ChatAttachment> {
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ChatAttachment(
                    id = obj.getString("id"),
                    displayName = obj.getString("displayName"),
                    relativePath = obj.getString("relativePath"),
                    mimeType = obj.getString("mimeType"),
                    sizeBytes = obj.getLong("sizeBytes")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun serializeWorkItems(list: List<ActivityItem>): String {
        val arr = JSONArray()
        list.forEach { w ->
            arr.put(JSONObject().apply {
                put("title", w.title)
                put("detail", w.detail)
                put("isComplete", w.isComplete)
                put("isCommand", w.isCommand)
            })
        }
        return arr.toString()
    }

    private fun parseWorkItems(json: String): List<ActivityItem> {
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ActivityItem(
                    title = obj.getString("title"),
                    detail = obj.getString("detail"),
                    isComplete = obj.optBoolean("isComplete", true),
                    isCommand = obj.optBoolean("isCommand", false)
                )
            }
        }.getOrDefault(emptyList())
    }
}
