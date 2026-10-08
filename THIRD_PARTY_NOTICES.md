# 第三方组件、版权与许可证

本项目原创部分的非商业许可不改变以下第三方组件的许可。第三方作者保留其版权；使用及再分发时必须遵循相应许可证并保留声明。

核对基于当前 0.2.7 的 Gradle `releaseRuntimeClasspath` 实际解析结果及发布 POM；完整坐标、版本与证据 URL 见 [docs/dependency-licenses.json](docs/dependency-licenses.json)。以下汇总包含传递依赖，而不仅是 build.gradle 的直接声明。

| 组件 / 权利人 | 当前版本 | 许可证与来源 |
| --- | --- | --- |
| AndroidX Activity、Core、Lifecycle、Compose、Material3 等 / The Android Open Source Project 贡献者 | 以清单为准；Compose 1.7.3，Material3 1.3.0 | Apache-2.0；[AndroidX 源码](https://cs.android.com/androidx/platform/frameworks/support) |
| Kotlin 标准库与 JetBrains annotations / JetBrains 与贡献者 | stdlib 2.0.21，annotations 23.0.0 | Apache-2.0；[Kotlin](https://github.com/JetBrains/kotlin) |
| kotlinx.coroutines / JetBrains 与贡献者 | 1.8.1 | Apache-2.0；[许可证](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt) |
| OkHttp / Square 与贡献者 | 4.12.0 | Apache-2.0；[许可证](https://github.com/square/okhttp/blob/parent-4.12.0/LICENSE.txt) |
| Okio / Square 与贡献者 | 3.9.0 | Apache-2.0；[许可证](https://github.com/square/okio/blob/3.9.0/LICENSE.txt) |
| Coil / Coil 贡献者 | 2.7.0 | Apache-2.0；[许可证](https://github.com/coil-kt/coil/blob/2.7.0/LICENSE.txt) |
| Accompanist DrawablePainter / Google 与贡献者 | 0.32.0 | Apache-2.0；[项目](https://github.com/google/accompanist) |
| Guava listenablefuture / Google 与贡献者 | 1.0 | Apache-2.0；[许可证](https://github.com/google/guava/blob/master/LICENSE)；该接口 POM 未列许可证，依据项目 LICENSE 核对 |
| jsoup / Jonathan Hedley 与贡献者 | 1.18.3 | MIT；[原文](licenses/jsoup-MIT.txt)与[官方许可页](https://jsoup.org/license) |
| JUnit / JUnit 作者与贡献者，仅单元测试 | 4.13.2 | EPL-1.0；[许可原文](licenses/JUnit-EPL-1.0.txt) |
| Hamcrest / Hamcrest 作者与贡献者，JUnit 测试传递依赖 | 1.3 | BSD-3-Clause；[原始版权及许可声明](licenses/hamcrest-core-LICENSE.txt) |
| JSON-java / JSON-java 贡献者，仅单元测试 | 20240303 | 该版本 LICENSE 为 Public Domain 声明；[原文](licenses/JSON-java-LICENSE.txt)。Android 运行时 org.json 由系统提供，不打包该测试库 |
| Gradle Wrapper / Gradle 与贡献者 | 8.9 | Apache-2.0；[原文](licenses/Gradle-LICENSE.txt)，wrapper 脚本原始版权头保留 |
| Android Gradle Plugin / Android Open Source Project；Kotlin Gradle Plugin / JetBrains，构建工具 | AGP 8.7.3；Kotlin 2.0.21 | Apache-2.0；由构建环境下载，不作为本项目原创代码重新授权 |

Apache-2.0 全文保留于 [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt)。运行时依赖归档中附带的 LICENSE/NOTICE 元数据按原样保留于 [licenses/runtime-artifact-notices.txt](licenses/runtime-artifact-notices.txt)。这些声明不表示第三方作者为本项目背书。

## 参考项目与素材

WeiboX、weibo-cli 与 Telegram 是调研参考，不是本项目编译依赖。详细版本和许可边界见 [ACKNOWLEDGEMENTS.md](ACKNOWLEDGEMENTS.md)。weibo-cli 的原始 Apache-2.0 声明保留于 licenses/weibo-cli-Apache-2.0.txt。WeiboX 未提供明确许可证，不得据此假定可复制其代码。

当前 WB 图标为 AI 辅助生成，不是新浪微博的官方眼睛标志；不承诺图标独占性或可注册性。历史提供的微博形象图标及含第三方用户内容的截图仅留在本机，不随本仓库发布。微博服务返回内容的版权归原作者或其他权利人。

依赖版本变更后需重新生成清单并核对声明；构建工具全部传递依赖未在运行时清单中穷举。本仓库发布源码，不附带 APK 或依赖二进制；若以后发布 APK，应同时携带适用的第三方许可与声明，并复核分发义务。
