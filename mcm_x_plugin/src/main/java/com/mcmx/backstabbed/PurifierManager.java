package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * 好人身份「净化者」的净化器：
 * <ul>
 *     <li>开局发放一个原版喷溅水瓶外观的“净化器”。</li>
 *     <li>右键使用，清除半径内所有玩家的 反胃 / 失明 / 缓慢。</li>
 *     <li>使用后净化器直接爆掉（不可再用）。</li>
 *     <li>爆掉后净化者自身获得 5 秒发光，位置暴露。</li>
 * </ul>
 */
public class PurifierManager implements Listener {

    private final McmxPlugin plugin;
    private final NamespacedKey purifierKey;

    public double radius;
    public int glowingTicks;

    public PurifierManager(McmxPlugin plugin) {
        this.plugin = plugin;
        this.purifierKey = new NamespacedKey(plugin, "purifier");
        loadConfig();
    }

    public void loadConfig() {
        radius = plugin.getConfig().getDouble("purifier.radius", 20.0D);
        glowingTicks = plugin.getConfig().getInt("purifier.glowing-ticks", 100);
    }

    // ------------------------------------------------------------------
    // 物品
    // ------------------------------------------------------------------

    public ItemStack createPurifier() {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("净化器", NamedTextColor.AQUA));
            meta.lore(List.of(
                    Component.text("右键使用：净化周围玩家", NamedTextColor.GRAY),
                    Component.text("清除 反胃 / 失明 / 缓慢", NamedTextColor.DARK_GRAY),
                    Component.text("用后爆裂，并暴露自身 5 秒", NamedTextColor.RED)));
            meta.getPersistentDataContainer().set(purifierKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isPurifier(ItemStack stack) {
        if (stack == null || stack.getType() != Material.SPLASH_POTION) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(purifierKey, PersistentDataType.BYTE);
    }

    public boolean hasPurifier(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isPurifier(stack)) {
                return true;
            }
        }
        return false;
    }

    public void givePurifier(Player player) {
        removePurifiers(player);
        player.getInventory().addItem(createPurifier()).forEach((index, leftover) ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    public void removePurifiers(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            if (isPurifier(contents[i])) {
                contents[i] = null;
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setContents(contents);
        }
    }

    // ------------------------------------------------------------------
    // 使用
    // ------------------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!isPurifier(item)) {
            return;
        }
        // 阻止原版把喷溅水瓶扔出去
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (McmxPlugin.roleOf(player) != RoleType.PURIFIER) {
            player.sendMessage(Component.text("只有净化者能使用净化器！", NamedTextColor.RED));
            return;
        }
        if (plugin.getBridge().getScore("CmdData", "$gamestate") != 1) {
            return;
        }

        removePurifiers(player);
        cleanse(player);

        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowingTicks, 0, false, false, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F);
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 1);
        player.sendMessage(Component.text("净化器已爆裂！你被暴露了 " + (glowingTicks / 20) + " 秒。", NamedTextColor.RED));
    }

    /** 清除半径内所有玩家的 反胃 / 失明 / 缓慢。 */
    private void cleanse(Player source) {
        double radiusSquared = radius * radius;
        for (Player target : source.getWorld().getPlayers()) {
            if (target.getLocation().distanceSquared(source.getLocation()) > radiusSquared) {
                continue;
            }
            target.removePotionEffect(PotionEffectType.NAUSEA);
            target.removePotionEffect(PotionEffectType.BLINDNESS);
            target.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }
}
