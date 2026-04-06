# Phase 3 交接文档 — 多币种 + 汇率

> 完成日期：2026-04-05

## 完成内容

### Domain 层
- **CurrencyRate**: 领域模型（baseCode, targetCode, rate:BigDecimal, isManualOverride）
- **CurrencyRepository**: 接口（getRate, fetchAndCacheRates, setManualRate）
- **ConvertCurrencyUseCase**: 币种转换
  - 同币种直接返回原金额
  - 不同币种通过汇率计算，BigDecimal 精度到 2 位（HALF_UP）
  - 无汇率数据返回 `NoRateAvailable`

### Data 层
- **ExchangeRateApi**: Retrofit 接口，调用 `open.er-api.com/v6/latest/{base}`
- **ExchangeRateResponse**: kotlinx.serialization 数据类
- **CurrencyRepositoryImpl**: 
  - `getRate`: 从本地 Room 缓存读取
  - `fetchAndCacheRates`: 从 API 获取并批量写入 Room
  - `setManualRate`: 手动设置汇率（标记 isManualOverride=true）
- **UserPreferences**: DataStore 封装，管理主币种偏好

### DI 模块
- **NetworkModule**: OkHttpClient + Retrofit + ExchangeRateApi 单例
- **DatabaseModule**: 新增 CurrencyRateDao 提供
- **RepositoryModule**: 新增 CurrencyRepository 绑定

### UI 层
- **HomeViewModel**: 
  - 新增 `convertCurrency` 和 `userPreferences` 依赖
  - `totalMonthlySpend`: 多币种汇总换算后的月度总花费
  - `primaryCurrency`: 主币种设置响应式更新
- **HomeScreen TotalSpendCard**: 显示格式化的总花费（如 ¥2,580.00/月）
- **SettingsViewModel**: 主币种选择 + 汇率同步
- **SettingsScreen**: 完整实现
  - 主币种选择（弹窗列表，11 种货币）
  - 分类管理入口
  - 汇率手动同步按钮 + 同步状态/结果反馈

### 测试 (68 个，全部通过)
| 测试类 | 数量 | 新增 |
|---|---|---|
| BillingCycleTest | 10 | |
| AddSubscriptionUseCaseTest | 9 | |
| GetSubscriptionsUseCaseTest | 5 | |
| DeleteSubscriptionUseCaseTest | 2 | |
| GetUpcomingRenewalsUseCaseTest | 4 | |
| ManageCategoryUseCaseTest | 11 | |
| CategoryManageViewModelTest | 7 | |
| **ConvertCurrencyUseCaseTest** | **5** | **Phase 3** |
| **CurrencyRepositoryImplTest** | **6** | **Phase 3** |
| **HomeViewModelTest** | **5** | **+1 新增** |
| AddEditViewModelTest | 5 | |

Phase 3 新增 12 个测试，总计 68 个。

## 新增文件
```
app/src/main/java/com/spendlist/app/
├── domain/model/CurrencyRate.kt
├── domain/repository/CurrencyRepository.kt
├── domain/usecase/currency/ConvertCurrencyUseCase.kt
├── data/remote/ExchangeRateApi.kt
├── data/repository/CurrencyRepositoryImpl.kt
├── data/datastore/UserPreferences.kt
├── di/NetworkModule.kt
└── ui/screen/settings/SettingsViewModel.kt

app/src/test/java/com/spendlist/app/
├── domain/usecase/currency/ConvertCurrencyUseCaseTest.kt
└── data/repository/CurrencyRepositoryImplTest.kt
```

## 修改文件
- `di/DatabaseModule.kt` — 新增 CurrencyRateDao 提供
- `di/RepositoryModule.kt` — 新增 CurrencyRepository 绑定
- `ui/screen/home/HomeViewModel.kt` — 新增汇率转换和总花费计算
- `ui/screen/home/HomeScreen.kt` — TotalSpendCard 显示换算后总花费
- `ui/screen/settings/SettingsScreen.kt` — 从占位替换为完整实现
- `values/strings.xml` — 新增汇率同步相关字符串
- `values-zh-rCN/strings.xml` — 同上中文版

## 汇率 API
- **端点**: `https://open.er-api.com/v6/latest/{base}`
- **免费**: 无需 API Key，每日更新
- **策略**: 手动触发同步 → 缓存到 Room → 查询时读本地缓存
- **手动覆盖**: `isManualOverride=true` 的记录不会被 API 同步覆盖（Room REPLACE 策略）

## 已知限制
- 汇率同步目前仅手动触发，ExchangeRateSyncWorker（WorkManager 自动同步）在 Phase 4 实现
- 手动覆盖汇率的 UI（汇率管理页面）尚未实现
- 同币种汇总直接相加，无汇率时 fallback 到原金额（不阻塞显示）

## 下一步：Phase 4 — 到期提醒
1. 写 RenewalReminderWorker 测试
2. 实现 Notification Channel + NotificationHelper
3. 实现 RenewalReminderWorker (WorkManager 每日检查)
4. 设置中的提醒开关和提前天数配置
5. ExchangeRateSyncWorker (每日自动同步汇率)
