package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * 赌徒 + 黑庄（共用一个开关）。
 *
 * <ul>
 *     <li>赌徒（好人）：聊天栏点玩家名 -> 再点坏人身份；押中赢，押错当场死亡。
 *         每次下注消耗 10 碎片。</li>
 *     <li>黑庄（坏人）：5 碎片可以随机赋予自己一个坏人伪身份，
 *         全局通报；赌徒必须猜中伪身份才算赢。</li>
 * </ul>
 */
public class GambleManager implements Listener {

    private final McmxPlugin plugin;
    private final GameBridge bridge;
    private final FragmentManager fragments;
    private final Random random = new Random();

    public int betCost = 10;
    public int blackDealerCost = 5;
    private long promptIntervalMs = 5000L;

    public GambleManager(McmxPlugin plugin, GameBridge bridge, FragmentManager fragments) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.fragments = fragments;
        loadConfig();
    }

    public void loadConfig() {
        betCost = plugin.getConfig().getInt("gamble.bet-cost", 10);
        blackDealerCost = plugin.getConfig().getInt("gamble.black-dealer-cost", 5);
        promptIntervalMs = plugin.getConfig().getLong("prompt-interval-ms", 5000L);
    }

    // ------------------------------------------------------------------
    // 轮询提示
    // ------------------------------------------------------------------

    public void tick() {
        long now = System.currentTimeMillis();
        for (Player player : bridge.matchPlayers()) {
            RoleType role = McmxPlugin.roleOf(player);
            PlayerState state = plugin.state(player);

            if (role == RoleType.BLACK_DEALER) {
                if (fragments.countScrap(player) >= blackDealerCost
                        && now - state.lastGamblePrompt > promptIntervalMs) {
                    state.lastGamblePrompt = now;
                    player.sendMessage(Component.text("你收集到了足够的碎片！", NamedTextColor.GREEN)
                            .append(Component.text(" [点击赋予自己一个伪身份]", NamedTextColor.LIGHT_PURPLE)
                                    .clickEvent(ClickEvent.runCommand("/mcmx blackdealer disguise"))
                                    .hoverEvent(HoverEvent.showText(Component.text("消耗 " + blackDealerCost + " 个碎片，随机变成一个坏人身份")))));
                }
            } else if (role == RoleType.GAMBLER) {
                if (fragments.countScrap(player) >= betCost
                        && now - state.lastGamblePrompt > promptIntervalMs) {
                    state.lastGamblePrompt = now;
                    player.sendMessage(Component.text("你有足够的碎片！", NamedTextColor.GREEN)
                            .append(Component.text(" [点击开始下注]", NamedTextColor.GOLD)
                                    .clickEvent(ClickEvent.runCommand("/mcmx gamble"))
                                    .hoverEvent(HoverEvent.showText(Component.text("每次下注消耗 " + betCost + " 个碎片")))));
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 赌徒
    // ------------------------------------------------------------------

    public void openBet(Player player) {
        if (!checkGambler(player)) {
            return;
        }
        player.sendMessage(Component.text("=== 选择你要赌的玩家（点击名字） ===", NamedTextColor.GOLD));
        for (Player target : bridge.matchPlayers()) {
            if (target.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            player.sendMessage(Component.text(" - ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/mcmx gamble " + target.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("选择 " + target.getName())))));
        }
    }

    public void chooseTarget(Player player, String targetName) {
        if (!checkGambler(player)) {
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !bridge.matchPlayers().contains(target)) {
            player.sendMessage(Component.text("找不到该玩家（可能已不在本局游戏中）。", NamedTextColor.RED));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("不能赌自己。", NamedTextColor.RED));
            return;
        }
        PlayerState state = plugin.state(player);
        state.gambleTarget = target.getUniqueId();
        state.gambleTargetName = target.getName();

        player.sendMessage(Component.text("你选择了 " + target.getName() + "，现在选择他的身份（点击）：", NamedTextColor.GOLD));
        for (RoleType role : RoleType.BAD_ROLES) {
            player.sendMessage(Component.text(" - ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(role.display(), NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/mcmx gamble " + target.getName() + " " + keyOf(role)))
                            .hoverEvent(HoverEvent.showText(Component.text("押注：是 " + role.display())))));
        }
    }

    public void placeBet(Player player, String targetName, String roleKey) {
        if (!checkGambler(player)) {
            return;
        }
        PlayerState state = plugin.state(player);
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || state.gambleTarget == null || !state.gambleTarget.equals(target.getUniqueId())) {
            player.sendMessage(Component.text("请先用 /mcmx gamble 选择下注目标。", NamedTextColor.RED));
            return;
        }
        RoleType guess = roleFromKey(roleKey);
        if (guess == null) {
            player.sendMessage(Component.text("无效的身份。", NamedTextColor.RED));
            return;
        }
        if (fragments.countScrap(player) < betCost) {
            player.sendMessage(Component.text("碎片不足，每次下注需要 " + betCost + " 个碎片。", NamedTextColor.RED));
            return;
        }

        fragments.removeScrap(player, betCost);
        RoleType actual = effectiveRole(target);

        state.gambleTarget = null;
        state.gambleTargetName = null;

        if (guess == actual) {
            bridge.broadcast(Component.text("赌徒“" + player.getName() + "”赌赢了！他押中了“"
                    + target.getName() + "”的身份是“" + actual.display() + "”。", NamedTextColor.GREEN));
        } else {
            // 赌错当场死亡
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "execute as " + player.getName() + " run function mcm:game/playerdeath");
            bridge.broadcast(Component.text("赌徒“" + player.getName()
                    + "”因为赌博失误家财散尽，流落街头。珍爱生命，远离赌博", NamedTextColor.RED));
        }
    }

    /**
     * 目标在赌徒眼里的“有效身份”：
     * 黑庄如果已经伪装，则必须猜中伪身份；其他人就是真实身份。
     */
    public RoleType effectiveRole(Player target) {
        RoleType real = McmxPlugin.roleOf(target);
        if (real == RoleType.BLACK_DEALER) {
            RoleType fake = plugin.state(target).fakeRole;
            if (fake != null) {
                return fake;
            }
        }
        return real;
    }

    // ------------------------------------------------------------------
    // 黑庄
    // ------------------------------------------------------------------

    public void disguise(Player player) {
        if (McmxPlugin.roleOf(player) != RoleType.BLACK_DEALER) {
            player.sendMessage(Component.text("你不是黑庄。", NamedTextColor.RED));
            return;
        }
      
        if (player.getGameMode() == GameMode.SPECTATOR || bridge.hasTag(player, "spectating")) {
            player.sendMessage(Component.text("你已经出局，不能使用技能。", NamedTextColor.RED));
            return;
        }
        if (fragments.countScrap(player) < blackDealerCost) {
            player.sendMessage(Component.text("碎片不足，需要 " + blackDealerCost + " 个。", NamedTextColor.RED));
            return;
        }
        fragments.removeScrap(player, blackDealerCost);
        RoleType fake = RoleType.DISGUISE_ROLES.get(random.nextInt(RoleType.DISGUISE_ROLES.size()));
        plugin.state(player).fakeRole = fake;

        player.sendMessage(Component.text("你现在伪装成：" + fake.display(), NamedTextColor.LIGHT_PURPLE));
        bridge.broadcast(Component.text("黑庄使用了技能赋予了自己伪身份，赌徒需猜中伪身份才算赢", NamedTextColor.RED));
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /** 赌徒不能捡枪（捡到枪会被数据包当成 gunner 并清空碎片，就没法下注了）。 */
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (McmxPlugin.roleOf(player) != RoleType.GAMBLER) {
            return;
        }
        Item item = event.getItem();
        if (plugin.getFragmentManager().isMcmGun(item.getItemStack())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("赌徒不能捡枪，碎片要留着下注。", NamedTextColor.RED));
        }
    }

    private boolean checkGambler(Player player) {
        if (McmxPlugin.roleOf(player) != RoleType.GAMBLER) {
            player.sendMessage(Component.text("你不是赌徒。", NamedTextColor.RED));
            return false;
        }
        if (player.getGameMode() == GameMode.SPECTATOR || bridge.hasTag(player, "spectating")) {
            player.sendMessage(Component.text("你已经出局，不能再下注。", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    public static String keyOf(RoleType role) {
        return switch (role) {
            case NORMAL_MURDERER -> "murderer";
            case MILK_DRAGON -> "milk";
            case SPY -> "spy";
            case BLACK_DEALER -> "blackdealer";
            default -> role.name().toLowerCase(Locale.ROOT);
        };
    }

    public static RoleType roleFromKey(String key) {
        if (key == null) {
            return null;
        }
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "murderer", "normal", "killer", "杀手", "普通杀手" -> RoleType.NORMAL_MURDERER;
            case "milk", "milk_dragon", "dragon", "奶龙" -> RoleType.MILK_DRAGON;
            case "spy", "mole", "眼线" -> RoleType.SPY;
            case "blackdealer", "black_dealer", "black", "黑庄" -> RoleType.BLACK_DEALER;
            default -> null;
        };
    }
}
