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

### 专注

番茄计时核心，支持工作、阅读、学习、运动、冥想五大常规场景，以及赛博撸宠、赛博吸烟两个 3D 互动场景（详见下文）。计时会话提升为全局单例 `FocusSessionManager`，页面与通知栏遥控共享同一次计时。支持开始/暂停/继续/结束/放弃完整生命周期，计时结束自动落库并可查看历史统计。另含「打印」功能：将专注记录生成小票样式（点阵打印机字体），编辑勾选项目和时长后预览打印动画并保存到相册。支持自定义专注分类。

### 习惯

每日习惯打卡与追踪。预设跑步、阅读、写作、冥想、健身、钢琴、喝水、早起等图标库，也可自定义名称和 emoji。点击即切换当日打卡状态（再点取消），数据库唯一索引兜底防重复。支持软删除（打卡历史保留但不展示）。月历视图查看打卡连续记录，直观展示每日完成情况。

### 笔记

图文笔记编辑与管理。两列瀑布流卡片展示，顶部分类标签筛选（支持自定义标签）。编辑页支持富文本输入、最多 9 张图片插入，停止输入 1 秒自动保存，返回时强制保存。笔记与待办共用「备忘录」页面，底部分段切换，黄色 FAB 快速新建。支持左滑删除。

### 瞬间

随手记录生活瞬间，瀑布流时间线展示。支持多图片（最多 9 张）、位置标注。编辑使用底部 ModalBottomSheet，支持图片预览和删除。左滑删除带二次确认。时间显示为相对时间格式（如「3 分钟前」）。与笔记模块共享图片存储和部分 UI 组件。

### 任务

待办清单管理，首页展示欢迎区 + 周历 + 环形进度统计卡 + 未完成待办列表。支持搜索筛选、优先级标记、完成/取消切换。集成实时天气（Open-Meteo API，失败时优雅隐藏）。待办添加/筛选入口在「备忘录和待办」页，首页仅展示未完成项，完成后自动归档。支持左滑删除和批量管理。

### 健身

运动记录与追踪，双 Tab 设计（概览 + 统计）。概览 Tab：月历卡（有运动日显示彩色圆点）+ 当日记录列表 + 锻炼时间汇总卡 + 本月之最卡（最长时长/最高强度等）。统计 Tab：支持周/月/年范围切换，汇总卡展示总时长和次数，横向条形图展示各项运动占比。预设跑步、骑行、步行、游泳、瑜伽、篮球、徒步等运动类型，支持自定义标签。记录通过 BottomSheet 完成：选项目 → 选时长 → 选强度 → 保存。

### 音乐

在线音乐播放与管理。播放控制统一收敛在全局 `MusicPlayerManager`（持有 MediaPlayer，管理播放/切歌/进度/循环/定时），前台服务 `MusicService` 负责保活与通知。支持列表循环/单曲循环切换、睡眠定时关闭（分钟数可选）、进度拖拽跳转。歌单同步：拉取个人网站 `playlist.json` 按 URL 去重追加新曲并持久化。封面自动匹配：无自定义封面的曲目逐首搜索（网易云优先、QQ 音乐兜底）。音乐上传：支持网易云歌曲 ID 解析（Meting API 获取歌名/歌手/音源直链）或本地文件上传，歌词自动获取（LRCLIB 检索，简体未命中转繁体重试），通过 GitHub Contents API 提交到网站仓库。

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

## 音乐上传 Token 配置

音乐上传功能通过 GitHub Contents API 将音源和歌单提交到网站仓库（`EthanWangHaven.github.io`），需要一个有 `repo` 权限的 GitHub Personal Access Token。

> **安全提示**：Token 绝不应硬编码在源码中。项目已移除硬编码 token，改为通过 BuildConfig 从 Gradle 属性注入。

### 配置步骤

1. **创建 GitHub Token**

   - 打开 [GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)](https://github.com/settings/tokens)
   - 点击「Generate new token (classic)」
   - 勾选 `repo` 权限（Full control of private repositories）
   - 生成后复制 token（格式 `ghp_xxxx...`）

2. **注入 Token 到构建**

   在项目根目录的 `gradle.properties`（或 `~/.gradle/gradle.properties`）中添加：

   ```properties
   GH_TOKEN=ghp_你的实际token
   ```

   构建时 Gradle 会读取此属性，通过 `buildConfigField` 注入到 `BuildConfig.GH_TOKEN`，运行时 `MusicUploader` 从 `BuildConfig.GH_TOKEN` 获取。

3. **确认 `.gitignore` 已排除 `gradle.properties`**

   项目根目录的 `gradle.properties` 通常已被 `.gitignore` 排除（包含 SDK 路径等本地配置）。确保你的 token 不会被提交：

   ```bash
   grep gradle.properties .gitignore
   ```

   如果没有排除，在 `.gitignore` 中添加 `gradle.properties`。

4. **验证**

   构建并安装后，进入音乐页尝试添加歌曲。如果 token 未配置，上传时会提示「未配置 GitHub Token」错误。配置正确则可正常上传音源和更新歌单。

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
