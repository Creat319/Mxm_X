package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /mcmx 命令入口。
 * <ul>
 *     <li>/mcmx query [玩家] —— 侦探 / 眼线查探（点击聊天栏也会调用这个）</li>
 *     <li>/mcmx detective unlock|gun —— 聊天栏点击解锁技能 / 兑换手枪</li>
 *     <li>/mcmx role list|&lt;身份&gt; &lt;on|off&gt; —— 管理员开关身份</li>
 *     <li>/mcmx reload —— 重载配置</li>
 * </ul>
 */
public class McmxCommand implements CommandExecutor, TabCompleter {

    private final McmxPlugin plugin;

    public McmxCommand(McmxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "role", "roles" -> handleRole(sender, args);
            case "query", "check" -> handleQuery(sender, args);
            case "detective", "det" -> handleDetective(sender, args);
            case "vote", "投票" -> handleVote(sender, args);
            case "preset", "预设" -> handlePreset(sender, args);
            case "version", "更新" -> handleVersion(sender, args);
            case "adventure", "冒险", "lobby", "大厅", "spawn" -> handleAdventure(sender);
            case "pg" -> handlePg(sender, args);
            case "gamble", "bet", "赌徒" -> handleGamble(sender, args);
            case "blackdealer", "black_dealer", "黑庄" -> handleBlackDealer(sender, args);
            case "reload" -> handleReload(sender);
            default -> help(sender);
        }
        return true;
    }

    private void handleRole(CommandSender sender, String[] args) {
        RoleManager roles = plugin.getRoleManager();

        // role list 是只读信息，所有人（含非 op）都可以查看
        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            sender.sendMessage(Component.text("=== McmX 身份开关 ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("murderer(普通杀手): " + onOff(roles.murdererEnabled), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("milk(奶龙): " + onOff(roles.milkEnabled), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("spy(眼线): " + onOff(roles.spyEnabled), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("detective(侦探): " + onOff(roles.detectiveEnabled), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("purifier(净化者): " + onOff(roles.purifierEnabled), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("gambler+blackdealer(赌徒和黑庄): " + onOff(roles.gambleEnabled), NamedTextColor.GRAY));
            boolean allSpecial = roles.milkEnabled && roles.spyEnabled && roles.detectiveEnabled
                    && roles.purifierEnabled && roles.gambleEnabled;
            sender.sendMessage(Component.text("special(所有特殊身份): " + onOff(allSpecial)
                    + "  # /mcmx role special on|off", NamedTextColor.GRAY));
            return;
        }

        // 下面的开关操作需要管理员权限
        if (!sender.hasPermission("mcmx.admin")) {
            sender.sendMessage(Component.text("你没有权限修改身份开关。", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("/mcmx role <murderer|milk|spy|detective|purifier|赌徒和黑庄> <on|off>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/mcmx role special <on|off>  # 一键开关所有特殊身份", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/mcmx role list", NamedTextColor.YELLOW));
            return;
        }

        String key = args[1].toLowerCase(Locale.ROOT);
        boolean enabled = args[2].equalsIgnoreCase("on")
                || args[2].equalsIgnoreCase("true")
                || args[2].equalsIgnoreCase("enable")
                || args[2].equalsIgnoreCase("1");

        // 一键开关所有特殊身份（不含 普通杀手/枪手/无辜者）
        if (key.equals("special") || key.equals("specials") || key.equals("all_special")
                || key.equals("allspecial") || key.equals("特殊") || key.equals("全部特殊")
                || key.equals("所有特殊") || key.equals("特殊身份")) {
            plugin.getConfig().set("roles.milk_dragon", enabled);
            plugin.getConfig().set("roles.spy", enabled);
            plugin.getConfig().set("roles.detective", enabled);
            plugin.getConfig().set("roles.purifier", enabled);
            plugin.getConfig().set("roles.gambler_black_dealer", enabled);
            plugin.saveConfig();
            plugin.reloadAll();
            sender.sendMessage(Component.text("已" + onOff(enabled)
                    + "所有特殊身份：奶龙 / 眼线 / 侦探 / 净化者 / 赌徒+黑庄", NamedTextColor.GREEN));
            return;
        }

        String configPath;
        switch (key) {
            case "murderer", "killer", "wolf", "杀手" -> configPath = "roles.murderer";
            case "milk", "milk_dragon", "milkdragon", "dragon", "奶龙" -> configPath = "roles.milk_dragon";
            case "spy", "mole", "眼线" -> configPath = "roles.spy";
            case "detective", "det", "侦探" -> configPath = "roles.detective";
            case "purifier", "purify", "净化者" -> configPath = "roles.purifier";
            case "gambler", "blackdealer", "black_dealer", "black", "赌徒", "黑庄", "赌徒和黑庄", "赌徒黑庄" -> configPath = "roles.gambler_black_dealer";
            default -> {
                sender.sendMessage(Component.text("未知身份：" + key, NamedTextColor.RED));
                return;
            }
        }

        plugin.getConfig().set(configPath, enabled);
        plugin.saveConfig();
        plugin.reloadAll();
        sender.sendMessage(Component.text("已设置 " + key + " = " + onOff(enabled), NamedTextColor.GREEN));
    }

    private void handleQuery(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行。", NamedTextColor.RED));
            return;
        }
        if (args.length == 1) {
            plugin.getQueryManager().openMenu(player);
        } else {
            plugin.getQueryManager().query(player, args[1]);
        }
    }

    private void handleDetective(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行。", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("/mcmx detective unlock|gun", NamedTextColor.YELLOW));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "unlock", "unlocked", "skill" -> plugin.getFragmentManager().unlockDetectiveSkill(player);
            case "gun", "weapon" -> plugin.getFragmentManager().buyGun(player);
            default -> player.sendMessage(Component.text("/mcmx detective unlock|gun", NamedTextColor.YELLOW));
        }
    }

    /**
     * /mcmx preset <玩家> <身份>  —— 预设该玩家下一局的身份
     * /mcmx preset <玩家> clear    —— 取消预设
     * /mcmx preset list            —— 查看当前所有预设
     */
    private void handlePreset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mcmx.admin")) {
            sender.sendMessage(Component.text("你没有权限使用该命令。", NamedTextColor.RED));
            return;
        }
        PresetManager presetManager = plugin.getPresetManager();

        if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
            if (presetManager.all().isEmpty()) {
                sender.sendMessage(Component.text("当前没有任何身份预设。", NamedTextColor.GRAY));
                return;
            }
            sender.sendMessage(Component.text("=== 身份预设 ===", NamedTextColor.GOLD));
            for (var entry : presetManager.all().entrySet()) {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                String name = player != null ? player.getName() : entry.getKey().toString();
                sender.sendMessage(Component.text("  " + name + " -> " + entry.getValue().display(), NamedTextColor.GRAY));
            }
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("/mcmx preset <玩家> <身份>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/mcmx preset <玩家> clear", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/mcmx preset list", NamedTextColor.YELLOW));
            return;
        }

        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("找不到在线玩家：" + args[1], NamedTextColor.RED));
            return;
        }

        if (args[2].equalsIgnoreCase("clear")) {
            presetManager.clear(target);
            sender.sendMessage(Component.text("已取消 " + target.getName() + " 的身份预设。", NamedTextColor.GREEN));
            return;
        }

        RoleType role = RoleType.fromName(args[2]);
        if (role == null) {
            sender.sendMessage(Component.text("未知身份：" + args[2], NamedTextColor.RED));
            return;
        }
        presetManager.set(target, role);
        sender.sendMessage(Component.text("已将 " + target.getName() + " 下一局预设为：" + role.display(), NamedTextColor.GREEN));
    }

    /**
     * /mcmx pg <玩家> —— 管理员指令：切换某玩家的“免出图保护”。
     * 开启后该玩家带 mcmx_pg 标签，数据包的出界传送 / 旁观拉回 / 边界判死都会跳过。
     */
    private void handlePg(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mcmx.admin")) {
            sender.sendMessage(Component.text("你没有权限使用该命令。", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getScoreboardTags().contains("mcmx_pg")) {
                    names.add(online.getName());
                }
            }
            if (names.isEmpty()) {
                sender.sendMessage(Component.text("当前没有开启免出图保护的玩家。", NamedTextColor.GRAY));
            } else {
                sender.sendMessage(Component.text("免出图保护玩家：" + String.join("、", names), NamedTextColor.GOLD));
            }
            sender.sendMessage(Component.text("/mx pg <玩家> —— 切换该玩家的免出图保护", NamedTextColor.YELLOW));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("找不到在线玩家：" + args[1], NamedTextColor.RED));
            return;
        }

        if (target.getScoreboardTags().contains("mcmx_pg")) {
            target.removeScoreboardTag("mcmx_pg");
            sender.sendMessage(Component.text("已取消 " + target.getName() + " 的免出图保护。", NamedTextColor.GREEN));
            target.sendMessage(Component.text("你的免出图保护已被取消。", NamedTextColor.YELLOW));
        } else {
            target.addScoreboardTag("mcmx_pg");
            sender.sendMessage(Component.text("已给 " + target.getName() + " 开启免出图保护（出图不会被传送/判死）。", NamedTextColor.GREEN));
            target.sendMessage(Component.text("你已获得免出图保护，可以自由离开地图（仅管理员）。", NamedTextColor.GREEN));
        }
    }

    /**
     * /mcmx adventure —— 卡旁观时切回冒险模式并返回大厅。
     * 只在游戏未进行时可用（$gamestate != 1）。
     */
    private void handleAdventure(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行。", NamedTextColor.RED));
            return;
        }
        int gamestate = plugin.getBridge().getScore("CmdData", "$gamestate");
        if (gamestate == 1) {
            player.sendMessage(Component.text("游戏进行中，不能切换模式；请等本局结束后再试。", NamedTextColor.RED));
            return;
        }
        if (plugin.getBridge().isMcmLoaded()) {
            // 数据包负责完整重置：切冒险、回大厅、清标记、发大厅物品
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "execute as " + player.getName() + " run function mcm:lobby/fix_spectator");
        }
        // 兜底：即使数据包没装也保证切回冒险
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        player.sendMessage(Component.text("已切回冒险模式并返回大厅。", NamedTextColor.GREEN));
    }

    /** /mcmx version [check] —— 查看版本 / 手动检查更新。 */
    private void handleVersion(CommandSender sender, String[] args) {
        UpdateChecker checker = plugin.getUpdateChecker();
        String current = plugin.getDescription().getVersion();

        if (args.length >= 2 && args[1].equalsIgnoreCase("check")) {
            checker.requestCheck();
            sender.sendMessage(Component.text("正在检查更新……", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("McmX 当前版本：" + current, NamedTextColor.GOLD));
        String latest = checker.getLatestVersion();
        if (latest == null) {
            sender.sendMessage(Component.text("尚未获取到最新版本，可用 /mcmx version check 手动检查。", NamedTextColor.GRAY));
        } else if (checker.isUrgent()) {
            sender.sendMessage(Component.text("紧急：当前版本存在严重 bug，请立即更新到 " + latest
                    + "+！https://github.com/Creat319/Mxm_X", NamedTextColor.RED));
        } else if (checker.isUpdateAvailable()) {
            sender.sendMessage(Component.text("发现新版本：" + latest
                    + "，请前往 https://github.com/Creat319/Mxm_X 更新。", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("已是最新版本。", NamedTextColor.GREEN));
        }
    }

    /** /mcmx vote [身份] [on|off] —— 身份投票。 */
    private void handleVote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行。", NamedTextColor.RED));
            return;
        }
        if (args.length == 1) {
            plugin.getRoleVoteManager().sendMenu(player);
            return;
        }
        if (args.length < 3) {
            player.sendMessage(Component.text("/mcmx vote <身份> <on|off>", NamedTextColor.YELLOW));
            return;
        }
        boolean value = args[2].equalsIgnoreCase("on")
                || args[2].equalsIgnoreCase("true")
                || args[2].equalsIgnoreCase("enable")
                || args[2].equalsIgnoreCase("1");
        plugin.getRoleVoteManager().vote(player, args[1], value);
    }

    /** /mcmx gamble [玩家] [身份] —— 赌徒下注。 */
    private void handleGamble(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行。", NamedTextColor.RED));
            return;
        }
        if (args.length == 1) {
            plugin.getGambleManager().openBet(player);
        } else if (args.length == 2) {
            plugin.getGambleManager().chooseTarget(player, args[1]);
        } else {
            plugin.getGambleManager().placeBet(player, args[1], args[2]);
        }
    }

    /** /mcmx blackdealer disguise —— 黑庄赋予自己伪身份。 */
    private void handleBlackDealer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行。", NamedTextColor.RED));
            return;
        }
        if (args.length >= 2 && (args[1].equalsIgnoreCase("disguise")
                || args[1].equalsIgnoreCase("disguised")
                || args[1].equalsIgnoreCase("伪装"))) {
            plugin.getGambleManager().disguise(player);
        } else {
            player.sendMessage(Component.text("/mcmx blackdealer disguise", NamedTextColor.YELLOW));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("mcmx.admin")) {
            sender.sendMessage(Component.text("你没有权限使用该命令。", NamedTextColor.RED));
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(Component.text("McmX 配置已重载。", NamedTextColor.GREEN));
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Component.text("=== McmX 命令 ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/mcmx query [玩家] - 侦探/眼线查探", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mcmx role list - 查看身份开关", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mcmx role <身份> <on|off> - 开关身份", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mcmx reload - 重载配置", NamedTextColor.GRAY));
    }

    private String onOff(boolean value) {
        return value ? "开启" : "关闭";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("query", "detective", "vote", "preset", "version", "adventure", "pg",
                    "gamble", "blackdealer", "role", "reload")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    result.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("role")) {
            for (String sub : List.of("list", "special", "murderer", "milk", "spy", "detective", "purifier", "gambler", "blackdealer")) {
                if (sub.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(sub);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("role")) {
            for (String sub : List.of("on", "off")) {
                if (sub.startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    result.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("preset")) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(player.getName());
                }
            }
            if ("list".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                result.add("list");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("preset")) {
            for (String key : List.of("murderer", "milk", "spy", "blackdealer", "detective",
                    "purifier", "gambler", "gunner", "innocent", "clear")) {
                if (key.startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    result.add(key);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("vote")) {
            for (String key : RoleVoteManager.ROLE_KEYS) {
                String commandKey = RoleVoteManager.commandKey(key);
                if (commandKey.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(commandKey);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("vote")) {
            for (String sub : List.of("on", "off")) {
                if (sub.startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    result.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("pg")) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(player.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("version")) {
            if ("check".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                result.add("check");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("blackdealer")) {
            for (String sub : List.of("disguise")) {
                if (sub.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("gamble")) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("gamble")) {
            for (RoleType role : RoleType.BAD_ROLES) {
                String key = GambleManager.keyOf(role);
                if (key.startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    result.add(key);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("detective")) {
            for (String sub : List.of("unlock", "gun")) {
                if (sub.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("query")) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    result.add(player.getName());
                }
            }
        }
        return result;
    }
}
