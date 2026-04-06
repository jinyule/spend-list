# 手机验证问题修复交接文档

> 完成日期：2026-04-06（更新：后续功能完善）

## 背景

用户在手机上验证 App 后发现若干功能问题，本会话完成了以下修复和新功能开发，随后完成了剩余的四个功能任务。

## 关键决策

| 决策项 | 选择 | 原因 |
|--------|------|------|
| 分类国际化 | 预设用 nameResKey，自定义用 name | 预设支持多语言，自定义保留用户输入 |
| 汇率转换显示 | 只显示主币种金额 | 用户确认简洁方案 |
| FAB 位置 | 可拖动 + 80% 透明度 | 防遮挡，用户可自定义位置 |
| 累计花费计算 | startDate → nextRenewalDate | nextRenewalDate 是下一次未付费日期 |
| 语言切换 | LocaleHelper 统一处理 API 33+/33- | API 33+ 用 LocaleManager，API <33 用 AppCompatDelegate |
| 导入导出 | SAF ActivityResultLauncher 在 MainActivity | Compose 中 SAF 需在 Activity 级别注册 |
| Release 签名 | keystore.properties 管理密钥 | 密钥文件不提交 Git，安全可控 |

## 新增功能

### 1. 分类名称国际化
- 预设分类（AI Tools 等）根据系统语言显示本地化名称
- 自定义分类显示用户输入的原始名称
- 影响文件：`AddEditScreen.kt`, `StatsScreen.kt`, `GetSpendingByCategoryUseCase.kt`

### 2. 汇率转换
- SubscriptionCard 显示转换后的主币种金额
- HomeViewModel 计算 `convertedAmounts` Map
- 年费订阅转换后显示月均金额 + "/月" 后缀

### 3. 主题切换
- Settings 页面新增主题选项（跟随系统/浅色/深色）
- UserPreferences 存储 `themeMode`
- Theme.kt 根据 `themeMode` 切换深浅主题

### 4. 提醒天数配置
- Settings 页面新增提醒天数多选（3天前/1天前/当天）
- UserPreferences 存储 `reminderDays: Set<Int>`

### 5. DatePicker
- AddEditScreen 开始日期支持 DatePicker 选择
- 点击日期字段或日历图标弹出 DatePickerDialog

### 6. 图标选择器
- AddEditScreen 新增图标选择入口
- 使用已有 IconPicker 组件

### 7. 续约功能
- 详情页新增"续约"按钮（仅活跃订阅显示）
- 点击续约更新 nextRenewalDate 并记录历史
- 显示续约次数和最近 5 条历史记录
- 数据库新增 `renewal_history` 表（迁移 v1→v2）

### 8. 累计花费
- 首页卡片显示"月总花费"和"累计花费"并排
- 累计花费 = 所有活跃订阅从 startDate 到 nextRenewalDate 的已付费总额
- 新增 `GetTotalSpentUseCase`

### 9. 可拖动 FAB
- 新增 `DraggableFloatingActionButton` 组件
- 80% 透明度背景
- 支持 4 方向拖动，位置保持

### 10. 语言设置（新增）
- Settings 页面新增语言选项（跟随系统/中文/English）
- UserPreferences 存储 `languageCode`
- LocaleHelper 统一处理 API 33+ (LocaleManager) 和 API <33 (AppCompatDelegate)
- MainActivity 使用 LaunchedEffect 监听语言变化并应用

### 11. 导入导出 SAF（新增）
- MainActivity 注册 CreateDocument 和 OpenDocument launcher
- 通过 SpendListNavHost 传递回调给 SettingsScreen
- JSON/CSV 导出通过系统文件选择器保存
- 导入通过系统文件选择器打开

### 12. 通知权限请求（新增）
- Settings 开启提醒时检查 POST_NOTIFICATIONS 权限
- Android 13+ 自动弹出权限请求对话框
- 权限授予后才启用提醒功能

### 13. Release 构建配置（新增）
- keystore.properties 管理签名密钥（添加到 .gitignore）
- keystore.properties.example 提供模板
- build.gradle.kts 读取密钥配置 signingConfigs
- 无 keystore.properties 时仍可正常构建 debug

## 修复问题

### 首页不刷新
**问题**：新增订阅返回首页后列表不刷新

**根因**：`HomeViewModel.loadSubscriptions()` 每次切换 filter 创建新 Flow 订阅，未取消旧订阅，导致竞态条件

**修复**：
```kotlin
private var loadJob: Job? = null

private fun loadSubscriptions() {
    loadJob?.cancel()  // 先取消旧订阅
    loadJob = viewModelScope.launch { ... }
}
```

## 架构变化

### 数据库变更
```sql
-- Migration 1→2
CREATE TABLE renewal_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    subscription_id INTEGER NOT NULL,
    previous_renewal_date INTEGER NOT NULL,
    new_renewal_date INTEGER NOT NULL,
    amount TEXT,
    note TEXT,
    renewed_at INTEGER NOT NULL,
    FOREIGN KEY(subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
);
CREATE INDEX index_renewal_history_subscription_id ON renewal_history(subscription_id);
```

### 新增文件
| 文件 | 用途 |
|------|------|
| `RenewalHistory.kt` | 续约记录领域模型 |
| `RenewalHistoryEntity.kt` | 续约记录数据库实体 |
| `RenewalHistoryDao.kt` | 续约记录 DAO |
| `RenewalHistoryRepository.kt` | 续约记录仓库接口 |
| `RenewalHistoryRepositoryImpl.kt` | 续约记录仓库实现 |
| `RecordRenewalUseCase.kt` | 续约用例 |
| `GetTotalSpentUseCase.kt` | 累计花费计算用例 |
| `DraggableFloatingActionButton.kt` | 可拖动 FAB 组件 |

### 依赖注入
- `DatabaseModule.kt` 添加 `RenewalHistoryDao` 提供
- `RepositoryModule.kt` 添加 `RenewalHistoryRepository` 绑定

## 验证结果

| 功能 | 状态 |
|------|------|
| 分类国际化（中文） | ✅ 显示"AI 工具" |
| 汇率转换 | ✅ 卡片显示主币种金额 |
| 主题切换 | ✅ 三种模式正常切换 |
| 提醒天数配置 | ✅ 多选正常 |
| DatePicker | ✅ 日期选择正常 |
| 图标选择 | ✅ 选择后显示正确 |
| 续约功能 | ✅ 按钮可用，历史记录显示 |
| 累计花费 | ✅ 并排显示，计算正确 |
| 可拖动 FAB | ✅ 可拖动，透明度正确 |
| 首页刷新 | ✅ 新增后自动刷新 |

## Git 提交

```
0e0787d feat: add language settings, SAF import/export, notification permission and release build config
0e16596 docs: add handover document for mobile verification fixes
7abe986 feat: add renewal, i18n, theme, currency conversion and cumulative spend
7cc0476 fix: resolve build and runtime issues
7b5c2b7 chore: initial commit
```

## 功能完成状态

所有计划功能已完成：

| 功能 | 状态 |
|------|------|
| 语言设置 | ✅ App 内切换已实现 |
| 导入导出 | ✅ SAF 已集成 |
| 通知权限 | ✅ Android 13+ 权限请求已实现 |
| Release 构建 | ✅ keystore.properties 配置完成 |

## Release 构建指南

```bash
# 1. 创建 keystore 文件
keytool -genkey -v -keystore spendlist-release.keystore -alias spendlist -keyalg RSA -keysize 2048 -validity 10000

# 2. 创建 keystore.properties（参考 keystore.properties.example）
storeFile=spendlist-release.keystore
storePassword=your_store_password
keyAlias=spendlist
keyPassword=your_key_password

# 3. 构建 Release APK
./gradlew assembleRelease
```