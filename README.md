# 氪金清单 (Spend List)

一款 Android 订阅管理应用，帮助用户追踪和管理各类订阅服务的花费。

## 功能特性

### 核心功能
- **订阅管理**：添加、编辑、删除订阅服务
- **分类系统**：预设分类 + 自定义分类，支持多语言
- **多币种支持**：支持多种货币，自动汇率转换
- **到期提醒**：自定义提醒天数（3天前/1天前/当天）
- **统计报表**：分类占比图表、月度趋势分析
- **导入导出**：JSON/CSV 格式，支持数据备份与迁移

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
| 架构 | MVVM + Clean Architecture |
| 依赖注入 | Hilt |
| 数据库 | Room |
| 数据存储 | DataStore (Preferences) |
| 异步 | Kotlin Coroutines + Flow |
| 后台任务 | WorkManager |
| 网络请求 | Retrofit + OkHttp |
| 图表 | Vico |
| 序列化 | kotlinx.serialization |

## 项目结构

```
app/src/main/java/com/spendlist/app/
├── data/                    # 数据层
│   ├── local/              # 本地数据源
│   │   ├── dao/           # Room DAO
│   │   ├── entity/        # 数据库实体
│   │   └── converter/     # 实体转换器
│   ├── datastore/         # DataStore 偏好存储
│   ├── remote/            # 远程数据源
│   └── repository/        # Repository 实现
├── domain/                  # 领域层
│   ├── model/             # 领域模型
│   ├── repository/        # Repository 接口
│   └── usecase/           # 用例
├── ui/                      # 表现层
│   ├── navigation/        # 导航
│   ├── screen/            # 页面
│   ├── component/         # 可复用组件
│   └── theme/             # 主题
├── notification/            # 通知
├── worker/                  # 后台任务
└── util/                    # 工具类
```

## 构建要求

- JDK 17
- Android SDK (compileSdk 36)
- Gradle 8.13

## 构建命令

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建（需先配置 keystore.properties）
./gradlew assembleRelease

# 运行测试
./gradlew test
```

## Release 构建

1. 生成密钥文件：
```bash
keytool -genkey -v -keystore spendlist-release.keystore -alias spendlist -keyalg RSA -keysize 2048 -validity 10000
```

2. 创建 `keystore.properties`：
```properties
storeFile=spendlist-release.keystore
storePassword=your_store_password
keyAlias=spendlist
keyPassword=your_key_password
```

3. 构建发布版本：
```bash
./gradlew assembleRelease
```

## 数据导入导出

### 导出格式

**JSON 格式**：
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
    "status": "ACTIVE"
  }
]
```

**CSV 格式**：
```csv
name,amount,currency,billingCycleType,billingCycleDays,startDate,nextRenewalDate,status,categoryId,note,manageUrl
Claude Pro,150,CNY,MONTHLY,,2024-01-12,2024-02-12,ACTIVE,1,AI assistant,https://claude.ai
```

## 提醒功能测试

通过 adb 触发测试提醒：

```bash
adb shell am broadcast -a com.spendlist.app.TEST_REMINDER -p com.spendlist.app
```

前置条件：
1. Settings → 到期提醒 → 开启
2. Android 13+ 需授予通知权限
3. 存在 3 天内到期的订阅

## 文档

- [Phase 1 交接](docs/handover-phase1.md) - 项目骨架 + CRUD
- [Phase 2 交接](docs/handover-phase2.md) - 分类系统
- [Phase 3 交接](docs/handover-phase3.md) - 多币种 + 汇率
- [Phase 4 交接](docs/handover-phase4.md) - 到期提醒
- [Phase 5 交接](docs/handover-phase5.md) - 统计报表
- [Phase 6 交接](docs/handover-phase6.md) - 导入导出 + i18n
- [Bug 修复交接](docs/handover-bug-fixes.md) - 问题修复记录

## License

MIT License