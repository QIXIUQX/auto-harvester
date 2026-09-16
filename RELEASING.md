# 发版约定

本模组需要跟随 Minecraft 版本多次发布，因此**版本号、Git 标签、Release 标题和 jar 文件名都要能直接看出对应的 Minecraft 版本**，避免出现「标签只写 0.0.3，看不出兼容哪个 MC 版本」的情况。

## 命名格式

统一使用 `<模组版本>+<Minecraft 版本>`，例如 `0.0.3+26.3`。

`+` 后面是构建元数据，写法沿用 Fabric API 自己的命名（例如 `0.160.6+26.3`），Fabric Loader 与 GitHub 都接受这种版本号。

| 位置 | 格式 | 示例 |
|------|------|------|
| `gradle.properties` 里的版本号 | `<模组版本>+<MC 版本>` | `0.0.3+26.3` |
| jar 文件名 | `auto-harvester-<完整版本>.jar` | `auto-harvester-0.0.3+26.3.jar` |
| Git 标签 | `<完整版本>`，不带 `v` 前缀（与已有的 `0.0.1`、`0.0.2` 保持一致） | `0.0.3+26.3` |
| Release 标题 | `v<完整版本>`（与已有的 `v0.0.1`、`v0.0.2` 保持一致） | `v0.0.3+26.3` |
| Release 正文 | 首行固定写「适用版本：Minecraft 26.3（Fabric）」，其后接 CHANGELOG 对应小节 | 见下 |
| 附件 | 上传主 jar（`auto-harvester-<完整版本>.jar`） | `auto-harvester-0.0.3+26.3.jar` |

> 历史标签 `0.0.1`、`0.0.2` 没有带 MC 版本。已推送的标签不重命名（改名会让引用失效），约定从下一个版本起生效。

## 版本号来源（待实施）

约定由 Gradle 自动拼接，避免 `gradle.properties` 里两处版本号各写一遍、升级时漏改：

- `gradle.properties`：改为 `mod_version=0.0.3`（只写模组版本，不再写完整版本号）
- `build.gradle`：`version = "${mod_version}+${minecraft_version}"`

这样升级 Minecraft 时只需要改 `minecraft_version`，jar 文件名和 `fabric.mod.json` 里的 `version`（走 `${version}` 占位符）都会自动带上 MC 版本。

**当前状态**：`gradle.properties` 仍是 `version=0.0.3` 的旧写法，上述改动留到下次发版时一起做。

## 发版步骤

1. 更新 `gradle.properties`：`minecraft_version`、`loader_version`、`loom_version`、`fabric_api_version`，以及模组版本号
2. 在 `CHANGELOG.md` 顶部补上该版本的小节
3. `./gradlew build` 并确认产物 `build/libs/` 里的 jar 文件名带上了 MC 版本
4. 提交并让 `dev` 与 `master` 指向同一个提交
5. 打标签并推送分支与标签：

   ```bash
   git tag 0.0.3+26.3
   git push origin dev master
   git push origin 0.0.3+26.3
   ```

6. 创建 GitHub Release：标题 `v0.0.3+26.3`，正文首行「适用版本：Minecraft 26.3（Fabric）」+ CHANGELOG 小节 + 环境要求，并上传主 jar

## 已有版本的适用版本对照

| 版本 | 标签 | 适用 Minecraft |
|------|------|----------------|
| 0.0.1 | `0.0.1` | 26.2 |
| 0.0.2 | `0.0.2` | 26.2 |
| 0.0.3 | （尚未发布） | 26.3 |
