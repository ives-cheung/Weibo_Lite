package com.weibolite.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weibolite.android.data.api.WeiboApi
import com.weibolite.android.data.model.*
import com.weibolite.android.data.repository.*
import com.weibolite.android.data.session.VisitorSession
import com.weibolite.android.data.storage.LocalStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val store = LocalStore(app)
    val visitor = VisitorSession()
    private val api = WeiboApi(store, visitor)
    private val hotRepository = HotSearchRepository(app, api)
    private val timelineRepository = TimelineRepository(api)
    private val searchRepository = SearchRepository(api)
    private val postsRepository = PostRepository(api)
    private val _fullTexts = MutableStateFlow<Map<String, FullTextState>>(emptyMap()); val fullTexts = _fullTexts.asStateFlow()
    private val usersRepository = UserRepository(api)
    private val _hot = MutableStateFlow(FeedState<HotTopic>()); val hot = _hot.asStateFlow()
    private val _home = MutableStateFlow(FeedState<Post>()); val home = _home.asStateFlow()
    private val _search = MutableStateFlow(FeedState<Any>()); val search = _search.asStateFlow()
    private val _comments = MutableStateFlow(FeedState<Comment>()); val comments = _comments.asStateFlow()
    private val _profile = MutableStateFlow<Pair<User?, FeedState<Post>>>(null to FeedState()); val profile = _profile.asStateFlow()
    private val _theme = MutableStateFlow(store.theme); val theme = _theme.asStateFlow()
    private val _history = MutableStateFlow(store.history()); val history = _history.asStateFlow()
    private val _account = MutableStateFlow(store.cookie().isNotBlank()); val account = _account.asStateFlow()
    private var searchJob: Job? = null
    private var hotJob: Job? = null
    private var query = ""; private var users = false
    private fun message(t: Throwable) = when (t) {
        is java.net.UnknownHostException -> "网络不可用，请检查连接后重试"
        is java.net.SocketTimeoutException -> "请求超时，请重试"
        is java.io.IOException -> "网络连接失败，请重试"
        else -> t.message ?: "加载失败，请重试"
    }
    init { refreshHome() }
    fun refreshHot(force: Boolean = false) {
        if (hotJob?.isActive == true) return
        hotJob = viewModelScope.launch {
            val cached = hotRepository.cached()
            if (_hot.value.items.isEmpty() && cached != null) _hot.value = FeedState(cached.first, updated = cached.second)
            _hot.value = _hot.value.copy(loading = true, error = null)
            try { val (rows, time) = hotRepository.refresh(force); _hot.value = FeedState(rows, updated = time) }
            catch (t: Exception) { if (t is CancellationException) throw t; _hot.value = _hot.value.copy(loading = false, error = message(t)) }
        }
    }
    fun refreshHome(more: Boolean = false) {
        if (_home.value.loading || (more && _home.value.next == null)) return
        val old = _home.value
        _home.value = old.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val page = timelineRepository.page(if (more) old.next else null)
                _home.value = FeedState(((if (more) old.items else emptyList()) + page.items).distinctBy { it.id }, next = page.next, updated = System.currentTimeMillis())
            } catch (t: Exception) { if (t is CancellationException) throw t; _home.value = old.copy(error = message(t)) }
        }
    }
    fun search(q: String, userMode: Boolean = false, more: Boolean = false) {
        val clean = q.trim(); if (clean.isBlank()) { searchJob?.cancel(); _search.value = FeedState(); return }
        if (more && (_search.value.loading || _search.value.next == null)) return
        searchJob?.cancel(); query = clean; users = userMode
        store.remember(clean); _history.value = store.history()
        val old = if (more) _search.value else FeedState()
        _search.value = old.copy(loading = true, error = null)
        searchJob = viewModelScope.launch {
            try {
                val page = searchRepository.page(clean, if (more) old.next?.toIntOrNull() ?: 1 else 1, userMode)
                _search.value = FeedState(((if (more) old.items else emptyList()) + page.items).distinctBy { if (it is Post) it.id else (it as User).id }, next = page.next)
            } catch (t: Exception) { if (t is CancellationException) throw t; _search.value = old.copy(error = message(t)) }
        }
    }
    fun retrySearch() = search(query, users)
    fun clearHistory() { store.clearHistory(); _history.value = emptyList() }
    fun theme(value: String) { store.theme = value; _theme.value = value }
    suspend fun saveCookie(value: String) = withContext(Dispatchers.IO) { store.saveCookie(value); _account.value = value.isNotBlank() }
    fun clearCache() { viewModelScope.launch { hotRepository.clear(); _hot.value = FeedState() } }
    fun expandFullText(id: String) {
        val current = _fullTexts.value[id]
        if (current?.loading == true || current?.text != null) return
        fun set(state: FullTextState) {
            val copy = _fullTexts.value.toMutableMap()
            if (!copy.containsKey(id) && copy.size >= 200) copy.keys.firstOrNull { copy[it]?.loading != true }?.let { copy.remove(it) }
            copy[id] = state; _fullTexts.value = copy
        }
        set(FullTextState(loading = true))
        viewModelScope.launch {
            try { set(FullTextState(text = postsRepository.fullText(id))) }
            catch (t: Exception) { if (t is CancellationException) throw t; set(FullTextState(error = message(t))) }
        }
    }
    fun loadComments(id: String) {
        _comments.value = FeedState(loading = true)
        viewModelScope.launch {
            try { _comments.value = FeedState(api.comments(id)) }
            catch (t: Exception) { if (t is CancellationException) throw t; _comments.value = FeedState(error = message(t)) }
        }
    }
    fun loadProfile(id: String) {
        _profile.value = null to FeedState(loading = true)
        viewModelScope.launch {
            try { val (user, posts) = usersRepository.profile(id); _profile.value = user to FeedState(posts) }
            catch (t: Exception) { if (t is CancellationException) throw t; _profile.value = null to FeedState(error = message(t)) }
        }
    }
}
