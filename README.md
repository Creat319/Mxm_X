<div align="center">

# McmX · Backstabbed! 扩展身份

**Backstabbed!（谁是杀手）数据包 + Paper 1.21.4 扩展插件**
**Backstabbed! Murder Mystery datapack + a Paper 1.21.4 role-expansion plugin**

![Paper](https://img.shields.io/badge/Paper-1.21.4-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![License](https://img.shields.io/badge/License-MIT-green)
![Version](https://img.shields.io/badge/Version-4.2-blueviolet)

[中文](#chinese) · [English](#english)

</div>

---

<a name="chinese"></a>
## 中文

本仓库包含两个部分：

| 目录 | 说明 |
|---|---|
| [`mcm/`](mcm/) | **Backstabbed!** 数据包（已包含 McmX 所需的配合改动） |
| [`mcm_x_plugin/`](mcm_x_plugin/) | **Paper 1.21.4** 插件，实现 侦探 / 奶龙 / 眼线 / 净化者 / 赌徒 / 黑庄 等扩展身份 |

> 原项目 **Backstabbed!**（数据包 + 地图）由 Bagel Buddies 制作，原作者链接：https://www.planetminecraft.com/project/backstabbed/

### 职业一览

| 身份 | 阵营 | 核心技能 |
|---|---|---|
| 侦探 Detective | 好人 | 3 碎片解锁查询（2 次）；死亡时播报凶手 |
| 奶龙 Milk Dragon | 坏人 | 山羊角：半径 20 内好人 反胃 / 缓慢III / 失明III（8 秒） |
| 眼线 Spy | 坏人 | 3 碎片查探，目标 10 秒失明 + 发光 |
| 净化者 Purifier | 好人 | 净化器清除周围 反胃 / 失明 / 缓慢，用后暴露 5 秒 |
| 赌徒 Gambler | 好人 | 10 碎片押注"玩家 + 坏人身份"，猜错当场死亡 |
| 黑庄 Black Dealer | 坏人 | 5 碎片伪装成随机坏人身份 |
| 枪手 / 平民 / 杀手 | — | 数据包原有身份 |

### 常用指令

| 指令 | 说明 |
|---|---|
| `/mcmx query [玩家]` | 侦探 / 眼线查探身份 |
| `/mcmx vote [身份] [on\|off]` | 开局身份投票 |
| `/mcmx preset <玩家> <身份>` | 预设下一局身份 |
| `/mcmx role <身份> <on\|off>` | 开关单个身份 |
| `/mcmx role special <on\|off>` | 一键开关所有特殊身份 |
| `/mcmx version [check]` | 查看版本 / 手动检查更新 |
| `/mcmx pg <玩家>` | 管理员：切换免出图保护（出图不被传送/判死） |
| `/mcmx adventure` | 卡旁观时切回冒险模式并回大厅（仅非游戏进行时） |
| `/mcmx reload` | 重载配置 |

插件内置更新检查：每小时查询一次版本接口，普通更新只在每天 12:00 提醒，紧急 bug 立即警告；
也可用 `/mcmx version` 查看版本、`/mcmx version check` 手动检查。

完整职业说明与指令用法见 [插件 README](mcm_x_plugin/README.md)。

### 快速开始

1. 构建插件：
   ```bash
   cd mcm_x_plugin
   mvn -DskipTests package
   ```
   产物：`mcm_x_plugin/target/McmX-4.2.jar`
2. 把 `McmX-4.2.jar` 放进服务器 `plugins/`。
3. 把 `mcm/` 整个文件夹放进 `world/datapacks/`。
4. `/reload` 或重启服务器。

详细功能、命令、配置见 [插件 README](mcm_x_plugin/README.md)（中英双语）。

### 环境要求

- Paper 1.21.4（数据包 `pack_format 61`）
- Java 21
- （可选）数据包配套资源包

### 许可证

- 插件：MIT，见 [`mcm_x_plugin/LICENSE`](mcm_x_plugin/LICENSE)。
- 数据包 **Backstabbed!**：MIT，作者 Bagel Buddies。原项目（数据包/地图）：https://www.planetminecraft.com/project/backstabbed/

---

<a name="english"></a>
## English

This repository contains two parts:

| Folder | Description |
|---|---|
| [`mcm/`](mcm/) | The **Backstabbed!** datapack (already patched with the McmX integration changes). |
| [`mcm_x_plugin/`](mcm_x_plugin/) | The **Paper 1.21.4** plugin adding Detective / Milk Dragon / Spy / Purifier / Gambler / Black Dealer. |

> The original **Backstabbed!** datapack & map is made by Bagel Buddies: https://www.planetminecraft.com/project/backstabbed/

### Roles

| Role | Side | Core ability |
|---|---|---|
| Detective | Good | 3 fragments unlock a 2-use check; announces the killer on death |
| Milk Dragon | Evil | Goat horn: Nausea / Slowness III / Blindness III (8s) to nearby good players |
| Spy | Evil | 3 fragments to scan; the target gets Blindness + Glowing |
| Purifier | Good | Purifier removes Nausea/Blindness/Slowness nearby; reveals self for 5s |
| Gambler | Good | Bet 10 fragments on a player + evil role; a wrong guess kills you |
| Black Dealer | Evil | 5 fragments for a random fake evil identity |
| Gunner / Innocent / Murderer | — | Original datapack roles |

### Commands

| Command | Description |
|---|---|
| `/mcmx query [player]` | Detective / Spy identity check |
| `/mcmx vote [role] [on\|off]` | Role vote |
| `/mcmx preset <player> <role>` | Preset a next-round role |
| `/mcmx role <role> <on\|off>` | Toggle a single role |
| `/mcmx role special <on\|off>` | Toggle all special roles |
| `/mcmx version [check]` | Show version / check for updates |
| `/mcmx pg <player>` | Admin: toggle out-of-bounds protection |
| `/mcmx adventure` | Fix a stuck spectator: adventure mode + lobby (only between games) |
| `/mcmx reload` | Reload config |

The plugin ships with an update checker: it queries the version API every hour, announces normal
updates only at 12:00, and warns immediately about critical bugs. Use `/mcmx version` to view the
version and `/mcmx version check` to check manually.

Full role and command docs: [plugin README](mcm_x_plugin/README.md).

### Quick start

1. Build the plugin:
   ```bash
   cd mcm_x_plugin
   mvn -DskipTests package
   ```
   Output: `mcm_x_plugin/target/McmX-4.2.jar`
2. Drop `McmX-4.2.jar` into `plugins/`.
3. Put the whole `mcm/` folder into `world/datapacks/`.
4. Run `/reload` or restart the server.

See the [plugin README](mcm_x_plugin/README.md) (bilingual) for features, commands and configuration.

### Requirements

- Paper 1.21.4 (datapack `pack_format 61`)
- Java 21
- (Optional) the datapack's resource pack

### License

- Plugin: MIT, see [`mcm_x_plugin/LICENSE`](mcm_x_plugin/LICENSE).
- Datapack **Backstabbed!**: MIT by Bagel Buddies. Original project (datapack & map): https://www.planetminecraft.com/project/backstabbed/
