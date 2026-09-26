<div align="center">

# McmX · Backstabbed! 扩展身份

**Backstabbed!（谁是杀手）数据包 + Paper 1.21.4 扩展插件**
**Backstabbed! Murder Mystery datapack + a Paper 1.21.4 role-expansion plugin**

![Paper](https://img.shields.io/badge/Paper-1.21.4-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![License](https://img.shields.io/badge/License-MIT-green)

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

### 快速开始

1. 构建插件：
   ```bash
   cd mcm_x_plugin
   mvn -DskipTests package
   ```
   产物：`mcm_x_plugin/target/McmX-1.0.0.jar`
2. 把 `McmX-1.0.0.jar` 放进服务器 `plugins/`。
3. 把 `mcm/` 整个文件夹放进 `world/datapacks/`。
4. `/reload` 或重启服务器。

详细功能、命令、配置见 [插件 README](mcm_x_plugin/README.md)（中英双语）。

### 环境要求

- Paper 1.21.4（数据包 `pack_format 61`）
- Java 21
- （可选）数据包配套资源包

### 许可证

- 插件：MIT，见 [`mcm_x_plugin/LICENSE`](mcm_x_plugin/LICENSE)。
- 数据包 **Backstabbed!**：MIT，作者 Bagel Buddies。

---

<a name="english"></a>
## English

This repository contains two parts:

| Folder | Description |
|---|---|
| [`mcm/`](mcm/) | The **Backstabbed!** datapack (already patched with the McmX integration changes). |
| [`mcm_x_plugin/`](mcm_x_plugin/) | The **Paper 1.21.4** plugin adding Detective / Milk Dragon / Spy / Purifier / Gambler / Black Dealer. |

### Quick start

1. Build the plugin:
   ```bash
   cd mcm_x_plugin
   mvn -DskipTests package
   ```
   Output: `mcm_x_plugin/target/McmX-1.0.0.jar`
2. Drop `McmX-1.0.0.jar` into `plugins/`.
3. Put the whole `mcm/` folder into `world/datapacks/`.
4. Run `/reload` or restart the server.

See the [plugin README](mcm_x_plugin/README.md) (bilingual) for features, commands and configuration.

### Requirements

- Paper 1.21.4 (datapack `pack_format 61`)
- Java 21
- (Optional) the datapack's resource pack

### License

- Plugin: MIT, see [`mcm_x_plugin/LICENSE`](mcm_x_plugin/LICENSE).
- Datapack **Backstabbed!**: MIT by Bagel Buddies.
