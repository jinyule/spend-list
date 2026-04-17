# 交接文档：废除自动续费 + 每日过期检查

## 背景

此前 `AutoRenewalWorker` 每日和 App 启动时自动把所有 ACTIVE 订阅的 `nextRenewalDate` 推进到今天之后，并生成 note="Auto-renewed" 的历史记录。这与用户的真实付费行为脱钩——用户可能实际已停付，但系统仍把订阅当作已续费，导致统计失真、风险隐性扩大。

用户要求：
1. 不再自动续约，所有续约必须由用户手动点击
2. 每日自动检查是否有订阅过期，并显式提醒

## 核心 UX 决策（与用户确认）

| 问题 | 选定方案 |
|------|---------|
| 过期提醒形式 | 系统通知 + Home 顶部横幅 + 列表 `StatusBadge`（三管齐下） |
| 过期订阅是否计入统计 | Worker 发现后自动 `status = EXPIRED`；`GetTotalSpentUseCase` 本就按 ACTIVE 过滤，自然不计入 |
| 手动续约若仍欠多期 | 一次只推进一期，已过去的不补 |
| 续约后状态恢复 | 若新 `nextRenewalDate >= today` 则恢复 ACTIVE；若仍 < today 则保持 EXPIRED |
| 通知频率 | 过期只通知一次（利用 state transition 天然去重） |
| 多订阅同日过期 | 合并一条聚合通知 |

## 实现变更

### 1. 新增：`MarkExpiredSubscriptionsUseCase`

**文件**：`domain/usecase/renewal/MarkExpiredSubscriptionsUseCase.kt`

- 扫描所有订阅，把 `ACTIVE` 且 `nextRenewalDate < today` 的改为 `EXPIRED`
- 返回**本次新过期**的列表（供通知使用）
- 已 EXPIRED / CANCELLED 的不再扫描（天然保证仅通知一次）
- **不写 RenewalHistory**——过期≠付款

### 2. 新增：`ExpirationCheckWorker`

**文件**：`worker/ExpirationCheckWorker.kt`

- HiltWorker，调用 `MarkExpiredSubscriptionsUseCase`
- 新过期列表非空时调用 `NotificationHelper.sendExpirationNotification(...)`
- 失败时 `Result.retry()`

### 3. 修改：`RecordRenewalUseCase`

**文件**：`domain/usecase/renewal/RecordRenewalUseCase.kt`

在 `copy(...)` 中加入 status 判断：
```kotlin
val newStatus = if (subscription.status == SubscriptionStatus.EXPIRED
    && !newRenewalDate.isBefore(LocalDate.now())
) SubscriptionStatus.ACTIVE else subscription.status
```

边界：
- 原 `ACTIVE` → 续约后仍 `ACTIVE`
- 原 `EXPIRED` + 推进后 ≥ today → 恢复 `ACTIVE`
- 原 `EXPIRED` + 推进后仍 < today（多期欠费）→ 保持 `EXPIRED`，用户可再续
- 原 `CANCELLED` → UI 隐藏 Renew 按钮，兜底不改动

### 4. 扩展：`NotificationHelper.sendExpirationNotification`

**文件**：`notification/NotificationHelper.kt`

- 单订阅文案：`%s 已过期，请续约或取消`
- 多订阅聚合文案：`%d 个订阅已过期：Netflix、Spotify、YouTube…`（超过 3 个加省略号）
- 使用固定通知 ID (`EXPIRATION_NOTIFICATION_ID = -1`) 避免堆积，新通知覆盖旧通知
- 加入 `BigTextStyle` 支持展开查看完整列表

### 5. 调度器改造

**文件**：`SpendListApplication.kt`

- **清理遗留 work**：启动时 `cancelUniqueWork("auto_renewal_immediate")` 和 `cancelUniqueWork("auto_renewal")`，让老版本安装过的 Worker 停掉
- 删除 `AutoRenewalWorker` 的 immediate + daily 两段调度
- 新增 `ExpirationCheckWorker` 的 immediate + daily 调度（`expiration_check_immediate` + `ExpirationCheckWorker.WORK_NAME = "expiration_check"`）
- `RenewalReminderWorker` / `ExchangeRateSyncWorker` 保持不变

### 6. Home 过期横幅

**文件**：`ui/screen/home/HomeViewModel.kt` + `HomeScreen.kt`

- `HomeUiState` 新增 `expiredCount: Int`（跨全部分类/筛选统计 EXPIRED）
- VM 构造参数注入 `SubscriptionRepository`——因为筛选列表后的 `subscriptions` 不包含 EXPIRED（当用户筛选 ACTIVE 时），必须独立查询
- HomeScreen 在 `TotalSpendCard` 下方、`CategoryFilterRow` 上方插入 `ExpiredBanner`（errorContainer 色系 Card）
- 点击横幅调用 `onStatusFilterChanged(SubscriptionStatus.EXPIRED)` 触发状态筛选

### 7. i18n

`values/strings.xml` + `values-zh-rCN/strings.xml` 各新增 4 条：
- `notification_expired_title`
- `notification_expired_single`
- `notification_expired_multiple` (2 参数：count + 预览字符串)
- `home_expired_banner`

### 8. 删除的文件

- `worker/AutoRenewalWorker.kt`
- `domain/usecase/renewal/AutoRenewSubscriptionsUseCase.kt`
- `test/.../AutoRenewSubscriptionsUseCaseTest.kt`

## 测试

**新增**：
- `MarkExpiredSubscriptionsUseCaseTest` — 7 个用例：ACTIVE 过期改 EXPIRED / 今天和未来不改 / 已 EXPIRED 不重复 / CANCELLED 不动 / 混合场景只返回新过期 / 不写 RenewalHistory
- `RecordRenewalUseCaseTest` — 6 个用例：订阅未找到报错 / ACTIVE 续约不变 / EXPIRED 推进到未来恢复 ACTIVE / EXPIRED 推进后仍过去保持 EXPIRED / RenewalHistory 正确写入 / CANCELLED 不误改

**修改**：
- `HomeViewModelTest` — 新增 `SubscriptionRepository` mock

**删除**：`AutoRenewSubscriptionsUseCaseTest`

## 验证结果

```
./gradlew test    # BUILD SUCCESSFUL, 119 tests, 0 failures
./gradlew assembleDebug  # BUILD SUCCESSFUL
```

全部 17 个测试类 119 个测试通过。

## 手动验证步骤

1. 新建订阅 A，`startDate=今天-60天`，月付 → `nextRenewalDate` ≈ 今天-30 天
2. 关闭 App 再打开 → 订阅 A 应显示 `Expired` badge、Home 顶部出现 `1 个订阅已过期 · 点击查看` 横幅、系统通知栏收到 `A 已过期，请续约或取消`
3. 打开详情页点 `Renew` → `nextRenewalDate` 推进到今天之后、badge 变 `Active`、横幅消失、`月总花费` 包含此订阅
4. 新建订阅 B，`startDate=今天-90天`，月付 → 启动后 B 标为 EXPIRED
5. 点 Renew 一次（只推进 30 天，仍过去）→ 保持 EXPIRED，不计入统计
6. 再点 Renew → 到未来 → 变 ACTIVE，计入统计
7. 两个订阅同时过期时 → 一条聚合通知 `2 个订阅已过期：A、B`
8. 旧版本升级：启动后 `auto_renewal` / `auto_renewal_immediate` 两条遗留 Work 被取消

## 未做的事

- **未清理历史 Auto-renewed RenewalHistory 记录**：保留为历史足迹，不回溯删除
- **未加通知 Action 按钮**（用户未选此项）
- **未合并 `ExpirationCheckWorker` 与 `RenewalReminderWorker`**：语义不同（过期 vs 即将到期），独立清晰
- **未改 `Subscription.isExpired` 计算属性**：保留为兜底——Worker 会及时切换，实际几乎不会再出现 `isExpired=true`，但删除无必要
- **未改 `GetTotalSpentUseCase`**：它已按 `status != ACTIVE` 过滤，天然满足新语义

## 后续可能的增强

- 通知 Action 按钮（一键续约 / 一键取消）
- 横幅右侧加关闭按钮让用户暂时隐藏
- 长期（30 天+）未处理的 EXPIRED 订阅自动降级为 CANCELLED 建议
- 进入设置页可手动触发一次过期检查（调试/刚安装时）

## Follow-up 2: 累计花费与月度趋势应反映历史实付，分类占比加切换

**问题**：上一个改动把过期订阅的 status 改成 EXPIRED 之后，Home 累计花费和 Stats 月度趋势都把它们**完全排除**，导致用户实际已经花出去的钱"从统计中蒸发"，续约后金额又会重新出现——这种"闪烁"违反客观事实。

**根因**：`GetTotalSpentUseCase` / `GetMonthlyTrendUseCase` 都用 `status != ACTIVE continue` 过滤，把"当前结构预测"和"历史实付"两种语义混用。

**核心原则**：`[startDate, nextRenewalDate)` 区间内是**已付**部分；`nextRenewalDate` 之后是**未付**部分。状态只影响后者是否继续增长，不应抹掉前者。

**修复**：

1. **`Subscription` 模型**（`domain/model/Subscription.kt`）
   - 新增 `fun paidCycles(): Long`——`[startDate, nextRenewalDate)` 的计费周期数
   - 新增 `val totalPaidAmount: BigDecimal`——已付周期 × 金额
   - 两个属性被 `GetTotalSpentUseCase` 和 `GetSpendingByCategoryUseCase` 共用

2. **`GetTotalSpentUseCase`**
   - 删除 `if (sub.status != ACTIVE) continue`
   - 改用 `sub.totalPaidAmount`
   - ACTIVE/EXPIRED/CANCELLED 三种状态的已付部分都计入

3. **`GetMonthlyTrendUseCase`**
   - 删除 ACTIVE 过滤
   - 条件从 `subStart <= month` 改为 `month ∈ [startYm, endYm)`
   - 每个订阅在"真实付过的月份"才累加 monthlyAmount，EXPIRED 之后自动降为 0
   - 用户续约推进 nextRenewalDate，趋势图自然反映新增的已付月份

4. **`GetSpendingByCategoryUseCase`**
   - 新增 `CategoryStatsMode` 枚举：`CURRENT_MONTHLY` / `HISTORICAL_TOTAL`
   - `invoke(currency, mode = CURRENT_MONTHLY)`——默认保持旧语义
   - `HISTORICAL_TOTAL` 走所有订阅 × `totalPaidAmount` 分组累计

5. **`HomeViewModel.calculateTotalSpend()`**
   - 删除 `if (activeSubscriptions.isEmpty()) { totalSpent = ZERO; return }` 早返回
   - 始终调用 `getTotalSpent()`——即使全是 EXPIRED，历史支出仍会显示

6. **`StatsViewModel` + `StatsScreen`**
   - 加 `selectedCategoryMode` 状态 + `onCategoryModeChanged`
   - 用 `MutableStateFlow<CategoryStatsMode>` + `flatMapLatest` 动态重订阅
   - CategoryTab 顶部加两个 FilterChip："当前每月" / "历史累计"
   - DonutChart 中心副文字随模式切换（`/月` ↔ `历史累计`）

7. **i18n**：`stats_mode_current` / `stats_mode_historical` 两条 key 中英文各加一份

**测试**：
- 新增 `GetTotalSpentUseCaseTest`（7 个用例）：ACTIVE/EXPIRED/CANCELLED 已付均计入、混合总和、续约后增长、零周期不计入、空列表
- 改 `GetMonthlyTrendUseCaseTest`：原 cancelled_excluded 改为 cancelled_paidMonthsIncluded；加 EXPIRED 月份测试
- 改 `GetSpendingByCategoryUseCaseTest`：拆成两个 mode 分别测试，验证二者返回不同总额
- 改 `HomeViewModelTest`：补测"仅 EXPIRED 订阅时 totalSpent 不为 0"

全部测试 130/130 通过。

**验证步骤**：
- 现有 EXPIRED 订阅 A：Home 累计花费**应包含** A 已付的部分（此前应显示为 0 或遗漏）
- Stats → 月度趋势：A 付费月份显非 0，之后月份为 0
- Stats → 分类占比：默认"当前每月"不含 A；切"历史累计"后 A 出现
- 续约 A → A 回 ACTIVE，累计花费 + 一期金额，月度趋势新增一期的月份

## Follow-up Fix: 详情页按钮可见条件放宽

**问题**：首次安装真机验证发现——点开过期订阅的详情页后，**续约和取消按钮都不显示**，用户无路可走。

**根因**：`SubscriptionDetailScreen.kt:300` 的按钮可见条件是 `status == SubscriptionStatus.ACTIVE`，EXPIRED 状态下整个按钮区被隐藏。之前设计"自动续费"时所有活跃订阅永远 ACTIVE，此判断无碍；引入 EXPIRED 状态后 UI 层未同步。

**修复**：条件改为 `status != SubscriptionStatus.CANCELLED`，使 EXPIRED 和 ACTIVE 状态下都显示 Renew + Mark as Cancelled；仅 CANCELLED 终态隐藏。VM 层 `onRenew()` / `onMarkCancelled()` 原本就不依赖 status，无需改动。

**验证**：
- 过期订阅点详情 → 续约/取消按钮可见
- 点续约 → 若推进后到未来则恢复 ACTIVE 且按钮仍可见；若仍过去则保持 EXPIRED 且按钮仍可见（允许继续补缴）
- 点取消 → status → CANCELLED，按钮消失（终态）
