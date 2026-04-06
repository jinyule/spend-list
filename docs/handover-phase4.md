# Phase 4 交接文档 — 到期提醒

> 完成日期：2026-04-05

## 完成内容

### Worker
- **RenewalReminderWorker**: 每日检查 3 天内到期的活跃订阅，发送系统通知
  - HiltWorker 注入 GetUpcomingRenewalsUseCase
  - 异常时返回 Result.retry()
- **ExchangeRateSyncWorker**: 每日自动同步汇率
  - 读取用户主币种偏好，调用 CurrencyRepository.fetchAndCacheRates
  - 异常时返回 Result.retry()

### 通知
- **NotificationHelper**: 静态工具类
  - 检查 POST_NOTIFICATIONS 权限
  - 当天到期用 `notification_renewal_today` 格式
  - 未来到期用 `notification_renewal_days` 格式（含天数）
  - PendingIntent 点击跳转到 MainActivity

### Worker 调度
- **SpendListApplication**: 应用启动时注册两个 PeriodicWork
  - `renewal_reminder`: 每日执行，KEEP 策略（不重复注册）
  - `exchange_rate_sync`: 每日执行，KEEP 策略

### 设置页面
- **SettingsViewModel**: 新增 `reminderEnabled` 状态 + `onReminderEnabledChanged`
- **SettingsScreen**: 新增"提醒"区块
  - 到期提醒开关（Switch）
- **UserPreferences**: 新增 `REMINDER_ENABLED` 布尔偏好（默认 true）

### 测试 (74 个，全部通过)
| 测试类 | 数量 | 新增 |
|---|---|---|
| BillingCycleTest | 10 | |
| AddSubscriptionUseCaseTest | 9 | |
| GetSubscriptionsUseCaseTest | 5 | |
| DeleteSubscriptionUseCaseTest | 2 | |
| GetUpcomingRenewalsUseCaseTest | 4 | |
| ManageCategoryUseCaseTest | 11 | |
| CategoryManageViewModelTest | 7 | |
| ConvertCurrencyUseCaseTest | 5 | |
| CurrencyRepositoryImplTest | 6 | |
| HomeViewModelTest | 5 | |
| AddEditViewModelTest | 5 | |
| **RenewalCheckLogicTest** | **6** | **Phase 4** |

Phase 4 新增 6 个测试，总计 74 个。

## 新增文件
```
app/src/main/java/com/spendlist/app/
├── notification/NotificationHelper.kt
├── worker/RenewalReminderWorker.kt
└── worker/ExchangeRateSyncWorker.kt

app/src/test/java/com/spendlist/app/
└── worker/RenewalCheckLogicTest.kt
```

## 修改文件
- `SpendListApplication.kt` — 新增 scheduleWorkers()，注册两个 PeriodicWork
- `data/datastore/UserPreferences.kt` — 新增 reminderEnabled 偏好
- `ui/screen/settings/SettingsViewModel.kt` — 新增提醒状态和开关
- `ui/screen/settings/SettingsScreen.kt` — 新增提醒 Switch UI

## 权限
- `INTERNET` — 汇率 API 调用
- `POST_NOTIFICATIONS` — Android 13+ 通知权限（需运行时请求）

## 已知限制
- 提醒开关目前仅保存偏好，未实际取消/恢复 WorkManager 任务（Worker 内部可检查偏好决定是否发送）
- 未实现运行时通知权限请求 UI（Android 13+ 首次需要用户授权）
- Worker 调度间隔为 1 天（WorkManager 最小间隔 15 分钟），首次执行时机由系统决定

## 下一步：Phase 5 — 统计报表
1. 写 GetSpendingByCategory / GetMonthlyTrend UseCase 测试
2. 实现统计 UseCase
3. DonutChart 自绘组件 (Canvas)
4. Vico 折线图 + 柱状图集成
5. StatsScreen 完整页面 (Tab 切换)
