# 氪金清单 (Spend List) — 项目指南

## 构建环境

- **JDK**: 17.0.2 (`D:/Software/JDK/jdk-17.0.2`，配置在 `gradle.properties`)
- **Android SDK**: `D:/Software/andriod-studio/andriod-sdk` (`local.properties`)
- **Java 24 与 Gradle 8.13 不兼容**，必须用 JDK 17 运行构建和测试

## 常用命令

```bash
./gradlew test          # 运行单元测试（必须用 JDK 17）
./gradlew assembleDebug # 构建 Debug APK
```

## 开发规范

- **TDD**: Outside-In，Red → Green → Refactor，UseCase/ViewModel 必须先写测试
- **架构**: MVVM + Clean Architecture，单模块包分层
- **i18n**: 中英双语，所有 UI 文本必须在 `strings.xml` 中定义
- **金额**: String 存储，运行时转 BigDecimal，避免浮点精度丢失

## 核心架构要点

### 订阅生命周期

- **状态枚举**：`SubscriptionStatus.{ACTIVE, EXPIRED, CANCELLED}`
- **状态转换**
  - `ACTIVE → EXPIRED`：由 `ExpirationCheckWorker` 每日自动触发（`nextRenewalDate < today`）
  - `EXPIRED → ACTIVE`：用户手动 `Renew` 且推进后新 `nextRenewalDate ≥ today`
  - `* → CANCELLED`：用户手动 `Mark as Cancelled`（终态，不再有按钮）
- **无自动续费**：`AutoRenewalWorker` 已废除；Worker 只标记过期，不推进日期

### 统计语义二分法

| 语义 | 计算基础 | 用于 |
|---|---|---|
| 当前结构预测 | 只看 `ACTIVE`，累加 `monthlyAmount` | Home 月花费、Stats 分类占比默认 |
| 历史实付 | 所有状态，按 `[startDate, nextRenewalDate)` 区间的已付周期 | Home 累计花费、Stats 月度趋势、Stats 分类占比"历史累计" |

**关键属性**（`Subscription` 上）：
- `paidCycles(): Long` — 已付周期数
- `totalPaidAmount: BigDecimal` — 已付历史总额

### Worker 职责

- `ExpirationCheckWorker` — 每日 + 启动，`ACTIVE → EXPIRED` 转换并发聚合通知（仅"新过期"订阅）
- `RenewalReminderWorker` — 每日，查 `withinDays` 内到期的 `ACTIVE` 订阅发提醒
- `ExchangeRateSyncWorker` — 每日，同步汇率

## 交接文档索引

| Phase | 文档 | 状态 |
|-------|------|------|
| Phase 1: 项目骨架 + 核心 CRUD | [docs/handover-phase1.md](docs/handover-phase1.md) | 已完成 |
| Phase 2: 分类系统 + 筛选 | [docs/handover-phase2.md](docs/handover-phase2.md) | 已完成 |
| Phase 3: 多币种 + 汇率 | [docs/handover-phase3.md](docs/handover-phase3.md) | 已完成 |
| Phase 4: 到期提醒 | [docs/handover-phase4.md](docs/handover-phase4.md) | 已完成 |
| Phase 5: 统计报表 | [docs/handover-phase5.md](docs/handover-phase5.md) | 已完成 |
| Phase 6: 导入导出 + i18n + 收尾 | [docs/handover-phase6.md](docs/handover-phase6.md) | 已完成 |
| 构建验证与修复 | [docs/handover-build-fix.md](docs/handover-build-fix.md) | 已完成 |
| 手机验证问题修复 | [docs/handover-mobile-verification.md](docs/handover-mobile-verification.md) | 已完成 |
| 代码审查与修复 | [docs/handover-code-review-fix.md](docs/handover-code-review-fix.md) | 已完成 |
| 季度周期 + 扣款日 + 自动续期 | [docs/handover-quarterly-billing-day-autorenew.md](docs/handover-quarterly-billing-day-autorenew.md) | 已完成 |
| 计费周期布局 + 扣款日选择修复 | [docs/handover-ui-fix-billing.md](docs/handover-ui-fix-billing.md) | 已完成 |
| 废除自动续费 + 每日过期检查 + 历史实付统计 | [docs/handover-manual-renew-expiration-check.md](docs/handover-manual-renew-expiration-check.md) | 已完成 |
