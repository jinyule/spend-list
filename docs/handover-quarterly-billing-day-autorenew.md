# 交接文档：季度周期 + 扣款日 + 自动续期

## 背景

用户在真机使用中发现三个问题并提出两个新需求：
1. **Bug**: ACTIVE 订阅过了 nextRenewalDate 后不会自动推进（如 ChatGPT Plus 一直显示过期）
2. **Bug**: TestReminderReceiver 测试代码残留在生产代码中
3. **新需求**: 新增季度（Quarterly）计费周期
4. **新需求**: 添加可选扣款日字段，支持不同厂家的计费方式（自然月 vs 固定天数）

## 已完成的变更

### Phase A: 清理测试代码

- **删除** `app/src/main/java/com/spendlist/app/receiver/TestReminderReceiver.kt`
- **修改** `AndroidManifest.xml` — 移除 TestReminderReceiver 的 `<receiver>` 注册块

### Phase B: 新增 Quarterly 季度计费周期

**BillingCycle.kt** — sealed class 新增 `data object Quarterly`：
- `calculateNextRenewalDate`: `plusMonths(3)`
- `toDays`: `90`
- `monthlyFactor`: `1.0 / 3.0`

**更新了所有 `when` 分支**（10 个文件）：
- `EntityMappers.kt` — 双向映射 `"QUARTERLY" ↔ Quarterly`
- `AddEditViewModel.kt` — `buildBillingCycle()` 和 `loadSubscription()`
- `AddEditScreen.kt` — RadioButton 列表新增季付选项
- `GetTotalSpentUseCase.kt` — 累计花费计算
- `SubscriptionCard.kt` — `formatCycleSuffix()` 显示 `/qtr`
- `SubscriptionDetailScreen.kt` — `cycleSuffix()` 和 `cycleLabel()`
- `ExportDataUseCase.kt` — 导出映射
- `ImportDataUseCase.kt` — 导入映射

**i18n 字符串**：
- `cycle_quarterly`: Quarterly / 季付
- `home_per_quarter`: /qtr / /季

### Phase C: 可选扣款日 + 数据库迁移

**域模型**：
- `Subscription.kt` — 新增 `billingDayOfMonth: Int? = null`
- `BillingCycle.kt` — 新增重载方法 `calculateNextRenewalDate(fromDate, billingDayOfMonth)`
  - 短月份处理：`billingDayOfMonth=31` + 2月 → clamp 到 28/29
  - Custom 类型忽略 billingDayOfMonth，使用默认逻辑

**数据层**：
- `SubscriptionEntity.kt` — 新增 `billing_day_of_month INTEGER` 列
- `SpendListDatabase.kt` — version 2 → 3, `MIGRATION_2_3`
- `DatabaseModule.kt` — 注册 `MIGRATION_2_3`
- `EntityMappers.kt` — 双向映射 `billingDayOfMonth`

**UI**：
- `AddEditViewModel.kt` — `AddEditUiState` 新增 `billingDayOfMonth`/`billingDayError`，新增 `onBillingDayChange()`
- `AddEditScreen.kt` — 非 Custom 周期时显示可选扣款日输入框

**导入导出**：
- `SubscriptionExportDto` — 新增 `billingDayOfMonth`
- CSV header 新增 `billingDayOfMonth` 列
- 导入时解析新字段，向后兼容（旧文件无此字段默认 null）

**调用方更新**：
- `RecordRenewalUseCase.kt` — 使用 `calculateNextRenewalDate(prev, sub.billingDayOfMonth)`

**i18n 字符串**：
- `field_billing_day`: Billing Day (optional) / 扣款日（可选）
- `field_billing_day_hint`: 1-31

### Phase D: 自动续期 Worker

**新增文件**：
- `AutoRenewSubscriptionsUseCase.kt` — 核心逻辑：
  - 遍历所有 ACTIVE 订阅
  - 如果 nextRenewalDate < today，循环推进到当前或未来日期
  - 每次推进生成一条 RenewalHistory 记录（note="Auto-renewed"）
  - 安全保护：最多迭代 365 次防止无限循环
- `AutoRenewalWorker.kt` — HiltWorker，调用 UseCase

**调度注册** (`SpendListApplication.kt`)：
- App 启动时立即执行一次 `OneTimeWork`（`REPLACE` 策略，确保 UI 始终显示最新状态）
- 每日 `PeriodicWork` 定期检查

## 测试

**新增测试**：
- `BillingCycleTest.kt` — 5 个 Quarterly 测试 + 8 个 billingDayOfMonth 测试
- `AutoRenewSubscriptionsUseCaseTest.kt` — 7 个测试覆盖：正常续期、已取消不处理、未过期不处理、多周期追赶、历史记录生成、扣款日计算

**测试结果**: `./gradlew test` BUILD SUCCESSFUL，所有测试通过
**编译结果**: `./gradlew assembleDebug` BUILD SUCCESSFUL

## 数据库迁移

| 版本 | 变更 |
|------|------|
| 1 → 2 | 添加 renewal_history 表（已有） |
| 2 → 3 | subscriptions 表新增 `billing_day_of_month INTEGER DEFAULT NULL` |

## 修改文件清单

| 文件 | 变更类型 |
|------|----------|
| `receiver/TestReminderReceiver.kt` | 删除 |
| `AndroidManifest.xml` | 移除 receiver 注册 |
| `domain/model/BillingCycle.kt` | 新增 Quarterly + billingDayOfMonth 重载 |
| `domain/model/Subscription.kt` | 新增 billingDayOfMonth 字段 |
| `data/local/entity/SubscriptionEntity.kt` | 新增列 |
| `data/local/SpendListDatabase.kt` | version 3 + MIGRATION_2_3 |
| `data/local/converter/EntityMappers.kt` | 映射更新 |
| `di/DatabaseModule.kt` | 注册迁移 |
| `ui/screen/addEdit/AddEditViewModel.kt` | 扣款日 UI 逻辑 |
| `ui/screen/addEdit/AddEditScreen.kt` | 扣款日输入框 + 季付选项 |
| `ui/component/SubscriptionCard.kt` | Quarterly 显示 |
| `ui/screen/detail/SubscriptionDetailScreen.kt` | Quarterly 显示 |
| `domain/usecase/subscription/GetTotalSpentUseCase.kt` | Quarterly 计算 |
| `domain/usecase/export/ExportDataUseCase.kt` | 新字段导出 |
| `domain/usecase/export/ImportDataUseCase.kt` | 新字段导入 |
| `domain/usecase/renewal/RecordRenewalUseCase.kt` | 使用重载方法 |
| `domain/usecase/renewal/AutoRenewSubscriptionsUseCase.kt` | 新增 |
| `worker/AutoRenewalWorker.kt` | 新增 |
| `SpendListApplication.kt` | 注册新 Worker |
| `values/strings.xml` | +4 字符串 |
| `values-zh-rCN/strings.xml` | +4 字符串 |
| `AutoRenewSubscriptionsUseCaseTest.kt` | 新增测试 |
| `BillingCycleTest.kt` | 新增 13 个测试 |
