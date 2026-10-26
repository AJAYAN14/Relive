# Relive 架构重构与迁移落地设计文档

| 项 | 内容 |
|---|---|
| 文档版本 | v1.1（2026-10-26）。v1.1 变更：**修订 D4**——界面文本一律收编 strings.xml（资源化提前到平移期）；配套执行级规则 `docs/rules-ui-fidelity.md`（颜色/字号/间距/动画保真铁律），与本文冲突时以 rules 为准 |
| 文档状态 | 已评审基准稿（作为重构执行蓝图） |
| 适用工程 | 新工程 `E:\Android\Relive`（Min SDK 35 / Kotlin 2.2 / Compose BOM 2026.02 / AGP 9.2） |
| 逻辑来源 | 旧工程 `E:\GitHub\photoo\android`（业务已全部跑通，全量平移来源） |
| 前置阅读 | 旧工程 `docs/modularization-design.md`（原地重构方案，本方案是其**战略替代**，模块拓扑与其保持同构兼容） |
| 文档范围 | **仅架构设计与迁移路线，不含业务实现代码**。文中代码块均为接口契约 / 构建配置层面的示意 |

---

## 1. 背景与目标

### 1.1 项目背景

photoo（旧）是一款隐私优先、零联网的本地相册整理工具，核心玩法为"滑动卡片整理"（左滑删除、右滑收藏、上滑保留），辅以相册分析、沉浸回顾、相似/低质清理、媒体压缩等能力。业务功能已全部跑通并验证。

因产品正式更名为 **Relive** 并将最低系统抬升至 **Android 15（Min SDK 35）**，决定放弃在旧工程上原地修补，改为**绿地重建模块化新工程 + 旧业务逻辑全量平移**。本文档即该重构的总体设计蓝图。

### 1.2 旧工程现状诊断（实测事实）

对 `E:\GitHub\photoo\android` 的扫描结论（关键行号已实测核对）：

| # | 事实 | 数据 / 证据 | 对重构的含义 |
|---|---|---|---|
| 1 | 单模块工程 | `settings.gradle` 仅 `include ':app'` | 无任何子模块可独立编译、测试 |
| 2 | 38 个 .kt 平铺单包 | 全部位于 `com.voicelike.app` | 同包免 import，隐式任意引用，耦合不可见 |
| 3 | 巨型入口文件 | `NativeVoiceLikeActivity.kt` 264KB，约 20 个顶层声明共存（`MediaItem`@146、`PreferencesManager`@184、`shareMedia`@689、`UndoAction`@769、`loadMedia`@5652 等） | 状态、UI、数据、工具全部交织 |
| 4 | 巨型 Composable | `MainScreen` 约 2850 行（935–3788 行），承载全局状态 + 路由 + 底部导航 | 任何功能改动都要动此文件，回归风险高度集中 |
| 5 | 共享类型扩散 | `MediaItem` 被 19 个文件引用；`MediaAnalysisManager` 被 7 个文件引用；`PreferencesManager` 被 6 个文件引用 | 数据模型与共享服务是拆分的天然"第一刀" |
| 6 | 无导航框架 | 手写状态机（BottomBar + 全屏浮层混合） | 绿地重建可自选现代路由方案，无迁移包袱 |
| 7 | 本地化为 Kotlin 常量表 | `Localization.kt` 100KB + `LocalizationExtra.kt` 92KB | 迁入独立模块可显著加速增量编译 |
| 8 | 版本管理陈旧 | Groovy DSL + `variables.gradle` + `rootProject.ext`，Compose BOM 2024.02、Kotlin 编译器扩展 1.5.8（Kotlin 1.9 时代）、Media3 1.2.0、Coil 2.5 | 全面升级到新工程基线 |
| 9 | 单例时序隐患 | `PreferencesManager` 在 Composable 内 `remember { ... }` 创建（@841、@938）；`VideoPlayerCache.get(context)` 经 `remember` 持有（@5396） | 生命周期不受控，平移时须上升为应用级单例 |
| 10 | 媒体写路径分散 | 除 `FileOperationManager` 走 `createWriteRequest` 规范路径外，存在 4 处散落的直接 `contentResolver.delete()`（`NativeVoiceLikeActivity:2113`、`LiveVideoFeedView:609`、`ImageCompressionView:461`、`VideoCompressionView:444`） | 依赖 `MANAGE_EXTERNAL_STORAGE` 静默授权，跨模块后必须统一收编（详见 6.3） |
| 11 | Manifest 权限问题 | 旧 Manifest 含历史遗留 `INTERNET` / `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`；**缺少** Android 14+ 必需的 `READ_MEDIA_VISUAL_USER_SELECTED` | 新工程权限矩阵需重新设计（详见 5.3） |
| 12 | `FileOperationManager` 内含 API 29 及以下三分支历史代码 | 127–165 行 | Min SDK 35 下全部成为死代码，平移时直接删除 |

**核心结论**（与旧文档一致）：本项目耦合是"平铺性耦合"（同包免 import 导致的隐式引用），而非恶性架构耦合。旧文档的模块拓扑分析依然成立、可完整继承；本方案改变的是**执行路线**——从"原地重构"改为"绿地重建 + 平移"。

### 1.3 新工程基线（实测事实）

`E:\Android\Relive` 为全新 Android Studio 模板工程，已具备：

| 项 | 现状 | 评估 |
|---|---|---|
| 构建体系 | Kotlin DSL + Version Catalog（`gradle/libs.versions.toml`）已就位 | ✅ 现代基线，直接沿用并扩展 |
| 版本 | AGP 9.2.1 / Gradle 9.4.1 / Kotlin 2.2.10 / Compose BOM 2026.02.01 | ✅ AGP 9 内置 Kotlin 支持，无需 kotlin-android 插件 |
| SDK | minSdk 35 / targetSdk 36 / compileSdk 36.1 | ✅ 无历史兼容包袱 |
| 构建性能 | Configuration Cache 已启用；`org.gradle.parallel` 未启用 | P0 补齐 parallel/caching |
| Release | 模板默认 `optimization { enable = false }`（未开启混淆压缩） | P6 收尾时开启并迁移 R8 规则 |
| 模块 | 仅 `:app`，namespace `com.jian.relive` | 11 个运行时模块待建 |
| JVM | 模板默认 Java 11 兼容 | Convention Plugin 统一提升到 Java 17 |

### 1.4 目标与非目标

**目标**：

1. **G1 模块化架构**：将单体结构解耦为"1 壳 + 4 核心层 + 6 业务模块"的标准分层多模块工程，依赖单向、边界清晰、可独立编译测试。
2. **G2 业务全量平移**：旧工程全部已验证业务能力（滑卡整理、撤销/回收站、过滤器、相册分析、沉浸回顾、相似/低质清理、图片/视频压缩、设置/多语言、分享、动图/Motion Photo 等）**行为保真**地迁入新工程。
3. **G3 拥抱新基线**：充分利用 Min SDK 35 与现代技术栈（Kotlin 2.2、Compose BOM 2026、AGP 9、类型安全导航），删除全部历史兼容分支。

**非目标（本阶段明确不做，防范围蔓延）**：

- ❌ 不做产品功能增删与交互改版（UX 保真优先）
- ❌ 不做性能专项优化（大相册分页加载、内存治理等另立迭代）
- ❌ 不替换序列化框架（Gson 保留平移，kotlinx.serialization 列为后续迭代）
- ❌ 不做本地化资源化改造（192KB Kotlin 常量表原样平移，strings.xml + per-app locale 列为后续迭代）
- ❌ 不引入 Hilt / RxJava / EventBus 等新重型依赖

### 1.5 关键架构决策记录（ADR 摘要）

| # | 决策点 | 结论 | 理由与后果 |
|---|---|---|---|
| D1 | applicationId 与数据延续 | 采用新身份 `com.jian.relive`，**不延续**旧应用偏好数据 | 正式更名即品牌切割；新旧包名不同天然支持双 APK 并装对拍（见 4.4）。后果：老用户的偏好设置（批次大小、语言等）不迁移；如需延续，后续以"从 photoo 导出/导入设置"功能补足，不进入本阶段 |
| D2 | 依赖注入 | **手动 DI**：`:app` 内 `AppGraph` 装配全部共享服务，构造注入 + `CompositionLocal` 提供 + `viewModelFactory` 构造 ViewModel | 单进程、约 6 个共享单例，Hilt/KSP 的构建复杂度不划算，与"隐私至上、依赖极简"调性一致。所有共享服务走「接口在 core、实现在 core 或 app」形态，保留未来迁移 Hilt 的缝 |
| D3 | 导航方案 | **Navigation Compose 类型安全路由**（NiA 流派：各 feature 自持 `@Serializable` route + `NavGraphBuilder` 扩展，壳聚合） | 绿地无迁移包袱；类型安全路由是 Min SDK 35 基线下的现代标准答案；跨 feature 跳转经壳接线回调，feature 间保持零依赖（见 3.4） |
| D4 | 本地化策略 | **（v1.1 修订）**：界面文本一律收编各模块 `res/values/strings.xml`（15 语言目录），严禁 Compose 硬编码文案；语言切换改走系统 per-app locale；`Localization.kt`/`LocalizationExtra.kt` 降级为**文案唯一提取源**（转换完成后废弃）。执行细则见 `rules-ui-fidelity.md` §2 | 原"平移期原样保留 Kotlin 常量表"策略废止；转换须分批机械执行 + 抽样对拍（每语言抽 10 条逐字比对 + 字段总数断言），见 rules §2.6 |
| D5 | 序列化框架 | 平移期**保留 Gson**（PreferencesManager / 压缩续传状态等） | 换序列化器必引入默认值/null 语义差异，违反平移铁律；kotlinx.serialization 列为后续迭代 |
| D6 | 构建体系 | Version Catalog（已有）+ 新增 `build-logic` 约定插件集 | 11 个模块不复制粘贴构建脚本；AGP 9 内置 Kotlin 使约定插件极简（仅需 `com.android.library` + Compose 插件） |
| D7 | 媒体权限与写路径 | 保留 `MANAGE_EXTERNAL_STORAGE`（可选授权的"静默模式"）+ `createWriteRequest/createDeleteRequest` 兜底；**全部写/删操作收编至 `:core:media` 的 `FileOperationManager`** | 滑卡批量删除的 UX 保真依赖静默删除；模块化后散落的 4 处直接 delete 必然收编（物理边界强制），行为等价性靠删除终态对拍验证（见 6.3） |
| D8 | 旧工程处置 | **冻结**：自本方案启动起，旧工程仅修致命 bug，所有功能演进停止 | 消除双线开发的漂移风险（见 4.5） |

---

## 2. 整体模块化架构设计

### 2.1 分层架构总览

```
E:\Android\Relive（Gradle 根 = 仓库根）
│
├── app/                       # :app                壳：组装、路由聚合、权限网关、Manifest
├── build-logic/               # included build      约定插件（非运行时模块）
│
├── core/                      # ◆ 核心层（低频稳定，向下依赖）
│   ├── model/                 # :core:model         纯 Kotlin 领域模型（零依赖）
│   ├── common/                # :core:common        本地化 · 偏好 · 工具 · 导航契约
│   ├── ui/                    # :core:ui            主题 · 动效 · 通用展示组件
│   └── media/                 # :core:media         MediaStore · 文件操作 · ML Kit · 播放缓存
│
├── feature/                   # ◆ 业务层（高频变更，向上被壳聚合）
│   ├── organize/              # :feature:organize   滑卡整理（核心业务，最后迁）
│   ├── feed/                  # :feature:feed       沉浸回顾 / 喜欢列表
│   ├── stats/                 # :feature:stats      相册分析 / 摄影画像
│   ├── clean/                 # :feature:clean      相似 / 低质清理
│   ├── compress/              # :feature:compress   图片 / 视频压缩
│   └── settings/              # :feature:settings   设置 / 语言 / 隐私 / 许可 / 欢迎
│
├── gradle/libs.versions.toml  # 唯一版本出口
└── docs/
```

运行时模块共 **11 个**（1 壳 + 4 core + 6 feature），与旧文档 `modularization-design.md` 的拓扑完全同构——两份文档间可直接建立文件级映射，降低认知成本。

### 2.2 模块清单（一句话职责）

| 模块 | 类型 | 职责 |
|---|---|---|
| `:app` | application | 应用组装与入口：MainActivity、权限网关、路由聚合、底部导航、AppGraph 装配、Manifest/签名/发布配置 |
| `:core:model` | library | 纯 Kotlin 数据类与领域类型，零 Android / 零第三方依赖 |
| `:core:common` | library | 本地化常量表、PreferencesManager、日期/格式化工具、`AppNavigator` 契约 |
| `:core:ui` | library | Material 主题、通用动效（FluidTransitions 等）、展示组件、FullScreenImageViewer |
| `:core:media` | library | MediaRepository（MediaStore 读取）、FileOperationManager（写/删/移动统一入口）、ML Kit 端侧分析、动图解析、视频播放缓存、系统分享 |
| `:feature:organize` | library | 滑卡整理主流程：批次加载、卡片交互、过滤器、撤销队列、回收站审阅、文件夹拖放、月份视图、庆祝动画 |
| `:feature:feed` | library | 沉浸式回顾：喜欢列表、竖滑视频/动图流 |
| `:feature:stats` | library | 相册分析：热力图、摄影画像 |
| `:feature:clean` | library | 智能清理：相似照片分组、低质量识别 |
| `:feature:compress` | library | 图片压缩、视频压缩（Media3 Transformer） |
| `:feature:settings` | library | 设置、语言切换、隐私说明、开源许可、首启欢迎页 |

### 2.3 单向依赖规则与依赖矩阵

**铁律**：依赖只能自上而下（`app → feature → core → model`），禁止横向依赖；core 之间唯一的横向例外是各 core 模块 → `:core:model` 与 `:core:common`。

| 模块 | → 依赖 |
|---|---|
| `:app` | 全部 6 feature + 全部 4 core |
| `:feature:organize` | core:media, core:ui, core:common, core:model |
| `:feature:feed` | core:media, core:ui, core:common, core:model |
| `:feature:stats` | core:media, core:ui, core:common, core:model |
| `:feature:clean` | core:media, core:ui, core:common, core:model |
| `:feature:compress` | core:media, core:ui, core:common, core:model |
| `:feature:settings` | core:ui, core:common, core:model（**不依赖 media**，保持最小） |
| `:core:media` | core:common, core:model |
| `:core:ui` | core:common, core:model |
| `:core:common` | core:model |
| `:core:model` | （无） |

依赖图为严格分层 DAG，**无环**。分层逻辑：按"变更频率"分层——feature 高频变更、core 低频稳定、model 零变更；上层随便改，下层不动。

### 2.4 可见性规则

1. **一律 `implementation`**（编译期隔离，加速增量构建）。
2. **唯一例外**：core 向 feature 暴露的类型用 `api`（如 `:core:media` 对 `MediaRepository`、`:core:model` 对 Gson 序列化模型）。
3. **feature 只暴露一个入口**：对外仅 public 的 route 常量 + `NavGraphBuilder` 扩展函数（见 3.2），内部组件全部 internal。
4. **资源前缀**：每个 library 模块强制 `resourcePrefix`（`organize_` / `feed_` / `stats_` / `clean_` / `compress_` / `settings_` / `media_` / `ui_` / `common_`），杜绝资源合并冲突。本项目资源极少（几乎全 Kotlin），负担可忽略。

### 2.5 构建体系设计

**Version Catalog 扩充**（在现有 `libs.versions.toml` 上追加，版本以接入时最新稳定版为准，关键约束如下）：

| 依赖 | 用途 | 关键约束 |
|---|---|---|
| `androidx.navigation:navigation-compose` | 类型安全导航 | ≥ 2.8（支持 Kotlin Serialization 路由） |
| `org.jetbrains.kotlin.plugin.serialization` + `kotlinx-serialization-core` | route 序列化 | 与 Kotlin 2.2 对齐 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | ViewModel + 手动 factory | 与现有 lifecycle 2.11 对齐 |
| `io.coil-kt.coil3:coil-compose`（+ media3 视频帧支持） | 图片/视频帧加载 | Coil 3 主线；本地 content:// 加载不引入网络引擎 |
| `androidx.media3:*`（exoplayer/ui/common/transformer） | 播放与压缩 | 全模块统一同一版本；较旧版 1.2 API 有变化，集中在 core:media 适配 |
| `com.google.code.gson:gson` | 偏好/续传状态序列化 | 平移期保留（D5） |
| `com.google.mlkit:image-labeling` / `face-detection` | 端侧 AI（bundled） | **仅 bundled 版本**（离线红线）；接入后验证无网络权限引入 |
| `androidx.exifinterface:exifinterface` | EXIF 读取 | — |
| `org.jetbrains.kotlinx:kotlinx-coroutines(-play-services)` | ML Kit `await()` | — |
| `androidx.compose.material:material-icons-extended` | 图标 | 随 BOM |

**`build-logic` 约定插件**（included build，3 个插件覆盖全部模块形态）：

| 插件 | 适用的模块 | 固化的约定 |
|---|---|---|
| `relive.android.library` | 全部 core/feature | `com.android.library`、namespace 前缀校验、minSdk 35、compileSdk 36、Java 17、resourcePrefix 由各模块覆写、单元测试装配 |
| `relive.compose.library` | core:ui、core:common（可选）、全部 feature | 应用 `org.jetbrains.kotlin.plugin.compose`、Compose BOM 对齐、`implementation` 标配（material3、ui、icons） |
| `relive.feature` | 全部 feature | 组合上述两个 + 标配依赖（4 个 core 模块、navigation-compose、lifecycle-viewmodel-compose） |

> AGP 9 内置 Kotlin 支持（新工程模板未使用 kotlin-android 插件即证），约定插件无需处理 Kotlin 插件版本对齐，复杂度大幅低于 AGP 8 时代。

**Gradle 性能开关**（P0 落入 `gradle.properties`）：`org.gradle.parallel=true`、`org.gradle.caching=true`（Configuration Cache 已启用）。

### 2.6 工程护栏（防劣化机制）

1. **目录即层级**：`core/` 下禁止 import `feature.*`、feature 之间禁止互相 import——物理结构上不可能成环。
2. **CI 校验**（接入后启用）：`dependency-analysis-gradle-plugin`（com.autonomousapps）做依赖环与冗余检测；简单 grep 规则拦截违规 import（feature 内出现非本模块的 `com.jian.relive.feature.*` import 即失败）。
3. **模块独立编译门**：CI 对每个 feature 执行 `:feature:xxx:compileDebugKotlin`，保证任意模块可独立编译。
4. **merged manifest 审计**：CI 检查合并后 Manifest 不出现 `INTERNET` 等 PRIV红线 权限（见 5.3）。

---

## 3. 模块边界与交互设计

### 3.1 模块职责与文件归属（含旧 → 新映射）

> 38 个旧源文件的完整归属见**附录 A**。本节定义职责边界与关键改造点。标注 ★ 的表示需先从 `NativeVoiceLikeActivity.kt` 巨石中**抽出为独立声明**再迁移。

#### 3.1.1 `:app` — 壳模块

**职责**：应用组装与入口，**不含任何业务逻辑**。

- `MainActivity`（平移自 `NativeVoiceLikeActivity` 的 Activity 壳部分，795–838 行）：`onCreate` + `setContent` + **权限网关**（`READ_MEDIA_IMAGES/VIDEO`、部分照片访问、`MANAGE_EXTERNAL_STORAGE` 的 `registerForActivityResult` 流全部上收到壳，结果经 `AppGraph` 的 StateFlow 广播）
- 导航聚合：`ReliveApp`（平移自 `VoiceLikeApp`）改造为 **NavHost 容器 + 各 feature 路由注册 + 底部导航**
- `AppBottomNavigationBar.kt` → 壳
- AndroidManifest（全部权限、FileProvider）、applicationId、签名、R8 全局规则
- `AppGraph`（手动 DI 装配，见 3.3）
- **瘦身验收**：壳内 Kotlin 代码总量目标 < 400 行（不含资源与 Manifest）

#### 3.1.2 `:core:model` — 领域模型（零依赖）

| 迁入内容 | 旧位置 |
|---|---|
| ★ `MediaItem`（核心媒体模型，19 文件引用） | 主 Activity @146 |
| ★ `AppStats`（统计模型） | @171 |
| ★ `CompressionResumeState`（压缩续传） | @178 |
| ★ `SwipeDirection` / `UndoAnimationTarget` / `UndoAction`（sealed） | @756/@763/@769 |
| ★ `ConfettiParticle` | @5854 |

**约束**：不依赖 Android / Compose / 任何第三方库；自带 `consumerProguardFiles`（Gson keep 规则，`MediaItem` 等序列化模型随模块下发）。

#### 3.1.3 `:core:common` — 通用基础

| 迁入内容 | 旧文件 / 位置 |
|---|---|
| `Localization.kt`（100KB）+ `LocalizationExtra.kt`（92KB） | **（v1.1）不再作为平移归宿**：降级为文案唯一提取源，随各 feature 平移分批转换为 strings.xml（rules §2）后废弃 |
| ★ `PreferencesManager`（@184，约 500 行） | 原样平移；**实例化位置上收**（见 6.1） |
| `FeedNavStrings.kt` / `HolidayUtils.kt` | 原文件平移 |
| ★ `formatShutterSpeed` | @5635 |
| `AppNavigator` 契约接口 | 新增（见 3.4） |

**约束**：不依赖 Compose、不依赖 ML Kit / Media3（保证上层按需引入、增量编译最快）。

#### 3.1.4 `:core:ui` — 通用 UI

| 迁入内容 | 旧文件 / 位置 |
|---|---|
| Material 主题（颜色/字号/形状常量，Material3 升级适配） | 主 Activity 内 ★ 抽出 |
| `AnimationUtils.kt` / `FluidTransitions.kt` / `StaggeredReveal.kt` / `SlideText.kt` / `RollingText.kt` / `SkeletonLoader.kt` | 原文件平移 |
| ★ `PageAnimatedVisibility` | @3788 |
| ★ `Modifier.shadow` | @5625 |
| `PredictiveBackWrapper.kt` | 原文件平移 |
| ★ `FullScreenImageViewer`（@4443） | 归入 core:ui（纯展示组件、被多 feature 复用，见 3.4 浮层归属规则） |

**约束**：依赖 Compose + Coil；不依赖业务模块。

#### 3.1.5 `:core:media` — 媒体域底座（重依赖集中地）

| 迁入内容 | 旧文件 / 位置 | 关键改造 |
|---|---|---|
| ★ `loadMedia` → **`MediaRepository`** | @5652 | 一次性同步查询 → 挂起函数 + `StateFlow<List<MediaItem>>`；查询语义（排序/字段/过滤条件）**逐字段照抄保真**（见 3.3） |
| `FileOperationManager.kt` | 原文件 | **删除 API ≤ 29 全部历史分支**（127–165 行死代码）；新增统一 `deleteMedia()` 收编 4 处散落的直接 delete（见 6.3） |
| `MediaAnalysisManager.kt`（7 文件共享的枢纽） | 原文件 | 单例上收到 AppGraph；分析结果缓存保持原语义 |
| `ImageAnalyzer.kt` / `MotionPhotoHelper.kt` / `VideoPlayerCache.kt` | 原文件 | `VideoPlayerCache` 由 Composable `remember` 持有改为 AppGraph 应用级持有（生命周期受控） |
| ★ `shareMedia` | @689 | FileProvider authority 用 `context.packageName` 动态拼接；provider 声明留壳 |

**约束**：Media3 / ML Kit / ExifInterface 等重依赖只在此模块（feature 内按需 `implementation` 声明使用），编译缓存收益最大。

#### 3.1.6 六个 feature 模块

| 模块 | 迁入文件 | 巨石内需抽出的声明（★） |
|---|---|---|
| `:feature:organize` | `PhotoCard.kt`、`FilterView.kt`、`DeepOrganizeView.kt`、`FolderDropTarget.kt`、`DeleteReviewModal.kt`、`MonthlyView.kt` | `MainScreen` 状态群 → `OrganizeViewModel`；`EmptyState`@3816、`DraggableCard`@4486、`VideoPlayer`@5376、`CelebrationConfetti`@5866、批次加载 `loadNextBatch`、`StatsOverlay`@4053（归 stats） |
| `:feature:feed` | `LikesView.kt`、`LiveVideoFeedView.kt` | —（其中直接 delete 调用点收编至 core:media） |
| `:feature:stats` | `ShootingStatsView.kt` | `StatsOverlay`@4053 |
| `:feature:clean` | `SimilarPhotosView.kt`、`SimilarPhotoManager.kt`、`LowQualityView.kt`、`LowQualityManager.kt` | — |
| `:feature:compress` | `ImageCompressionView.kt`、`VideoCompressionView.kt` | —（压缩后删原图的调用点收编至 core:media） |
| `:feature:settings` | `SettingsView.kt`、`LanguageView.kt`、`PrivacyView.kt`、`OpenSourceLicensesView.kt`、`WelcomeView.kt` | — |

**feature 内部统一包结构**（物理一模块、逻辑三层）：

```
com.jian.relive.feature.organize/
├── OrganizeRoute.kt          # @Serializable 路由对象 + NavGraphBuilder.organizeScreen() 扩展（唯一对外出口）
├── OrganizeScreen.kt         # 屏级 Composable（布局 + collectAsState）
├── OrganizeViewModel.kt      # 状态持有者（StateFlow）
├── domain/                   # 业务逻辑（UndoController、批次/过滤规则等）
└── ui/                       # 业务专属组件（PhotoCard、FilterView...）
```

### 3.2 模块对外契约

**每个 feature 对外只暴露两样东西**（其余全部 `internal`）：

```kotlin
// ① 路由对象（本模块自持，壳不需要知道内部参数结构）
@Serializable
data object OrganizeRoute

// ② 路由注册扩展（壳的 NavHost 中调用一次即完成接线）
fun NavGraphBuilder.organizeScreen(
    appGraph: AppGraph,          // 共享服务（或经 LocalAppGraph 提供）
    onOpenFeed: () -> Unit,      // 跨 feature 跳转 = 壳接线的回调（见 3.4）
    /* 其他跨域回调 */
)
```

**core 模块对外暴露**：`MediaRepository`、`FileOperationManager`、`MediaAnalysisManager`、`PreferencesManager`、主题与通用组件、`MediaItem` 等模型——全部经 `AppGraph` 注入，feature 不得自行实例化共享服务。

### 3.3 跨模块数据流规范：共享仓库 + StateFlow 单向流

**选型结论**：单进程纯本地应用，**不引入 EventBus / RxJava / 消息总线**。跨模块通信的唯一答案是「core 层共享仓库的 `StateFlow` 状态流，feature 订阅」——状态驱动，而非消息驱动。

**单向数据流**：

```
feature（用户意图）→ core:media 挂起函数（FileOperationManager.deleteMedia / moveMedia ...）
                              ↓ 成功
                    MediaRepository.refresh()
                              ↓ StateFlow 发射
        全部订阅方（organize / stats / clean / feed ...）自动收到新数据
```

**关键规则**：

1. **单一事实源**：`MediaRepository.items: StateFlow<List<MediaItem>>` 是媒体数据的唯一权威副本；任何写操作成功后必须 `refresh()`，禁止 feature 各自维护缓存列表后手工同步。
2. **写路径唯一入口**：所有 `insert/update/delete/move` 只能经 `FileOperationManager`（见 D7 / 6.3），feature 禁止直连 `ContentResolver`。
3. **ViewModel 边界**：feature 的 ViewModel 只持有本业务状态（批次队列、过滤器、撤销栈等），跨模块数据一律订阅共享仓库；ViewModel 之间零感知。
4. **共享服务生命周期**：全部由 `AppGraph`（`:app`，Activity `onCreate` 装配一次）持有，经 `LocalAppGraph`（CompositionLocal）或构造参数注入；**严禁**在 Composable 内 `remember { PreferencesManager(...) }`（旧工程 @841/@938 的反模式，见 6.1）。

**手动 DI 装配形态**（D2，示意）：

```kotlin
// :app
class AppGraph(context: Context) {
    val preferences = PreferencesManager(context)          // core:common
    val mediaRepository = MediaRepository(context)          // core:media
    val analysisManager = MediaAnalysisManager(...)         // core:media
    val fileOps = FileOperationManager                      // core:media
    val playerCache = VideoPlayerCache(context)             // core:media
}

// feature 内获取 ViewModel（无 Hilt 的标准姿势）
val vm: OrganizeViewModel = viewModel(factory = viewModelFactory {
    initializer { OrganizeViewModel(appGraph.mediaRepository, appGraph.fileOps, ...) }
})
```

### 3.4 导航解耦设计（D3）

**方案**：Navigation Compose **类型安全路由** + 「feature 自持 route、壳聚合、跨域回调接线」——即 Google 官方参考架构 Now in Android 的模块化导航模式。

**职责划分**：

| 角色 | 职责 |
|---|---|
| 各 feature | 自持 `@Serializable` route 对象；暴露 `NavGraphBuilder.xxxScreen()` 扩展；feature **内部**子页面跳转用本地 route / 内部状态 |
| `:app` 壳 | NavHost 聚合全部 feature 的注册扩展；持有 `NavController`；底部导航与 tab 状态；**跨 feature 跳转在此接线** |
| `:core:common` | `AppNavigator` 契约（`navigate(route)/back()` 封装，供不便持有 NavController 的深层组件使用） |

**跨 feature 跳转的接线模式**（feature 间零 import 的关键）：

```kotlin
// 壳中接线（feature 只声明"我要跳"的意图回调，不认识目标 feature 的任何类型）
organizeScreen(appGraph, onOpenFeed = { navController.navigate(FeedRoute) })
feedScreen(appGraph, onOpenStats = { navController.navigate(StatsRoute) })
```

本项目实际存在的跨域跳转极少（盘点自旧工程）：`organize 空态 → 查看喜欢（feed）`、`organize → 统计（stats）` 等约 2~3 处，回调接线成本可忽略。

**浮层归属规则**（旧工程 BottomBar + 全屏浮层混合的映射决策）：

| 旧浮层形态 | 新归属 | 理由 |
|---|---|---|
| `DeleteReviewModal`（删除审阅）、`FilterView`（过滤器）、`FolderDropTarget` 等 | **feature 内部状态**（ModalBottomSheet / Dialog / 覆盖层） | 业务私有、随 feature 状态生灭，不进全局路由 |
| `FullScreenImageViewer`（全屏查看） | **`:core:ui` 共享组件**，各 feature 以本地覆盖层使用 | 纯展示、被 organize/feed 等多处复用、无独立业务状态 |
| `StatsOverlay`（统计浮层） | `:feature:stats` 内部 | stats 私有 |
| 六大主页面 + `WelcomeView`（首启） | **路由目的地**（类型安全 route） | 全局可达、需 back stack 语义 |

**回退行为**：Min SDK 35 上预测性返回（Predictive Back）为系统默认，`PredictiveBackWrapper` 平移到 core:ui 后需在真机走查返回手势与动画（见 6.4 风险 R8）。

---

## 4. 平移与迁移路线图

### 4.1 总体策略：绿地重建 + 逻辑平移（Strangler 模式）

旧文档 `modularization-design.md` 设计的是**原地重构**（在旧工程内 P0→P6 渐进拆分）。本次改为**绿地策略**：

> 在新工程中先搭好模块化骨架（空壳可编译、可导航），再按「由简到繁」的顺序把旧工程已验证的业务逻辑逐模块**平移**进来；旧工程冻结作为行为参照系与回退兜底。

**选择绿地而非原地重构的理由**（对照）：

| 维度 | 原地重构（旧方案） | 绿地平移（本方案） |
|---|---|---|
| 新基线利用 | 需先在旧工程升 Kotlin/AGP/DSL，升级风险与拆分风险叠加 | 新工程天生 Kotlin 2.2 / AGP 9 / Catalog，零升级债 |
| 兼容包袱 | 每步都要维持 minSdk 24 行为 | minSdk 35 直接删光死分支（如 FileOperationManager 的 API 29- 分支） |
| 回滚粒度 | 拆坏了回滚即丢重构成果 | 旧工程完整保留，随时对照、对拍、回退 |
| 验证手段 | 只能自测 | **新旧双 APK 并装对拍**（包名不同天然支持，见 4.4） |
| 认知负担 | 在巨石里边拆边改 | 平移目标结构先行确立，"搬家"方向单一 |

**代价**（如实记录）：无法 git 历史跟随（新仓库历史从零开始）；文件移动靠 IDE 复制 + import 修复而非 `Refactor → Move` 跨工程联动。缓解：附录 A 的映射表作为权威对照清单，每平移一个文件在新仓库提交信息中登记旧文件路径。

### 4.2 阶段计划（P0 → P6，每步可编译、每步一提交）

> **验证门分级**：🟢 编译门 = `./gradlew assembleDebug` 全绿；🟡 冒烟门 = 相关功能真机手工走查；🔴 对拍门 = 金相册回归（P4 起启用，见 4.4）。

#### P0 工程地基（复杂度：低）

| 步骤 | 内容 | 验证门 |
|---|---|---|
| 0.1 | `libs.versions.toml` 补全全部依赖（2.5 清单）；`gradle.properties` 开启 parallel/caching | 🟢 sync 通过 |
| 0.2 | 建立 `build-logic` included build，实现 3 个约定插件 | 🟢 `:app` 套用插件后编译通过 |
| 0.3 | 创建全部 11 个模块骨架（空 `build.gradle.kts` + 约定插件 + namespace + resourcePrefix），`settings.gradle.kts` 注册 | 🟢 空壳全量编译；模块独立编译抽查 |

#### P1 壳与导航骨架（复杂度：低）

| 步骤 | 内容 | 验证门 |
|---|---|---|
| 1.1 | 平移主题常量至 `:core:ui` `theme/Color.kt + Type.kt + Theme.kt`（rules §1：色值集中定义于 Color.kt，**删除 app 模块的模板紫色主题**）；`MainActivity` + `AppGraph` 骨架装配 | 🟢 启动白壳 |
| 1.2 | 类型安全路由 + NavHost + `AppBottomNavigationBar` 平移 + 6 个 feature 空占位页 | 🟡 真机可启动、tab 可切换、返回手势正常 |

#### P2 core 四层平移（复杂度：中）

| 步骤 | 内容 | 验证门 |
|---|---|---|
| 2.1 | `:core:model`：从巨石抽出 6 组模型声明（附录 A★清单）迁入 | 🟢 + 模型单测（Gson 序列化快照测试） |
| 2.2 | `:core:common`：`PreferencesManager`（@184 段落整体迁出）、`FeedNavStrings`/`HolidayUtils`/`formatShutterSpeed`；本地化双表**不再平移**——降级为各 feature 文案提取源（随各 feature 平移分批转换，rules §2） | 🟢 + 偏好读写单测（schema 与旧版逐字段对照） |
| 2.3 | `:core:ui`：动效与展示组件平移（`AnimationUtils`/`FluidTransitions`/`StaggeredReveal`/`SlideText`/`RollingText`/`SkeletonLoader`/`PredictiveBackWrapper`/`PageAnimatedVisibility`/`Modifier.shadow`/`FullScreenImageViewer`） | 🟢 |
| 2.4 | `:core:media`：`MediaRepository`（`loadMedia`@5652 照抄查询语义 + StateFlow 化）、`FileOperationManager`（删死分支 + `deleteMedia` 统一入口）、`MediaAnalysisManager`/`ImageAnalyzer`/`MotionPhotoHelper`/`VideoPlayerCache`/`shareMedia` | 🟢 + 仓库冒烟（真机授权后加载相册列表条数与旧版一致） |

#### P3 试点 feature：settings（复杂度：低）

> 选 settings 做首个 feature：文件独立、不依赖 media、逻辑最简，用它验证「feature 模块全链路」（约定插件 → 路由注册 → AppGraph 注入 → 本地化 → 偏好读写 → 冒烟）。

| 步骤 | 内容 | 验证门 |
|---|---|---|
| 3.1 | 平移 `SettingsView`/`LanguageView`（语言切换改走 per-app locale，rules §2.4）`/PrivacyView`/`OpenSourceLicensesView`/`WelcomeView` 五文件 + 首启欢迎流程接路由 + 文案收编 settings 模块 strings.xml（**首批转换，验证 15 语言全链路**） | 🟡 设置页全功能真机走查：15 种语言全量切换各验一遍、偏好写入后重启保持 |

#### P4 独立 feature 平移：compress → clean → stats → feed（复杂度：中）

> 由简到繁逐个平移；每个 feature 完成即触发一次 🔴 对拍（该 feature 的金相册断言）。

| 步骤 | 内容 | 验证门 |
|---|---|---|
| 4.1 | `:feature:compress`（`ImageCompressionView`/`VideoCompressionView`；压缩后删原图改走 `FileOperationManager.deleteMedia`） | 🔴 压缩输出尺寸/时长与旧版同区间；续传状态重启恢复 |
| 4.2 | `:feature:clean`（相似/低质四文件；`MediaAnalysisManager` 订阅接线） | 🔴 同一批种子的相似分组数量、低质判定集合与旧版一致 |
| 4.3 | `:feature:stats`（`ShootingStatsView` + `StatsOverlay` 抽出） | 🔴 热力图/统计数值与旧版完全一致 |
| 4.4 | `:feature:feed`（`LikesView`/`LiveVideoFeedView`；Live Photo 删除路径收编） | 🔴 喜欢列表一致；视频流播放/滑动行为走查 |

#### P5 核心战役：organize（复杂度：高）

> 旧工程 80% 的复杂度集中于此（`MainScreen` 2850 行状态群 + 巨石内十余个子声明）。平移顺序=「先数据与状态、后视图」，每小步保持可编译。

| 步骤 | 内容 | 验证门 |
|---|---|---|
| 5.1 | **状态层**：`MainScreen` 状态群 → `OrganizeViewModel` + `UndoController`（状态清单见附录 B；批次队列/过滤器三元组/撤销栈/回收站/已处理集合逐项迁移，`remember` → `StateFlow`） | 🟢 + **纯逻辑单测**（批次切换、过滤器组合、撤销/重做序列——旧工程首次具备可测性） |
| 5.2 | **卡片与交互层**：`DraggableCard`@4486、`PhotoCard.kt`、`EmptyState`@3816、`CelebrationConfetti`@5866、`VideoPlayer`@5376、滑动手势与音效（SoundPool 上收 AppGraph） | 🟡 滑卡三向手势、动效、音效真机走查 |
| 5.3 | **业务浮层**：`FilterView`、`DeleteReviewModal`（回收站审阅）、`FolderDropTarget`（文件夹拖放 → `FileOperationManager.moveMedia`）、`MonthlyView`、`DeepOrganizeView` | 🟡 过滤组合后批次内容、回收站终态、移动落点对拍 |
| 5.4 | **集成**：`onOpenFeed` 等跨域回调接线；批次庆祝→空态→重启批次全流程 | 🔴 organize 全路径金相册对拍 |

#### P6 系统收尾与总验收（复杂度：中）

| 步骤 | 内容 | 验证门 |
|---|---|---|
| 6.1 | 权限矩阵终态落地（5.3）；FileProvider + 分享链路终验；`MANAGE_EXTERNAL_STORAGE` 授权/拒绝双路径走查 | 🟡 双路径下删除/移动行为符合设计 |
| 6.2 | 开启 release `optimization`（minify + shrinkResources）；迁移 R8 规则（Gson keep、ML Kit、kotlinx-serialization）；release 包真机全量走查 | 🟢 release 构建全绿 + 冒烟 |
| 6.3 | 死代码清理、resourcePrefix 终检、护栏脚本接入 CI、APK 体积对比 | 🔴 **总验收 DoD**（见第 7 章） |

### 4.3 平移铁律（每一步的纪律）

1. **只搬家，不改逻辑**：平移阶段禁止"顺手重构/优化/改名"。发现旧代码问题 → 记入 `docs/migration-notes.md` 待办清单，不在平移提交中处理。唯一例外是本方案明确列出的「受控重构点」（MediaRepository 化、deleteMedia 收编、单例上收——三者均有对拍兜底）。
2. **每步可编译**：任何提交点上 `assembleDebug` 必须全绿；拆到一半的"坏状态"不允许进主干。
3. **对照登记**：每次平移提交的 message 登记旧文件路径（`source: photoo android/app/.../PhotoCard.kt`），保持映射可追溯。
4. **三处受控重构点之外的行为差异 = 缺陷**：发现即修，不带病推进。

### 4.4 验证体系：金相册对拍（双轨回归）

新旧 applicationId 不同（`com.jian.relive` vs `com.photoo.app`），**可并装对照**——这是绿地策略最重要的质量红利。

**金相册测试集**（P4 前构建，纳入 `docs/` 或仓库 `testdata/` 管理）：

- 种子媒体 30–50 项，覆盖：有/无 EXIF、有/无地理位置、Live Photo、Motion Photo、相似组（3 组以上）、低质样本（模糊/暗光/过曝）、大视频、跨月份、跨文件夹
- 建议用 adb 脚本播种，保证两台设备（或重置后同一台）媒体集合一致

**断言清单**（新旧并跑、结果必须一致的项目）：

| 域 | 断言 |
|---|---|
| 加载 | 相册总数、各文件夹/月份/类型过滤器下的计数 |
| 整理 | 同过滤器下待处理总数（`remainingCount`）、处理一批后 `processedIds` 终态 |
| 删除 | 回收站审阅确认后，文件系统/MediaStore 终态（项目级 uri 集合）完全一致 |
| 收藏/保留 | 喜欢列表集合一致；"释放已保留"后队列恢复一致 |
| 统计 | `AppStats` 全字段逐项相等 |
| 清理 | 相似分组数量与组员、低质集合 |
| 压缩 | 输出文件尺寸区间、续传状态恢复 |
| 撤销 | 撤销/重做 N 步后的状态快照 |

**人工走查清单**（无法程序断言的 UX 项）：滑卡动效手感、音效、庆祝动画、语言切换、返回手势、底部导航过渡。

### 4.5 版本管理与回滚

- 新工程单主干推进：**每个步骤一次提交**（对应 4.2 表格行），Phase 完成打 tag（`p0-foundation`、`p3-settings-e2e`...）。
- 旧工程冻结（D8）：只接受致命 bug 修复；修复需评估是否同步平移到新工程（登记到 `migration-notes.md`）。
- 回滚单位 = 步骤提交（git revert 单步可逆）；Phase 级回退 = 回到上一个 tag 重新进入该 Phase。

---

## 5. Android 15 基线的现代化落地

### 5.1 立即删除的兼容包袱（平移时直接执行）

| 旧代码位置 | 删除内容 | 理由 |
|---|---|---|
| `FileOperationManager.kt` 100–126 行 | API 29（Android 10）`RecoverableSecurityException` 单条处理分支 | minSdk 35 全设备 ≥ R(30) |
| `FileOperationManager.kt` 127–165 行 | API ≤ 28 Legacy File API 整段 | 同上 |
| 全工程散布的 `Build.VERSION.SDK_INT < R/Q` 分支 | 平移时逐个甄别删除 | 同上（甄别原则：分支只为旧版本服务且不影响 R+ 行为） |
| 旧 Manifest | `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | API 33+ 上无效；minSdk 35 下纯噪音 |
| 旧 Manifest | `INTERNET` | **零联网红线**：ML Kit bundled / Coil 本地加载 / Media3 本地播放均不需要网络；merged manifest 审计固化（见 5.3） |
| `google-services.json` try-catch 逻辑（旧 build.gradle 94–101 行） | 推送相关装配 | Relive 无推送；零联网红线下禁止 reintroduce |

### 5.2 技术栈升级对照

| 维度 | 旧（photoo） | 新（Relive） | 平移注意事项 |
|---|---|---|---|
| 语言/编译 | Kotlin 1.9 + compose-compiler-ext 1.5.8 | Kotlin 2.2 + Compose Compiler Gradle 插件 | K2 编译器更严格（exhaustive when 等），编译警告即修 |
| Compose | BOM 2024.02 / M3 早期 | BOM 2026.02 / M3 演进两年 | **视觉与组件默认行为可能漂移**（见风险 R8）；平移时以旧版视觉为准逐页对照 |
| 权限申请 | `Build.VERSION` 多代兜底 | 统一 `READ_MEDIA_*` + `READ_MEDIA_VISUAL_USER_SELECTED` | 部分照片访问（用户勾选"仅部分授权"）必须显式支持（见 5.3） |
| 媒体播放 | Media3 1.2.0 | Media3 最新稳定 | ExoPlayer→Player API、Transformer API 有演进，**集中适配于 core:media 与两个 feature**，改动面可控 |
| 图片加载 | Coil 2.5（compose + video） | Coil 3 | Coil 3 包名/API 调整、视频帧走 media3；集中在 core:ui/core:media 适配 |
| 序列化 | Gson 2.10.1 | Gson（保留，D5） | 后续迭代再评估 kotlinx.serialization |
| 构建 | Groovy + ext 变量 | Kotlin DSL + Catalog + 约定插件 | 已就绪 |
| 边缘到边缘 | targetSdk 36 已适配 | Android 15 强制 edge-to-edge | 走查状态栏/insets 处理 |
| 预测性返回 | 已有 `PredictiveBackWrapper` | 系统默认开启 | 真机走查返回动画与手势冲突 |

### 5.3 权限矩阵终态

| 权限 | 旧 | 新 | 说明 |
|---|---|---|---|
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` | ✅ | ✅ 保留 | 核心能力 |
| `READ_MEDIA_VISUAL_USER_SELECTED` | ❌ **缺失** | ✅ **新增** | Android 14+ 用户选择"部分照片访问"时，未声明此权限的应用会**误判为完全拒绝**（拿不到用户选中的子集）。旧工程存在此缺陷，新工程必须修复 |
| `ACCESS_MEDIA_LOCATION` | ✅ | ✅ 保留 | EXIF 位置显示（卡片信息）必需 |
| `MANAGE_EXTERNAL_STORAGE` | ✅ | ✅ 保留（可选授权） | "静默批量整理"体验依赖（D7）；拒绝授权时兜底走 `createDeleteRequest/createWriteRequest` 系统同意流 |
| `INTERNET` | ✅ 遗留 | ❌ **移除** | 零联网红线的最终落地；CI merged manifest 审计防第三方 SDK 偷偷带回 |
| `READ/WRITE_EXTERNAL_STORAGE` | ✅ 遗留 | ❌ 移除 | minSdk 35 下无效 |

> **CI 审计规则**：release 合并 Manifest 中出现 `android.permission.INTERNET` 或任何 `ACCESS_NETWORK_STATE` 类权限 → 构建失败。这是本项目隐私承诺的工程化落地。

### 5.4 明确暂缓的现代化项（P6 之后独立立项）

| 项 | 目标态 | 暂缓理由 |
|---|---|---|
| ~~本地化资源化~~ | **（v1.1 修订）已提前到平移期执行**，见 `rules-ui-fidelity.md` §2 | — |
| 序列化现代化 | Gson → kotlinx.serialization | 序列化语义差异风险；独立迭代 + 数据迁移期 |
| 动态取色 | Material You Dynamic Color | UX 变化，需产品决策 |
| Baseline Profile | 启动性能 | 性能范畴，非平移目标 |
| 大相册分页加载 | MediaStore 分页查询 | 性能范畴（旧版一次性全量查询行为保持） |

---

## 6. 架构风险与避坑指南

### 6.1 风险总表

| # | 风险 | 等级 | 域 | 规避动作 |
|---|---|---|---|---|
| R1 | 巨石状态群平移失真（`MainScreen` 2850 行、数十个状态交织） | 🔴 高 | 状态管理 | 附录 B 状态对照表逐项迁移 + P5.1 纯逻辑单测 + 🔴对拍兜底；状态迁移分四步走（5.1→5.4） |
| R2 | 共享服务生命周期错位（`PreferencesManager` 在 Composable `remember` 创建、`VideoPlayerCache` 无受控释放） | 🟠 中高 | 状态管理 | 全部上收 `AppGraph`（3.3 规则 4）；平移后 grep 校验无 `remember { PreferencesManager` 类模式 |
| R3 | 媒体写路径行为差异（4 处直接 `contentResolver.delete` 收编后的等价性） | 🔴 高 | 行为兼容 | D7 收编至 `deleteMedia()`；删除终态对拍（uri 集合级比对）；`MANAGE` 授权/拒绝双路径各测一轮 |
| R4 | 部分照片访问未适配（旧工程已存在的缺陷） | 🟠 中 | 行为兼容 | 新增 `READ_MEDIA_VISUAL_USER_SELECTED` + 权限网关覆盖"部分授权"态；真机三种授权态（全量/部分/拒绝）各走查一遍 |
| R5 | 隐式依赖爆炸（同包免 import → 平移后大量引用缺失） | 🟠 中 | 工程纪律 | 平移用 IDE 复制 + 让编译器暴露全部引用；**逐个补 import 而非回退包结构**；附录 A 作权威映射 |
| R6 | Compose BOM 跨两年的组件行为漂移（M3 默认动画/尺寸/配色变化） | 🟠 中 | 构建/UX | P1.1 主题逐项对照；每 feature 平移后视觉走查；差异登记 `migration-notes.md`，仅修"违背旧版视觉"的项 |
| R7 | Media3 1.2→最新 / Coil 2→3 的 API 断裂 | 🟠 中 | 构建 | 适配面物理隔离在 core:media / core:ui / compress / feed；P2.4 / P4.1 / P4.4 各自真机验证 |
| R8 | edge-to-edge / 预测性返回的系统级行为变化 | 🟡 低中 | UX | P1.2 起每次真机走查包含 insets 与返回手势专项 |
| R9 | R8/混淆破坏（Gson 反射、ML Kit、序列化 route） | 🟠 中 | 构建 | `:core:model` consumer rules 随模块分发；P6.2 开混淆后 release 全量走查；混淆崩溃即刻补规则 |
| R10 | 资源合并冲突（多模块 res） | 🟡 低 | 资源隔离 | resourcePrefix 全覆盖（2.4）；项目资源极少，负担可忽略 |
| R11 | FileProvider authority 跨模块失效 | 🟡 低 | 行为兼容 | authority 动态拼接 `context.packageName + ".fileprovider"`；provider 声明留壳；分享链路 P6.1 终验 |
| R12 | 双工程并行漂移（旧工程改动未同步新工程） | 🟠 中 | 工程纪律 | D8 冻结令 + `migration-notes.md` 登记制；唯一例外=致命 bug，须评估双写 |
| R13 | ML Kit 单例初始化时序（首帧分析延迟、并发调用） | 🟡 低中 | 状态管理 | `MediaAnalysisManager` 由 AppGraph 在启动时构造；分析结果缓存语义照抄；首启冷启动走查 |
| R14 | 范围蔓延（平移中"顺手现代化"） | 🟠 中 | 工程纪律 | 4.3 铁律 + `migration-notes.md` 待办池；受控重构点白名单仅 3 项 |
| R15 | 大相册内存/性能（一次性全量加载 + StateFlow 全量发射） | 🟡 低 | 性能 | 平移期保持旧版行为（非目标）；风险登记，1.x 立项分页 |

### 6.2 状态管理专项（R1/R2 详解）

**问题本质**：旧 `MainScreen` 将「全局状态 + 派生计算 + 事件处理 + 布局」四层压在一个 2850 行函数里；60% 的代码是 `remember` 状态群的读写。平移不是复制代码，而是**状态语义的重新归位**：

| 状态类别 | 旧形态 | 新归位 | 迁移判别法 |
|---|---|---|---|
| 业务状态（批次队列、过滤器、撤销栈、回收站、已处理集合、剩余计数） | `remember { mutableStateOf(...) }` 群 | `OrganizeViewModel` 的 `StateFlow<OrganizeUiState>` + 意图方法 | "存了什么"逐个摘为 state 字段；"怎么变"逐个摘为 ViewModel 方法 |
| 跨模块数据（全量媒体、分析结果） | `allMedia` 局部变量 + 手工重查 | 订阅 `MediaRepository.items` / `MediaAnalysisManager` | 删除本地缓存，一律订阅 |
| 动画状态（`Animatable` 群、入场偏移、粒子） | `remember { Animatable(...) }` | **留在 Composable**（动画天然属于 UI 层，禁止塞进 ViewModel） | 只关心当前帧的即留 UI |
| 音效（SoundPool + 4 个 soundId） | Composable 内 `remember` 创建 | AppGraph 应用级持有（资源句柄），播放动作由 ViewModel 事件触发 | 系统资源句柄一律上收 |
| 持久化偏好（processedIds、folderLastUsed、批次大小、语言） | Composable 直接读写 `PreferencesManager` | ViewModel 经 `PreferencesManager`（AppGraph 注入）读写，读写时机照抄旧版 | 读写点不变，只是收口调用方 |

**撤销队列时序**（R1 的最深水区）：`UndoAction` sealed 层级、动画目标（`UndoAnimationTarget`）、回收站（pendingTrash）与文件系统终态之间存在严格时序（先动画→再入栈→审阅→批量执行→refresh）。平移时把时序本身当作领域规则写进 `UndoController` 单测：构造序列（滑删 A → 撤销 → 滑删 B → 审阅确认 → 终态断言），新旧版本跑同一序列断言结果一致。

### 6.3 行为兼容专项（R3/R4/R11 详解）

**媒体写路径审计表**（平移前必须完成的一次性梳理，产出物进 `docs/migration-notes.md`）：

| 旧调用点 | 旧行为 | 新路径 |
|---|---|---|
| `NativeVoiceLikeActivity:2113`（回收站批量删除） | `MANAGE` 授权时静默直删；未授权时行为依赖系统（可能抛异常/静默失败） | `FileOperationManager.deleteMedia()`：授权→静默删；未授权→聚合 `createDeleteRequest` 返回 IntentSender 交壳发起系统同意流 |
| `LiveVideoFeedView:609`（Live Photo 双 uri 删除） | 直接 `contentResolver.delete` ×2 | 同上收编（一次请求覆盖双 uri） |
| `ImageCompressionView:461` / `VideoCompressionView:444`（压缩后删原图） | 直接 `contentResolver.delete` | 同上收编 |
| `FileOperationManager.moveMedia`（文件夹拖放） | 三代分支（API 29/30+/≤28） | 只保留 R+ 两分支（`MANAGE` 静默 / `createWriteRequest`） |

**审计原则**：删除/移动/重命名的**终态**（MediaStore 中 uri 集合与文件落点）必须与旧版一致；中间路径（直删 vs 系统同意流）允许按 D7 设计收敛。授权态 × 操作类型构成 2×N 的走查矩阵，P6.1 全部跑完。

**部分照片访问**（R4）：权限网关需处理三态——全量授权 / **部分授权（`READ_MEDIA_VISUAL_USER_SELECTED`，此时 MediaStore 只返回用户选中子集）** / 拒绝。UI 上应把"部分授权"呈现为"当前可见相册范围"，与全量态共享同一数据流（MediaStore 行为天然如此），但要防止将其误判为"用户只有这几张照片"进而污染统计页的呈现文案。走查清单覆盖三态。

### 6.4 资源与构建专项（R6/R7/R9/R10）

1. **版本唯一出口**：一切第三方版本只进 `libs.versions.toml`；模块脚本禁止裸写版本号；Media3 各 artifact 必须同版本。
2. **Compose 对齐**：Compose Compiler 已随 Kotlin 插件（2.2）天然对齐；BOM 只声明一次（convention plugin 内 platform 引入）。
3. **混淆**：release 开启前先跑一轮 debug 全量走查对照；开启后 crash 优先怀疑规则缺失（Gson `@SerializedName` 字段 keep、ML Kit consumer rules、route 序列化器）。
4. **资源隔离**：resourcePrefix + 壳独占 Manifest 组件声明（FileProvider、launcher Activity）。
5. **构建速度**：parallel + caching + configuration cache 三开关齐备后，11 模块全量构建预期仍快于旧单模块巨石（192KB 本地化表与重依赖隔离的增量收益）。

### 6.5 数据与工程纪律专项（R5/R12/R14）

1. **平移冲动管控**：任何"这里明显可以更好"的念头 → `migration-notes.md`，不进平移提交。PR 自查问题："这个提交里有没有不是搬家的改动？"
2. **旧工程冻结执行**：旧仓库 main 分支设保护（或团队约定），仅 hotfix 通道；hotfix 提交必须同步评估新工程对应点。
3. **映射表权威性**：附录 A 与实际平移不一致时，以实际提交为准回写文档（文档随代码走）。

---

## 7. 验收标准（DoD）

**构建与结构**：

- [ ] `./gradlew assembleDebug` / `assembleRelease` 全绿（release 开启 minify + shrinkResources）
- [ ] 11 个运行时模块全部存在且独立可编译（`:feature:settings:compileDebugKotlin` 等抽查通过）
- [ ] `:core:model` 零第三方依赖（依赖树断言）
- [ ] feature 之间零 import（grep 护栏通过）
- [ ] Version Catalog 为唯一版本出口（模块脚本无裸版本号）

**壳与巨石消解**：

- [ ] `:app` 壳 Kotlin 代码 < 400 行；旧 `NativeVoiceLikeActivity`（264KB）的全部声明在附录 A 中有唯一归属
- [ ] 无 `remember { PreferencesManager(...) }` 类反模式（grep 校验通过）

**行为保真（对拍）**：

- [ ] 金相册断言清单（4.4 表）全项通过：加载/整理/删除/收藏/统计/清理/压缩/撤销
- [ ] 权限三态（全量/部分/拒绝）走查通过
- [ ] `MANAGE_EXTERNAL_STORAGE` 授权与拒绝双路径下，删除/移动终态符合设计
- [ ] 六大核心路径人工冒烟通过（整理一批→撤销→审阅删除→统计→清理→压缩→回顾）
- [ ] 预测性返回、edge-to-edge、语言切换、首启欢迎流程真机走查通过

**隐私红线**：

- [ ] release 合并 Manifest 无 `INTERNET` 及任何网络类权限（CI 审计规则生效）
- [ ] 依赖树无任何非 bundled 的 ML Kit 变体、无广告/统计/崩溃上报 SDK

**质量基线**：

- [ ] `UndoController`、批次/过滤逻辑、`PreferencesManager`、模型序列化具备单元测试且全绿
- [ ] release APK 体积与旧版差异在 ±10% 以内（超出需书面归因）

---

## 8. 附录

### 附录 A：旧源文件 → 新模块完整映射表

> ★ = 需先从 `NativeVoiceLikeActivity.kt` 抽出为独立声明再迁移。每完成一项，在平移提交 message 中登记 `source:` 路径。

**→ `:app`**

| 旧文件 / 声明 | 内容 |
|---|---|
| `NativeVoiceLikeActivity.kt`（Activity 壳 795–838 行） | `MainActivity`（onCreate + setContent + 权限网关） |
| ★ `VoiceLikeApp`（@839） | `ReliveApp`（NavHost 容器 + 路由聚合） |
| ★ `MainScreen`（@935–3788，拆解产物见附录 B） | 路由表 + 底部导航骨架；其余产物分入各 feature |
| `AppBottomNavigationBar.kt` | 底部导航 |
| Manifest / `file_paths.xml` | 权限矩阵 + FileProvider 声明 |
| 新增 `AppGraph` | 手动 DI 装配（D2） |

**→ `:core:model`**

| 旧声明（行号） | 内容 |
|---|---|
| ★ `MediaItem`（@146） | 核心媒体模型 |
| ★ `AppStats`（@171） | 统计模型 |
| ★ `CompressionResumeState`（@178） | 压缩续传 |
| ★ `SwipeDirection`（@756） | 滑动方向 |
| ★ `UndoAnimationTarget`（@763） | 撤销动画目标 |
| ★ `UndoAction`（@769，sealed） | 撤销动作 |
| ★ `ConfettiParticle`（@5854） | 庆祝粒子 |

**→ `:core:common`**

| 旧文件 / 声明 | 内容 |
|---|---|
| `Localization.kt` | （v1.1）多语言主表——文案提取源，转 strings.xml 后废弃 |
| `LocalizationExtra.kt` | （v1.1）多语言补充表——文案提取源，转 strings.xml 后废弃 |
| `FeedNavStrings.kt` / `HolidayUtils.kt` | 文案与日期工具 |
| ★ `PreferencesManager`（@184） | 偏好持久化 |
| ★ `formatShutterSpeed`（@5635） | 快门格式化 |
| 新增 `AppNavigator` | 导航契约接口 |

**→ `:core:ui`**

| 旧文件 / 声明 | 内容 |
|---|---|
| ★ 主题常量（巨石内） | Material3 主题 |
| `AnimationUtils.kt` / `FluidTransitions.kt` / `StaggeredReveal.kt` / `SlideText.kt` / `RollingText.kt` / `SkeletonLoader.kt` | 动效与展示组件 |
| ★ `PageAnimatedVisibility`（@3788） | 页面出入场 |
| ★ `Modifier.shadow`（@5625） | 阴影修饰符 |
| `PredictiveBackWrapper.kt` | 预测性返回封装 |
| ★ `FullScreenImageViewer`（@4443） | 全屏查看（共享组件） |

**→ `:core:media`**

| 旧文件 / 声明 | 内容 |
|---|---|
| ★ `loadMedia`（@5652） | `MediaRepository`（StateFlow 化，受控重构点①） |
| `FileOperationManager.kt` | 写路径统一入口（删死分支 + `deleteMedia` 收编，受控重构点②） |
| `MediaAnalysisManager.kt` / `ImageAnalyzer.kt` | ML Kit 分析与缓存 |
| `MotionPhotoHelper.kt` / `VideoPlayerCache.kt` | 动图解析 / 播放缓存（单例上收，受控重构点③） |
| ★ `shareMedia`（@689） | 系统分享 |

**→ `:feature:organize`**

| 旧文件 / 声明 | 内容 |
|---|---|
| ★ `MainScreen` 状态群（@935–3788） | `OrganizeViewModel` + `UndoController` + `domain/` |
| `PhotoCard.kt` / `FilterView.kt` / `DeepOrganizeView.kt` / `FolderDropTarget.kt` / `DeleteReviewModal.kt` / `MonthlyView.kt` | 业务组件 |
| ★ `EmptyState`（@3816） / ★ `DraggableCard`（@4486） / ★ `VideoPlayer`（@5376） / ★ `CelebrationConfetti`（@5866）/ ★ `loadNextBatch` | 整理流组件与批次逻辑 |

**→ `:feature:feed`**：`LikesView.kt`、`LiveVideoFeedView.kt`

**→ `:feature:stats`**：`ShootingStatsView.kt`、★ `StatsOverlay`（@4053）

**→ `:feature:clean`**：`SimilarPhotosView.kt`、`SimilarPhotoManager.kt`、`LowQualityView.kt`、`LowQualityManager.kt`

**→ `:feature:compress`**：`ImageCompressionView.kt`、`VideoCompressionView.kt`

**→ `:feature:settings`**：`SettingsView.kt`、`LanguageView.kt`、`PrivacyView.kt`、`OpenSourceLicensesView.kt`、`WelcomeView.kt`

### 附录 B：`MainScreen` 状态 → `OrganizeViewModel` 迁移对照（P5.1 执行清单）

> 依据：实测 grep + `function.md`（EmptyState 参数即状态暴露面）。P5.1 开始前需以此为骨架做一次全量补录。

| 旧状态（证据行号） | 语义 | 新归属 |
|---|---|---|
| `allMedia` / `sortedAllMedia`（@1561） | 全量媒体与排序副本 | 订阅 `MediaRepository.items`；排序规则入 ViewModel 派生 |
| `displayMedia` + 批次游标 | 当前批次（默认 16 张） | `OrganizeUiState.displayMedia` + `loadNextBatch()` |
| `processedIds`（prefs 持久化） | 已处理集合 | ViewModel 状态 + `PreferencesManager` 读写（时机照抄） |
| `lastCheckedIds`（@1564） | 相似检测游标 | ViewModel 状态 |
| `filterFolder / filterType / filterMonth`（默认 "All"） | 过滤器三元组 | `OrganizeUiState.filters` + `setFilter()` |
| 撤销栈（`UndoAction` 序列 + `UndoAnimationTarget`） | 后悔药 | `UndoController`（独立可单测） |
| `pendingTrash` | 回收站待删队列 | `OrganizeUiState.pendingTrash` + 审阅确认流 |
| `remainingCount` / `isFullAlbumDone` | 空态派生 | UiState 派生属性 |
| `topCardSimilarGroup`（@1610）/ `similarGroups` | 相似组联动 | 订阅 `MediaAnalysisManager` + 派生 |
| `folderLastUsedState`（@2264）/ `folderList`（@3296） | 拖放目标 | UiState + `FileOperationManager.moveMedia` |
| 音效（`soundPool` @984 + 4 个 soundId @992-995） | 手感反馈 | SoundPool 句柄入 AppGraph；播放由 ViewModel 事件触发 |
| `cardEntryOffsets/Rotations`（@977-978）、卡片 `Animatable` 群（@4541-4544 等） | 入场/滑动/退出动画 | **留在 Composable**（UI 层动画，不入 ViewModel） |
| ExoPlayer（`VideoPlayerCache.get` @5396） | 视频卡播放 | AppGraph 应用级持有 + Composable 订用 |

### 附录 C：参考

- 旧工程架构分析：`E:\GitHub\photoo\docs\modularization-design.md`（原地重构方案；模块拓扑与本方案同构，本方案取代其执行路线）
- Google 官方架构指南：developer.android.com → Architecture（Modern Android Development / Now in Android 参考工程：模块化导航 + 类型安全路由模式）
- Android 15 行为变更：edge-to-edge 强制、预测性返回默认、部分照片访问（`READ_MEDIA_VISUAL_USER_SELECTED`）
- 本文档姊妹文件（随迁移推进维护）：`docs/migration-notes.md`（平移发现登记 · 受控重构点记录 · 旧工程 hotfix 双写台账）
