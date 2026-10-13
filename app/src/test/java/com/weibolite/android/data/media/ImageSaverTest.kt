package com.weibolite.android.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSaverTest {
    @Test fun originalFormatsGetMatchingExtensions() {
        assertEquals("jpg", imageExtension("image/jpeg"))
        assertEquals("png", imageExtension("image/png"))
        assertEquals("gif", imageExtension("image/gif"))
        assertEquals("webp", imageExtension("image/webp"))
        assertEquals("avif", imageExtension("image/avif"))
    }
    @Test(expected = IllegalStateException::class)
    fun htmlResponsesCannotBecomeImages() { imageExtension("text/html") }
}
