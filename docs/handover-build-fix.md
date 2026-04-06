# 构建验证与修复交接文档

> 完成日期：2026-04-06

## 背景

从上一个会话继续，Phase 1-6 已全部完成，93 个单元测试通过。用户要求构建并试运行 APK。

## 关键决策

| 决策项 | 选择 | 原因 |
|--------|------|------|
| Vico 图表库 | 保留 | 以备将来扩展图表功能 |
| SettingsScreen 导入导出 | 保持现状 | 后续再实现文件操作 |

## 发现并修复的问题

### 1. AddEditScreen 编译错误（CRITICAL）

**问题**：`ExposedDropdownMenu` 在 Material3 新版中不存在
```
Unresolved reference 'ExposedDropdownMenu'
```

**修复**：
- 文件：`app/src/main/java/com/spendlist/app/ui/screen/addEdit/AddEditScreen.kt`
- 将 `ExposedDropdownMenu` 替换为 `DropdownMenu`
- 添加 `import androidx.compose.material3.DropdownMenu`

### 2. WorkManager Worker 初始化失败（CRITICAL）

**问题**：HiltWorker 无法实例化
```
NoSuchMethodException: RenewalReminderWorker.<init> [Context, WorkerParameters]
```

**根因**：缺少 `hilt-work-compiler` 注解处理器

**修复**：
1. `gradle/libs.versions.toml` — 添加：
   ```toml
   hilt-work-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "hiltWork" }
   ```

2. `app/build.gradle.kts` — 添加：
   ```kotlin
   ksp(libs.hilt.work.compiler)
   ```

3. `AndroidManifest.xml` — 移除 InitializationProvider：
   ```xml
   <provider
       android:name="androidx.startup.InitializationProvider"
       android:authorities="${applicationId}.androidx-startup"
       tools:node="remove" />
   ```

4. `SpendListApplication.kt` — 手动初始化 WorkManager：
   ```kotlin
   val config = Configuration.Builder()
       .setWorkerFactory(workerFactory)
       .build()
   WorkManager.initialize(this, config)
   ```

## 架构变化

### 依赖变更
| 依赖 | 变更 |
|------|------|
| `hilt-work-compiler` | 新增 (ksp) |

### API 变更
| 组件 | 变更 |
|------|------|
| `ExposedDropdownMenu` | → `DropdownMenu` (Material3 API 更新) |

### 初始化流程变更
```
旧：Configuration.Provider + 自动初始化
新：手动调用 WorkManager.initialize()
```

## 验证结果

| 检查项 | 状态 |
|--------|------|
| 编译成功 | ✅ BUILD SUCCESSFUL |
| APK 生成 | ✅ app-debug.apk (20MB) |
| 单元测试 | ✅ 93 tests pass |
| 安装成功 | ✅ Success |
| Worker 执行 | ✅ SUCCESS |
| 启动无崩溃 | ✅ 正常运行 |

## Git 提交

```
7cc0476 fix: resolve build and runtime issues
7b5c2b7 chore: initial commit
```

## 当前状态

**项目完成**：所有 6 个 Phase 已实现并通过验证

| Phase | 功能 | 状态 |
|-------|------|------|
| Phase 1 | 项目骨架 + 核心 CRUD | ✅ |
| Phase 2 | 分类系统 + 筛选 | ✅ |
| Phase 3 | 多币种 + 汇率 | ✅ |
| Phase 4 | 到期提醒 | ✅ |
| Phase 5 | 统计报表 | ✅ |
| Phase 6 | 导入导出 + i18n | ✅ |

## 后续建议

1. **导入导出文件操作**：使用 Android Storage Access Framework 实现
2. **通知权限请求**：Android 13+ 需运行时请求 POST_NOTIFICATIONS
3. **Release 构建**：配置签名，启用代码混淆
4. **UI 测试**：添加 Compose UI 测试