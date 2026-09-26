package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            for (String sub : List.of("query", "detective", "vote", "preset", "gamble", "blackdealer", "role", "reload")) {
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
