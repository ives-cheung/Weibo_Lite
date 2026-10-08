package com.weibolite.android.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

private val socialPattern = Regex("#[^#\\r\\n]+#|(?<![A-Za-z0-9_@])@[\\p{L}\\p{N}_·-]+")

/** Only complete hashtag runs are copy links; mentions retain their blue styling. */
internal fun socialText(text: String, accent: Color, onTopic: ((String) -> Unit)? = null): AnnotatedString = buildAnnotatedString {
    append(text)
    socialPattern.findAll(text).forEach { match ->
        val start = match.range.first
        val end = match.range.last + 1
        addStyle(SpanStyle(color = accent), start, end)
        if (match.value.startsWith('#') && onTopic != null) {
            addLink(LinkAnnotation.Clickable(tag = match.value, linkInteractionListener = { onTopic(match.value) }), start, end)
        }
    }
}

@Composable
internal fun copyableSocialText(text: String, accent: Color): AnnotatedString {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    return remember(text, accent, clipboard, context) {
        socialText(text, accent) { topic ->
            clipboard.setText(AnnotatedString(topic))
            Toast.makeText(context, "话题已复制", Toast.LENGTH_SHORT).show()
        }
    }
}
