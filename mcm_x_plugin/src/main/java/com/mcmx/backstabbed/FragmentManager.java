package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 碎片（数据包里的 netherite_scrap）统计，以及侦探技能/兑换枪的入口。
 */
public class FragmentManager {

    private final McmxPlugin plugin;
    private final GameBridge bridge;

    public int detectiveUnlockCost;
    public int detectiveGunCost;
    public int detectiveUses;
    public int spyCost;
    public int spyUses;
    private long promptIntervalMs;

    public FragmentManager(McmxPlugin plugin, GameBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
        loadConfig();
    }

    public void loadConfig() {
        detectiveUnlockCost = plugin.getConfig().getInt("fragments.detective-unlock-cost", 3);
        detectiveGunCost = plugin.getConfig().getInt("fragments.detective-gun-cost", 10);
        detectiveUses = plugin.getConfig().getInt("fragments.detective-uses", 2);
        spyCost = plugin.getConfig().getInt("fragments.spy-cost", 3);
        spyUses = plugin.getConfig().getInt("fragments.spy-uses", 2);
        promptIntervalMs = plugin.getConfig().getLong("prompt-interval-ms", 5000L);
    }

    public int countScrap(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == Material.NETHERITE_SCRAP) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public void removeScrap(Player player, int amount) {
        int left = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != Material.NETHERITE_SCRAP) {
                continue;
            }
            int take = Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            left -= take;
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
    }

    /** 判断玩家是否已经拥有 mcm 的枪（通过 item_model 识别）。 */
    public boolean hasGun(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isMcmGun(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMcmGun(ItemStack stack) {
        if (stack == null || stack.getType() != Material.WARPED_FUNGUS_ON_A_STICK) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasItemModel() || meta.getItemModel() == null) {
            return false;
        }
        return meta.getItemModel().toString().equals("minecraft:role_items/gun");
    }

    /** 由主循环每 10 tick 调用一次。 */
    public void tick() {
        long now = System.currentTimeMillis();
        for (Player player : bridge.matchPlayers()) {
            RoleType role = McmxPlugin.roleOf(player);
            PlayerState state = plugin.state(player);
            int scrap = countScrap(player);

            if (role == RoleType.DETECTIVE) {
                if (!state.detectiveSkillUnlocked
                        && scrap >= detectiveUnlockCost
                        && now - state.lastFragmentPrompt > promptIntervalMs) {
                    state.lastFragmentPrompt = now;
                    player.sendMessage(Component.text("你收集到了足够的碎片！", NamedTextColor.GREEN)
                            .append(Component.text(" [点击消耗 " + detectiveUnlockCost + " 个碎片获得技能：查询身份]", NamedTextColor.YELLOW)
                                    .clickEvent(ClickEvent.runCommand("/mcmx detective unlock"))
                                    .hoverEvent(HoverEvent.showText(Component.text("解锁后可查询 " + detectiveUses + " 次")))));
                }
                if (!state.gotGunFromFragments
                        && scrap >= detectiveGunCost
                        && !hasGun(player)
                        && now - state.lastFragmentPrompt > promptIntervalMs) {
                    state.lastFragmentPrompt = now;
                    player.sendMessage(Component.text("你收集到了 " + detectiveGunCost + " 个碎片！", NamedTextColor.GREEN)
                            .append(Component.text(" [点击兑换手枪]", NamedTextColor.GOLD)
                                    .clickEvent(ClickEvent.runCommand("/mcmx detective gun"))
                                    .hoverEvent(HoverEvent.showText(Component.text("消耗 " + detectiveGunCost + " 个碎片")))));
                }
            } else if (role == RoleType.SPY) {
                if (state.queryUses < spyUses
                        && scrap >= spyCost
                        && now - state.lastFragmentPrompt > promptIntervalMs) {
                    state.lastFragmentPrompt = now;
                    player.sendMessage(Component.text("你的碎片足够了！", NamedTextColor.DARK_PURPLE)
                            .append(Component.text(" [输入 /mcmx query 选择查探目标，消耗 " + spyCost + " 个碎片，剩余 "
                                    + (spyUses - state.queryUses) + " 次]", NamedTextColor.LIGHT_PURPLE)));
                }
            }
        }
    }

    /** /mcmx detective unlock */
    public void unlockDetectiveSkill(Player player) {
        if (McmxPlugin.roleOf(player) != RoleType.DETECTIVE) {
            player.sendMessage(Component.text("你不是侦探。", NamedTextColor.RED));
            return;
        }
        PlayerState state = plugin.state(player);
        if (state.detectiveSkillUnlocked) {
            player.sendMessage(Component.text("你已经获得过技能了，不能再选。", NamedTextColor.RED));
            return;
        }
        if (countScrap(player) < detectiveUnlockCost) {
            player.sendMessage(Component.text("碎片不足，需要 " + detectiveUnlockCost + " 个。", NamedTextColor.RED));
            return;
        }
        removeScrap(player, detectiveUnlockCost);
        state.detectiveSkillUnlocked = true;
        state.queryUses = detectiveUses;
        player.sendMessage(Component.text("你获得了技能：查询身份（剩余 " + detectiveUses + " 次）。输入 /mcmx query 使用。", NamedTextColor.GREEN));
        bridge.broadcastToEvil(Component.text("侦探已获得技能，注意安全！", NamedTextColor.RED));
    }

    /** /mcmx detective gun */
    public void buyGun(Player player) {
        if (McmxPlugin.roleOf(player) != RoleType.DETECTIVE) {
            player.sendMessage(Component.text("你不是侦探。", NamedTextColor.RED));
            return;
        }
        PlayerState state = plugin.state(player);
        if (state.gotGunFromFragments) {
            player.sendMessage(Component.text("你已经兑换过手枪了。", NamedTextColor.RED));
            return;
        }
        if (countScrap(player) < detectiveGunCost) {
            player.sendMessage(Component.text("碎片不足，需要 " + detectiveGunCost + " 个。", NamedTextColor.RED));
            return;
        }
        removeScrap(player, detectiveGunCost);
        state.gotGunFromFragments = true;
        bridge.giveMcmItem(player, "gun");
        player.sendMessage(Component.text("你用 " + detectiveGunCost + " 个碎片兑换了一把手枪。", NamedTextColor.GOLD));
    }
}
