# Relive 现代 Android 工程防翻车与架构底线纪律（Rules）

> **适用对象**：本仓库的所有 AI 与人工开发工作（底层数据流、并发调度、存储适配、UI 性能与架构评审）  
> **生效范围**：项目全局架构与技术实现（与 `rules-ui-fidelity.md` UI 纪律、`rules-code-style.md` 代码风格并列为三大底线铁律）  
> **基线环境**：**最低支持版本为 Android 15（API 35，minSdk = 35）**，专为现代 Android 原生架构打造，彻底剔除历史兼容包袱。  
> **配套文档**：`.agent/rules/nemorules.md`（行为硬约束）· `docs/rules-ui-fidelity.md` · `docs/rules-code-style.md`

---

## 0. 核心总纲与 Android 15+ 专属铁律

> **“跑得通”不等于“合格”。**  
> 必须确保每一行代码都满足：**真机 120 帧不掉帧、切到后台不发热偷跑电、现代系统不闪退、模块之间不缠绕、异常发生不崩溃。**

### 0.1 零版本判断铁律（严禁 `Build.VERSION.SDK_INT` 污染）
- **铁律**：本项目**仅运行在 Android 15 及以上设备（minSdk = 35）**。
- **严禁**在代码中编写任何 `if (Build.VERSION.SDK_INT >= ...)` 历史版本分支判断！
- 所有老旧系统的兼容分支、向下兼容库、过渡性 API 全部视为**垃圾代码与代码污染**，直接且唯一地调用 Android 15 现代原生 API。

---

## 1. 流畅度与性能铁律（Jetpack Compose 防掉帧）

相册类应用具有海量图片、快速滑动的特点，对渲染性能要求极高。凡违反以下规则的代码一律拒绝入库：

### 1.1 列表 `items()` 强制显式 Key 绑定
- `LazyColumn`、`LazyRow`、`LazyVerticalGrid`、`LazyHorizontalGrid` 的 `items()` **必须提供显式唯一且稳定的 Key**。
- **严禁**省略 `key` 参数，**严禁**使用列表索引 `index` 作为 key。

```kotlin
// ❌ 违规：缺失 key 或使用 index，滑动、增删照片时会导致整个网格重组闪烁卡顿
items(photos) { photo -> MediaCard(photo) }
itemsIndexed(photos) { index, photo -> MediaCard(photo) }

// ✅ 合规：绑定数据库唯一稳定 ID，Compose 准确复用节点缓存
items(items = photos, key = { it.id }) { photo ->
    MediaCard(photo)
}
```

### 1.2 状态容器与集合稳定性（Stability）
- 在 `UiState` 中直接使用标准 `List<T>` / `Set<T>` / `Map<T>` 会被 Compose 编译器判定为 **Unstable（不稳定参数）**，导致即使数据未变，依赖它的组件每次都会强制全量重组。
- **强制规范**：
  1. 优先使用 Kotlinx 不可变集合：`ImmutableList<T>`、`PersistentList<T>`；
  2. 或在 UI 状态类上显式标注 `@Immutable` / `@Stable` 注解；
  3. 数据类中的字段必须全部为 `val`，严禁在 `UiState` 中声明 `var`。

```kotlin
// ❌ 违规：使用可变或未声明稳定性的标准 List，导致父级重组时子组件无条件跟跑
data class OrganizeUiState(
    val photos: List<MediaItem> = emptyList()
)

// ✅ 合规：使用不可变集合，开启极致 Skippable（智能跳过重组）
@Immutable
data class OrganizeUiState(
    val photos: ImmutableList<MediaItem> = persistentListOf(),
    val isSelectionMode: Boolean = false
)
```

### 1.3 耗时计算下沉，严禁在 Composable 内部做复杂变换
- **严禁**在 Composable 渲染函数体内直接调用大列表的 `sortedBy`、`filter`、`groupBy`、`distinctBy` 等高开销操作。
- **强制规范**：所有数据派生与排序运算必须在 ViewModel 的协程数据流（如 `map`、`combine`）中计算完成；对于仅限 UI 视角的临时派生，必须使用 `remember(key) { ... }` 包装。

---

## 2. 协程并发与省电铁律（防止发烫、偷跑电与内存泄漏）

### 2.1 绝对禁止使用 `GlobalScope`
- **严禁在工程任何地方出现 `GlobalScope`**（违规直接阻断）。生命周期无法约束的野生协程是导致内存泄漏与无法取消的主要罪魁祸首。

### 2.2 调度器强制依赖注入（禁止硬编码 Dispatchers）
- **严禁在 ViewModel、Repository、DataSource 中直接写死 `Dispatchers.IO` 或 `Dispatchers.Default`**。
- **强制规范**：所有协程调度器必须通过构造参数注入（可带默认值），保证单元测试可通过 `StandardTestDispatcher` 实现毫秒级精确验证。
- **强制配套（防反射闪退）**：凡构造参数带默认值或非 `(Application)` / `()` 标准签名的 ViewModel，**必须**通过 `viewModelFactory { initializer { ... } }` 显式工厂构造（`companion object` 暴露 `Factory` + Route 层 `viewModel(factory = XxxViewModel.Factory)`），**严禁**依赖默认 `AndroidViewModelFactory` 反射构造——该反射仅匹配 `(Application)` / `()` 两种签名，带默认参数的 Kotlin 构造函数不会生成对应 Java 重载，将直接触发运行时 `NoSuchMethodException` 闪退。

### 2.3 Compose 消费数据流强制生命周期感知（省电防偷跑）
- Compose 收集 ViewModel 中的 `StateFlow` 时，**强制使用 `collectAsStateWithLifecycle()`**。
- **严禁**使用无生命周期感知的裸 `collectAsState()`。用户退到桌面或息屏时，`collectAsStateWithLifecycle` 会自动暂停流收集，防止后台空耗 CPU 和电池。

```kotlin
// ❌ 违规：应用压入后台依然持续收集流，偷跑 CPU
val uiState by viewModel.uiState.collectAsState()

// ✅ 合规：自动跟随 Lifecycle.State.STARTED 开启/休眠
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

### 2.4 协程取消树保护（禁止吞掉 `CancellationException`）
- 在捕获通用异常时，**严禁将 `CancellationException` 顺带吞掉**。一旦吞掉取消异常，协程将无法正常中断退出。

```kotlin
// ❌ 违规：协程被外部 cancel 后，依然被 catch 吞掉继续往下跑
try {
    processLargeMediaBatch()
} catch (e: Exception) {
    // 破坏了协程取消机制！
}

// ✅ 合规：遇到取消异常直接向上重新抛出
try {
    processLargeMediaBatch()
} catch (e: Exception) {
    if (e is CancellationException) throw e
    // 仅处理常规业务/IO 异常
}
```

---

## 3. 相册与 Android 15 现代系统能力铁律

Relive 运行在 Android 15 原生环境上，享受纯粹的现代 API 规范：

### 3.1 现代分区存储唯一规范（Scoped Storage）
- **严禁**通过字符串拼接（如 `"/storage/emulated/0/DCIM/..."`）使用 `java.io.File` 操作公有媒体资产（触发安全异常崩溃）。
- **强制规范**：
  - 媒体读取/查询：统一通过 `ContentResolver.query()` 结合 `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` 获取 Uri。
  - 批量删除/修改/移动到废纸篓：直接使用 Android 现代媒体批处理 API——`MediaStore.createTrashRequest()`、`MediaStore.createDeleteRequest()` 或 `MediaStore.createWriteRequest()` 唤起系统原生确认弹窗。

### 3.2 纯净的 Android 15 权限模型
- **相册权限唯一范围**：
  1. `android.permission.READ_MEDIA_IMAGES`（完整相册访问）
  2. `android.permission.READ_MEDIA_VISUAL_USER_SELECTED`（用户部分照片访问选择）
- **严禁**引入古董权限（如 `READ_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE`）。
- 权限申请与状态响应统一下沉至 `:core:permission` 模块统一处理，业务 UI 仅响应授权结果。

### 3.3 原生 Edge-to-Edge 沉浸式与预测性返回
- Android 15 强制所有应用开启端到端沉浸式（Edge-to-Edge）。
- 界面布局强制直接适配 `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` 或其细分分量，**严禁引入老旧的透明状态栏 Hack、fitsSystemWindows 兼容补丁**。
- 导航默认适配 Android 15 原生预测性返回（Predictive Back）。

---

## 4. 模块解耦与单向依赖铁律（防止架构缠绕与循环依赖）

```
                     ┌──────────┐
                     │   :app   │ (聚合路由与装配)
                     └────┬─────┘
         ┌────────────────┼────────────────┐
         ▼                ▼                ▼
┌────────────────┐┌────────────────┐┌────────────────┐
│:feature:organize││ :feature:feed  ││:feature:settings│ (各 Feature 互不知晓)
└────────┬───────┘└────────┬───────┘└────────┬───────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           ▼
               ┌───────────────────────┐
               │        :core:*        │ (基础库，禁止反向依赖 Feature)
               └───────────────────────┘
```

1. **Feature 间横向隔离（绝对禁令）**：
   - `:feature:organize` **严禁直接依赖** `:feature:settings`、`:feature:feed` 等任何同级 Feature 模块！
   - 跨模块交互必须通过 **Navigation 类型安全路由（Route）** 或通过下沉至 `:core` 共享接口进行。
2. **Core 模块纯净性（禁止向上反向依赖）**：
   - `:core:ui`、`:core:data`、`:core:domain`、`:core:common` 等模块，**严禁引入任何 `:feature:*` 模块的依赖**。
3. **依赖暴露控制**：
   - 内部依赖一律使用 `implementation`，仅在需要向下游传递 API 时才允许使用 `api`，杜绝依赖无限传递泄露。

---

## 5. 健壮性与防崩溃铁律（安全底线）

### 5.1 严禁空 Catch 假装没看见
- **严禁**出现无任何处理的空异常捕获：
  ```kotlin
  // ❌ 违规阻断
  try {
      deleteMedia()
  } catch (e: Exception) {}
  ```
- **合规**：必须记录结构化日志（非打印堆栈），并向调用端返回失败状态或通过状态机展示给用户。

### 5.2 底层系统异常不上浮到 UI
- 数据库异常、IO 异常、系统权限异常，在 Repository / DataSource 边界必须统一捕获，并包装成密封类 `ReliveResult.Error(CustomDomainException)`。
- UI 层只负责响应 `ReliveResult` 的成功与失败状态展示界面，严禁因底层未捕获异常导致前台崩溃退出（Crash）。

---

## 6. 自动化审计违规清单（提交前机器与人工阻断核验）

| 编号 | 检查项 | 违规判据（命中即阻断） | 阻断级别 |
|---|---|---|---|
| **E1** | 全局野协程 | 代码中出现 `GlobalScope` | 🔴 阻断 |
| **E2** | 调度器硬编码 / ViewModel 反射构造 | ViewModel / Repository 中直接使用 `Dispatchers.IO` / `Dispatchers.Default`；或非标准构造签名的 ViewModel 未经显式 factory 构造 | 🔴 阻断 |
| **E3** | 不安全 Flow 收集 | Composable 中直接使用 `.collectAsState()` | 🔴 阻断 |
| **E4** | 列表缺失稳定 Key | `LazyColumn` / `LazyVerticalGrid` 的 `items()` 未指定 `key` 参数 | 🔴 阻断 |
| **E5** | 绝对路径越权访问 | 媒体操作中直接使用 `File("/storage/` 或硬编码路径 | 🔴 阻断 |
| **E6** | Feature 模块横向交叉 | `build.gradle.kts` 中一个 Feature 模块 `implementation(project(":feature:..."))` 依赖另一个 Feature | 🔴 阻断 |
| **E7** | 吞掉协程取消 | `catch` 块捕获 `Exception` 未重新抛出 `CancellationException` | 🔴 阻断 |
| **E8** | 空异常吞没 | 代码中出现 `catch (...) {}` 且主体为空 | 🔴 阻断 |
| **E9** | 历史版本分支污染 | 源码中出现 `Build.VERSION.SDK_INT` 或任何版本判断代码（强制 minSdk 35） | 🔴 阻断 |

---

> **给 AI 的最终纪律**：你编写的代码不仅要“长得像旧工程”，更要具备世界一流的现代 Android 工程品质。违背上述任何一条性能与稳定性铁律的代码，即便视觉对齐，也视同劣质代码，禁止合入。
