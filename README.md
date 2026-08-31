# 密匣 MimaVault（Android App）

本地加密密码管理器 · Android 端（原生 Java）

与 PC 端密匣 MimaVault 数据 1:1 互通：`.pmaster` 备份文件、加密参数、数据库结构完全兼容，支持双向导入导出与二维码传输。

> 项目定位：轻量级学习与参赛作品，本地优先，不做云同步，不商业化。

## 功能特性

| 模块 | 说明 |
|------|------|
| 主密码保护 | PBKDF2-SHA256（600000 次迭代）派生密钥，AES-256-GCM 加密存储 |
| 生物识别 | 指纹 / 面部解锁（BiometricPrompt，需设备硬件支持） |
| 条目管理 | 增 / 删 / 改 / 查，分类（网站 / 应用 / 其他） |
| 模糊搜索 | 按平台、账号、手机号即时搜索 |
| 密码工具 | 随机密码生成、强度检测（6 维评分）、复制后 30 秒自动清除剪贴板 |
| 九宫格手势 | 每条目可设置 4+ 点连线手势，详情页展示 |
| 图片附件 | 每条目可附加一张图片，本地存储 |
| 文件互通 | `.pmaster` 导出 / 导入，与 PC 端双向兼容 |
| 二维码传输 | 分段二维码导出 / 扫码或剪贴板导入（zxing） |

## 加密与格式（与 PC 端 1:1）

- 加密算法：AES-256-GCM，密文 `Base64(iv + ciphertext)`
- 密钥派生：PBKDF2WithHmacSHA256，迭代 600000 次，盐 16 字节
- 备份格式：`.pmaster` v2 = `MimaVault1$盐hex$迭代次数$Base64密文`
- 数据库：SQLite，表 `settings` / `entries`（category, platform, account, password_enc, phone, email, note, image_path, gesture_seq, sync_status）

## 使用说明

### 安装

- 要求：Android 7.0（API 24）及以上
- 安装包：`apk/MimaVault-A8.31.1.apk`（开发签名包，安装时需允许"未知来源"）
- 首次启动设置主密码：**主密码即保险库密钥，忘记无法找回**

### 与 PC 端互通

| 方向 | 操作 |
|------|------|
| PC → 安卓 | PC 端导出 `.pmaster` → 传到手机 → 安卓端菜单"导入 .pmaster"（合并模式按平台+账号去重） |
| 安卓 → PC | 安卓端菜单"导出 .pmaster" → 传到 PC → PC 端"导入备份" |
| 二维码 | 安卓端菜单"二维码导出"分页展示 → 另一端"二维码导入"扫码 / 剪贴板粘贴 |

## 构建

```bash
# 环境：JDK 17+、Android SDK 36、Gradle 8.13
gradle assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 目录结构

```
Password-Master-app/
├── app/
│   ├── build.gradle          # 应用构建配置（AGP 8.13）
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mimavault/
│       │   ├── crypto/       # AES-256-GCM 加解密
│       │   ├── db/           # SQLite 数据库
│       │   ├── model/        # 数据模型（Entry / BackupModel）
│       │   ├── service/      # 备份、密码、会话、生物识别
│       │   ├── ui/           # 界面（主界面/详情/编辑/解锁/二维码/手势）
│       │   └── util/         # 工具（剪贴板/手势解析/图片/密码强度）
│       └── res/              # 布局 / 资源
├── apk/
│   └── MimaVault-A8.31.1.apk # Android 安装包
├── CHANGELOG.md              # 更新日志
└── README.md
```

## 更新日志

详见 [CHANGELOG.md](CHANGELOG.md)。

## 免责声明

- 本项目为学习与参赛用途，请勿用于存储重要生产数据
- 密码数据仅存本地，请妥善保管主密码与备份文件
