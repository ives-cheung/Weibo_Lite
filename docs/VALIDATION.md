# Milestone 1 验证记录

日期：2026-10-06，Asia/Shanghai。

## 构建

命令：`./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintDebug`。
结果：BUILD SUCCESSFUL。3 个解析单元测试，0 失败；Lint 0 errors、13 warnings。
警告为依赖新版本提示、target SDK 提示、访客 WebView JavaScript 必需功能及图标配置提示。没有通过禁用 Lint 错误来完成构建。

APK：`dist/Weibo_Lite-0.1.0.apk`，1,721,069 字节（1.64 MiB）。
R8/资源压缩已启用。apksigner 验证成功，APK v2 签名，使用本机测试 key。

## 实机：PKX110

- 安装 debug 与最终优化 APK 成功。
- 未配置用户 Cookie，首页返回真实公开微博及缩略图。
- 点击「热搜」返回真实实时榜单；排名/热度/新标签展示正确。
- 点击第一条热搜，原关键词进入原生应用内搜索，显示真实相关微博。
- 点击微博进入详情，读取真实公开评论。
- 点击头像进入用户主页，读取真实简介、统计与微博。
- 冷启动再次进入热搜，仍显示此前 15:55 的缓存更新时间（缓存未过期）。

## 模拟器：LinkNavManual（手机尺寸显示）

安装最终 1.64 MiB APK。
- 冷启动成功；公开首页、图片、实时热搜加载成功。
- 点击热搜后显示原生微博搜索结果，未配置登录 Cookie。
- 搜索 CCTV 并切换「用户」，返回 CCTV电视剧/CCTV4 等真实账号。
- 「我的」显示访客模式；浅色/深色/跟随系统切换正常，系统栏颜色匹配。
- AndroidRuntime 日志未发现本包 FATAL EXCEPTION。
- 界面截图位于 docs/screenshots；只作为验证记录，应用内容不使用截图。

## 单元测试

- 保留接口 realpos，过滤广告；缺少热度/排名/标签时不生成假字段。
- 非成功热搜响应不能被解析成成功数据。
- 信息流使用缩略图，图片查看器使用原图；正文 HTML 与换行解析正确。

真实首页分页接口另外验证两页：每页 10 条，第二页新增 10 个不同 ID，max_id 从 1 更新为 2。

## 验收范围

本阶段完成「打开 App → 热搜 → 未登录真实热搜 → 点击条目 → 应用内真实微博搜索」。
未以用户账号执行评论/点赞/转发写入。未进行正式发布签名或应用商店发布。
Cookie 安全存储、备份排除及无日志通过源码/Manifest 检查；未使用用户真实 Cookie 做登录测试。

## 0.1.1：互动计数图标

新增转发箭头、评论气泡、点赞拇指矢量图标，共用 PostMetric 组件，保留文字及真实计数。
assembleRelease、lintDebug 通过；APK 签名验证通过。新版 APK 1,740,065 字节。
最终 APK 已在模拟器安装运行并检查截图，三项图标正常显示，无文字重叠，辅助功能可读出对应名称和计数。
截图：docs/screenshots/home-with-icons.png。

## 0.1.2：全文展开与社交文本配色

同时处理列表 10 行折叠及服务端长文摘要，保留 isLongText、转发原文 ID 与内联 longText。
新增按微博 ID 获取真实全文的 Repository/ViewModel 状态与缓存，移动端 statuses/extend 与桌面 statuses/longtext 互为备用。
展开、收起、加载提示与重试入口应用于所有微博列表，详情页自动获取长文。
真实微博实测摘要 146 字，展开后 359 字，收起后恢复 146 字；没有以原摘要冒充全文。
#话题# 与 @用户 用蓝色文字区分正文，适用于摘要、全文和转发原文。
最终优化构建与 Lint 通过；7 个单元测试、0 失败。补充用例覆盖长文标记、原文 ID、内联全文、段落与失败响应。
截图：full-text-collapsed.png、full-text-expanded.png、social-text.png。

## 0.1.3：评论分隔线与最终设备安装

相邻评论间新增 0.5dp 细横线，与正文左缘对齐，最后一条后不添加分隔线。
评论内的话题与 @ 提及同样使用蓝色。关闭 HTML 格式化产生的额外空白，保留原有段落。
真实热搜结果的长微博已验证展开到 553 字，全文中的话题继续显示蓝色。
截图：search-full-text.png、comment-dividers.png（最后的段落空白修复前截图）。
最终 assembleRelease、lintDebug 通过（0 errors）；8 项单元测试，0 失败；APK 签名验证通过。
最终 APK 1,756,449 字节，已通过 adb install -r 安装到 PKX110，设备确认 versionCode=4、versionName=0.1.3。

## 0.2.0：玻璃风格界面

导航及工具栏采用渐变半透明色层、高光描边、圆角与柔和阴影；底部悬浮胶囊拥有选中状态颜色动画。
微博、热搜及设置使用圆角容器，保留全文、社交文本和评论分隔线。
在 1080x2340 / 420dpi 与 560dpi 模拟器上检查布局，修正深色文字继承与窄屏互动计数换行。
互动区以图标和计数显示，辅助功能保留「转发/评论/点赞」名称。
实现为轻量 Compose 玻璃视觉效果，不宣称具备 iOS 原生实时折射或背景模糊。

最终发行构建与 Lint 通过（0 errors），8 项数据单元测试通过。APK 签名验证通过，已更新安装 PKX110，设备确认 versionCode=5 / versionName=0.2.0。
截图：glass-home.png、glass-dark-narrow.png；窄屏互动区复查无换行。

## 0.2.1：真实背景玻璃

正文使用独立 GraphicsLayer；工具栏及导航按屏幕坐标重放正文层，RenderEffect GPU 模糊不包含控件自身。
Android 13+ 使用 RuntimeShader 对玻璃边缘采样坐标轻微内弯；Android 12 保留 GPU 模糊，更早版本使用透明色层。
列表使用 contentPadding 而非整体遮挡区裁切，内容可滚动到工具栏及底部导航下方；状态/系统导航区域独立加遮罩。
已在 API 35 模拟器观察滚动前后实际图片颜色和轮廓变化，热搜及原生搜索仍可正常进入。

最终 assembleRelease / lintDebug 通过（0 errors）；AGSL 在 API 35 模拟器正常运行，未出现 AndroidRuntime 崩溃。浅色、深色控件与背景独立显示。
截图：backdrop-light-before.png、backdrop-light-after.png、backdrop-light-image.png、backdrop-dark.png。
最终 APK 签名验证通过，PKX110 已覆盖安装并确认 versionCode=6、versionName=0.2.1。

## 0.2.2：气泡切换、清透玻璃与正文左对齐

选中气泡使用两个弹簧驱动的边缘坐标，领先边缘先移动，形成伸长、回弹、稳定的过渡。已录屏逐帧确认首页到热搜以及返回的中间位置与宽度变化。
玻璃模糊半径从 7dp 降为 3dp，并降低浅色/深色色层不透明度。
微博标题区保留头像与用户名，正文、引用、图片和互动区位于标题区下方，左缘统一到卡片 16dp 内边距；截图 full-width-body.png。动效录屏 bubble-transition.mp4。
发行构建与 lintDebug 通过，签名验证通过，PKX110 已安装并确认 versionCode=7、versionName=0.2.2。

## 0.2.3：评论分组列表

评论采用一个圆角分组容器，34dp 头像、13sp 半粗用户名与右侧时间，回复对象独立提示行，正文 25sp 行高，5 行折叠/展开/收起，保留细分隔线及蓝色提及。
真实热搜中师永刚同一微博的评论已验证回复拆分、长评论展开/收起；浅色及深色均检查。截图 grouped-comments-light.png、grouped-comments-dark.png（深色截图为最终表情解析修复前）。
修复 HTML 表情图片的 alt 文本丢失，纯表情评论不再为空；新增回归用例。9 项测试全通过，发行构建及 Lint 通过（0 errors），签名验证通过。
PKX110 已覆盖安装，设备确认 versionCode=8、versionName=0.2.3。

## 0.2.4：底部磨砂加强

底部导航独立启用 frosted 样式，GPU 模糊从 3dp 调为 9dp，浅色及深色色层不透明度提升到约 65–70%。顶部玻璃及气泡动效保持现有参数。
发行构建与 Lint 通过，签名验证通过。已安装 PKX110，设备确认 versionCode=9 / versionName=0.2.4。本次未额外进行截图验收。

## 0.2.5 · WB 满版图标

替换为蓝色全幅背景与白色 WB 矢量字母，更新各密度 PNG 和自适应图标资源。1024px 预览已检查；assembleRelease、lintDebug 成功（0 错误，14 警告），apksigner 验证通过。ADB 覆盖安装 Success，设备 PKX110 确认 versionCode=10、versionName=0.2.5。

## 0.2.6 · WB 融合标志

使用内置 image_gen 生成并检查 WB 带状字标及安全裁切版本，蓝色满版背景，替换 launcher 各密度 PNG、自适应图标背景。assembleRelease、lintDebug 成功；签名验证通过。PKX110 ADB install -r Success，确认 versionCode=11、versionName=0.2.6。生成提示词摘要在 assets/app-icon-ribbon-prompt.txt。

## 0.2.7 · 点击复制话题

使用 Compose LinkAnnotation.Clickable 为完整 #话题# 添加独立复制动作，系统剪贴板保存含井号的原文并显示 Toast。适用于微博正文、引用全文及评论正文；@ 用户仅保留蓝色样式。新增 3 项测试覆盖相邻话题独立回调、未闭合话题/提及不生成复制链接、多行话题精确范围，全部 12 项单元测试通过。assembleRelease、lintDebug、签名验证通过。PKX110 install -r Success，确认 versionCode=12、versionName=0.2.7。未在实体手机上操作界面。
