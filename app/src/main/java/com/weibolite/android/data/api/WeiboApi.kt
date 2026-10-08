package com.weibolite.android.data.api

import com.weibolite.android.data.model.*
import com.weibolite.android.data.session.VisitorSession
import com.weibolite.android.data.storage.LocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class AccessException(message: String) : Exception(message)
class WeiboApi(private val store: LocalStore, private val visitor: VisitorSession) {
    private val desktopMutex = Mutex()
    private var desktopCookie = ""
    private suspend fun desktopVisitor(): String = desktopMutex.withLock {
        if (desktopCookie.isBlank()) {
            val body = get("https://passport.weibo.com/visitor/genvisitor2?cb=visitor_gray_callback&reg=0")
            val response = json(body.substringAfter("(").substringBeforeLast(")"))
            if (response.optInt("retcode") != 20000000) throw AccessException("无法建立访客会话，请稍后重试")
            val data = response.getJSONObject("data")
            desktopCookie = "SUB=${data.getString("sub")}; SUBP=${data.getString("subp")}"
        }
        desktopCookie
    }
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).connectTimeout(12, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()
    companion object {
        const val DESKTOP = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        const val MOBILE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
    }
    private suspend fun get(url: String, session: Boolean = false): String = withContext(Dispatchers.IO) {
        val host = url.toHttpUrl().host
        require(host in setOf("weibo.com", "m.weibo.cn", "s.weibo.com", "passport.weibo.com"))
        val mobile = host == "m.weibo.cn"
        val cookie = if (session) store.cookie().ifBlank { if (mobile) visitor.cookie() else desktopVisitor() } else ""
        val request = Request.Builder().url(url).header("User-Agent", if (mobile) MOBILE else DESKTOP).header("Referer", if (mobile) "https://m.weibo.cn/" else "https://weibo.com/").header("Accept-Language", "zh-CN,zh;q=0.9").header("Accept", "application/json, text/plain, */*").apply {
            if (mobile) { header("MWeibo-Pwa", "1"); header("X-Requested-With", "XMLHttpRequest") }
            if (cookie.isNotBlank()) header("Cookie", cookie)
        }.build()
        client.newCall(request).execute().use { r ->
            if (r.isRedirect) throw AccessException("微博要求验证或登录，请稍后重试或配置 Cookie")
            if (r.code in listOf(401,403,432)) throw AccessException("微博限制了此请求，可稍后重试或在「我的」配置 Cookie")
            check(r.isSuccessful) { "网络请求失败（${r.code}）" }
            r.body?.string() ?: error("服务器返回了空响应")
        }
    }
    private fun json(s: String): JSONObject = try { JSONObject(s) } catch (_: Exception) { throw AccessException("微博返回了验证页面，请稍后重试或配置 Cookie") }
    private fun checked(j: JSONObject): JSONObject {
        if (j.optInt("ok") == -100) throw AccessException("微博需要有效会话，请重试或在「我的」配置 Cookie")
        if (j.optInt("ok") != 1) throw AccessException(j.optString("msg").ifBlank { "微博暂时无法返回数据，请稍后重试" })
        return j
    }
    suspend fun hot(): JSONObject = json(get("https://weibo.com/ajax/side/hotSearch"))
    suspend fun timeline(cursor: String?): Page<Post> {
        val u = "https://weibo.com/ajax/feed/hottimeline".toHttpUrl().newBuilder().addQueryParameter("refresh", if (cursor == null) "1" else "0").addQueryParameter("group_id", "102803").addQueryParameter("containerid", "102803").addQueryParameter("extparam", "discover|new_feed").addQueryParameter("max_id", cursor ?: "0").addQueryParameter("count", "20").build()
        val j = checked(json(get(u.toString(), true)))
        val a = j.optJSONArray("statuses") ?: JSONArray()
        val posts = (0 until a.length()).mapNotNull { val item = a.getJSONObject(it); if (item.optBoolean("isAd") || item.optInt("is_ad") == 1) null else Models.post(item) }
        return Page(posts, j.optString("max_id").takeIf { it.isNotBlank() && it != "0" && it != cursor })
    }
    suspend fun search(q: String, page: Int, users: Boolean): Page<Any> {
        val u = "https://m.weibo.cn/api/container/getIndex".toHttpUrl().newBuilder().addQueryParameter("containerid", "100103type=${if (users) 3 else 1}&q=$q").addQueryParameter("page_type", "searchall").addQueryParameter("page", "$page").build()
        val j = checked(json(get(u.toString(), true)))
        val cards = j.optJSONObject("data")?.optJSONArray("cards") ?: JSONArray()
        val rows = mutableListOf<Any>()
        fun collect(a: JSONArray) {
            for (i in 0 until a.length()) {
                val c = a.optJSONObject(i) ?: continue
                if (users) c.optJSONObject("user")?.let { rows.add(Models.user(it)) }
                else c.optJSONObject("mblog")?.let { rows.add(Models.post(it)) }
                c.optJSONArray("card_group")?.let { collect(it) }
            }
        }
        collect(cards)
        return Page(rows.distinctBy { if (it is Post) it.id else (it as User).id }, if (rows.isEmpty()) null else "${page + 1}")
    }
    suspend fun fullText(id: String): String {
        require(id.isNotBlank() && id.all(Char::isDigit)) { "微博 ID 不正确" }
        var failure: Exception? = null
        for (base in listOf("https://m.weibo.cn/statuses/extend", "https://weibo.com/ajax/statuses/longtext")) {
            try {
                val url = base.toHttpUrl().newBuilder().addQueryParameter("id", id).build()
                return Models.fullText(checked(json(get(url.toString(), true))))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                failure = e
            }
        }
        throw failure ?: AccessException("全文加载失败，请重试")
    }
    suspend fun comments(id: String): List<Comment> {
        val u = "https://m.weibo.cn/api/comments/show".toHttpUrl().newBuilder().addQueryParameter("id", id).addQueryParameter("page", "1").build()
        val j = checked(json(get(u.toString(), true)))
        val a = j.optJSONObject("data")?.optJSONArray("data") ?: JSONArray()
        return (0 until a.length()).map { val c = a.getJSONObject(it); Comment(c.optString("id"), Models.user(c.optJSONObject("user") ?: JSONObject()), Models.text(c.optString("text")), c.optString("created_at")) }
    }
    suspend fun profile(id: String): Pair<User, List<Post>> {
        val u = "https://m.weibo.cn/api/container/getIndex".toHttpUrl().newBuilder().addQueryParameter("containerid", "100505$id").build()
        val info = checked(json(get(u.toString(), true))).getJSONObject("data").getJSONObject("userInfo")
        val feedUrl = "https://m.weibo.cn/api/container/getIndex".toHttpUrl().newBuilder().addQueryParameter("containerid", "107603$id").addQueryParameter("page", "1").build()
        val cards = checked(json(get(feedUrl.toString(), true))).optJSONObject("data")?.optJSONArray("cards") ?: JSONArray()
        return Models.user(info) to (0 until cards.length()).mapNotNull { cards.getJSONObject(it).optJSONObject("mblog")?.let(Models::post) }
    }
}
