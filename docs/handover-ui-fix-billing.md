# 交接文档：计费周期布局 + 扣款日选择修复

## 背景

用户安装更新后反馈两个 UI 问题：
1. 计费周期 4 个单选按钮（月付/季付/年付/自定义）挤在一行，"自定义"文字被竖向压缩变形
2. 扣款日字段是纯文本输入，无法直观选择日期

## 已完成的变更

### Fix 1: 计费周期 FlowRow 自动换行

**文件**: `ui/screen/addEdit/AddEditScreen.kt`

- `Row` → `FlowRow`，4 个 RadioButton 在窄屏手机上自动换行为 2+2 布局
- 新增 `verticalArrangement = Arrangement.spacedBy(4.dp)` 控制行间距
- 新增 import: `ExperimentalLayoutApi`, `FlowRow`
- `@OptIn` 注解添加 `ExperimentalLayoutApi::class`

### Fix 2: 扣款日改为下拉选择

**文件**: `ui/screen/addEdit/AddEditScreen.kt`

- 新增私有组件 `BillingDayDropdown`，使用 `ExposedDropdownMenuBox`（与币种/分类下拉风格一致）
- 选项：第一项"不设置"（清除）+ 1-31 共 31 个日期选项
- `readOnly = true`，点击弹出下拉菜单
- ViewModel 无需修改，`onBillingDayChange(String)` 接口完全兼容

**i18n 字符串新增**:
- `field_billing_day_none`: None / 不设置

## 修改文件清单

| 文件 | 变更类型 |
|------|----------|
| `ui/screen/addEdit/AddEditScreen.kt` | FlowRow + BillingDayDropdown 组件 |
| `values/strings.xml` | +1 字符串 (`field_billing_day_none`) |
| `values-zh-rCN/strings.xml` | +1 字符串 (`field_billing_day_none`) |

## 验证结果

- `./gradlew assembleDebug` BUILD SUCCESSFUL
- 真机安装验证通过：计费周期正常换行，扣款日下拉可用
