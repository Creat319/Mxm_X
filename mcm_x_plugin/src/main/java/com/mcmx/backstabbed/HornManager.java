package com.mcmx.backstabbed;

import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

/**
 * 奶龙的山羊角技能：
 * <ul>
 *     <li>右键使用后消失，{@code cooldown-ticks} 后返还。</li>
 *     <li>对半径内除使用者外的玩家施加 反胃 + 缓慢 + 失明。</li>
 *     <li>掉落在地上后，只有号角主人（且仍是奶龙）能捡起/使用。</li>
 * </ul>
 * 使用检测有三重保障：Paper 的物品冷却事件、右键事件、每 tick 轮询冷却状态。
 */
public class HornManager implements Listener {

    private final McmxPlugin plugin;
    private final NamespacedKey hornKey;

    public double radius;
    public int durationTicks;
    public int cooldownTicks;
    public int nauseaAmplifier;
    public int slownessAmplifier;
    public int blindnessAmplifier;

    public HornManager(McmxPlugin plugin) {
        this.plugin = plugin;
        this.hornKey = new NamespacedKey(plugin, "milk_horn");
        loadConfig();
    }

    public void loadConfig() {
        radius = plugin.getConfig().getDouble("milk-dragon.radius", 20.0D);
        durationTicks = plugin.getConfig().getInt("milk-dragon.duration-ticks", 160);
        cooldownTicks = plugin.getConfig().getInt("milk-dragon.cooldown-ticks", 1200);
        nauseaAmplifier = plugin.getConfig().getInt("milk-dragon.nausea-amplifier", 0);
        slownessAmplifier = plugin.getConfig().getInt("milk-dragon.slowness-amplifier", 2);
        blindnessAmplifier = plugin.getConfig().getInt("milk-dragon.blindness-amplifier", 2);
    }

    // ------------------------------------------------------------------
    // 物品
    // ------------------------------------------------------------------

    public ItemStack createHorn(Player owner) {
        ItemStack horn = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = horn.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("奶龙号角", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(
                    Component.text("右键使用：奶龙爆笑", NamedTextColor.GRAY),
                    Component.text("半径 " + (int) radius + " 内的其他玩家反胃、缓慢、失明", NamedTextColor.DARK_GRAY)));
            meta.getPersistentDataContainer().set(hornKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            horn.setItemMeta(meta);
        }
        return horn;
    }

    public boolean isHorn(ItemStack stack) {
        if (stack == null || stack.getType() != Material.GOAT_HORN) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(hornKey, PersistentDataType.STRING);
    }

    public UUID getOwner(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = meta.getPersistentDataContainer().get(hornKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean hasHorn(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isHorn(stack)) {
                return true;
            }
        }
        return false;
    }

    public void giveHorn(Player player) {
        if (hasHorn(player)) {
            return;
        }
        ItemStack horn = createHorn(player);
        player.getInventory().addItem(horn).forEach((index, leftover) ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    /** 从玩家背包里移除所有奶龙号角（回合结束 / 重载清理）。 */
    public void removeHorns(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            if (isHorn(contents[i])) {
                contents[i] = null;
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setContents(contents);
        }
    }

    // ------------------------------------------------------------------
    // 使用检测
    // ------------------------------------------------------------------

    @EventHandler
    public void onItemCooldown(PlayerItemCooldownEvent event) {
        if (event.getType() == Material.GOAT_HORN) {
            tryUse(event.getPlayer());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!isHorn(item)) {
            return;
        }
        Player player = event.getPlayer();
        if (McmxPlugin.roleOf(player) != RoleType.MILK_DRAGON) {
            event.setCancelled(true);
            player.sendMessage(Component.text("只有奶龙能使用这个号角！", NamedTextColor.RED));
            return;
        }
        // 只有在没有冷却时才算一次真正的吹奏（防止连点误触发）。
        if (player.hasCooldown(Material.GOAT_HORN)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> tryUse(player), 1L);
    }

    /** 每 tick 轮询：冷却从无到有，说明玩家吹了号角。 */
    public void tickPoll(Player player) {
        if (McmxPlugin.roleOf(player) != RoleType.MILK_DRAGON) {
            plugin.state(player).hornCooldownActive = false;
            return;
        }
        PlayerState state = plugin.state(player);
        boolean cooling = player.hasCooldown(Material.GOAT_HORN);
        if (cooling && !state.hornCooldownActive) {
            tryUse(player);
        }
        state.hornCooldownActive = cooling;
    }

    public void tryUse(Player player) {
        if (McmxPlugin.roleOf(player) != RoleType.MILK_DRAGON) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (plugin.getBridge().getScore("CmdData", "$gamestate") != 1) {
            return;
        }
        PlayerState state = plugin.state(player);
        long now = System.currentTimeMillis();
        if (now - state.lastHornUse < 1500L) {
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        boolean useMain = isHorn(main);
        if (!useMain && !isHorn(off)) {
            return;
        }

        state.lastHornUse = now;
        if (useMain) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInOffHand(null);
        }

        applyEffects(player);
        player.sendMessage(Component.text("你发动了【奶龙爆笑】！号角将在 " + (cooldownTicks / 20) + " 秒后返还。", NamedTextColor.LIGHT_PURPLE));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (McmxPlugin.roleOf(player) != RoleType.MILK_DRAGON) {
                return;
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                return;
            }
            if (plugin.getBridge().getScore("CmdData", "$gamestate") != 1) {
                return;
            }
            giveHorn(player);
            player.sendMessage(Component.text("你的奶龙号角已恢复。", NamedTextColor.LIGHT_PURPLE));
        }, cooldownTicks);
    }

    private void applyEffects(Player source) {
        GameBridge bridge = plugin.getBridge();
        double radiusSquared = radius * radius;
        for (Player target : source.getWorld().getPlayers()) {
            // 不作用自己
            if (target.getUniqueId().equals(source.getUniqueId())) {
                continue;
            }
            // 不作用旁观的死人
            if (target.getGameMode() == GameMode.SPECTATOR || bridge.hasTag(target, "spectating")) {
                continue;
            }
            // 不作用狼队友（奶龙/眼线/黑庄等所有带 murderer 标签的坏人）
            if (bridge.hasTag(target, "murderer")) {
                continue;
            }
            // 只作用本局玩家
            if (!bridge.hasTag(target, "queued")) {
                continue;
            }
            if (target.getLocation().distanceSquared(source.getLocation()) > radiusSquared) {
                continue;
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, durationTicks, nauseaAmplifier, false, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, slownessAmplifier, false, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, blindnessAmplifier, false, false, true));
        }
    }

    // ------------------------------------------------------------------
    // 掉落保护
    // ------------------------------------------------------------------

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        ItemStack stack = item.getItemStack();
        if (!isHorn(stack)) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        if (McmxPlugin.roleOf(player) != RoleType.MILK_DRAGON) {
            event.setCancelled(true);
            return;
        }
        UUID owner = getOwner(stack);
        if (owner != null && !owner.equals(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
