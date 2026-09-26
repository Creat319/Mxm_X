package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 侦探 / 眼线的查探系统。
 * <ul>
 *     <li>侦探：解锁后免费查探 2 次，向自己显示目标身份。</li>
 *     <li>眼线：每次消耗 3 个碎片，共 2 次，被查者获得 10 秒失明 + 发光。</li>
 * </ul>
 */
public class QueryManager {

    private final McmxPlugin plugin;
    private final GameBridge bridge;
    private final FragmentManager fragments;

    public QueryManager(McmxPlugin plugin, GameBridge bridge, FragmentManager fragments) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.fragments = fragments;
    }

    /** /mcmx query —— 打开可点击的玩家列表。 */
    public void openMenu(Player player) {
        RoleType role = McmxPlugin.roleOf(player);
        PlayerState state = plugin.state(player);

        if (role == RoleType.DETECTIVE) {
            if (!state.detectiveSkillUnlocked) {
                player.sendMessage(Component.text("你还没有解锁查询技能，先收集碎片吧。", NamedTextColor.RED));
                return;
            }
            if (state.queryUses <= 0) {
                player.sendMessage(Component.text("你的查询次数已用完。", NamedTextColor.RED));
                return;
            }
        } else if (role == RoleType.SPY) {
            if (state.queryUses >= fragments.spyUses) {
                player.sendMessage(Component.text("你的查探次数已用完。", NamedTextColor.RED));
                return;
            }
            if (fragments.countScrap(player) < fragments.spyCost) {
                player.sendMessage(Component.text("碎片不足，需要 " + fragments.spyCost + " 个。", NamedTextColor.RED));
                return;
            }
        } else {
            player.sendMessage(Component.text("你不是侦探或眼线。", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("=== 选择要查探的玩家（点击名字） ===", NamedTextColor.GOLD));
        for (Player target : bridge.matchPlayers()) {
            if (target.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            player.sendMessage(Component.text(" - ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/mcmx query " + target.getName()))
                            .hoverEvent(HoverEvent.showText(Component.text("点击查探 " + target.getName())))));
        }
        if (role == RoleType.DETECTIVE) {
            player.sendMessage(Component.text("剩余查询次数：" + state.queryUses, NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("剩余查探次数：" + (fragments.spyUses - state.queryUses)
                    + "，当前碎片：" + fragments.countScrap(player), NamedTextColor.GRAY));
        }
    }

    /** /mcmx query <玩家名> */
    public void query(Player player, String targetName) {
        RoleType role = McmxPlugin.roleOf(player);
        PlayerState state = plugin.state(player);

        // 查询冷却：防止连点/手速过快时瞬间查两次
        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfig().getLong("query.cooldown-ms", 1000L);
        long elapsed = now - state.lastQueryTime;
        if (elapsed < cooldownMs) {
            long waitSeconds = Math.max(1L, (cooldownMs - elapsed + 999L) / 1000L);
            player.sendMessage(Component.text("查询太快了，你还有 " + state.queryUses
                    + " 次查询，请 " + waitSeconds + " 秒后再试。", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !bridge.matchPlayers().contains(target)) {
            player.sendMessage(Component.text("找不到该玩家（可能已不在本局游戏中）。", NamedTextColor.RED));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("不能查探自己。", NamedTextColor.RED));
            return;
        }

        if (role == RoleType.DETECTIVE) {
            if (!state.detectiveSkillUnlocked) {
                player.sendMessage(Component.text("你还没有解锁查询技能。", NamedTextColor.RED));
                return;
            }
            if (state.queryUses <= 0) {
                player.sendMessage(Component.text("你的查询次数已用完。", NamedTextColor.RED));
                return;
            }
            state.queryUses--;
            state.lastQueryTime = now;
            reveal(player, target);
            if (state.queryUses > 0) {
                player.sendMessage(Component.text("剩余查询次数：" + state.queryUses + "　", NamedTextColor.GRAY)
                        .append(Component.text("[点击继续查询]", NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/mcmx query"))
                                .hoverEvent(HoverEvent.showText(Component.text("冷却 " + (cooldownMs / 1000) + " 秒后可再次查询")))));
            } else {
                player.sendMessage(Component.text("剩余查询次数：0（已用完）", NamedTextColor.GRAY));
            }
        } else if (role == RoleType.SPY) {
            if (state.queryUses >= fragments.spyUses) {
                player.sendMessage(Component.text("你的查探次数已用完。", NamedTextColor.RED));
                return;
            }
            if (fragments.countScrap(player) < fragments.spyCost) {
                player.sendMessage(Component.text("碎片不足，需要 " + fragments.spyCost + " 个。", NamedTextColor.RED));
                return;
            }
            fragments.removeScrap(player, fragments.spyCost);
            state.queryUses++;
            state.lastQueryTime = now;

            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0, false, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, false, false, true));

            reveal(player, target);
            player.sendMessage(Component.text("剩余查探次数：" + (fragments.spyUses - state.queryUses), NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("你不是侦探或眼线。", NamedTextColor.RED));
        }
    }

    /** 只把身份告诉查探者本人。 */
    private void reveal(Player viewer, Player target) {
        RoleType targetRole = McmxPlugin.roleOf(target);
        NamedTextColor color = targetRole.isEvil() ? NamedTextColor.RED : NamedTextColor.GREEN;
        viewer.sendMessage(Component.text("查探结果：", NamedTextColor.GOLD)
                .append(Component.text(target.getName(), NamedTextColor.AQUA))
                .append(Component.text(" 的身份是 ", NamedTextColor.GOLD))
                .append(Component.text(targetRole.display(), color)));
    }
}
