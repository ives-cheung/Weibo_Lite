package com.weibolite.android.data.storage

import android.content.Context
import android.annotation.SuppressLint
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    private val secretPrefs = context.getSharedPreferences("account_encrypted", Context.MODE_PRIVATE)
    var theme: String
        get() = prefs.getString("theme", "system") ?: "system"
        set(value) { prefs.edit().putString("theme", value).apply() }
    fun history(): List<String> = runCatching { val a = JSONArray(prefs.getString("history", "[]")); (0 until a.length()).map { a.getString(it) } }.getOrDefault(emptyList())
    fun remember(q: String) { prefs.edit().putString("history", JSONArray((listOf(q) + history()).distinct().take(20)).toString()).apply() }
    fun clearHistory() { prefs.edit().remove("history").apply() }
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("weibo_lite_cookie", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("weibo_lite_cookie", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun cookie(): String = runCatching {
        val raw = secretPrefs.getString("ciphertext", null) ?: return ""
        val iv = Base64.decode(secretPrefs.getString("iv", ""), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(Base64.decode(raw, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrDefault("")
    @SuppressLint("ApplySharedPref") // Called on IO; report success only after encryption is persisted.
    fun saveCookie(cookie: String) {
        if (cookie.isBlank()) { check(secretPrefs.edit().clear().commit()) { "无法删除 Cookie" }; return }
        require(cookie.length <= 16384 && !cookie.contains('\n') && !cookie.contains('\r')) { "Cookie 格式不正确" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(cookie.trim().toByteArray(Charsets.UTF_8))
        check(secretPrefs.edit().putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) { "无法保存 Cookie" }
    }
}
