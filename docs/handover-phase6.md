# Phase 6 交接文档 — 导入导出 + i18n + 收尾

> 完成日期：2026-04-06

## 完成内容

### Domain 层
- **ExportDataUseCase**: 导出订阅数据
  - `exportJson()`: 序列化为 JSON 数组（kotlinx.serialization）
  - `exportCsv()`: 生成 CSV 文件（含表头，支持逗号/引号转义）
  - 空数据时 JSON 返回 `[]`，CSV 返回仅表头
- **ImportDataUseCase**: 导入订阅数据（追加模式，不覆盖已有数据）
  - `importJson(jsonString)`: 解析 JSON 数组，逐条插入
  - `importCsv(csvString)`: 解析 CSV（支持引号包裹字段），逐条插入
  - 返回 `Result.Success(count)` / `Result.Error(message)`
- **SubscriptionExportDto**: 序列化/反序列化中间模型

### UI 层
- **SettingsScreen**: 新增导入导出入口
  - 导出 JSON / 导出 CSV / 导入数据 三个 ListItem
  - 导出通过回调 `onExportData(data, filename)` 传给上层处理
  - 导入通过回调 `onRequestImport()` 触发 SAF 文件选择
- **SettingsViewModel**: 新增 onExportJson/onExportCsv/onImportJson/onImportCsv
- **CategoryName**: `resolvedCategoryName()` Composable 函数
  - 通过 `nameResKey` 动态解析 string resource
  - fallback 到 category.name（自定义分类或解析失败时）

### i18n 动态解析
- HomeScreen 分类筛选 Chip — 使用 `resolvedCategoryName()`
- CategoryManageScreen 分类列表 — 使用 `resolvedCategoryName()`
- 预设分类随系统语言自动切换显示名（如 "AI Tools" ↔ "AI 工具"）

### 测试 (93 个，全部通过)
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
| RenewalCheckLogicTest | 6 | |
| GetSpendingByCategoryUseCaseTest | 4 | |
| GetMonthlyTrendUseCaseTest | 4 | |
| **ExportDataUseCaseTest** | **5** | **Phase 6** |
| **ImportDataUseCaseTest** | **6** | **Phase 6** |

Phase 6 新增 11 个测试，总计 93 个。

## 新增文件
```
app/src/main/java/com/spendlist/app/
├── domain/usecase/export/ExportDataUseCase.kt
├── domain/usecase/export/ImportDataUseCase.kt
└── ui/component/CategoryName.kt

app/src/test/java/com/spendlist/app/
├── domain/usecase/export/ExportDataUseCaseTest.kt
└── domain/usecase/export/ImportDataUseCaseTest.kt
```

## 修改文件
- `ui/screen/settings/SettingsViewModel.kt` — 新增导入导出方法
- `ui/screen/settings/SettingsScreen.kt` — 新增导入导出 UI + 导出/导入回调参数
- `ui/screen/home/HomeScreen.kt` — 分类 Chip 使用 resolvedCategoryName
- `ui/screen/category/CategoryManageScreen.kt` — 分类列表使用 resolvedCategoryName

## 导入导出格式

### JSON 格式
```json
[
  {
    "name": "Claude Pro",
    "categoryId": 1,
    "amount": "150",
    "currency": "CNY",
    "billingCycleType": "MONTHLY",
    "startDate": "2024-01-12",
    "nextRenewalDate": "2024-02-12",
    "note": "AI assistant",
    "manageUrl": "https://claude.ai",
    "status": "ACTIVE"
  }
]
```

### CSV 格式
```
name,amount,currency,billingCycleType,billingCycleDays,startDate,nextRenewalDate,status,categoryId,note,manageUrl
Claude Pro,150,CNY,MONTHLY,,2024-01-12,2024-02-12,ACTIVE,1,AI assistant,https://claude.ai
```

## 已知限制
- 导入导出的文件 I/O（SAF ActivityResultLauncher）需在 Activity/NavHost 层集成，当前 SettingsScreen 仅通过回调暴露接口
- 导入为追加模式，不做去重检查
- CSV 解析仅支持逗号分隔，不支持制表符或其他分隔符

## 项目完成总结

所有 6 个 Phase 已全部完成：

| Phase | 核心功能 | 测试数 |
|-------|---------|--------|
| Phase 1 | 项目骨架 + 核心 CRUD | 39 |
| Phase 2 | 分类系统 + 筛选 | +18 = 57 |
| Phase 3 | 多币种 + 汇率 | +11 = 68 |
| Phase 4 | 到期提醒 | +6 = 74 |
| Phase 5 | 统计报表 | +8 = 82 |
| Phase 6 | 导入导出 + i18n | +11 = 93 |
