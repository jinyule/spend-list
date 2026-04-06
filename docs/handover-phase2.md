# Phase 2 交接文档 — 分类系统 + 筛选

> 完成日期：2026-04-05

## 完成内容

### Domain 层
- **ManageCategoryUseCase**: add/update/delete 三个操作
  - 名称非空校验
  - 名称重复校验（不区分大小写，update 时排除自身）
  - 预设分类不可删除
  - 返回 `Result.Success` / `Result.ValidationError`

### ViewModel
- **CategoryManageViewModel**: 分类管理页面状态管理
  - 自动加载并分离预设/自定义分类
  - onAddCategory / onUpdateCategory / onDeleteCategory / onClearError
- **HomeViewModel**: 新增 `categoryRepository` 依赖
  - 加载分类列表到 `HomeUiState.categories`
  - 分类筛选 Chip 数据源

### UI 组件
| 组件 | 文件 | 说明 |
|---|---|---|
| IconPicker | `ui/component/IconPicker.kt` | 20 个 Material Icon 网格选择器（5列） |
| ColorPicker | `ui/component/ColorPicker.kt` | 16 种预设颜色网格选择器（8列） |
| CategoryManageScreen | `ui/screen/category/CategoryManageScreen.kt` | 完整分类管理页面 |
| CategoryFilterRow | `ui/screen/home/HomeScreen.kt` | 首页分类筛选 Chip 行 |

### CategoryManageScreen 功能
- 预设分类列表（只读，不可编辑/删除）
- 自定义分类列表（可编辑/删除）
- 新增分类对话框（名称 + 图标选择 + 颜色选择）
- 编辑分类对话框（同上）
- 删除确认对话框
- 错误提示 Snackbar

### HomeScreen 分类筛选
- 分类 Chip 行显示在状态筛选行上方
- "全部" + 各分类 Chip
- 点击 Chip 触发 `onCategoryFilterChanged` 筛选

### i18n 新增字符串
- `category_edit`, `category_name`, `category_icon`, `category_color`
- `category_empty_custom`, `category_delete_confirm_title/message`
- `category_cannot_delete_preset`
- `error_category_name_required`, `error_category_name_duplicate`

### 测试 (56 个，全部通过)
| 测试类 | 数量 | 新增 |
|---|---|---|
| BillingCycleTest | 10 | |
| AddSubscriptionUseCaseTest | 9 | |
| GetSubscriptionsUseCaseTest | 5 | |
| DeleteSubscriptionUseCaseTest | 2 | |
| GetUpcomingRenewalsUseCaseTest | 4 | |
| HomeViewModelTest | 4 | |
| AddEditViewModelTest | 5 | |
| **ManageCategoryUseCaseTest** | **11** | **Phase 2** |
| **CategoryManageViewModelTest** | **7** | **Phase 2** |

Phase 2 新增 18 个测试（ManageCategoryUseCase 11 + CategoryManageViewModel 7），总计 57 个。

## 新增文件
```
app/src/main/java/com/spendlist/app/
├── domain/usecase/category/ManageCategoryUseCase.kt
├── ui/screen/category/CategoryManageViewModel.kt
├── ui/component/IconPicker.kt
└── ui/component/ColorPicker.kt

app/src/test/java/com/spendlist/app/
├── domain/usecase/category/ManageCategoryUseCaseTest.kt
└── ui/screen/category/CategoryManageViewModelTest.kt
```

## 修改文件
- `ui/screen/category/CategoryManageScreen.kt` — 从占位替换为完整实现
- `ui/screen/home/HomeViewModel.kt` — 新增 categoryRepository 依赖和分类加载
- `ui/screen/home/HomeScreen.kt` — 新增 CategoryFilterRow
- `values/strings.xml` — 新增分类管理字符串
- `values-zh-rCN/strings.xml` — 新增分类管理中文字符串

## 已知限制
- IconPicker 使用 Material Icons filled 变体的子集（20个），后续可扩展
- ColorPicker 提供 16 种预设颜色，暂不支持自定义颜色输入
- 预设分类名称在 UI 中显示的是 Entity 的 name 字段（英文），尚未通过 nameResKey 做 i18n 动态解析（Phase 6 收尾）

## 下一步：Phase 3 — 多币种 + 汇率
1. 写 ConvertCurrencyUseCase 测试
2. 写 CurrencyRepository 测试
3. 实现 ExchangeRateApi (Retrofit)
4. 实现 CurrencyRepository + UseCase
5. 总花费统一换算显示
6. 设置中的主币种选择和汇率管理
7. ExchangeRateSyncWorker (每日自动同步)
