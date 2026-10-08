package com.weibolite.android.data.repository

import android.content.Context
import com.weibolite.android.data.api.WeiboApi
import com.weibolite.android.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class HotSearchRepository(context: Context, private val api: WeiboApi) {
    private val file = File(context.cacheDir, "hot-search.json")
    private val mutex = Mutex()
    private var snapshot: Pair<List<HotTopic>, Long>? = null
    suspend fun cached(): Pair<List<HotTopic>, Long>? = mutex.withLock { withContext(Dispatchers.IO) {
        snapshot ?: runCatching {
            if (!file.exists()) return@runCatching null
            val j = JSONObject(file.readText()); (Models.hot(j.getJSONObject("response")) to j.getLong("time")).also { snapshot = it }
        }.getOrNull()
    } }
    suspend fun refresh(force: Boolean = false): Pair<List<HotTopic>, Long> = mutex.withLock { withContext(Dispatchers.IO) {
        snapshot?.let { if (!force && System.currentTimeMillis() - it.second in 0..420_000) return@withContext it }
        val raw = api.hot(); val rows = Models.hot(raw); val time = System.currentTimeMillis()
        val temp = File(file.parentFile, "hot-search.tmp")
        temp.writeText(JSONObject().put("response", raw).put("time", time).toString())
        if (!temp.renameTo(file)) { file.writeText(temp.readText()); temp.delete() }
        (rows to time).also { snapshot = it }
    } }
    suspend fun clear() = mutex.withLock { withContext(Dispatchers.IO) { snapshot = null; file.delete(); Unit } }
}
class TimelineRepository(private val api: WeiboApi) { suspend fun page(cursor: String?) = api.timeline(cursor) }
class SearchRepository(private val api: WeiboApi) { suspend fun page(q: String, page: Int, users: Boolean) = api.search(q, page, users) }
class UserRepository(private val api: WeiboApi) { suspend fun profile(id: String) = api.profile(id) }

class PostRepository(private val api: WeiboApi) {
    private val mutex = Mutex()
    private val fullTexts = LinkedHashMap<String, String>()
    suspend fun fullText(id: String): String = mutex.withLock {
        fullTexts[id] ?: api.fullText(id).also {
            if (fullTexts.size >= 200) fullTexts.remove(fullTexts.keys.first())
            fullTexts[id] = it
        }
    }
}
