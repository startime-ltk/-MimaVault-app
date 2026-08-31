# 更新日志

## A8.30.1 UI 修复（2026-08-31）

修复多处"文字与背景颜色相近导致看不清"的显示问题。

### 修复

- 根因：Material3 主题下 `Button` 被替换为 `MaterialButton`，默认 backgroundTint 覆盖自定义背景 drawable，导致浅紫底按钮实际显示深紫底、深色文字看不清
- 全部布局 Button 增加 `app:backgroundTint="@null"`，恢复自定义背景（浅紫 chip / 白底 / 渐变底）正常显示
- 详情页平台名：白字 → 深色（原在浅灰背景上不可见）
- 详情页分类标签：白字 → 深紫
- 主界面 ☰ 菜单按钮：白字 → 深紫
- 主界面版本号：半透明白 → 纯白
- 主界面动态分类 chip：白字 → 灰色

## A8.30.1（2026-08-31）

Android 端首个可交付版本，与 PC 端数据互通闭环。

### 新增功能

- 主密码保护：PBKDF2-SHA256（600000 次迭代）+ AES-256-GCM 加密，与 PC 端 1:1 兼容
- 生物识别解锁：指纹 / 面部（BiometricPrompt）
- 条目管理：新增 / 编辑 / 删除 / 详情，分类（网站 / 应用 / 其他）
- 模糊搜索：按平台、账号、手机号实时过滤
- 密码工具：随机生成、强度检测（6 维评分）、复制 30 秒后自动清除剪贴板
- 九宫格手势：条目级手势设置与详情页展示
- 图片附件：条目附加图片，本地存储
- 文件互通：`.pmaster` 导出 / 导入（与 PC 端 Password Master 双向兼容，合并去重 / 覆盖两种模式）
- 二维码传输：分段二维码导出（zxing）、扫码 / 剪贴板粘贴导入

### 修复

- 修复 PC → 安卓导入失败：`BackupService.importFromContent` 抛出 `AEADBadTagException: BAD_DECRYPT` 时静默吞异常，改为 `Log.e` 输出真实异常，便于定位
- 修复 `MainActivity.doImport` 导入结果无日志，补充 `importFromContent OK` / `restore OK` 日志

### 技术栈

- 原生 Java，minSdk 24 / targetSdk 36
- Android Gradle Plugin 8.13，Gradle 8.13
- zxing 3.5.3 + zxing-android-embedded 4.3.0（二维码）
- Biometric 1.1.0（生物识别）
