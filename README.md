# Allineat｜全能厨师

以食材为起点的离线菜肴组合工具。首版提供常见食材选择、用量与切法调整、调味搭配、烹饪方式组合、餐具选择和本地规则生成菜肴建议。

## 当前版本

- Android 原生应用：Kotlin + Jetpack Compose + Material 3
- 中式 / 西式风格切换
- 本地食材库与搜索、分类筛选
- 食材用量、切法调整
- 调味料多选
- 多种烹饪方式组合
- 餐具选择与菜肴步骤生成
- 不依赖在线 AI API

## 构建

GitHub Actions 会在推送到 `main` 或手动触发时构建 Debug APK。构建完成后，在 Actions 对应运行记录的 Artifacts 中下载 `Allineat-debug-apk`。

本地构建需要 JDK 17 和 Gradle 8.9：

```bash
gradle assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

## 说明

当前菜谱生成为规则原型，不等同于经过营养学或食品安全验证的专业菜谱。肉类、鱼类和海鲜的熟制程度应遵循可靠的食品安全指南。
