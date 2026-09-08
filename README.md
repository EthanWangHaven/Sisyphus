# Sisyphus / Lumi

一款个人生活管理 Android 应用，集成专注计时、习惯打卡、笔记、瞬间记录、任务待办、健身记录、音乐播放等功能。

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **架构**：MVVM + Hilt 依赖注入
- **数据库**：Room
- **导航**：Navigation Compose
- **3D 渲染**：SceneView 2.3.0（基于 Google Filament）
- **后端**：Python FastAPI（`backend/` 目录）

## 主要功能

| 模块 | 说明 |
|------|------|
| 专注 | 番茄计时，支持工作/学习/阅读/冥想/撸宠/吸烟/打印等场景 |
| 习惯 | 每日习惯打卡，连续记录统计 |
| 笔记 | 图文笔记编辑与管理 |
| 瞬间 | 随手记录生活瞬间，支持图片 |
| 任务 | 待办清单，看板卡片 |
| 健身 | 运动记录与追踪 |
| 音乐 | 在线音乐搜索、播放、歌单同步 |

## 专注页特色场景

### 赛博撸宠

3D 模型宠物（猫/狗），支持滑动撸动（爱心粒子反馈）、喂食、切换宠物种类。

**当前状态：有待完善**——模型交互细节、动画丰富度等方面仍在迭代中。

### 赛博吸烟

3D 圆柱体香烟（普通/细支/雪茄三种款式）+ Canvas 烟雾粒子叠加，支持按住吸入（火星变亮、烟灰增长、蓄力光环）、松开吐圈、弹灰、换款式。

**当前状态：有待完善**——3D 渲染效果、烟雾粒子物理、交互手感等方面仍在迭代中。

## 构建

```bash
# 编译 Debug APK
./gradlew assembleDebug

# 产物路径
app/build/outputs/apk/debug/Sisyphus-<versionName>.apk
```

## 项目结构

```
Sisyphus/
├── app/                    # Android 应用
│   └── src/main/java/cn/wangce/lumi/
│       ├── ui/
│       │   ├── focus/       # 专注页（含赛博撸宠、赛博吸烟、打印）
│       │   ├── habits/     # 习惯打卡
│       │   ├── notes/      # 笔记
│       │   ├── moments/    # 瞬间记录
│       │   ├── tasks/      # 任务待办
│       │   ├── workout/    # 健身
│       │   ├── music/      # 音乐
│       │   ├── settings/   # 设置
│       │   └── components/ # 通用 UI 组件
│       ├── data/           # Room 数据库、DataStore
│       ├── music/          # 音乐上传与播放服务
│       ├── navigation/     # 导航图
│       └── di/             # Hilt 依赖注入
├── backend/                # Python FastAPI 后端
└── build.gradle.kts
```
