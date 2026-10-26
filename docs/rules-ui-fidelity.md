# Relive 迁移执行铁律 —— UI 保真与资源纪律（Rules）

> **适用对象**：本仓库的一切 AI / 人工开发工作（含代码生成、平移、修复、评审）
> **生效范围**：高于 `docs/relive-architecture-migration-design.md` 中与之冲突的条目（本规则 **v1.0 已修订设计文档 D4**：文本资源化提前到平移期执行）
> **配套文档**：`docs/relive-architecture-migration-design.md`（迁移路线图 P0–P6）· `docs/migration-notes.md`（发现登记台账）

---

## 0. 一句话总纲

**旧工程代码是唯一的设计稿。在 Relive 的一切 UI 实现都是"提取与搬运"，不是"创作"。**

| 铁律 | 内容 |
|---|---|
| **R1 来源唯一** | 所有视觉参数（颜色、字号、字重、间距、圆角、尺寸、动画时长/弹性/曲线、布局结构）的唯一来源是旧工程 `E:\GitHub\photoo\android` 的代码，**逐个提取、逐个对照** |
| **R2 禁止臆造** | 旧代码里没有的值，一律不得出现。**严禁自行编造任何颜色**（包括"看起来差不多"的色值） |
| **R3 禁止顺手优化** | 不四舍五入（`13.dp` → `14.dp` ❌）、不"规范化"、不"更现代"（tween ↔ spring 互换 ❌）、不省略步骤 |
| **R4 模板即污染** | 新工程 Android Studio 模板自带的默认值（紫色主题、默认 Typography、模板字符串）**一律视为臆造源，必须清除替换**，禁止残留 |
| **R5 登记制** | 每次平移提交 message 登记 `source: photoo android/app/src/main/com/voicelike/app/Xxx.kt`（溯源信息仅存 Git Commit 与台账，严禁污染源码） |
| **R6 有疑必查** | 任何不确定的值：停下，去旧工程查证。**不许猜。** 查不到 → 记入 `migration-notes.md` 待办，暂缓该处 |
| **R7 注释纯净化** | **严禁在源码中写入任何“按照原项目设计”、“保持与旧工程一致”、“移植自旧工程 Xxx”等元注释/口水注释**。代码溯源信息唯一合法位置是 Commit Message 与 `migration-notes.md` |

---

## 1. 颜色纪律（硬性：集中管理 + 严格保真）

### 1.1 唯一合法居所

全部色值字面量集中定义在 **`:core:ui` 的 `theme/Color.kt`**：

```
core/ui/src/main/kotlin/com/jian/relive/core/ui/theme/
├── Color.kt     # 全项目唯一允许出现 Color(0x...) 字面量的文件
├── Type.kt      # Typography 与命名 TextStyle（引用 Color.kt 常量，零字面量）
└── Theme.kt     # 只做 colorScheme / 主题组装（引用 Color.kt 常量，零字面量）
```

- `Theme.kt` **只组装、不定义**：内部禁止出现任何 `Color(0x...)` 字面量。
- `app/` 模块现存的模板 `ui/theme/Color.kt、Theme.kt、Type.kt`（Material3 紫色系 Purple80/Purple40 等）是 **R4 污染源**：P1 迁移主题时整体删除重建，**一个模板色值都不许带走**。

### 1.2 提取与命名

- **提取源**：旧工程约 **213 处 `Color(0x...)` 字面量，散落在 18 个文件**（巨石 Activity 内 37 处，ShootingStatsView 30 处、MonthlyView 22 处……）。平移哪个文件，就提取该文件用到的全部色值入 `Color.kt`。
- **命名**：语义化命名（依据用途而非色相），前缀统一 `Relive`，例如卡片背景、主强调色、危险色。同一色值有多个用途时，**按用途分别命名**（禁止 `ReliveBlue1/ReliveBlue2` 这类无语义命名）。
- **取用规则**（优先级从高到低）：
  1. 能映射进 `MaterialTheme.colorScheme` 语义槽位（primary/surface/background…）的，组装进 `Theme.kt` 的 colorScheme，组件里用 `MaterialTheme.colorScheme.xxx`
  2. 槽位装不下的品牌/场景色 → 引用 `Color.kt` 命名常量
  3. `Color.Transparent / Color.White / Color.Black` 等系统命名常量：**旧代码怎么用就怎么搬**（允许）
- **禁止**：任何组件 / Screen / ViewModel 代码中出现 `Color(0x` 字面量。

### 1.3 严禁 MD3 动态取色与色盘推导（硬性禁止）

- **严禁动态取色（Dynamic Color）**：
  - 严禁在 `Theme.kt` 或任何主题入口中调用 `dynamicLightColorScheme()` 或 `dynamicDarkColorScheme()`。
  - 严禁在主题 Composable（如 `ReliveTheme`）中保留模板自带的 `dynamicColor: Boolean = true` 开关参数。
  - 必须彻底禁用壁纸动态提取色，全平台无论系统版本（即便在 Android 12+ / minSdk 35 上）均强制采用从旧工程提取出的固定静态配色，确保视觉绝对保真。
- **严禁 MD3 默认调色盘推导（Tonal Palettes）**：
  - 严禁引入 Material Theme Builder 的调色板推算逻辑或依据单主色自动衍生浅深色阶。
  - `MaterialTheme.colorScheme` 中配置的槽位色（primary、surface、background、outline 等），必须逐一明确映射到旧工程在 `Color.kt` 中声明的静态常量，旧工程没有定义的派生阶梯色一律不得凭空生成。

### 1.4 审计（可机器执行）

| 检查 | 违规判据 |
|---|---|
| 色值泄漏 | 除 `core/ui/**/theme/Color.kt` 外，任何 `.kt` 出现 `Color(0x` |
| 模板残留 | 任何文件出现 `Purple80 / Purple40 / PurpleGrey / Pink40` 等模板色名 |
| 动态取色泄漏 | 任何文件出现 `dynamicLightColorScheme` / `dynamicDarkColorScheme` / `dynamicColor` |

---

## 2. 文本资源纪律（硬性：收编 strings.xml，严禁硬编码）

> 本节为 **D4 修订**：原"平移期保留 Localization.kt 常量表"策略废止；本地化资源化**提前到平移期**执行。

### 2.1 基本规则

- 所有界面展示文本（含 `Text`、`contentDescription`、Toast/Snackbar、按钮/标签/标题/占位符文案）**一律收编进各模块 `res/values/strings.xml`** 及其语言变体。
- **严禁在 Compose 代码中硬编码中英文字符串字面量**。
- Composable 内取用：`stringResource(R.string.xxx)`；非组合上下文（VM/工具类）：`context.getString(R.string.xxx)`。
- 带参数文案：资源内用 `%1$d` / `%1$s` 占位，`stringResource(R.string.xxx, arg)` 传参。

### 2.2 提取源与 key 规范

- **唯一文案来源**：旧工程 `Localization.kt`（100KB，`LocalizedStrings` data class）+ `LocalizationExtra.kt`（92KB）。**逐字段照抄文案，严禁改写、润色、增删。** 旧代码里散落的硬编码 UI 文本同样收编，文案照抄。
- **key 命名**：`{模块前缀}_{旧字段名驼峰转下划线}`，与模块 resourcePrefix 一致，防多模块资源合并冲突：

| 文本归属 | 资源位置 | key 前缀 |
|---|---|---|
| feature 私有文案 | 该 feature 模块 `res/values/strings.xml` | `organize_` / `feed_` / `stats_` / `clean_` / `compress_` / `settings_` |
| 跨模块共享文案 | `:core:common` `res/values/strings.xml` | `common_` |
| core:ui 组件自带文案 | `:core:ui` `res/values/strings.xml` | `ui_` |
| 应用名等壳级文案 | `:app` `res/values/strings.xml` | （无前缀，如 `app_name`） |

### 2.3 语言目录映射（旧表 15 语言）

默认语言英文（与旧 `getStrings` 默认 `StringsEn` 一致）：`values/` 放英文，其余放变体目录：

| 旧 AppLanguage | 资源目录 | 备注 |
|---|---|---|
| English | `values/`（默认） | 兜底语言 |
| SimplifiedChinese (`zh-SC`) | `values-zh-rCN` | |
| TraditionalChinese (`zh-TC`) | `values-zh-rTW` | |
| Korean / Japanese | `values-ko` / `values-ja` | |
| French / German / Italian | `values-fr` / `values-de` / `values-it` | |
| Portuguese / Spanish | `values-pt` / `values-es` | |
| Russian / Thai / Vietnamese | `values-ru` / `values-th` / `values-vi` | |
| **Indonesian (`id`)** | **`values-in`** | ⚠️ 坑：Android 资源限定符用历史 ISO 码 `in`，**不是 `values-id`** |
| Arabic (`ar`) | `values-ar` | RTL 语言，`supportsRtl` 已开启 |

**例外（允许留在 kt 文件）**：语言选择器的"语言自称名"（English / 简体中文 / 한국어…）沿用旧 `AppLanguage` 枚举的 `displayName`——它属于**语言元数据**（每种语言恒显示自称，不随界面 locale 变化），不属于界面文案。

### 2.4 应用内语言切换（实现指引）

旧实现是 CompositionLocal 切换（`LocalAppStrings`）。新实现统一走 **系统 per-app locale**（minSdk 35 原生支持）：

- 设置语言：`LocaleManager.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))`
- 系统自动重建 Activity + 自动持久化（替代旧的 PreferencesManager 手动存语言 + 手动切表）
- `LanguageView` 平移时按此改造；语言列表数据照抄旧 `AppLanguage` 枚举

### 2.5 硬编码例外白名单（仅以下允许字面量）

1. 测试代码（`src/test` / `src/androidTest`）与 Log 输出
2. 类型安全路由的 `@Serializable` 对象/类名（非 UI 展示）
3. 正则表达式、文件名模板、URI/MIME 等技术字符串
4. `String` 拼接的**非展示型**技术值（如 EXIF 字段名）；**展示型拼接必须先资源化字符串部分**

### 2.6 转换方法建议

15 语言 × 双表 ≈ 数千条资源条目，必须系统化执行：按语言分批机械转换（建议写一次性脚本从 `LocalizedStrings` 表生成，或按语言逐批平移），每批完成后**抽样对拍**（每语言抽 10 条与旧表逐字比对 + 字段总数断言，缺一条即失败）。

---

## 3. 字号与排版纪律（硬性保真：值照抄）

- `fontSize` / `fontWeight` / `fontFamily` / `lineHeight` / `letterSpacing` 的值**逐个照抄旧代码**（全工程约 238 处 `fontSize`，散落 20 个文件）。
- **组织引导**（非强制）：能归入 `MaterialTheme.typography` 槽位（display/headline/title/body/label × large/medium/small）的组装进 `Type.kt`；槽位装不下的，在 `Type.kt` 定义命名 `TextStyle` 常量；组件内直接写 `fontSize = 18.sp` 亦可（旧代码如此则照抄）。
- **审计**：新增组件的任何字号值，必须能在旧工程找到同值来源（review 时对照 `source:` 登记）。

---

## 4. 间距与布局纪律（硬性保真：值照抄）

- `padding` / `spacing` / 宽高尺寸 / `roundedCorner` 圆角 / 边框粗细 / `elevation` / `Spacer` 高宽：**值逐个照抄**。
- 布局结构（Column/Row/Box 层级、`weight`、对齐方式、约束）照抄旧实现。
- **Dimens 提炼条件**：仅当**多个旧文件使用同一值**时，才可提炼为 `:core:ui` 的 `theme/Dimens.kt` 命名常量（如全项目统一的页面边距）；**禁止凭空设计一套"标准间距表"再把旧值往里凑**。
- 禁止一切"取整 / 规范化 / 对齐 4dp 网格"式改动。

---

## 5. 动效纪律（硬性保真：参数 + 实现方式照抄）

- **严格照抄**（实测旧工程参数分布，供提取参照）：
  - 时长：`tween(160 / 180 / 200 / 220 / 260 / 300)` 及其余原值
  - spring：`dampingRatio = 0.6f / 0.7f / 0.75f`、`stiffness = 320f / 350f`、`Spring.StiffnessMedium`、`Spring.DampingRatioMediumBouncy`
  - easing：`FastOutSlowInEasing` / `FastOutLinearInEasing` / `LinearOutSlowInEasing` / `LinearEasing`（用哪个搬哪个）
  - `Animatable` 驱动方式、多动画并发 `launch` 结构、入场偏移计算式
  - `CelebrationConfetti` 粒子物理公式（初速度、重力、衰减、粒子数）**逐参数照抄**
- **集中动效库整体平移**：`AnimationUtils.kt`、`FluidTransitions.kt` 原文件迁入 `:core:ui`，内部参数不动。
- **禁止**：tween ↔ spring 互换、改时长/弹性/曲线、"看起来一样就行"、用 `animate*AsState` 重写旧 `Animatable + launch` 结构（实现方式本身是设计的一部分）。
- 可选：高频复用动效常量集中到 `:core:ui` 的 `theme/Motion.kt`——**值必须来自旧代码**，命名后引用。

---

## 6. 总审计清单（提交前自检 / CI 可执行）

| # | 检查项 | 判据 | 级别 |
|---|---|---|---|
| A1 | 色值泄漏 | `Color(0x` 出现在 `core/ui/theme/Color.kt` 之外 | 🔴 阻断 |
| A2 | 模板污染 | `Purple80` 等模板色名残留 | 🔴 阻断 |
| A3 | 文本硬编码 | Composable 内 `Text("` 直接跟字符串字面量（非 `stringResource`） | 🔴 阻断 |
| A4 | 展示文本未收编 | 代码中出现中文/英文句子字面量且不在白名单（2.5） | 🔴 阻断 |
| A5 | 字号无源 | 新增 `fontSize` / `.sp` 值在旧工程无同值 | 🟠 review 拦截 |
| A6 | 间距无源 | 新增 `.dp` 布局值在旧工程无同值（除非由 Dimens 常量引用） | 🟠 review 拦截 |
| A7 | 动画无源 | 新增 tween 时长 / spring 参数 / easing 与旧工程不符 | 🟠 review 拦截 |
| A8 | 提交无登记 | 平移类提交缺少 `source:` 路径 | 🟠 review 拦截 |
| A9 | 动态取色泄漏 | 代码中出现 `dynamicLightColorScheme` / `dynamicDarkColorScheme` / `dynamicColor` | 🔴 阻断 |
| A10 | 口水注释污染 | 源码中出现“按照原项目”、“旧工程”、“移植自”等溯源元注释（详见 `rules-code-style.md`） | 🔴 阻断 |

**A1–A4、A9–A10 可直接落地为 CI grep 脚本；A5–A8 依赖 code review 对照（`source:` 登记是对照入口）。**

## 7. 违规处置

发现违规 → **立即修复后才能继续**；对来源存疑（旧代码查不到对应值）→ 按铁律 **R6** 处理：登记 `migration-notes.md`、暂缓该处、推进其余部分。**严禁"先写个差不多的，回头再对"。**

---

## 附：正反示例速查

```kotlin
// ❌ 违规 1：臆造颜色 + 硬编码文案 + 无源字号间距 + 防御性口水注释
// 按照原项目 Photoo.kt 第 50 行的设计设置 14.dp 边距与字体
Text("删除照片", color = Color(0xFFE53935), fontSize = 15.sp,
     modifier = Modifier.padding(14.dp))

// ❌ 违规 2：主题中包含动态取色逻辑
@Composable
fun ReliveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // 严禁！导致旧工程配色被壁纸覆盖
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) // 严禁！
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    // ...
}

// ✅ 合规 1：色值入 Color.kt、文案入 strings.xml、值均有旧代码来源、源码无垃圾元注释
Text(
    stringResource(R.string.organize_delete_photo),           // strings.xml（文案照抄旧表）
    color = MaterialTheme.colorScheme.error,                  // 槽位色（Color.kt 提取值组装）
    fontSize = 15.sp,
    modifier = Modifier.padding(14.dp)
)

// ✅ 合规 2：主题坚决禁用动态取色，全平台锁死提取自旧工程的固定配色
@Composable
fun ReliveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 强制只走预定义固定 ColorScheme，零动态壁纸取色污染
    val colorScheme = if (darkTheme) ReliveDarkColorScheme else ReliveLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReliveTypography,
        content = content
    )
}
```

> **给 AI 的最后一句**：你不是这个应用的设计者，旧工程才是。你的每一次提交都应该能回答——"这个值/这段文案/这个参数，来自旧工程哪个文件哪一行？"答不上来，就是违规。
