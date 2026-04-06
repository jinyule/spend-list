# Phase 1 交接文档 — 项目骨架 + 核心 CRUD

> 完成日期：2026-04-05

## 完成内容

### 项目初始化
- Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`)
- AGP 8.9.1, Kotlin 2.1.20, Compose BOM 2025.04.00
- compileSdk 36, minSdk 26, targetSdk 36

### 架构 (MVVM + Clean Architecture，单模块包分层)
```
com.spendlist.app/
├── data/local/         Entity, DAO, Database, EntityMappers
├── data/repository/    SubscriptionRepositoryImpl, CategoryRepositoryImpl
├── domain/model/       Subscription, Category, BillingCycle, Currency, SubscriptionStatus
├── domain/repository/  SubscriptionRepository, CategoryRepository (接口)
├── domain/usecase/     Add/Get/Update/Delete/GetById/GetUpcoming Subscription
├── ui/theme/           MD3 Theme (亮/暗 + 动态取色)
├── ui/navigation/      Screen routes, BottomNavBar, SpendListNavHost
├── ui/screen/          home/, addEdit/, detail/, stats/, settings/, category/
├── ui/component/       SubscriptionCard
├── di/                 AppModule, DatabaseModule, RepositoryModule
└── SpendListApplication.kt
```

### 数据库 (Room)
- **SubscriptionEntity**: name, categoryId(FK), amount(String), currencyCode, billingCycleType, billingCycleDays, startDate, nextRenewalDate, note, manageUrl, iconUri, status, createdAt, updatedAt
- **CategoryEntity**: name, nameResKey, iconName, color, isPreset, sortOrder
- **CurrencyRateEntity**: baseCode, targetCode, rate, isManualOverride, updatedAt
- 预设分类种子数据：AI工具/基础设施/娱乐/工具/云服务/域名/存储/其他

### UI 页面
| 页面 | 状态 | 说明 |
|---|---|---|
| HomeScreen | ✅ 完整 | 总花费卡片 + 状态筛选 Chip + 订阅卡片列表 + FAB |
| AddEditScreen | ✅ 完整 | 全字段表单（名称/分类/金额/币种/周期/日期/链接/备注） |
| DetailScreen | ✅ 完整 | 信息展示 + 管理链接跳转 + 标记取消/删除 |
| StatsScreen | 🔲 占位 | Phase 5 实现 |
| SettingsScreen | 🔲 占位 | Phase 6 实现 |
| CategoryManageScreen | 🔲 占位 | Phase 2 实现 |

### i18n
- `values/strings.xml` (英文，默认)
- `values-zh-rCN/strings.xml` (简体中文)
- 完整覆盖所有 UI 文本

### 测试 (39 个，全部通过)
| 测试类 | 数量 |
|---|---|
| BillingCycleTest | 10 |
| AddSubscriptionUseCaseTest | 9 |
| GetSubscriptionsUseCaseTest | 5 |
| DeleteSubscriptionUseCaseTest | 2 |
| GetUpcomingRenewalsUseCaseTest | 4 |
| HomeViewModelTest | 4 |
| AddEditViewModelTest | 5 |

## 构建环境

| 项目 | 值 |
|---|---|
| JDK | 17.0.2 (`D:/Software/JDK/jdk-17.0.2`，配置在 gradle.properties) |
| Android SDK | `D:/Software/andriod-studio/andriod-sdk` (local.properties) |
| 可用平台 | android-34, android-36 |
| Build Tools | 34.0.0, 35.0.0, 36.1.0 |

**注意**: Java 24 与 Gradle 8.13 测试任务不兼容，必须用 JDK 17 运行 `./gradlew test`。

## 已知限制
- HomeScreen 总花费卡片目前只显示订阅数量，尚未做多币种汇总（Phase 3）
- AddEditScreen 日期选择器是只读 TextField，尚未集成 DatePicker（可在后续优化）
- 图标选择目前仅使用首字母，Phase 2 将加入 Material Icon 选择器
