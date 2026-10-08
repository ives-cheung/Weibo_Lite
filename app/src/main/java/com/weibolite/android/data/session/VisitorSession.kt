package com.weibolite.android.data.session

import android.app.Activity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.*
import kotlin.coroutines.resume

/** Only anonymous cookies are captured; never logged or copied to app storage. */
class VisitorSession {
    @Volatile private var cookies = ""
    private var pending: Deferred<Unit>? = null
    fun start(activity: Activity, scope: CoroutineScope) {
        if (pending != null && pending?.isCancelled != true) return
        pending = scope.async(Dispatchers.Main.immediate) {
            val cm = CookieManager.getInstance(); cm.setAcceptCookie(true)
            // App WebView is used exclusively for visitor bootstrapping, never login.
            suspendCancellableCoroutine<Unit> { continuation ->
                cm.removeAllCookies { if (continuation.isActive) continuation.resume(Unit) }
            }
            val web = WebView(activity)
            web.settings.javaScriptEnabled = true; web.settings.domStorageEnabled = true
            cm.setAcceptThirdPartyCookies(web, true)
            web.webViewClient = WebViewClient()
            activity.addContentView(web, ViewGroup.LayoutParams(1, 1))
            try {
                web.loadUrl("https://m.weibo.cn/")
                repeat(20) {
                    delay(1000)
                    val c = cm.getCookie("https://m.weibo.cn/") ?: ""
                    if (c.contains("SUB=") && c.contains("XSRF-TOKEN=")) { cookies = c; return@async }
                }
            } finally {
                web.stopLoading(); (web.parent as? ViewGroup)?.removeView(web); web.destroy()
                cm.removeAllCookies(null)
            }
        }
    }
    suspend fun cookie(): String { pending?.await(); return cookies }
}
