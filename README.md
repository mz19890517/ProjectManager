# ProjectManager

opencode 项目文件夹可视化管理器。

## 功能

- 查看所有 opencode 项目
- 新建项目（自动创建文件夹 + 对话）
- 删除项目（删除文件夹 + 对话记录）
- 重命名项目
- 文件浏览（面包屑导航）
- 新建文件/文件夹
- 重命名、删除、复制、剪切、粘贴
- 对话管理（查看、新建、删除）
- 对话绑定/换绑到不同文件夹

## 构建

推送到 `master` 分支后 GitHub Actions 自动构建 APK。

手动构建：
```bash
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`
