# 氪金清单 (Spend List)

一款 Android 订阅管理应用，帮助用户追踪和管理各类订阅服务的花费。强调**用户对订阅状态负责**——系统不会代替用户续费，而是每日提醒已过期的订阅，用户手动决定续约或取消。

## 功能特性

### 核心功能

- **订阅管理**：添加、编辑、删除订阅服务；手动续约、标记已取消
- **分类系统**：预设分类（AI 工具/基建/娱乐/工具/云/域名/存储/其他）+ 自定义分类，支持多语言
- **多币种支持**：多种货币自动汇率换算，主币种可配置
- **计费周期**：月付 / 季付 / 年付 / 自定义天数，可指定每月固定扣款日（1–31，月末自动 clamp）

### 到期检测 + 提醒

系统每日自动扫描订阅状态，无自动续费：

- **过期检测**（`ExpirationCheckWorker`）：App 启动 + 每日扫描，`ACTIVE` 且 `nextRenewalDate < today` 的订阅自动改为 `EXPIRED`
- **即将到期提醒**（`RenewalReminderWorker`）：自定义提醒天数（3 天前 / 1 天前 / 当天）
- **三管齐下的提醒**：系统通知（同日多订阅自动聚合为一条） + Home 顶部红色横幅 + 列表项状态徽章
- **手动续约**：详情页 `续约` 按钮推进一个周期；若因欠多期推进后仍 `< today`，保持 `EXPIRED` 供继续补缴；推进到未来则自动恢复 `ACTIVE`
- **去重保证**：过期仅通知一次（Worker 只处理 `ACTIVE → EXPIRED` 转换，EXPIRED 不再被扫描）

### 统计报表

明确区分两种统计语义：

- **当前结构预测**（仅 `ACTIVE`）：Home 月总花费、Stats 分类占比默认视图
- **历史实付**（所有状态按 `paidCycles × amount`）：Home 累计花费、Stats 月度趋势、Stats 分类占比"历史累计"视图

| 统计 | 语义 | 包含状态 |
|---|---|---|
| Home 月总花费 | 当前每月预测 | ACTIVE |
| Home 累计花费 | 历史实付总额 | ACTIVE + EXPIRED + CANCELLED |
| Stats 月度趋势 | 过去 12 个月每月实付 | 按 `[startYm, endYm)` 判断 |
| Stats 分类占比（默认） | 当前每月预测 | ACTIVE |
| Stats 分类占比（切换） | 历史累计实付 | 所有已付周期 > 0 |
| 即将到期提醒 | 未来扣款 | ACTIVE |

**设计原则**：已经花出去的钱永远计入历史，不因订阅当前状态而消失；续约后 `nextRenewalDate` 推进，新的已付周期自动重新计入。

### 导入导出

- JSON / CSV 双格式
- 支持数据备份、设备迁移、跨平台同步

### 界面特性

- **主题切换**：跟随系统 / 浅色 / 深色
- **语言切换**：中文 / English
- **Material Design 3**：现代化 UI 设计
- **可拖动 FAB**：自定义悬浮按钮位置

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| 架构 | MVVM + Clean Architecture（单模块包分层） |
| 依赖注入 | Hilt |
| 数据库 | Room |
| 数据存储 | DataStore (Preferences) |
| 异步 | Kotlin Coroutines + Flow |
| 后台任务 | WorkManager（HiltWorker） |
| 网络请求 | Retrofit + OkHttp |
| 图表 | Vico + 自绘 Canvas |
| 序列化 | kotlinx.serialization |

## 项目结构

```
app/src/main/java/com/spendlist/app/
├── data/                    # 数据层
│   ├── local/              # Room：DAO / Entity / Converter
│   ├── datastore/          # DataStore 偏好存储
│   ├── remote/             # 汇率 API
│   └── repository/         # Repository 实现
├── domain/                  # 领域层
│   ├── model/              # 领域模型（Subscription、BillingCycle、SubscriptionStatus）
│   ├── repository/         # Repository 接口
│   └── usecase/
│       ├── subscription/   # CRUD、GetTotalSpent
│       ├── renewal/        # RecordRenewal、MarkExpiredSubscriptions
│       ├── stats/          # 分类占比、月度趋势
│       ├── currency/       # 汇率换算
│       └── export/         # 导入导出
├── ui/                      # 表现层
│   ├── screen/             # Home / AddEdit / Detail / Stats / Settings / Category
│   ├── component/          # 可复用 Compose 组件
│   ├── navigation/         # 导航
│   └── theme/              # 主题
├── notification/            # 通知（续约 + 过期聚合）
├── worker/                  # ExpirationCheck / RenewalReminder / ExchangeRateSync
└── util/
```

## 关键领域模型

- **`Subscription`**
  - `status: SubscriptionStatus`：`ACTIVE` / `EXPIRED` / `CANCELLED`
  - `billingCycle: BillingCycle`：`Monthly` / `Quarterly` / `Yearly` / `Custom(days)`
  - `billingDayOfMonth: Int?`：可选 1–31 固定扣款日
  - `paidCycles(): Long`：`[startDate, nextRenewalDate)` 区间的已付周期数
  - `totalPaidAmount: BigDecimal`：历史已付总额 = `paidCycles × amount`
- **`SubscriptionStatus` 转换**（由 Worker 或用户触发）
  - `ACTIVE → EXPIRED`：Worker 每日自动（`nextRenewalDate < today` 时）
  - `EXPIRED → ACTIVE`：用户手动续约且新 `nextRenewalDate ≥ today`
  - `ACTIVE / EXPIRED → CANCELLED`：用户手动标记取消（终态）

## 构建要求

- JDK 17
- Android SDK (compileSdk 36)
- Gradle 8.13

## 构建命令

```bash
./gradlew test          # 运行单元测试（130+ 覆盖）
./gradlew assembleDebug # Debug APK
./gradlew installDebug  # 安装到已连接设备
./gradlew assembleRelease # Release（需配置 keystore.properties）
```

## Release 签名

1. 生成 keystore：
   ```bash
   keytool -genkey -v -keystore spendlist-release.keystore -alias spendlist -keyalg RSA -keysize 2048 -validity 10000
   ```
2. 创建 `keystore.properties`：
   ```properties
   storeFile=spendlist-release.keystore
   storePassword=***
   keyAlias=spendlist
   keyPassword=***
   ```
3. `./gradlew assembleRelease`

## 数据格式

**JSON**：
```json
[
  {
    "name": "Claude Pro",
    "categoryId": 1,
    "amount": "150",
    "currency": "CNY",
    "billingCycleType": "MONTHLY",
    "billingDayOfMonth": 12,
    "startDate": "2024-01-12",
    "nextRenewalDate": "2024-02-12",
    "note": "AI assistant",
    "status": "ACTIVE"
  }
]
```

**CSV**：
```csv
name,amount,currency,billingCycleType,billingCycleDays,startDate,nextRenewalDate,status,categoryId,note,manageUrl
Claude Pro,150,CNY,MONTHLY,,2024-01-12,2024-02-12,ACTIVE,1,AI assistant,https://claude.ai
```

## 提醒测试

通过 adb 触发一次即将到期提醒：

```bash
adb shell am broadcast -a com.spendlist.app.TEST_REMINDER -p com.spendlist.app
```

前置条件：
1. Settings → 到期提醒 → 开启
2. Android 13+ 已授予通知权限
3. 存在 3 天内到期的 `ACTIVE` 订阅

## 验证过期检测

测试"每日过期检查"流程：

1. 新建订阅 A：`startDate = 今天 - 60 天`，月付 → `nextRenewalDate ≈ 今天 - 30 天`
2. 杀掉 App 后重开 → A 应被标为 EXPIRED
3. 系统通知栏：`A 已过期，请续约或取消`；Home 顶部出现红色横幅
4. 打开 A 详情页点 `续约` → 若推进后 ≥ today 则恢复 ACTIVE 且横幅消失
5. 过期订阅的已付金额应出现在 Home 累计花费、Stats 月度趋势（startDate 当月非 0）、Stats 分类占比"历史累计"视图

## 文档

| 阶段 | 文档 |
|---|---|
| Phase 1: 项目骨架 + 核心 CRUD | [docs/handover-phase1.md](docs/handover-phase1.md) |
| Phase 2: 分类系统 + 筛选 | [docs/handover-phase2.md](docs/handover-phase2.md) |
| Phase 3: 多币种 + 汇率 | [docs/handover-phase3.md](docs/handover-phase3.md) |
| Phase 4: 到期提醒 | [docs/handover-phase4.md](docs/handover-phase4.md) |
| Phase 5: 统计报表 | [docs/handover-phase5.md](docs/handover-phase5.md) |
| Phase 6: 导入导出 + i18n | [docs/handover-phase6.md](docs/handover-phase6.md) |
| 构建验证与修复 | [docs/handover-build-fix.md](docs/handover-build-fix.md) |
| 手机验证问题修复 | [docs/handover-mobile-verification.md](docs/handover-mobile-verification.md) |
| 代码审查与修复 | [docs/handover-code-review-fix.md](docs/handover-code-review-fix.md) |
| 季度周期 + 扣款日 + 自动续期 | [docs/handover-quarterly-billing-day-autorenew.md](docs/handover-quarterly-billing-day-autorenew.md) |
| 计费周期布局 + 扣款日选择修复 | [docs/handover-ui-fix-billing.md](docs/handover-ui-fix-billing.md) |
| 废除自动续费 + 每日过期检查 + 历史实付统计 | [docs/handover-manual-renew-expiration-check.md](docs/handover-manual-renew-expiration-check.md) |

## License

MIT License
