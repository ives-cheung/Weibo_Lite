package com.weibolite.android.data.model

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ModelsTest {
    @Test fun preservesServerRankAndMissingFieldsAndFiltersAds() {
        val root = JSONObject("""{"ok":1,"data":{"realtime":[{"word":"first","realpos":1,"rank":0,"word_scheme":"#first#","num":120000,"label_name":"新"},{"word":"advert","is_ad":1,"realpos":2},{"word":"third","realpos":3,"rank":2},{"word":"unknown"}]}}""")
        val items = Models.hot(root)
        assertEquals(3, items.size)
        assertEquals(1, items[0].rank); assertEquals("#first#", items[0].query)
        assertEquals(120000L, items[0].heat); assertEquals("新", items[0].label)
        assertEquals(3, items[1].rank); assertNull(items[1].heat); assertEquals("", items[1].label)
        assertNull(items[2].rank)
    }
    @Test(expected = IllegalArgumentException::class) fun refusesFailedEnvelope() {
        Models.hot(JSONObject("""{"ok":-100,"data":{"realtime":[]}}"""))
    }
    @Test fun usesThumbnailInsteadOfOriginalInFeed() {
        val p = Models.post(JSONObject("""{"idstr":"123","text":"hello<br>world &amp; you","user":{"id":12,"screen_name":"user"},"pic_ids":["a"],"pic_infos":{"a":{"bmiddle":{"url":"https://img/thumb.jpg"},"original":{"url":"https://img/full.jpg"}}}}"""))
        assertEquals("123", p.id); assertEquals("12", p.user.id)
        assertEquals("https://img/thumb.jpg", p.images.first()); assertEquals("https://img/full.jpg", p.originals.first())
        assertTrue(p.text.contains("world & you")); assertTrue(p.text.contains('\n'))
    }
    @Test fun preservesLongFlagsAndOriginalId() {
        val p = Models.post(JSONObject("""{"id":"123","text":"summary","isLongText":true,"retweeted_status":{"idstr":"456","text":"quoted summary","isLongText":true,"user":{"screen_name":"original"}}}"""))
        assertTrue(p.isLong); assertTrue(p.quotedIsLong); assertEquals("456", p.quotedId)
        assertEquals("@original\nquoted summary", p.quoted)
    }
    @Test fun prefersInlineFullBodyWithoutAnotherRequest() {
        val p = Models.post(JSONObject("""{"id":"123","text":"summary","isLongText":true,"longText":{"longTextContent":"full<br>body &amp; more"}}"""))
        assertFalse(p.isLong); assertEquals("full\nbody & more", p.text)
    }
    @Test fun parsesFullBodyAndRetainsParagraphs() {
        assertEquals("first\nsecond & more", Models.fullText(JSONObject("""{"ok":1,"data":{"ok":1,"longTextContent":"first<br>second &amp; more"}}""")))
    }
    @Test(expected = IllegalStateException::class) fun refusesMissingFullBody() {
        Models.fullText(JSONObject("""{"ok":1,"data":{}}"""))
    }
    @Test fun keepsSummaryParagraphsWithoutHtmlFormattingWhitespace() {
        val p = Models.post(JSONObject("""{"id":"123","text":"first<br/><br/>second"}"""))
        assertEquals("first\n\nsecond", p.text)
    }
    @org.junit.Test fun preservesEmojiOnlyCommentText() {
        org.junit.Assert.assertEquals("[赞][心]", Models.text("<span><img alt='[赞]' src='emoji.png'/><img alt='[心]' src='heart.png'/></span>"))
    }
}
