package com.weibolite.android.data.model

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

data class HotTopic(val rank: Int?, val word: String, val query: String, val heat: Long?, val label: String)
data class User(val id: String, val name: String, val avatar: String, val verified: Boolean = false, val bio: String = "", val followers: String = "", val following: String = "", val count: String = "")
data class Post(val id: String, val user: User, val text: String, val time: String, val images: List<String>, val originals: List<String>, val reposts: String, val comments: String, val likes: String, val quoted: String = "", val isLong: Boolean = false, val quotedId: String = "", val quotedIsLong: Boolean = false)
data class FullTextState(val text: String? = null, val loading: Boolean = false, val error: String? = null)
data class Comment(val id: String, val user: User, val text: String, val time: String)
data class Page<T>(val items: List<T>, val next: String?)
data class FeedState<T>(val items: List<T> = emptyList(), val loading: Boolean = false, val error: String? = null, val next: String? = null, val updated: Long = 0)

object Models {
    fun text(html: String): String {
        val doc = Jsoup.parse(html.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n"))
        doc.select("img[alt]").forEach { image -> image.replaceWith(org.jsoup.nodes.TextNode(image.attr("alt"))) }
        return doc.wholeText().trim()
    }
    fun user(j: JSONObject) = User(j.optString("idstr").ifBlank { j.optString("id") }, j.optString("screen_name"), j.optString("profile_image_url").ifBlank { j.optString("avatar_hd") }, j.optBoolean("verified"), j.optString("description"), j.optString("followers_count"), j.optString("friends_count").ifBlank { j.optString("follow_count") }, j.optString("statuses_count"))
    private fun inlineFull(j: JSONObject): String = j.optJSONObject("longText")?.optString("longTextContent")?.takeIf { it.isNotBlank() } ?: ""
    private fun summary(j: JSONObject): String {
        val inline = inlineFull(j)
        if (inline.isNotBlank()) return text(inline)
        if (j.optString("text_raw").isNotBlank()) return text(j.optString("text_raw"))
        val html = j.optString("text")
        val doc = Jsoup.parse(html)
        doc.outputSettings().prettyPrint(false)
        doc.select("a").filter { it.text() in listOf("全文", "展开全文") && (it.attr("href").contains("/status") || it.hasAttr("action-type")) }.forEach { it.remove() }
        return text(doc.body().html())
    }
    private fun needsFull(j: JSONObject) = inlineFull(j).isBlank() && (j.optBoolean("isLongText") || j.optInt("isLongText") == 1 || Jsoup.parse(j.optString("text")).select("a").any { it.text() in listOf("全文", "展开全文") && it.attr("href").contains("/status") })
    fun fullText(root: JSONObject): String {
        require(root.optInt("ok") == 1) { "全文暂时无法获取，请重试" }
        val data = root.optJSONObject("data") ?: error("全文数据格式已变化")
        if (data.has("ok")) require(data.optInt("ok") == 1) { "全文暂时无法获取，请重试" }
        val full = data.optString("longTextContent").ifBlank { data.optString("longTextContent_raw") }
        return text(full).takeIf { it.isNotBlank() } ?: error("接口未返回全文，请重试")
    }
    fun post(j: JSONObject): Post {
        val pics = j.optJSONArray("pics")
        val small = mutableListOf<String>(); val large = mutableListOf<String>()
        if (pics != null) for (i in 0 until pics.length()) {
            val p = pics.getJSONObject(i); small.add(p.optString("url")); large.add(p.optJSONObject("large")?.optString("url") ?: p.optString("url"))
        } else {
            val ids = j.optJSONArray("pic_ids") ?: JSONArray(); val info = j.optJSONObject("pic_infos")
            for (i in 0 until ids.length()) {
                val p = info?.optJSONObject(ids.getString(i)) ?: continue
                val thumb = p.optJSONObject("bmiddle")?.optString("url") ?: p.optJSONObject("thumbnail")?.optString("url") ?: ""
                small.add(thumb); large.add(p.optJSONObject("original")?.optString("url") ?: thumb)
            }
        }
        val u = j.optJSONObject("user") ?: JSONObject()
        val original = j.optJSONObject("retweeted_status")
        val quote = original?.let { "@${it.optJSONObject("user")?.optString("screen_name") ?: ""}\n${summary(it)}" } ?: ""
        return Post(j.optString("idstr").ifBlank { j.optString("id") }, user(u), summary(j), j.optString("created_at"), small.filter { it.isNotBlank() }, large.filter { it.isNotBlank() }, j.optString("reposts_count"), j.optString("comments_count"), j.optString("attitudes_count"), quote, needsFull(j), original?.optString("idstr")?.ifBlank { original.optString("id") } ?: "", original?.let(::needsFull) ?: false)
    }
    fun hot(root: JSONObject): List<HotTopic> {
        require(root.optInt("ok") == 1) { "热搜暂时无法访问，请稍后重试" }
        val rows = root.optJSONObject("data")?.optJSONArray("realtime") ?: error("热搜数据格式已变化")
        return (0 until rows.length()).mapNotNull { i ->
            val j = rows.getJSONObject(i)
            if (j.optInt("is_ad") == 1 || j.optString("word").isBlank()) null else {
                val rank = if (j.has("realpos")) j.optInt("realpos").takeIf { it > 0 } else if (j.has("rank")) j.optInt("rank") + 1 else null
                HotTopic(rank, j.getString("word"), j.optString("word_scheme").ifBlank { j.getString("word") }, if (j.has("num") && !j.isNull("num")) j.optLong("num") else null, j.optString("label_name"))
            }
        }
    }
}
