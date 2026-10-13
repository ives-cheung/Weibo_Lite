package com.weibolite.android.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** Downloads original image bytes, then publishes a completed image to the system gallery. */
object ImageSaver {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS).build()

    suspend fun save(context: Context, rawUrl: String) = withContext(Dispatchers.IO) {
        val url = rawUrl.toHttpUrl().newBuilder().scheme("https").build()
        val temp = File.createTempFile("weibo-image-", ".tmp", context.cacheDir)
        try {
            val request = Request.Builder().url(url).header("Referer", "https://weibo.com/").build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "图片下载失败（${response.code}）" }
                val body = response.body ?: error("图片内容为空")
                body.byteStream().use { input -> temp.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                } }
            }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temp.absolutePath, options)
            check(options.outWidth > 0 && options.outHeight > 0) { "服务器未返回可保存的图片" }
            val mime = options.outMimeType ?: error("无法识别图片格式")
            val extension = imageExtension(mime)
            val name = "WeiboLite_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
            val resolver = context.contentResolver
            var legacyFile: File? = null
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/WeiboLite")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                } else {
                    val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "WeiboLite")
                    check(folder.isDirectory || folder.mkdirs()) { "无法创建相册目录" }
                    legacyFile = File(folder, name)
                    @Suppress("DEPRECATION")
                    put(MediaStore.Images.Media.DATA, legacyFile!!.absolutePath)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(collection, values) ?: error("无法创建相册文件")
            try {
                resolver.openOutputStream(uri)?.use { output -> temp.inputStream().use { it.copyTo(output) } } ?: error("无法写入相册")
                coroutineContext.ensureActive()
                if (Build.VERSION.SDK_INT >= 29) {
                    check(resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null) == 1) { "无法完成图片保存" }
                }
                uri
            } catch (e: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                legacyFile?.delete()
                throw e
            }
        } finally {
            temp.delete()
        }
    }
}

internal fun imageExtension(mime: String): String = when (mime.lowercase()) {
    "image/jpeg" -> "jpg"
    "image/png" -> "png"
    "image/gif" -> "gif"
    "image/webp" -> "webp"
    "image/heic", "image/heif" -> "heic"
    "image/avif" -> "avif"
    else -> error("不支持保存该图片格式")
}
