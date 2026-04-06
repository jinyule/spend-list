# Phase 5 交接文档 — 统计报表

> 完成日期：2026-04-06

## 完成内容

### Domain 层
- **GetSpendingByCategoryUseCase**: 按分类汇总活跃订阅的月度花费
  - 过滤已取消订阅
  - 多币种通过 ConvertCurrencyUseCase 统一换算
  - 返回 Flow<List<CategorySpending>>（含分类名、颜色、金额、百分比）
  - 按金额降序排列
- **GetMonthlyTrendUseCase**: 近 12 个月花费趋势
  - 判断订阅在每月是否活跃（startDate <= month）
  - 多币种统一换算
  - 返回 Flow<List<MonthlySpending>>（含 YearMonth 和金额）

### UI 组件（全部 Canvas 自绘，无第三方图表库）
| 组件 | 文件 | 说明 |
|---|---|---|
| DonutChart | `ui/component/DonutChart.kt` | 环形图，中间显示总金额 |
| SimpleLineChart | `ui/component/SimpleLineChart.kt` | 折线图，带网格线和数据点 |
| SimpleBarChart | `ui/component/SimpleBarChart.kt` | 柱状图，自动缩放高度 |

### StatsScreen（Tab 切换）
| Tab | 内容 |
|---|---|
| 分类占比 | DonutChart + 分类明细列表（颜色点 + 名称 + 金额 + 百分比） |
| 月度趋势 | SimpleLineChart 显示近 12 个月花费折线 |
| 分类对比 | SimpleBarChart 按分类金额柱状对比 |

### ViewModel
- **StatsViewModel**: 加载主币种偏好，订阅 getSpendingByCategory 和 getMonthlyTrend 两个 Flow

### 测试 (82 个，全部通过)
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
| **GetSpendingByCategoryUseCaseTest** | **4** | **Phase 5** |
| **GetMonthlyTrendUseCaseTest** | **4** | **Phase 5** |

Phase 5 新增 8 个测试，总计 82 个。

## 新增文件
```
app/src/main/java/com/spendlist/app/
├── domain/usecase/stats/GetSpendingByCategoryUseCase.kt
├── domain/usecase/stats/GetMonthlyTrendUseCase.kt
├── ui/screen/stats/StatsViewModel.kt
├── ui/component/DonutChart.kt
├── ui/component/SimpleLineChart.kt
└── ui/component/SimpleBarChart.kt

app/src/test/java/com/spendlist/app/
├── domain/usecase/stats/GetSpendingByCategoryUseCaseTest.kt
└── domain/usecase/stats/GetMonthlyTrendUseCaseTest.kt
```

## 修改文件
- `ui/screen/stats/StatsScreen.kt` — 从占位替换为完整 Tab 切换页面

## 设计决策
- **图表全部 Canvas 自绘**：DonutChart ~50 行、LineChart ~80 行、BarChart ~50 行，避免引入 Vico 等第三方库
- **月度趋势判定逻辑**：订阅的 startDate <= 当月即认为该月活跃，简化为固定月费模型

## 已知限制
- 图表不支持手势交互（缩放、拖拽、点击详情）
- 月度趋势未考虑订阅取消日期（仅按当前状态判断）
- 分类占比百分比四舍五入到整数显示，总和可能不严格等于 100%

## 下一步：Phase 6 — 导入导出 + i18n + 收尾
1. 写 Export/Import UseCase 测试
2. 实现 JSON/CSV 导出导入
3. 预设分类 i18n 动态解析
4. UI 打磨
