package com.weibolite.android.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.weibolite.android.data.media.ImageSaver
import kotlinx.coroutines.CancellationException
import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.weibolite.android.data.model.*
import com.weibolite.android.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private data class FullTextUi(val states: Map<String, FullTextState> = emptyMap(), val request: (String) -> Unit = {})
private val LocalFullTextUi = staticCompositionLocalOf { FullTextUi() }

private val Blue = Color(0xFF2678F5)
private fun colors(dark: Boolean) = if (dark) darkColorScheme(primary = Blue, background = Color(0xFF0D1420), surface = Color(0xFF182230), onSurface = Color(0xFFF0F3F8), onBackground = Color(0xFFF0F3F8), surfaceVariant = Color(0xFF253344), onSurfaceVariant = Color(0xFFA9B7CA), outlineVariant = Color(0xFF344155)) else lightColorScheme(primary = Blue, background = Color(0xFFF0F3F9), surface = Color.White, onSurface = Color(0xFF172235), onBackground = Color(0xFF172235), surfaceVariant = Color(0xFFEAF0F8), onSurfaceVariant = Color(0xFF66768C), outlineVariant = Color(0xFFDDE5EF))

private val LocalBackdrop = staticCompositionLocalOf<GraphicsLayer?> { null }
private val LocalFeedPadding = staticCompositionLocalOf { PaddingValues() }

/** Replays only the content layer, never the controls, into a GPU-blurred glass surface. */
@Composable private fun GlassPanel(modifier: Modifier = Modifier, radius: Int = 28, frosted: Boolean = false, content: @Composable () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.red < 0.2f
    val shape = RoundedCornerShape(radius.dp)
    val backdrop = LocalBackdrop.current
    var position by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    var glassSize by remember { mutableStateOf(IntSize.Zero) }
    val blur = remember(density, glassSize, frosted) { glassEffect(glassSize, density.density, if (frosted) 9f else 3f) }
    Box(modifier.shadow(10.dp, shape, ambientColor = Blue.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.12f))
        .clip(shape).onSizeChanged { glassSize = it }.onGloballyPositioned { position = it.positionInRoot() }
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) 0.48f else 0.9f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.55f))), shape)) {
        Canvas(Modifier.matchParentSize().graphicsLayer { renderEffect = blur }) {
            if (backdrop != null) translate(-position.x, -position.y) { drawLayer(backdrop) }
        }
        Box(Modifier.matchParentSize().background(Brush.linearGradient(if (frosted && dark) listOf(Color(0xB325374D), Color(0xA6101C2D), Color(0xB32D405B)) else if (frosted) listOf(Color(0xB3FFFFFF), Color(0xA6E3F0FF), Color(0xB3FFFFFF)) else if (dark) listOf(Color(0x7725374D), Color(0x66101C2D), Color(0x772D405B)) else listOf(Color(0x77FFFFFF), Color(0x66E3F0FF), Color(0x77FFFFFF)))))
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = if (dark) 0.12f else 0.35f), Color.Transparent), center = Offset(size.width * 0.15f, 0f), radius = size.width * 0.65f))
        }
        content()
    }
}

/** A single spring-driven bubble travels between tabs; its leading edge moves first. */
@Composable private fun BubbleNavigation(tab: Int, select: (Int) -> Unit) {
    val transition = updateTransition(tab, label = "navigationBubble")
    val left by transition.animateFloat(transitionSpec = {
        spring(dampingRatio = 0.72f, stiffness = if (targetState > initialState) 220f else 500f)
    }, label = "bubbleLeft") { it.toFloat() }
    val right by transition.animateFloat(transitionSpec = {
        spring(dampingRatio = 0.72f, stiffness = if (targetState > initialState) 500f else 220f)
    }, label = "bubbleRight") { it + 1f }
    BoxWithConstraints(Modifier.fillMaxWidth().height(66.dp).padding(5.dp).clip(RoundedCornerShape(29.dp))) {
        val slot = maxWidth / 4
        val density = LocalDensity.current
        Box(Modifier.offset { IntOffset(with(density) { (slot * left).toPx() }.roundToInt(), 0) }
            .width(slot * (right - left).coerceAtLeast(0.65f)).fillMaxHeight().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Blue.copy(alpha = 0.22f), Blue.copy(alpha = 0.10f))))
            .border(0.7.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(28.dp)))
        Row(Modifier.fillMaxSize()) {
            val labels = listOf("首页", "热搜", "搜索", "我的")
            val icons = listOf(Icons.Default.Home, Icons.Default.Star, Icons.Default.Search, Icons.Default.Person)
            labels.forEachIndexed { i, label ->
                Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(28.dp)).selectable(selected = tab == i, role = Role.Tab, onClick = { select(i) }), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(icons[i], label, Modifier.size(23.dp), tint = if (tab == i) Blue else MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(3.dp))
                    Text(label, fontSize = 11.sp, color = if (tab == i) Blue else MaterialTheme.colorScheme.onSurface, fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WeiboApp(vm: MainViewModel) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val fullTexts by vm.fullTexts.collectAsStateWithLifecycle()
    val dark = theme == "dark" || (theme == "system" && isSystemInDarkTheme())
    val activity = LocalContext.current as? Activity
    SideEffect { activity?.window?.let { window -> WindowCompat.getInsetsController(window, window.decorView).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark } } }
    val typography = Typography(
        bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
        titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium)
    )
    MaterialTheme(colorScheme = colors(dark), typography = typography) {
        CompositionLocalProvider(LocalFullTextUi provides FullTextUi(fullTexts, vm::expandFullText)) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background)))) { CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { AppContent(vm) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AppContent(vm: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var searchOriginTab by rememberSaveable { mutableIntStateOf(0) }
    val hotListState = rememberLazyListState()
    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var userMode by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Post?>(null) }
    var profileId by remember { mutableStateOf<String?>(null) }
    var gallery by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var cookieDialog by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    val home by vm.home.collectAsStateWithLifecycle(); val hot by vm.hot.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle(); val comments by vm.comments.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle(); val history by vm.history.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle(); val theme by vm.theme.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val snack = remember { SnackbarHostState() }; val scope = rememberCoroutineScope()
    fun openPost(p: Post) { selected = p; vm.loadComments(p.id) }
    fun openUser(u: User) { profileId = u.id; vm.loadProfile(u.id) }
    fun returnFromSearch() { tab = searchOriginTab; keyboard?.hide() }
    fun doSearch(q: String) { if (tab != 2) searchOriginTab = tab; query = q; submitted = true; tab = 2; userMode = false; keyboard?.hide(); vm.search(q) }
    BackHandler(selected != null || profileId != null || tab != 0) {
        when { profileId != null -> profileId = null; selected != null -> selected = null; tab == 2 -> returnFromSearch(); else -> tab = 0 }
    }
    LaunchedEffect(tab, submitted) { if (tab == 1) vm.refreshHot(); if (tab == 2 && !submitted) { focus.requestFocus(); keyboard?.show() } }
    val backdrop = rememberGraphicsLayer()
    val backdropBrush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background))
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            GlassPanel(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(), radius = 26) {
            if (tab == 2 && selected == null && profileId == null) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { returnFromSearch() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, if (searchOriginTab == 1) "返回热搜界面" else "返回首页") }
                    TextField(value = query, onValueChange = { query = it; submitted = false }, modifier = Modifier.weight(1f).focusRequester(focus), placeholder = { Text("搜索微博或用户", fontSize = 16.sp) }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) { submitted = true; keyboard?.hide(); vm.search(query, userMode) } }), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                    IconButton(onClick = { if (query.isNotBlank()) { submitted = true; keyboard?.hide(); vm.search(query, userMode) } }) { Icon(Icons.Default.Search, "执行搜索") }
                }
            } else {
                TopAppBar(title = { Column { Text(when { profileId != null -> "用户主页"; selected != null -> selected!!.user.name; tab == 1 -> "微博热搜"; tab == 3 -> "我的"; else -> "Weibo Lite" }, fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (tab == 0 && selected == null && profileId == null) Text("公开微博 · 访客浏览", fontSize = 11.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }, navigationIcon = { if (selected != null || profileId != null) IconButton(onClick = { if (profileId != null) profileId = null else selected = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, actions = {
                    if (selected == null && profileId == null && tab in 0..1) IconButton(onClick = { if (tab == 1) vm.refreshHot(true) else vm.refreshHome() }) { Icon(Icons.Default.Refresh, "刷新") }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent), windowInsets = WindowInsets(0, 0, 0, 0))
            }
            }
        },
        bottomBar = {
            if (selected == null && profileId == null) {
                GlassPanel(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp).fillMaxWidth(), radius = 34, frosted = true) {
                    BubbleNavigation(tab) { i -> tab = i; if (i == 2) { submitted = false; searchOriginTab = 0 }; keyboard?.hide() }
                }
            }
        }
    ) { padding ->
        CompositionLocalProvider(LocalFeedPadding provides padding) {
        Box(Modifier.fillMaxSize().drawWithContent {
            backdrop.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop)
        }.background(backdropBrush)) {
            when {
                profileId != null -> {
                    val (user, state) = profile
                    LazyColumn(contentPadding = LocalFeedPadding.current) { item { if (user != null) ProfileHeader(user) }
                        item { StateMessage(state.loading, state.items.isEmpty(), state.error, "暂无公开微博") { vm.loadProfile(profileId!!) } }
                        items(state.items, key = { it.id }) { p -> PostRow(p, { openPost(p) }, { openUser(p.user) }, { images, at -> gallery = images to at }) }
                    }
                }
                selected != null -> {
                    val p = selected!!
                    LazyColumn(contentPadding = LocalFeedPadding.current) {
                        item { PostRow(p, {}, { openUser(p.user) }, { images, at -> gallery = images to at }, expanded = true) }
                        item {
                            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 22.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("评论", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                Text(p.comments.ifBlank { comments.items.size.toString() }, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item { StateMessage(comments.loading, comments.items.isEmpty(), comments.error, "暂无公开评论") { vm.loadComments(p.id) } }
                        itemsIndexed(comments.items, key = { _, c -> c.id }) { index, c ->
                            CommentRow(c, index == 0, index == comments.items.lastIndex) { openUser(c.user) }
                        }
                    }
                }
                tab == 0 -> PostFeed(home, { vm.refreshHome() }, { vm.refreshHome(true) }, ::openPost, ::openUser, { images, at -> gallery = images to at })
                tab == 1 -> PullToRefreshBox(isRefreshing = hot.loading && hot.items.isNotEmpty(), onRefresh = { vm.refreshHot(true) }) {
                    LazyColumn(Modifier.fillMaxSize(), state = hotListState, contentPadding = LocalFeedPadding.current) {
                        item { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("实时榜单", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(if (hot.updated == 0L) "下拉刷新" else "更新于 ${SimpleDateFormat("HH:mm", Locale.CHINA).format(hot.updated)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        item { StateMessage(hot.loading, hot.items.isEmpty(), hot.error, "暂无热搜") { vm.refreshHot(true) } }
                        items(hot.items, key = { it.word }) { topic ->
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)).heightIn(min = 60.dp).clickable { doSearch(topic.query) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(topic.rank?.toString()?.padStart(2, '0') ?: "—", Modifier.width(36.dp), color = if ((topic.rank ?: 99) <= 3) Blue else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text(topic.word, Modifier.weight(1f), fontSize = 16.sp, lineHeight = 22.sp)
                                if (topic.label.isNotBlank()) Text(topic.label, Modifier.padding(start = 6.dp), color = if (topic.label in listOf("爆", "沸")) Color(0xFFE36D49) else Blue, fontSize = 11.sp)
                                topic.heat?.let { Text(heat(it), Modifier.padding(start = 12.dp).widthIn(min = 40.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
                tab == 2 -> Column(Modifier.padding(top = padding.calculateTopPadding())) {
                    CompositionLocalProvider(LocalFeedPadding provides PaddingValues(bottom = padding.calculateBottomPadding())) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        listOf(false to "微博", true to "用户").forEach { (mode, label) -> TextButton(onClick = { userMode = mode; if (submitted) vm.search(query, mode) }) { Text(label, color = if (userMode == mode) Blue else MaterialTheme.colorScheme.onSurfaceVariant) } }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                    if (!submitted) LazyColumn(contentPadding = LocalFeedPadding.current) {
                        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Section("最近搜索"); TextButton(onClick = { vm.clearHistory() }) { Text("清除") } } }
                        if (history.isEmpty()) item { Text("搜索记录仅保存在本机", Modifier.padding(16.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(history) { q -> Row(Modifier.fillMaxWidth().clickable { query = q; submitted = true; keyboard?.hide(); vm.search(q, userMode) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Text(q, Modifier.padding(start = 14.dp), fontSize = 15.sp) } }
                    } else SearchFeed(search, { vm.retrySearch() }, { vm.search(query, userMode, true) }, ::openPost, ::openUser, { images, at -> gallery = images to at }, { cookieDialog = true })
                }
                    }
                else -> LazyColumn(contentPadding = LocalFeedPadding.current) {
                    item { Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) { Box(Modifier.size(60.dp).clip(CircleShape).background(Blue.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, Modifier.size(32.dp), tint = Blue) }; Column { Text(if (account) "Cookie 已配置" else "访客模式", fontSize = 20.sp, fontWeight = FontWeight.Medium); Text("轻量浏览，内容优先", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                    item { Section("账号") }
                    item { SettingsRow("登录 / Cookie", if (account) "已加密保存在本机" else "热搜无需登录") { cookieDialog = true } }
                    item { Section("外观") }
                    item { Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) { listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (mode, name) -> TextButton(onClick = { vm.theme(mode) }) { Text(name, color = if (theme == mode) Blue else MaterialTheme.colorScheme.onSurfaceVariant) } } } }
                    item { Section("存储与隐私") }
                    item { SettingsRow("清除热搜缓存", "下次进入重新获取") { vm.clearCache(); scope.launch { snack.showSnackbar("热搜缓存已清除") } } }
                    item { SettingsRow("清除搜索记录", "记录仅存储在本机") { vm.clearHistory(); scope.launch { snack.showSnackbar("搜索记录已清除") } } }
                    item { SettingsRow("关于 Weibo Lite", "0.2.9 · 独立第三方客户端") { about = true } }
                }
            }
        }
    }
    }
    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)))
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)))
    }
    }
    if (cookieDialog) CookieDialog(vm, { cookieDialog = false }, { scope.launch { snack.showSnackbar(it) } })
    if (about) AlertDialog(onDismissRequest = { about = false }, title = { Text("Weibo Lite") }, text = { Text("轻量第三方微博阅读器\n\nAndroid 10 及以上仅需网络权限；Android 8–9 保存图片时会请求存储权限。无广告、统计或推送 SDK。Cookie 由 Android Keystore 加密保存在本机，仅向微博服务发送。\n\n当前版本提供公开首页、热搜和搜索；微博可能要求有效会话或验证。与微博官方无关联。") }, confirmButton = { TextButton(onClick = { about = false }) { Text("知道了") } })
    gallery?.let { (images, at) -> ImageViewer(images, at) { gallery = null } }
}

private val ReplyPrefix = Regex("^回复\\s*(@[^:：\\n]+)[:：]\\s*")

@Composable private fun CommentRow(comment: Comment, first: Boolean, last: Boolean, openUser: () -> Unit) {
    val corners = RoundedCornerShape(topStart = if (first) 24.dp else 0.dp, topEnd = if (first) 24.dp else 0.dp, bottomStart = if (last) 24.dp else 0.dp, bottomEnd = if (last) 24.dp else 0.dp)
    val reply = remember(comment.text) { ReplyPrefix.find(comment.text) }
    val body = remember(comment.text) { reply?.let { comment.text.substring(it.range.last + 1) } ?: comment.text }
    var expanded by rememberSaveable(comment.id) { mutableStateOf(false) }
    var overflow by remember(comment.id, body) { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 12.dp).fillMaxWidth().clip(corners).background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Blue.copy(alpha = 0.1f)).clickable(onClick = openUser), contentAlignment = Alignment.Center) {
                Text(comment.user.name.take(1), color = Blue, fontSize = 13.sp)
                if (comment.user.avatar.isNotBlank()) AsyncImage(image(comment.user.avatar), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(comment.user.name, Modifier.weight(1f).clickable(onClick = openUser), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(comment.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (reply != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(remember(reply.value) { socialText("回复 ${reply.groupValues[1]}", Blue) }, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(7.dp))
                Text(copyableSocialText(body, Blue), fontSize = 15.sp, lineHeight = 25.sp, maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis, onTextLayout = { if (!expanded && it.hasVisualOverflow) overflow = true })
                if (overflow) TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) { Text(if (expanded) "收起" else "展开", fontSize = 13.sp) }
            }
        }
        if (!last) HorizontalDivider(Modifier.padding(start = 60.dp, end = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    }
}

@Composable private fun Section(title: String) { Text(title, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
@Composable private fun SettingsRow(title: String, subtitle: String, action: () -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)).clickable(onClick = action).padding(horizontal = 16.dp, vertical = 16.dp)) { Text(title, fontSize = 16.sp); Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
@Composable private fun StateMessage(loading: Boolean, empty: Boolean, error: String?, emptyMessage: String, retry: () -> Unit) {
    if (loading && empty) Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
    else if (error != null) Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp); TextButton(onClick = retry) { Text("重试") } }
    else if (empty) Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) { Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
}
@Composable private fun Avatar(user: User, action: () -> Unit = {}) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(Blue.copy(alpha = 0.1f)).clickable(onClick = action), contentAlignment = Alignment.Center) {
        Text(user.name.take(1), color = Blue)
        if (user.avatar.isNotBlank()) AsyncImage(image(user.avatar), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}
@Composable private fun image(url: String) = ImageRequest.Builder(LocalContext.current).data(url).addHeader("Referer", "https://weibo.com/").crossfade(150).build()
@Composable private fun PostRow(p: Post, click: () -> Unit, user: () -> Unit, picture: (List<String>, Int) -> Unit, expanded: Boolean = false) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth().shadow(3.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(24.dp)).clickable(onClick = click).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.user, user)
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.user.name, Modifier.weight(1f).clickable(onClick = user), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (p.user.verified) Text("✓", Modifier.padding(start = 4.dp), color = Blue, fontSize = 12.sp)
                }
                Text(time(p.time), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth()) {
            LongPostText(p.id, p.text, p.isLong, expanded)
            if (p.quoted.isNotBlank()) Column(Modifier.padding(top = 8.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                LongPostText(p.quotedId, p.quoted, p.quotedIsLong, expanded, quotePrefix = p.quoted.substringBefore('\n') + "\n", quoted = true)
            }
            if (p.images.isNotEmpty()) Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                p.images.chunked(3).forEachIndexed { row, urls -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    urls.forEachIndexed { col, url -> AsyncImage(image(url), "微博图片 ${row * 3 + col + 1}", Modifier.weight(1f).aspectRatio(if (p.images.size == 1) 1.35f else 1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { picture(p.originals.ifEmpty { p.images }, row * 3 + col) }, contentScale = ContentScale.Crop) }
                    if (p.images.size > 1) repeat(3 - urls.size) { Spacer(Modifier.weight(1f)) }
                } }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                PostMetric(R.drawable.ic_repost, "转发", p.reposts)
                PostMetric(R.drawable.ic_comment, "评论", p.comments)
                PostMetric(R.drawable.ic_like, "点赞", p.likes)
            }
        }
    }
}
@Composable private fun LongPostText(id: String, preview: String, requiresFull: Boolean, initiallyExpanded: Boolean, quotePrefix: String = "", quoted: Boolean = false) {
    val ui = LocalFullTextUi.current
    val state = ui.states[id] ?: FullTextState()
    var expanded by rememberSaveable(id, preview) { mutableStateOf(initiallyExpanded) }
    var overflowed by remember(id, preview) { mutableStateOf(false) }
    val body = if (expanded && state.text != null) quotePrefix + state.text else preview
    LaunchedEffect(id, initiallyExpanded, requiresFull) {
        if (initiallyExpanded && requiresFull && id.isNotBlank()) ui.request(id)
    }
    val styledBody = copyableSocialText(body, Blue)
    Text(styledBody, fontSize = if (quoted) 14.sp else 15.sp, lineHeight = if (quoted) 20.sp else 22.sp,
        color = if (quoted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        maxLines = if (expanded) Int.MAX_VALUE else 10, overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (!expanded && it.hasVisualOverflow) overflowed = true })
    if (requiresFull || overflowed) {
        if (expanded && state.error != null) Text(state.error, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (expanded && requiresFull && state.text == null) {
                TextButton(onClick = { ui.request(id) }, enabled = !state.loading, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text(if (state.loading) "正在加载全文…" else "重试全文", fontSize = 13.sp)
                }
                Spacer(Modifier.width(16.dp))
            }
            TextButton(onClick = {
                expanded = !expanded
                if (expanded && requiresFull && id.isNotBlank()) ui.request(id)
            }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text(if (expanded) "收起" else "展开全文", fontSize = 13.sp)
            }
        }
    }
}
@Composable private fun PostMetric(icon: Int, label: String, count: String) {
    val description = "$label ${count.ifBlank { "—" }}"
    Row(Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 7.dp).clearAndSetSemantics { contentDescription = description }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(count.ifBlank { "—" }, fontSize = 12.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PostFeed(state: FeedState<Post>, refresh: () -> Unit, more: () -> Unit, post: (Post) -> Unit, user: (User) -> Unit, picture: (List<String>, Int) -> Unit) {
    val list = rememberLazyListState()
    val bottom by remember { derivedStateOf { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 } }
    LaunchedEffect(bottom, state.items.size, state.next) { if (state.items.isNotEmpty() && bottom >= state.items.size - 3 && !state.loading && state.error == null && state.next != null) more() }
    PullToRefreshBox(isRefreshing = state.loading && state.items.isNotEmpty(), onRefresh = refresh) { LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = LocalFeedPadding.current) {
        item { StateMessage(state.loading, state.items.isEmpty(), state.error, "暂无公开微博", refresh) }
        items(state.items, key = { it.id }) { p -> PostRow(p, { post(p) }, { user(p.user) }, picture) }
        if (state.items.isNotEmpty()) item { TextButton(onClick = more, enabled = !state.loading && state.next != null, modifier = Modifier.fillMaxWidth()) { Text(if (state.loading) "加载中…" else if (state.next == null) "已浏览完本页" else "加载更多") } }
    } }
}
@Composable private fun SearchFeed(state: FeedState<Any>, retry: () -> Unit, more: () -> Unit, post: (Post) -> Unit, user: (User) -> Unit, picture: (List<String>, Int) -> Unit, account: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = LocalFeedPadding.current) {
        item { StateMessage(state.loading, state.items.isEmpty(), state.error, "没有找到相关结果", retry) }
        if (state.error != null) item { TextButton(onClick = account, modifier = Modifier.fillMaxWidth()) { Text("配置 Cookie") } }
        items(state.items, key = { if (it is Post) "post-${it.id}" else "user-${(it as User).id}" }) { row ->
            if (row is Post) PostRow(row, { post(row) }, { user(row.user) }, picture)
            else if (row is User) Row(Modifier.fillMaxWidth().clickable { user(row) }.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { Avatar(row); Column { Text(row.name, fontSize = 16.sp, fontWeight = FontWeight.Medium); Text(row.bio, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
        }
        if (state.next != null) item { TextButton(onClick = more, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text(if (state.loading) "加载中…" else "加载更多") } }
    }
}
@Composable private fun ProfileHeader(user: User) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Avatar(user); Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Medium) }
        if (user.bio.isNotBlank()) Text(user.bio, fontSize = 14.sp)
        Text("关注 ${user.following}    粉丝 ${user.followers}    微博 ${user.count}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun CookieDialog(vm: MainViewModel, dismiss: () -> Unit, message: (String) -> Unit) {
    var value by remember { mutableStateOf("") }; var saving by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope(); val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) { activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE); onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) } }
    fun save(cookie: String) { saving = true; scope.launch { try { vm.saveCookie(cookie); dismiss(); message(if (cookie.isBlank()) "Cookie 已删除" else "Cookie 已加密保存，请重新搜索") } catch (e: Exception) { error = e.message ?: "保存失败"; saving = false } } }
    AlertDialog(onDismissRequest = { if (!saving) dismiss() }, title = { Text("Cookie 登录") }, text = { Column {
        Text("仅保存在本机，用于微博请求。请粘贴浏览器 Cookie；不会记录日志或云端备份。", fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value, { value = it }, label = { Text("SUB=…; SUBP=…") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), maxLines = 4)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    } }, confirmButton = { TextButton(onClick = { save(value) }, enabled = !saving && value.contains("SUB=")) { Text(if (saving) "保存中…" else "保存") } }, dismissButton = { Row { TextButton(onClick = { save("") }, enabled = !saving) { Text("删除 Cookie") }; TextButton(onClick = dismiss, enabled = !saving) { Text("取消") } } })
}
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun ImageViewer(images: List<String>, initial: Int, close: () -> Unit) {
    if (images.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    fun save(url: String) {
        if (saving) return
        saving = true
        scope.launch {
            try {
                ImageSaver.save(context.applicationContext, url)
                Toast.makeText(context, "已保存到相册 · Pictures/WeiboLite", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Toast.makeText(context, e.message ?: "保存失败，请重试", Toast.LENGTH_LONG).show()
            } finally { saving = false }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val url = pendingUrl
        pendingUrl = null
        if (granted && url != null) save(url)
        else Toast.makeText(context, "未获得存储权限，图片未保存", Toast.LENGTH_SHORT).show()
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val pager = rememberPagerState(initialPage = initial.coerceIn(images.indices)) { images.size }
        var zoomed by remember { mutableStateOf(false) }
        LaunchedEffect(pager.currentPage) { zoomed = false }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = !zoomed) { at ->
                var scale by remember(pager.currentPage) { mutableFloatStateOf(1f) }
                var x by remember(pager.currentPage) { mutableFloatStateOf(0f) }
                var y by remember(pager.currentPage) { mutableFloatStateOf(0f) }
                val transform = rememberTransformableState { zoom, pan, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) { x += pan.x; y += pan.y } else { x = 0f; y = 0f }
                    if (at == pager.currentPage) zoomed = scale > 1f
                }
                AsyncImage(image(images[at]), "图片 ${at + 1}", Modifier.fillMaxSize()
                    .transformable(state = transform, canPan = { scale > 1f })
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = x, translationY = y), contentScale = ContentScale.Fit)
            }
            Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent))).statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = close) { Icon(Icons.Default.Close, "关闭图片", tint = Color.White) }
                Text("${pager.currentPage + 1} / ${images.size}", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                TextButton(enabled = !saving && pendingUrl == null, onClick = {
                    val url = images[pager.currentPage]
                    if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        pendingUrl = url
                        permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else save(url)
                }) { Text(if (saving) "保存中…" else "保存", color = Color.White.copy(alpha = if (saving) 0.65f else 1f), fontSize = 15.sp) }
            }
        }
    }
}

private fun heat(n: Long): String = if (n >= 10000) String.format(Locale.CHINA, "%.1f万", n / 10000.0) else n.toString()
private fun time(raw: String): String = runCatching { val d = SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH).parse(raw) ?: return raw; SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(d) }.getOrDefault(raw)
