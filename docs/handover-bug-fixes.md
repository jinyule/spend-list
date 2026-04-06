# Bug 修复交接文档

> 完成日期：2026-04-06

## 背景

用户验证后续功能后发现四个 Bug，本会话完成修复。

## 修复问题

### 1. 语言切换无限循环导致闪退

**现象**：Settings → Language → 切换语言后，App 无限闪烁

**根因**：`MainActivity` 使用 `LaunchedEffect` 监听 `languageCode` Flow，每次 Flow 发射值时调用 `setLocale()`，而 `setLocale()` 触发 Activity recreate，recreate 后 Flow 再次发射值，形成无限循环

**修复**：移除 `MainActivity` 的自动监听，改为在 `SettingsScreen` 用户主动选择语言时调用 `LocaleHelper.setLocale()`

**影响文件**：`MainActivity.kt`, `SettingsScreen.kt`

---

### 2. 自定义周期天数为 0 导致崩溃

**现象**：存在 days=0 的订阅记录时，打开 App 直接崩溃

**根因**：`BillingCycle.Custom(0)` 时，`monthlyFactor()` 计算 `30.0 / 0.0 = Infinity`，BigDecimal 无法解析导致 `NumberFormatException`

**修复**：
- `BillingCycle.Custom` 添加 `safeDays` 属性，确保返回值 >= 1
- `EntityMappers.kt` 使用 `coerceAtLeast(1)` 兼容旧数据
- `AddEditViewModel.kt` 添加验证，customDays 必须 >= 1

**影响文件**：`BillingCycle.kt`, `EntityMappers.kt`, `AddEditViewModel.kt`, `AddEditScreen.kt`

---

### 3. Settings 页面无法滚动

**现象**：Settings 页面内容过多，底部导出功能无法查看

**根因**：`SettingsScreen` 使用普通 `Column`，缺少 `verticalScroll` modifier

**修复**：添加 `verticalScroll(rememberScrollState())` modifier

**影响文件**：`SettingsScreen.kt`

---

### 4. CSV 导出不可用

**现象**：导出 CSV 文件无法正常使用

**根因**：CSV 导出复用 JSON 的 launcher，MIME 类型固定为 `application/json`

**修复**：为 JSON 和 CSV 分别创建独立的 launcher，CSV 使用 `text/csv` MIME 类型

**影响文件**：`MainActivity.kt`

---

## Git 提交

```
f738849 fix: resolve language switch loop, custom days=0 crash, settings scroll and CSV export
0e0787d feat: add language settings, SAF import/export, notification permission and release build config
```

## 验证结果

| 功能 | 状态 |
|------|------|
| 语言切换 | ✅ 正常切换，无闪烁 |
| 自定义周期验证 | ✅ days < 1 时显示错误 |
| days=0 兼容 | ✅ 自动修正为 1，不再崩溃 |
| Settings 滚动 | ✅ 可滚动查看导出功能 |
| JSON 导出 | ✅ 正常 |
| CSV 导出 | ✅ 正常 |
| 到期提醒 | ✅ Worker 正常执行，通知显示 |

---

## 提醒功能测试方法

通过 adb 发送测试广播触发提醒：

```bash
adb shell am broadcast -a com.spendlist.app.TEST_REMINDER -p com.spendlist.app
```

前置条件：
1. Settings → 到期提醒 → 开启
2. Android 13+ 需授予通知权限
3. 存在 3 天内到期的订阅