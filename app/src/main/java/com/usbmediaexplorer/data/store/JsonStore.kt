package com.usbmediaexplorer.data.store

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

enum class JsonStoreIssueKind { CORRUPT, BACKUP_RESTORED, PERSIST_FAILED }
data class JsonStoreIssue(val fileName: String, val kind: JsonStoreIssueKind)

object JsonStoreDiagnostics {
    private val _issues = MutableSharedFlow<JsonStoreIssue>(extraBufferCapacity = 16)
    val issues: SharedFlow<JsonStoreIssue> = _issues.asSharedFlow()
    internal fun report(fileName: String, kind: JsonStoreIssueKind) { _issues.tryEmit(JsonStoreIssue(fileName, kind)) }
}

/** Small versioned JSON store with recovery-safe writes and observable failures. */
open class JsonStore(
    context: Context,
    fileName: String,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    protected val file: File = File(context.filesDir, fileName)
    private val mutex = Mutex()
    private val _root = MutableStateFlow(JSONObject())
    val root: StateFlow<JSONObject> = _root.asStateFlow()

    init { scope.launch { reload() } }

    suspend fun reload() = withContext(Dispatchers.IO) {
        mutex.withLock { _root.value = readFromDisk() }
    }

    private fun readFromDisk(): JSONObject {
        if (!file.exists()) return JSONObject().put("schemaVersion", JsonStoreMigrations.CURRENT_SCHEMA_VERSION)
        return try {
            JsonStoreMigrations.migrate(JSONObject(file.readText(Charsets.UTF_8)))
        } catch (_: JSONException) {
            val corrupt = File(file.parentFile, file.name + ".corrupt." + System.currentTimeMillis())
            runCatching { file.renameTo(corrupt) }
            JsonStoreDiagnostics.report(file.name, JsonStoreIssueKind.CORRUPT)
            restoreBackupOrEmpty()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to read ${file.name}", t)
            JsonStoreDiagnostics.report(file.name, JsonStoreIssueKind.CORRUPT)
            restoreBackupOrEmpty()
        }
    }

    private fun restoreBackupOrEmpty(): JSONObject {
        val backup = File(file.parentFile, file.name + ".bak")
        return runCatching {
            if (backup.isFile) {
                val restored = JsonStoreMigrations.migrate(JSONObject(backup.readText(Charsets.UTF_8)))
                backup.copyTo(file, overwrite = true)
                JsonStoreDiagnostics.report(file.name, JsonStoreIssueKind.BACKUP_RESTORED)
                restored
            } else {
                JSONObject().put("schemaVersion", JsonStoreMigrations.CURRENT_SCHEMA_VERSION)
            }
        }.getOrElse {
            JSONObject().put("schemaVersion", JsonStoreMigrations.CURRENT_SCHEMA_VERSION)
        }
    }

    suspend fun <T> mutate(block: (JSONObject) -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = if (_root.value.length() == 0) readFromDisk() else JSONObject(_root.value.toString())
            val result = block(current)
            writeToDisk(JsonStoreMigrations.migrate(current))
            _root.value = current
            result
        }
    }

    private fun writeToDisk(json: JSONObject) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        val backup = File(file.parentFile, file.name + ".bak")
        try {
            file.parentFile?.mkdirs()
            tmp.writeText(json.toString(), Charsets.UTF_8)
            if (file.isFile) file.copyTo(backup, overwrite = true)
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to persist ${file.name}", t)
            JsonStoreDiagnostics.report(file.name, JsonStoreIssueKind.PERSIST_FAILED)
            tmp.delete()
        }
    }

    protected fun JSONObject.array(key: String): JSONArray = optJSONArray(key) ?: JSONArray().also { put(key, it) }
    protected fun JSONObject.optArray(key: String): JSONArray = optJSONArray(key) ?: JSONArray()
    protected fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
    protected fun JSONObject.string(key: String, fallback: String = ""): String = optString(key, fallback)
    protected fun JSONObject.long(key: String, fallback: Long = 0L): Long = if (has(key)) optLong(key, fallback) else fallback
    protected fun safeParse(block: () -> JSONObject): JSONObject = try { block() } catch (_: JSONException) { JSONObject() }

    private companion object { const val TAG = "JsonStore" }
}
