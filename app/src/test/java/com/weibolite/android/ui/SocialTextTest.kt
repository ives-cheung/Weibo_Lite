package com.weibolite.android.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import org.junit.Assert.*
import org.junit.Test

class SocialTextTest {
    @Test fun adjacentTopicsCopyIndependentlyWithHashes() {
        val copied = mutableListOf<String>()
        val raw = "#对手说孙颖莎身体看起来不太好##对手谈孙颖莎比赛状态# 正文"
        val result = socialText(raw, Color.Blue) { copied.add(it) }
        val links = result.getLinkAnnotations(0, result.length)
        assertEquals(raw, result.text)
        assertEquals(2, links.size)
        links.forEach { range ->
            val link = range.item as LinkAnnotation.Clickable
            link.linkInteractionListener!!.onClick(link)
        }
        assertEquals(listOf("#对手说孙颖莎身体看起来不太好#", "#对手谈孙颖莎比赛状态#"), copied)
    }

    @Test fun mentionsAndIncompleteTopicsAreNotCopyLinks() {
        val result = socialText("回复 @用户: 正文 #未闭合\n下一行#", Color.Blue) {}
        assertTrue(result.getLinkAnnotations(0, result.length).isEmpty())
        assertEquals(1, result.spanStyles.size)
    }

    @Test fun newlineDelimitedTopicsKeepExactText() {
        val result = socialText("正文\n#话题 A#\n#话题B#", Color.Blue) {}
        assertEquals(listOf("#话题 A#", "#话题B#"), result.getLinkAnnotations(0, result.length).map { result.text.substring(it.start, it.end) })
    }
}
