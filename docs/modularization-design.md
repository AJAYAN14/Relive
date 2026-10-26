# PHOTOO 模块化架构设计方案

> 版本：v1.0（2026-10）
> 范围：仅架构设计，不涉及代码改动。作为后续重构的执行蓝图。

---

## 一、现状诊断（调研结论）

对 `android/` 工程的实际扫描结果：

| 事实 | 数据 | 影响 |
|---|---|---|
| 单模块 | 仅 `:app` 一个模块 | 无法独立编译/测试任何子功能 |
| 文件平铺 | 38 个 .kt 全部在 `com.voicelike.app` 单包下 | 同包免 import，文件间引用零约束，耦合不可见 |
| 巨型入口 | `NativeVoiceLikeActivity.kt` 264KB，内含约 20 个顶层声明（`MediaItem`、`PreferencesManager`、`UndoAction`、`shareMedia`、`loadMedia` 等全在文件内） | 状态、UI、数据、工具全部交织 |
| 巨型 Composable | `MainScreen` 约 2850 行（935-3788 行），承担全局状态 + 页面路由 + 底部导航 | 改任何功能都要动这个文件，冲突与回归风险集中 |
| 共享类型扩散 | `MediaItem` 被 19 个文件引用；`MediaAnalysisManager` 被 7 个文件引用；`PreferencesManager` 被 6 个文件引用 | 数据模型与共享服务是拆分的天然"第一刀" |
| 无导航框架 | 未引入 Navigation Compose，纯手写状态机切换 | 迁移成本反而低，可自选路由方案 |
| 本地化非资源文件 | `Localization.kt` 100KB + `LocalizationExtra.kt` 92KB 为 Kotlin 常量表 | 拆入独立模块可显著加速增量编译 |
| 依赖管理 | `variables.gradle` + `rootProject.ext`，无 Version Catalog | 多模块前需统一版本出口 |
| Manifest | 单 Activity + FileProvider；权限集中在壳 | 权限与组件声明天然归壳，无迁移障碍 |
| 品牌残留 | namespace=`com.voicelike.app`，applicationId=`com.photoo.app` | 模块化正是统一包名的最佳时机 |

**核心结论**：本项目的耦合不是"架构性耦合"（无循环引用的恶性结构），而是"平铺性耦合"（同包免 import 导致的隐式任意引用）。拆分策略应当：**先按包分层让依赖显式化，再按模块物理隔离**。

---

## 二、总体架构

### 2.1 目标结构：1 壳 + 4 核心 + 6 业务 = 11 个模块

```
android/
├── app/                        # :app                壳模块
├── core/
│   ├── model/                  # :core:model         纯 Kotlin 领域模型
│   ├── common/                 # :core:common        本地化 · 偏好 · 工具
│   ├── ui/                     # :core:ui            主题 · 通用 Compose 组件
│   └── media/                  # :core:media         媒体读取 · 文件操作 · ML Kit
├── feature/
│   ├── organize/               # :feature:organize   滑卡整理（核心业务）
│   ├── feed/                   # :feature:feed       沉浸式回顾
│   ├── stats/                  # :feature:stats      相册分析 / 摄影画像
│   ├── clean/                  # :feature:clean      相似 / 低质清理
│   ├── compress/               # :feature:compress   图片 / 视频压缩
│   └── settings/               # :feature:settings   设置 / 语言 / 关于
├── build.gradle
├── settings.gradle
└── gradle/libs.versions.toml   # Phase 0 新增：Version Catalog
```

### 2.2 设计原则

1. **依赖单向向下**：`app → feature → core → model`，feature 之间、core 之间禁止横向依赖（唯一的 core 内横向：`common → model`，见依赖矩阵）。
2. **按"变更频率"分层**：业务 feature 高频变更，core 低频稳定，model 零依赖零变更——上层随便改，下层不动。
3. **不过度拆分**：11 个模块对单人维护的开源项目已是上限，不再细分 `data/domain/presentation` 三层模块（在模块内部用包分层代替，见 3.2）。
4. **纯本地约束传递**：所有模块继承"无网络权限"红线，ML Kit 只用 bundled 模型，依赖声明集中管理，防止第三方 SDK 混入。

---

## 三、模块职责与文件归类

> 38 个源文件的完整归属。标注 ★ 的表示需要先从 `NativeVoiceLikeActivity.kt` 中"抽出为独立文件"再迁移（目前内嵌在主文件里）。

### 3.1 `:app` — 壳模块

**职责**：应用组装与入口，不含任何业务逻辑。

- `NativeVoiceLikeActivity`（瘦身后只剩 `onCreate` + `setContent` + 权限请求）
- `VoiceLikeApp` / `MainScreen` 改造为 **导航容器 + 路由表 + 底部导航**
- `AppBottomNavigationBar.kt`
- AndroidManifest（全部权限、FileProvider 声明）、applicationId、ABI splits、签名、google-services.json 可选逻辑、ProGuard 全局规则
- 依赖装配（AppGraph，见 5.2）

### 3.2 `:core:model` — 领域模型（零依赖）

**职责**：纯 Kotlin 数据类与领域类型，**不依赖任何 Android/第三方库**。可被全部模块引用，编译最快、最稳定。

| 文件 | 说明 |
|---|---|
| ★ `MediaItem` | 核心媒体数据模型（原内嵌主 Activity 146 行处） |
| ★ `AppStats` | 相册统计模型 |
| ★ `CompressionResumeState` | 压缩任务续传状态 |
| ★ `SwipeDirection` | 滑动方向枚举 |
| ★ `UndoAnimationTarget` | 撤销动画目标 |
| ★ `UndoAction`（sealed） | 撤销队列动作 |

### 3.3 `:core:common` — 通用基础（Android + Gson，无 Compose 重依赖）

**职责**：本地化、偏好存储、纯工具。是"变更最少"的底座。

| 文件 | 说明 |
|---|---|
| `Localization.kt`（100KB） | 多语言主表 |
| `LocalizationExtra.kt`（92KB） | 多语言补充表 |
| `FeedNavStrings.kt` | 导航文案 |
| `HolidayUtils.kt` | 节日/日期工具 |
| ★ `PreferencesManager` | 偏好持久化（原内嵌主 Activity） |
| ★ `formatShutterSpeed` | 快门速度格式化 |

依赖：`core:model` + Gson + Android Context。**不依赖 Compose**（保证上层按需引入）。

### 3.4 `:core:ui` — 通用 UI 组件

**职责**：主题、通用动效与展示组件，依赖 Compose + Coil。

| 文件 | 说明 |
|---|---|
| `AnimationUtils.kt` | 动画工具 |
| `FluidTransitions.kt` | 流体转场 |
| `StaggeredReveal.kt` / `SlideText.kt` / `RollingText.kt` / `SkeletonLoader.kt` | 展示组件 |
| `PredictiveBackWrapper.kt` | 预测性返回封装 |
| ★ `Modifier.shadow` | 阴影修饰符 |
| ★ `PageAnimatedVisibility` | 页面级出入场动画 |

依赖：`core:model` + `core:common` + Compose + Coil。

### 3.5 `:core:media` — 媒体域底座（重依赖集中地）

**职责**：MediaStore 读取、文件写入/删除/撤销执行、ML Kit 端侧分析、动图与播放缓存。**全项目重依赖（Media3、ML Kit）只出现在此模块与 feature 内按需声明**，编译缓存收益最大。

| 文件 | 说明 |
|---|---|
| ★ `loadMedia` → 重构为 `MediaRepository` | MediaStore 查询（原内嵌主 Activity 5652 行处） |
| `FileOperationManager.kt` | 文件操作执行器 |
| `MediaAnalysisManager.kt` | ML Kit 分析调度 + 结果缓存（被 7 个文件共享，是数据通信枢纽） |
| `ImageAnalyzer.kt` | ML Kit 图像标注/人脸检测 |
| `MotionPhotoHelper.kt` | 动图解析 |
| `VideoPlayerCache.kt` | 播放缓存 |
| ★ `shareMedia` | 系统分享（需 FileProvider authority，见风险清单） |

依赖：`core:model` + `core:common` + Media3 + ML Kit + ExifInterface + Coil。

### 3.6 六个业务 Feature 模块

| 模块 | 职责 | 归属文件 |
|---|---|---|
| `:feature:organize` | 滑卡整理主流程：批次加载、卡片交互、过滤器、撤销队列、删除确认、文件夹拖放、月份视图、庆祝动画 | `PhotoCard.kt`、`FilterView.kt`、`DeepOrganizeView.kt`、`FolderDropTarget.kt`、`DeleteReviewModal.kt`、`MonthlyView.kt`；★ 抽出：`EmptyState`、`DraggableCard`、`FullScreenImageViewer`、`VideoPlayer`、`CelebrationConfetti`（+`ConfettiParticle`）、`OrganizeScreen`、`OrganizeViewModel`、`UndoController` |
| `:feature:feed` | 沉浸式回顾：喜欢列表、竖滑视频/动图流 | `LikesView.kt`、`LiveVideoFeedView.kt` |
| `:feature:stats` | 相册分析：热力图、摄影画像 | `ShootingStatsView.kt`；★ 抽出：`StatsOverlay` |
| `:feature:clean` | 智能清理：相似照片分组、低质量识别 | `SimilarPhotosView.kt`、`SimilarPhotoManager.kt`、`LowQualityView.kt`、`LowQualityManager.kt` |
| `:feature:compress` | 图片/视频压缩（Media3 Transformer） | `ImageCompressionView.kt`、`VideoCompressionView.kt` |
| `:feature:settings` | 设置、语言切换、隐私说明、开源许可、首启欢迎页 | `SettingsView.kt`、`LanguageView.kt`、`PrivacyView.kt`、`OpenSourceLicensesView.kt`、`WelcomeView.kt` |

**feature 模块内部统一包结构**（逻辑三层，物理一模块）：

```
com.photoo.feature.organize/
├── OrganizeScreen.kt          # 对壳暴露的唯一 Composable 入口
├── OrganizeViewModel.kt       # 状态持有者
├── ui/                        # 该业务专属 Compose 组件
└── domain/                    # 该业务专属逻辑（UndoController 等）
```

---

## 四、模块依赖关系

### 4.1 依赖矩阵（→ 表示依赖）

| 模块 | → 依赖 |
|---|---|
| `:app` | 全部 6 feature + 全部 4 core |
| `:feature:organize` | core:media, core:ui, core:common, core:model |
| `:feature:feed` | core:media, core:ui, core:common, core:model |
| `:feature:stats` | core:media, core:ui, core:common, core:model |
| `:feature:clean` | core:media, core:ui, core:common, core:model |
| `:feature:compress` | core:media, core:ui, core:common, core:model |
| `:feature:settings` | core:ui, core:common, core:model（**不依赖 media**） |
| `:core:media` | core:common, core:model |
| `:core:ui` | core:common, core:model |
| `:core:common` | core:model |
| `:core:model` | （无） |

依赖关系图为严格分层的 DAG（见对话中的架构图），**无任何环**。

### 4.2 `api` vs `implementation` 规则

- 一律使用 `implementation`（编译期隔离，加速构建）。
- **唯一例外**：core 层向 feature 暴露的类型必须用 `api`（如 `:core:media` 对 `MediaRepository`、`:core:model` 对 `MediaItem` 用 `api` 暴露 Gson 等传递依赖），否则 feature 拿不到类型。
- 禁止模块间 `compileOnly`/循环 `testFixtures` 等花活。

### 4.3 防循环机制（工程护栏）

1. **结构约束**：目录即层级——`core/` 下模块禁止引用 `feature/`，feature 之间禁止互相引用，物理上就不可能出现环。
2. **Gradle 校验**：CI 中执行 `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` 人工抽查 + 引入依赖分析插件（如 `com.autonomousapps.dependency-analysis`）做自动闭环检测。
3. **代码规范**：feature 内部 import 出现 `com.photoo.feature.*`（非本模块前缀）即违规，可在 CI 用简单的 grep 脚本拦截。

---

## 五、解耦设计

### 5.1 跨模块页面跳转：轻量路由表（不引入 Navigation Compose）

**选型理由**：项目现有导航是手写状态机（BottomBar + 全屏浮层混合），强行引入 Navigation Compose 需要重造全部页面为栈式路由，违背"小步快跑"。采用**接口 + 路由常量 + 壳内注册表**，与现状最贴近，且保留未来切换 Navigation Compose 的可能（路由常量可直接映射为 route 字符串）。

**设计**（定义在 `:core:common`）：

```kotlin
sealed interface AppRoute {
    data object Organize : AppRoute      // 主整理流
    data object Feed : AppRoute           // 沉浸回顾
    data object Stats : AppRoute
    data object Clean : AppRoute
    data object Compress : AppRoute
    data object Settings : AppRoute
    data object Welcome : AppRoute        // 首启
    data class FullScreenViewer(val item: MediaItem) : AppRoute  // 带参数
}

interface AppNavigator {
    fun navigate(route: AppRoute)
    fun back()
}
```

**每个 feature 只暴露一个入口 Composable**（不暴露内部组件）：

```kotlin
// :feature:organize 对外唯一入口
@Composable
fun OrganizeScreen(
    navigator: AppNavigator,
    dependencies: OrganizeDependencies   // 接口注入仓库，见 5.2
)
```

**壳内路由表**（`:app` 的 MainScreen 改造后）：

```kotlin
@Composable
fun MainScreen(navigator: AppNavigator) {
    when (currentRoute) {
        AppRoute.Organize  -> OrganizeScreen(navigator, deps)
        AppRoute.Feed      -> FeedScreen(navigator, deps)
        AppRoute.Stats     -> StatsScreen(navigator, deps)
        // ...
    }
}
```

**效果**：feature 只认识 `AppRoute` 常量与 `AppNavigator` 接口（来自 core:common），互相之间零感知；跨 feature 跳转（如"整理完成 → 查看统计"）经壳转发，依赖方向始终向上收敛。

### 5.2 跨模块数据通信：共享仓库 + StateFlow 单向数据流

**选型理由**：纯本地应用、单进程，**不需要 EventBus / RxJava / 消息总线**。用「core 层提供共享仓库 StateFlow，feature 订阅」即可覆盖全部通信场景，且数据流向可追溯。

**关键改造**：

1. `loadMedia(context)`（一次性查询）→ **`MediaRepository`**：

```kotlin
class MediaRepository(private val context: Context) {
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    suspend fun refresh() { _items.value = queryMediaStore() }
}
```

2. 删除/收藏等写操作：feature 调用 `FileOperationManager` 挂起函数 → 成功后 `repository.refresh()` → **所有订阅该仓库的 feature（stats、clean、feed…）自动收到新数据**，无需任何显式通知机制。这是本项目"跨模块事件"的唯一答案：**状态驱动，而非消息驱动**。
3. `MediaAnalysisManager`（分析结果缓存，7 个文件共享）→ 保持单例形态挂入 AppGraph。

**依赖装配（手动 DI，不引入 Hilt）**：

```kotlin
// :app 内
class AppGraph(context: Context) {
    val preferences = PreferencesManager(context)
    val mediaRepository = MediaRepository(context)
    val analysisManager = MediaAnalysisManager(...)
    val fileOps = FileOperationManager(...)
}

// Activity onCreate 中初始化一次，通过构造参数逐层传入 feature
```

**选型理由**：Hilt/KSP 会给这个无后端小项目引入可观的构建复杂度；手动装配 5 个单例完全可控，且与"隐私至上、依赖极简"的项目调性一致。将来若膨胀再迁移 Hilt，接口形态（`OrganizeDependencies`）无需变化。

### 5.3 `NativeVoiceLikeActivity.kt`（264KB）六步拆解

拆解原则：**每一步完成后项目必须可编译运行**；顺序为"先抽无依赖的，后抽有依赖的"。

| 步骤 | 抽取内容 | 去向 | 验证点 |
|---|---|---|---|
| S1 | `MediaItem`、`AppStats`、`UndoAction` 等 6 个纯数据声明 | 独立文件 → `:core:model` | 数据类无逻辑，编译即过 |
| S2 | `PreferencesManager`（184-688 行，约 500 行） | 独立文件 → `:core:common` | 设置读写回归 |
| S3 | `loadMedia`（5652 行起）→ `MediaRepository`；`shareMedia`（689 行起） | `:core:media` | 相册列表加载正常 |
| S4 | `MainScreen` 内的**状态群**（批次队列、过滤器、撤销栈、已处理集合）→ `OrganizeViewModel` + `UndoController` | `:feature:organize` | ViewModel 可单测（纯逻辑） |
| S5 | 全屏查看器、`DraggableCard`、`VideoPlayer`、`EmptyState`、`CelebrationConfetti`、`StatsOverlay` 等子 Composable | 各自归属 feature / core:ui | 逐个页面冒烟 |
| S6 | `MainScreen` 剩余部分 → 路由表 + 底部导航，Activity 瘦身为 ~50 行 | `:app` | 全功能回归 |

**S4 是关键战役**：`MainScreen`（935-3788 行）里 60% 是 `remember` 状态群的读写与派生计算。判别方法——把"存了什么"（state）与"怎么变"（event handler）逐个摘到 ViewModel 的 `StateFlow` + 方法，Composable 只保留布局与 `collectAsState`。摘完后 `MainScreen` 预计缩至 500 行以内，且撤销/批次逻辑首次具备可测试性。

---

## 六、迁移路线图（每步可编译）

> 铁律：**每个 Phase 结束必须通过 `./gradlew assembleDebug` + 核心路径手工冒烟（整理一批照片 → 撤销 → 查看统计 → 清理/压缩各跑一次）才能进入下一步**。每个 Phase 建议单独提交（commit），出问题可按 Phase 回滚。

| Phase | 内容 | 产出 | 风险 |
|---|---|---|---|
| **P0 基建** | ① 引入 `gradle/libs.versions.toml` Version Catalog，替代 variables.gradle 分散版本；② 在 `:app` 内部**先按目标包结构分包**（`model/ common/ ui/ media/ feature/organize/...`），38 个文件用 Android Studio `Refactor → Move` 迁移，自动补 import | 单模块但**逻辑分层完成**，所有隐式依赖显式化 | 低；Move 重构自动处理引用 |
| **P1 模型层** | 新建 `:core:model`（com.android.library + 纯 Kotlin），移入 model 包下 6 个文件 | 首个物理模块 | 极低；零依赖代码 |
| **P2 通用层** | 新建 `:core:common`（localization、preferences、utils）+ `:core:ui`（Compose 组件） | 本地化 192KB 大文件进独立模块，增量编译提速立竿见影 | 低 |
| **P3 媒体层** | 新建 `:core:media`，迁入 MediaStore/文件操作/ML Kit/播放缓存；`loadMedia` → `MediaRepository` | 重依赖（Media3/ML Kit）隔离完成 | 中；分享的 FileProvider authority 需检查（见风险清单） |
| **P4 状态拆解** | 在 `:app` 内完成 5.3 的 S4：MainScreen 状态群 → `OrganizeViewModel` 等 ViewModel | 270KB 巨石内部的"状态/视图"分离，为 feature 迁移铺平道路 | 中高；纯重构无功能变更，靠冒烟覆盖 |
| **P5 业务外迁** | 按 **settings → compress → clean → stats → feed → organize** 顺序（由简到繁）逐个建 feature 模块并迁移。每个 feature 一次提交 | 6 个业务模块 | 中；每迁一个跑一轮冒烟 |
| **P6 收尾** | `:app` 瘦身为壳：路由表 + AppGraph + Manifest；删除残留死代码；开启各模块 `resourcePrefix` | 模块化完成 | 低 |

**为什么 organize 最后迁**：它依赖全部 4 个 core 模块、承载 MainScreen 拆解产物、是状态最复杂的业务——等其他 feature 都就位、AppGraph/Navigator 路径验证成熟后再动，返工最少。

**为什么 settings 最先迁**：文件独立、依赖最少（不依赖 media）、逻辑简单，是验证"feature 模块脚手架 → 迁移 → 冒烟"全流程的最佳小白鼠。

---

## 七、风险清单与注意事项

| # | 风险 | 应对 |
|---|---|---|
| 1 | **同包零 import → 拆包需补大量 import** | 全程使用 Android Studio `Refactor → Move`（非手工剪切粘贴），IDE 自动修复引用；禁止手动改包名 |
| 2 | **FileProvider authority 跨模块**：`shareMedia` 使用 `${applicationId}.fileprovider`，provider 声明留在壳 | 迁移时确认 authority 用 `context.packageName + ".fileprovider"` 动态拼接而非硬编码字符串；provider 声明保留在 `:app` 的 Manifest |
| 3 | **包名品牌统一**：namespace `com.voicelike.app` 与 applicationId `com.photoo.app` 不一致 | 借模块化一步到位：各模块 namespace 用 `com.photoo.core.*` / `com.photoo.feature.*`；壳的 launcher Activity 类名同步更新 Manifest |
| 4 | **资源冲突**：多模块 res 合并 | 每个库模块配置 `resourcePrefix "organize_"` 等；项目资源极少（几乎全 Kotlin），负担小 |
| 5 | **Compose 编译器版本**：kotlinCompilerExtensionVersion 1.5.8 必须各模块一致 | 由 Version Catalog 统一管理，P0 完成 |
| 6 | **ProGuard 规则**：Gson 反射模型、ML Kit 混淆规则 | `:core:model` 自带 `consumerProguardFiles`（keep MediaItem 等序列化模型）；其余规则留在壳 |
| 7 | **ABI splits / 签名 / google-services try-catch** | 全部留在 `:app`，不随模块迁移 |
| 8 | **ML Kit 隐私红线**：确保新依赖不引入联网 SDK | Catalog 中 ML Kit 仅 bundled 版本；可在 CI 检查 merged manifest 无新增 INTERNET 相关权限（INTERNET 权限本身在现有 Manifest 中存在，属历史遗留，模块化时建议评估是否移除——移除前需验证 ML Kit bundled 初始化不触发联网） |
| 9 | **单人项目节奏**：P4（状态拆解）最耗时 | 允许拆成多个子提交分几天完成，但每个子提交必须可编译 |

---

## 八、验收标准

- [ ] `./gradlew assembleDebug` / `assembleRelease` 全绿
- [ ] 11 个模块编译，`:core:model` 零第三方依赖
- [ ] 任意 feature 模块可独立 `:feature:settings:compileDebugKotlin` 通过
- [ ] feature 之间零 import（grep 校验）
- [ ] `NativeVoiceLikeActivity.kt` < 200 行；`MainScreen` < 500 行
- [ ] 核心路径冒烟全通过（整理/撤销/统计/清理/压缩/回顾）
- [ ] APK 体积与功能与拆分前一致（release 对比）
