# 交接文档：代码审查与修复

## 背景

GLM5 在 Opus 完成 6 个开发阶段（93 测试通过）后接手继续开发，新增了续期历史、主题切换、语言切换、提醒天数配置等功能。Opus 回归后进行全面代码审查，发现并修复了以下问题。

## 已修复的问题

### 1. 测试编译错误 — HomeViewModelTest

**问题**: `HomeViewModel` 新增了 `GetTotalSpentUseCase` 构造参数（用于计算累计花费），但 `HomeViewModelTest` 的 3 处 ViewModel 创建代码未更新，导致测试编译失败。

**修复文件**: `app/src/test/java/.../ui/screen/home/HomeViewModelTest.kt`
- 添加 `GetTotalSpentUseCase` mock 字段
- 在 `setup()` 中初始化并配置默认返回值
- 在 3 处 `HomeViewModel(...)` 构造调用中传入 `getTotalSpent` 参数

### 2. 导入功能断路

**问题**: `MainActivity.kt` 通过 SAF 选择文件后，将内容写入 `savedStateHandle.set("import_content", content)`，但 SettingsScreen 从未读取这个值，导致导入功能实际上不生效。

**修复文件**:
- `SpendListNavHost.kt` — 在 Settings 路由中观察 `backStackEntry.savedStateHandle` 的 `import_content` 键，提取后清除，传给 SettingsScreen
- `SettingsScreen.kt` — 新增 `importContent: String?` 参数，通过 `LaunchedEffect` 检测内容类型（JSON/CSV），调用 `viewModel.onImportJson()` 或 `viewModel.onImportCsv()`
- `SettingsScreen.kt` — 新增导入结果消息 UI（成功/失败卡片），与汇率同步消息样式一致

**新增字符串**:
- `settings_import_success` / `settings_import_error`（中英文）

### 3. 硬编码字符串违反 i18n 规范

**问题**: `SettingsScreen.kt:178` 硬编码 `"Remind me:"`，在中文环境下显示英文。

**修复**: 
- 新增 `settings_remind_subtitle` 字符串资源（EN: "Remind me:", ZH: "提醒时间："）
- 替换为 `stringResource(R.string.settings_remind_subtitle)`

### 4. RenewalReminderWorker 不尊重用户配置

**问题**: Worker 硬编码 `withinDays = 3`，不读取用户在 Settings 中配置的提醒天数（3天/1天/当天）和提醒开关。

**修复文件**: `app/src/main/java/.../worker/RenewalReminderWorker.kt`
- 注入 `UserPreferences`
- 读取 `reminderEnabled`，如果关闭直接返回 success
- 读取 `reminderDays` 配置，取最大值作为查询范围
- 遍历即将到期的订阅时，只对 `daysUntilRenewal` 在用户选择天数集合内的订阅发送通知

## 已验证的功能（ADB 真机验证）

设备: Vivo V2307A (Android 14), 分辨率 1260x2800

| 页面 | 状态 | 验证内容 |
|------|------|----------|
| 首页 | ✅ 正常 | 月总花费、累计花费、分类/状态筛选、订阅列表、FAB 按钮 |
| 订阅详情 | ✅ 正常 | 金额、币种、周期、日期、续约/取消按钮 |
| 统计报表 | ✅ 正常 | 甜甜圈图(分类占比)、三个 Tab |
| 设置 | ✅ 正常 | 主币种、主题、语言、提醒配置、导出/导入按钮均可见可滚动 |
| i18n | ✅ 正常 | 中文显示正确，"提醒时间："修复后显示正常 |
| 无崩溃 | ✅ 确认 | logcat 无 FATAL/Exception |

## 编译与测试状态

- `./gradlew assembleDebug` ✅ BUILD SUCCESSFUL
- `./gradlew testDebugUnitTest` ✅ BUILD SUCCESSFUL (所有测试通过)

## 已知但未修复的次要问题

以下问题已确认但本次未修复（可在后续迭代处理）：

### 1. 缺失的单元测试
GLM5 新增了大量功能但未补测试：
- `RecordRenewalUseCase` — 续期核心逻辑无测试
- `GetTotalSpentUseCase` — 累计花费计算无测试
- `UpdateSubscriptionUseCase` — 更新无测试
- `SubscriptionDetailViewModel` — 详情页 ViewModel 无测试
- `SettingsViewModel` — 设置 ViewModel 无测试

### 2. GetTotalSpentUseCase 整除精度
`GetTotalSpentUseCase.kt:36` 中 `days / sub.billingCycle.days` 是 Long 整除 Int，会丢失小数部分。

### 3. StatsScreen 重复定义 resolvedCategoryName
`StatsScreen.kt:23-28` 有私有的 `resolvedCategoryName()` 函数，与 `ui/component/CategoryName.kt` 中的公共函数重复。

### 4. SettingsScreen 货币选择对话框
30+ 个货币选项在小屏设备上可能溢出，`Column` 缺少滚动支持。

## 修改文件清单

| 文件 | 变更 |
|------|------|
| `HomeViewModelTest.kt` | 添加 GetTotalSpentUseCase mock |
| `SettingsScreen.kt` | 导入处理 + importMessage UI + i18n 修复 |
| `SpendListNavHost.kt` | 导入内容从 savedStateHandle 传递到 SettingsScreen |
| `RenewalReminderWorker.kt` | 读取用户偏好配置 |
| `values/strings.xml` | +3 字符串 |
| `values-zh-rCN/strings.xml` | +3 字符串 |
