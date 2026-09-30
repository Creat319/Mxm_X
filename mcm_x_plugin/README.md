<div align="center">

# McmX

**Backstabbed!（mcm / 谁是杀手）数据包的 Paper 1.21.4 扩展身份插件**
**A Paper 1.21.4 role-expansion add-on for the Backstabbed! (`mcm`) Murder Mystery datapack**

![Paper](https://img.shields.io/badge/Paper-1.21.4-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![License](https://img.shields.io/badge/License-MIT-green)
![Version](https://img.shields.io/badge/Version-4.2-blueviolet)

[中文](#chinese) · [English](#english)

</div>

---

<a name="chinese"></a>
## 中文

### 简介

McmX 是一个配合 **Backstabbed! / 谁是杀手**（数据包命名空间 `mcm`）使用的 Paper 服务端插件。
它给经典"杀手 / 枪手 / 平民"玩法增加了 **侦探、奶龙、眼线、净化者、赌徒、黑庄** 六个身份，
以及 **开局身份投票**、**下一局身份预设**、**身份开关** 等管理功能。

数据包负责基础阵营、装备、胜负判定；插件负责扩展身份、技能、聊天交互与身份播报。
两者通过计分板（`CmdData`）与标签协作，不会出现"一个人两个身份"。

原项目 **Backstabbed!**（数据包 + 地图）由 Bagel Buddies 制作，原作者链接：https://www.planetminecraft.com/project/backstabbed/

### 职业总览

> 坏人总数由玩家人数决定：≤7 人 1 狼、8~17 人 2 狼、≥18 人 3 狼（数据包 `smart_murderer_update`）。
> 坏人身份从 **普通杀手 / 奶龙 / 眼线 / 黑庄** 中随机；黑庄随机到才会出现，并同时配一个赌徒。

| 身份 | 阵营 | 出现条件 | 核心技能 |
|---|---|---|---|
| 普通杀手 Murderer | 坏人 | 每局必有 | 刀近战/投掷击杀、手枪；1 次免费召回刀 |
| 奶龙 Milk Dragon | 坏人 | 随机 | 山羊角：半径 20 内**存活好人** 反胃 + 缓慢III + 失明III（8 秒） |
| 眼线 Spy | 坏人 | 随机 | 3 碎片查探一人（2 次），目标 10 秒失明 + 发光 |
| 黑庄 Black Dealer | 坏人 | 随机（随机到会配一个赌徒） | 5 碎片获得随机坏人伪身份，赌徒必须猜中伪身份 |
| 枪手 Gunner | 好人 | 每局 1 名 | 数据包原手枪玩法；开局屏幕红字"铲除一切害人虫" |
| 平民 Innocent | 好人 | 其余玩家 | 收集碎片、躲藏、配合枪手 |
| 侦探 Detective | 好人 | 坏人 ≥2 | 3 碎片解锁查询（2 次）；死亡时播报凶手 |
| 净化者 Purifier | 好人 | 默认每局 | 净化器清除周围 反胃/失明/缓慢，用后暴露 5 秒 |
| 赌徒 Gambler | 好人 | 随机到黑庄时成对出现 | 10 碎片押注"玩家 + 坏人身份"，猜错当场死亡 |

其它功能：

- **开局身份投票**：宽限期内聊天栏弹出投票，玩家对 奶龙/眼线/侦探/净化者/赌徒+黑庄 点 `[启用]`/`[禁用]`；
  启用票必须**严格多于**禁用票才启用，无票/平票/禁用多一律禁用；结果公布每个身份是谁投的。
- **下一局身份预设**：`/mcmx preset <玩家> <身份>`，数据包 + 插件配合保证一个人只有一个身份。
- **身份开关**：`/mcmx role ...`，支持一键开关所有特殊身份。
- 所有 `/mcmx` 命令默认所有人可用（可通过权限 `mcmx.admin` 收回）。

### 环境要求

- **Paper 1.21.4**（对应数据包 `pack_format 61`）
- **Java 21**
- **Backstabbed!（`mcm`）数据包**（仓库内已包含改好的版本）
- （可选）数据包配套资源包：物品模型、音效、部分翻译键来自资源包

### 安装

1. 构建或下载 `McmX-4.2.jar`，放入服务器 `plugins/`。
2. 把仓库内的 `mcm` 数据包（已包含所需改动）整个文件夹放入 `world/datapacks/`。
3. `/reload` 或重启服务器。

> 使用原版未改动的 `mcm` 也能运行，但身份预设、身份统一播报、赌徒禁枪等功能会不完整。

### 构建

需要 JDK 21 与 Maven：

```bash
mvn -DskipTests package
```

产物：`target/McmX-4.2.jar`

### 指令使用方法

格式约定：`<必填>`、`[可选]`；命令别名 `/mx`；身份参数支持中英文。

#### 玩家指令（所有人可用）

| 指令 | 用法 | 示例 |
|---|---|---|
| 查探 | `/mcmx query` 打开玩家列表；`/mcmx query <玩家>` 直接查探 | `/mcmx query 小明` |
| 侦探解锁 | `/mcmx detective unlock`（消耗 3 碎片，只能解锁一次） | `/mcmx detective unlock` |
| 侦探换枪 | `/mcmx detective gun`（消耗 10 碎片） | `/mcmx detective gun` |
| 赌徒下注 | `/mcmx gamble` 打开列表；`/mcmx gamble <玩家> <身份>` 直接下注 | `/mcmx gamble 小明 奶龙` |
| 黑庄伪装 | `/mcmx blackdealer disguise`（消耗 5 碎片） | `/mcmx blackdealer disguise` |
| 身份投票 | `/mcmx vote` 打开投票菜单；`/mcmx vote <身份> <on\|off>` 直接投票 | `/mcmx vote milk on` |
| 查看版本 | `/mcmx version` 查看当前/最新版本；`/mcmx version check` 手动检查更新 | `/mcmx version check` |
| 切回冒险 | `/mcmx adventure` 卡旁观时切回冒险模式并返回大厅（仅游戏未进行时可用） | `/mcmx adventure` |

#### 管理指令（权限 `mcmx.admin`，默认所有人）

| 指令 | 用法 | 示例 |
|---|---|---|
| 预设身份 | `/mcmx preset <玩家> <身份>` | `/mcmx preset 小明 侦探` |
| 取消预设 | `/mcmx preset <玩家> clear` | `/mcmx preset 小明 clear` |
| 查看预设 | `/mcmx preset list` | `/mcmx preset list` |
| 身份开关 | `/mcmx role <身份> <on\|off>` | `/mcmx role 奶龙 off` |
| 一键开关特殊身份 | `/mcmx role special <on\|off>` | `/mcmx role special off` |
| 查看身份开关 | `/mcmx role list` | `/mcmx role list` |
| 免出图保护 | `/mcmx pg <玩家>` 切换该玩家的免出图保护（出图不被传送/判死） | `/mx pg 小明` |
| 重载配置 | `/mcmx reload` | `/mcmx reload` |
| 查看版本 | `/mcmx version [check]` | `/mcmx version` |

#### 身份参数对照

| 中文 | 英文关键字 |
|---|---|
| 杀手 | `murderer` / `killer` |
| 奶龙 | `milk` / `milk_dragon` |
| 眼线 | `spy` |
| 黑庄 | `blackdealer` / `black_dealer` |
| 侦探 | `detective` / `det` |
| 净化者 | `purifier` |
| 赌徒 | `gambler` |
| 枪手 | `gunner` |
| 平民 | `innocent` |

#### 权限说明

- `mcmx.admin`：管理预设、身份开关、重载；**默认 `true`（所有人可用）**。
- 想限制时用权限插件收回，例如 LuckPerms：
  `/lp group default permission set mcmx.admin false`

### 配置

主要配置在 `plugins/McmX/config.yml`：

```yaml
roles:
  murderer: true            # 普通杀手（始终开启，不受投票影响）
  milk_dragon: true         # 奶龙
  spy: true                 # 眼线
  detective: true           # 侦探
  purifier: true            # 净化者
  gambler_black_dealer: true# 赌徒 + 黑庄（共用一个开关）

query:
  cooldown-ms: 1000         # 两次查询之间的冷却

role-vote:
  enabled: true             # 开局身份投票
  finalize-at-graceperiod: 20

gamble:
  bet-cost: 10              # 赌徒每次下注消耗碎片
  black-dealer-cost: 5      # 黑庄伪装消耗碎片

purifier:
  radius: 20.0
  glowing-ticks: 100
  require-milk-dragon: false

fragments:
  detective-unlock-cost: 3
  detective-gun-cost: 10
  detective-uses: 2
  spy-cost: 3
  spy-uses: 2

milk-dragon:
  radius: 20.0
  duration-ticks: 160
  cooldown-ticks: 1200
  nausea-amplifier: 0
  slowness-amplifier: 2
  blindness-amplifier: 2

prompt-interval-ms: 5000
messages:
  gunner-title: "铲除一切害人虫"
```

### 更新检查

- 插件启动后、以及每 `update-checker.check-interval-minutes` 分钟（默认 **60**）异步请求一次版本接口。
- 接口：`http://carovo.shop/api/version.php?format=json`
  - 正常返回：`{"code":0,"version":"4.2"}`
  - 紧急返回：`{"code":0,"version":"4.2_4.1"}` → 表示 4.1 有严重 bug；当前版本 ≤ 4.1 时**立即警告**。
- **普通更新**：只在每天 `daily-reminder`（默认 **12:00**）随机选一条文案提醒在线玩家。
- **紧急 bug**：每小时检测到就立即警告，不受每日 12:00 限制。
- 当前版本：`4.2`。命令 `/mcmx version` 查看版本，`/mcmx version check` 手动检查。
- 配置：
  ```yaml
  update-checker:
    enabled: true
    api-url: "http://carovo.shop/api/version.php?format=json"
    check-interval-minutes: 60
    daily-reminder: "12:00"
  ```

### 数据包配合改动

仓库内的 `mcm` 数据包已经包含以下改动（相对原版 Backstabbed!）：

| 文件 | 作用 |
|---|---|
| `game/pick_roles.mcfunction` | 强制预设坏人成为杀手、修正杀手数量、优先预设枪手、把身份播报交给插件（`$mcmx`） |
| `game/assign_murderer.mcfunction` | 随机抽杀手时排除预设好人 |
| `game/role_messages.mcfunction` | 新增：原本的身份播报，仅在没装插件时执行 |
| `game/items/scrap_function.mcfunction` | 排除赌徒的"10 碎片自动做枪" |

插件启动后会把 `$mcmx CmdData` 设为 `1`，数据包据此把身份播报交给插件，避免"数据包说你是平民、插件又说你是黑庄"。

### 工作原理

- 插件读取 `CmdData` 计分板：`$gamestate`（游戏阶段）与 `$pickedroles`（是否已选身份）。
- 数据包负责基础阵营 `murderer / gunner / innocent` 与所有装备、胜负逻辑。
- 插件在 `$pickedroles == 1` 后细分 奶龙/眼线/黑庄/侦探/净化者/赌徒，并统一播报最终身份。
- 扩展身份都用标签表示，且仍保留基础阵营标签（坏人带 `murderer`、好人带 `innocent`），
  因此数据包的胜负、Bossbar、掉落等逻辑完全兼容。
- 预设通过 `mcmx_preset_*` 标签在选身份前传给数据包。

### 常见问题

- **必须用仓库内的数据包吗？** 想要完整的预设、投票、统一播报与赌徒禁枪，请直接使用仓库内的 `mcm`。
- **没有资源包会怎样？** 自定义物品模型/音效会缺失，但玩法逻辑不受影响。
- **支持其它版本吗？** 目前针对 Paper 1.21.4（`pack_format 61`）编译。
- **`/mcmx` 命令谁能用？** 默认所有玩家；`mcmx.admin` 默认 `true`，可用权限插件收回。

### 许可证与致谢

- 本插件：MIT License，见 [LICENSE](LICENSE)。
- 原数据包 **Backstabbed!**：MIT License，作者 Bagel Buddies。原作者链接（数据包/地图）：https://www.planetminecraft.com/project/backstabbed/

---

<a name="english"></a>
## English

### Introduction

McmX is a Paper server plugin built as an add-on for the **Backstabbed!**
Murder Mystery datapack (namespace `mcm`).
It expands the classic Killer / Gunner / Innocent loop with six new roles —
**Detective, Milk Dragon, Spy, Purifier, Gambler and Black Dealer** — plus
in-game **role voting**, **next-round role presets** and **role toggles**.

The datapack owns base factions, items and win conditions; the plugin owns the
special roles, abilities, chat interactions and role announcements. They talk to
each other through the `CmdData` scoreboard and scoreboard tags, so a player can
never end up with two identities.

The original **Backstabbed!** datapack & map is made by Bagel Buddies: https://www.planetminecraft.com/project/backstabbed/

### Role overview

> The number of evil players depends on the lobby size: 1 for ≤7, 2 for 8–17, 3 for ≥18
> (datapack `smart_murderer_update`). Evil roles are drawn from
> **Murderer / Milk Dragon / Spy / Black Dealer**; Black Dealer only appears if randomly
> selected, and then a Gambler is added as well.

| Role | Side | Appears when | Core ability |
|---|---|---|---|
| Murderer | Evil | always | Knife melee/throw, gun, one free knife recall |
| Milk Dragon | Evil | random | Goat horn: Nausea + Slowness III + Blindness III (8s) to nearby living good players |
| Spy | Evil | random | 3 fragments to scan a player (2 uses); the target gets 10s Blindness + Glowing |
| Black Dealer | Evil | random (pairs with Gambler) | 5 fragments for a random fake evil identity; the Gambler must guess it |
| Gunner | Good | one per round | Datapack gun gameplay; big red intro title |
| Innocent | Good | everyone else | Collect fragments, survive, support the Gunner |
| Detective | Good | 2+ evil players | 3 fragments unlock a 2-use identity check; announces the killer on death |
| Purifier | Good | every round by default | Purifier removes Nausea/Blindness/Slowness nearby; reveals the Purifier for 5s |
| Gambler | Good | paired with Black Dealer | Bet 10 fragments on "player + evil role"; a wrong guess kills you |

Other features:

- **Role vote** during the grace period (Milk Dragon / Spy / Detective / Purifier / Gambler+Black Dealer).
  A role is enabled only if enable votes **strictly outnumber** disable votes; ties/no votes are disabled.
- **Next-round presets** with `/mcmx preset <player> <role>`, coordinated with the datapack so nobody gets two roles.
- **Role toggles** with `/mcmx role ...`, including a one-shot "all special roles" toggle.
- All `/mcmx` commands are available to everyone by default (revocable via `mcmx.admin`).

### Requirements

- **Paper 1.21.4** (datapack `pack_format 61`)
- **Java 21**
- The **Backstabbed! (`mcm`)** datapack (the patched version is bundled in this repository)
- (Optional) the datapack's resource pack for custom item models, sounds and some translation keys

### Installation

1. Build/download `McmX-4.2.jar` and drop it into `plugins/`.
2. Put the bundled `mcm` datapack (already patched) into `world/datapacks/`.
3. Run `/reload` or restart the server.

> The plugin can run with the unmodified datapack, but presets, unified role
> announcements and the "gambler cannot craft guns" fix will be incomplete.

### Building

Requires JDK 21 and Maven:

```bash
mvn -DskipTests package
```

Output: `target/McmX-4.2.jar`

### Command usage

Notation: `<required>` / `[optional]`. Alias: `/mx`. Chinese role names are also accepted.

#### Player commands (everyone)

| Command | Usage | Example |
|---|---|---|
| Scan | `/mcmx query` opens the player list; `/mcmx query <player>` scans directly | `/mcmx query Alex` |
| Detective unlock | `/mcmx detective unlock` (costs 3 fragments, once) | `/mcmx detective unlock` |
| Detective gun | `/mcmx detective gun` (costs 10 fragments) | `/mcmx detective gun` |
| Gambler bet | `/mcmx gamble` opens the list; `/mcmx gamble <player> <role>` bets directly | `/mcmx gamble Alex milk` |
| Black Dealer disguise | `/mcmx blackdealer disguise` (costs 5 fragments) | `/mcmx blackdealer disguise` |
| Role vote | `/mcmx vote` opens the menu; `/mcmx vote <role> <on\|off>` votes directly | `/mcmx vote milk on` |
| Version | `/mcmx version` shows the current/latest version; `/mcmx version check` checks manually | `/mcmx version check` |
| Back to adventure | `/mcmx adventure` fixes a stuck spectator: adventure mode + back to lobby (only when no game is running) | `/mcmx adventure` |

#### Admin commands (`mcmx.admin`, default everyone)

| Command | Usage | Example |
|---|---|---|
| Preset role | `/mcmx preset <player> <role>` | `/mcmx preset Alex detective` |
| Clear preset | `/mcmx preset <player> clear` | `/mcmx preset Alex clear` |
| List presets | `/mcmx preset list` | `/mcmx preset list` |
| Toggle role | `/mcmx role <role> <on\|off>` | `/mcmx role milk off` |
| Toggle all special roles | `/mcmx role special <on\|off>` | `/mcmx role special off` |
| List role toggles | `/mcmx role list` | `/mcmx role list` |
| No-escape protection | `/mcmx pg <player>` toggles out-of-bounds protection for a player | `/mx pg Alex` |
| Reload config | `/mcmx reload` | `/mcmx reload` |
| Version | `/mcmx version [check]` | `/mcmx version` |

#### Role name arguments

| Chinese | English keys |
|---|---|
| 杀手 | `murderer` / `killer` |
| 奶龙 | `milk` / `milk_dragon` |
| 眼线 | `spy` |
| 黑庄 | `blackdealer` / `black_dealer` |
| 侦探 | `detective` / `det` |
| 净化者 | `purifier` |
| 赌徒 | `gambler` |
| 枪手 | `gunner` |
| 平民 | `innocent` |

#### Permissions

- `mcmx.admin`: presets, role toggles and reload. **Defaults to `true` (everyone).**
- Revoke it with a permissions plugin if needed, e.g. LuckPerms:
  `/lp group default permission set mcmx.admin false`

### Configuration

See `plugins/McmX/config.yml` (the same keys as the Chinese section above).
Key entries: `roles.*`, `query.cooldown-ms`, `role-vote.*`, `gamble.*`,
`purifier.*`, `fragments.*`, `milk-dragon.*`, `messages.gunner-title`.

### Update checker

- On startup and every `update-checker.check-interval-minutes` (default **60**) the plugin queries the version API asynchronously.
- API: `http://carovo.shop/api/version.php?format=json`
  - Normal: `{"code":0,"version":"4.2"}`
  - Critical: `{"code":0,"version":"4.2_4.1"}` → 4.1 has a severe bug; if the current version ≤ 4.1 an **immediate warning** is broadcast.
- **Normal updates** are announced only at `daily-reminder` (default **12:00**) with a random message.
- **Critical bugs** are announced immediately on every hourly check, not just at 12:00.
- Current version: `4.2`. Use `/mcmx version` to view it and `/mcmx version check` to check manually.
- Config:
  ```yaml
  update-checker:
    enabled: true
    api-url: "http://carovo.shop/api/version.php?format=json"
    check-interval-minutes: 60
    daily-reminder: "12:00"
  ```

### Datapack integration

The bundled `mcm` datapack already includes the following changes (compared to vanilla Backstabbed!):

| File | Purpose |
|---|---|
| `game/pick_roles.mcfunction` | Force preset evil players, fix the murderer count, prefer the preset gunner, and delegate role messages to the plugin (`$mcmx`). |
| `game/assign_murderer.mcfunction` | Never pick a preset good player as a random murderer. |
| `game/role_messages.mcfunction` | New: the vanilla role announcements, only used when the plugin is absent. |
| `game/items/scrap_function.mcfunction` | Exclude the Gambler from the automatic "10 scrap → gun" mechanic. |

On startup the plugin sets `$mcmx CmdData` to `1`; the datapack then skips its
own role messages and lets the plugin announce the final roles.

### How it works

- The plugin reads the `CmdData` objective: `$gamestate` and `$pickedroles`.
- The datapack keeps base factions (`murderer` / `gunner` / `innocent`), items and win conditions.
- Once `$pickedroles == 1`, the plugin assigns the special roles and announces the final role of every player.
- Special roles keep their base faction tag (evil roles keep `murderer`, good roles keep `innocent`),
  so the datapack's win conditions, bossbar and drops stay fully compatible.
- Presets are passed to the datapack before role assignment via `mcmx_preset_*` tags.

### FAQ

- **Do I have to use the bundled datapack?** For presets, role votes, unified announcements and the gambler gun fix, please use the bundled `mcm`.
- **Is the resource pack required?** No, but custom item models and sounds will be missing without it.
- **Which Minecraft versions?** Built for Paper 1.21.4 (`pack_format 61`).
- **Who can use `/mcmx`?** Everyone by default; `mcmx.admin` defaults to `true` and can be revoked with a permissions plugin.

### License & Credits

- This plugin: MIT License, see [LICENSE](LICENSE).
- Original datapack **Backstabbed!**: MIT License by Bagel Buddies. Original project (datapack & map): https://www.planetminecraft.com/project/backstabbed/
