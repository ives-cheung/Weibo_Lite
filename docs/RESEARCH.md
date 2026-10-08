# 第一阶段调研 — 2026-10-06

## 参考源码
- WeiboX：https://github.com/bidabrain/weiboX，源码位于 research/weiboX。
  WeiboApi.kt：m.weibo.cn 用户/微博/评论 API；genvisitor2 + s.weibo.com 用户 HTML 搜索。
  MainActivity.kt：隐藏 WebView 执行 m.weibo.cn 访客初始化，CookieManager 获取会话。
  WeiboRepository.kt：Room 缓存，本地关注用户聚合时间线，按页拉取。该时间线并非账号关注时间线。
  VisitorSession.kt：内存 StateFlow。AppPreferences 明文 DataStore、HTTP header 日志和 WebDAV Cookie 备份不能沿用。
  当前源码没有微博正文关键词搜索；必须新增 SearchApi。
- weibo-cli：https://github.com/tamnd/weibo-cli，api.go/types.go/wire.go/weibo.go。
  GET https://weibo.com/ajax/side/hotSearch；Chrome UA + Referer https://weibo.com/。
  ok=1 → data.realtime；word/word_scheme/num/realpos/rank/label_name/is_ad。
  其展示序号由数组顺序生成，本项目优先 realpos，保留真实排名，过滤广告，不补造标签。
- Telegram：https://github.com/DrKLO/Telegram，选读源码位于 research/telegram。
  ActionBar：紧凑标题及内联搜索。DialogCell：头像、文本、时间层级。
  RecyclerListView：列表复用、事件反馈、滚动处理。SearchAdapter：搜索请求序号避免过期响应覆盖。
  ProfileActivity：个人信息与扁平设置行。ThemeActivity：主题设置。
  本项目重新实现 Compose UI，不复制 Telegram 品牌资产或 GPL UI 源码。

## 实测
本机 HTTPS 请求，无用户登录 Cookie：
- hotSearch：HTTP 200、ok=1、51 条 realtime，字段含 realpos/num/word_scheme。
- weibo.com/ajax/feed/hottimeline?refresh=1&group_id=102803&containerid=102803&extparam=discover|new_feed&max_id=0&count=10：ok=1、真实 statuses。
- m.weibo.cn 搜索 getIndex（type=1）：ok=-100；推荐 102803：HTTP 432。
- genvisitor2：retcode=20000000，但仅将其 SUB/SUBP 附入 m.weibo.cn 仍返回 -100。
- s.weibo.com/user：访客 token 可获取用户结果；s.weibo.com/weibo 则转登录。
因此搜索必须测试完整浏览器访客 incarnate 会话；接口拒绝时明确报告限制，绝不用首页内容冒充搜索结果。

## 架构
Screen → AndroidViewModel(StateFlow) → Repository → API → 共享 OkHttp。
HotSearchRepository：7 分钟本地缓存、先显示缓存、后台刷新、Mutex 请求合并。
TimelineRepository：公开推荐按 max_id 分页，避免并发及重复项。
SearchRepository：type=1/3、完整访客会话、分页、本地最近搜索。
安全存储：用户 Cookie 使用 Android Keystore AES-GCM，禁备份；访客 Cookie 仅内存。
图像：Coil 缩略图缓存。仅 INTERNET 权限。无分析/广告/推送 SDK。

## UI design system
背景 #FFFFFF / #17212B；文字 #17212B / #E6EBEF；次级文字 #7D8994。
Accent #2678F5，使用用户提供图标。App bar 56dp；列表横向 16dp。
正文 15sp、作者 15sp medium、辅助 12sp；热搜行最小 56dp。
头像 40dp；0.5dp 分隔；不使用大圆角 Card；点击目标至少 48dp。
四个 tab：首页/热搜/搜索/我的，22dp 线性图标、11sp 标签。
Light/Dark/System；刷新保留旧数据；加载/空/错误明确区分。

## 新增文件计划
Gradle wrapper、根/app 构建配置、Manifest、launcher 资源。
data/model/Models.kt；data/api/WeiboApi.kt；data/session/VisitorSession.kt；
data/storage/LocalStore.kt；data/repository/Repositories.kt；
ui/MainViewModel.kt；ui/WeiboApp.kt；MainActivity.kt。
README.md、docs/RESEARCH.md、docs/VALIDATION.md；APK 输出到 dist/。

## 编码后的验证结论
完整移动 WebView 访客初始化在 PKX110 实机成功，type=1 搜索返回真实微博；type=3 用户搜索也已在模拟器验证，CCTV 关键词返回真实账号列表。
公开首页接口需要桌面访客 Cookie（genvisitor2 返回的 SUB/SUBP），不能用裸请求；已在共享 API 中新增互斥保护的桌面访客会话。热搜保持无 Cookie。
数据层按 hostname 分别使用桌面/移动会话，禁用 HTTP 自动重定向，避免携带 Cookie 跨站跳转。
