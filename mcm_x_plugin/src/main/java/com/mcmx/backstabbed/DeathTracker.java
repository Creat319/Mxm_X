package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 记录最后伤害来源 + 检测 mcm 的“伪死亡”（dead 计分板），
 * 用于侦探死亡时播报“最后的情报”。
 */
public class DeathTracker implements Listener {

    private static final long KILLER_MEMORY_MS = 30_000L;

    private final McmxPlugin plugin;
    private final GameBridge bridge;
    private final Map<UUID, KillerInfo> lastDamager = new HashMap<>();

    public DeathTracker(McmxPlugin plugin, GameBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    private record KillerInfo(UUID uuid, String name, long time) {
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player killer = resolveKiller(event.getDamager());
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        lastDamager.put(victim.getUniqueId(), new KillerInfo(killer.getUniqueId(), killer.getName(), System.currentTimeMillis()));
    }

    private Player resolveKiller(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    /** 主循环调用。 */
    public void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!bridge.hasTag(player, "queued")) {
                continue;
            }
            PlayerState state = plugin.state(player);
            int dead = bridge.getScore("dead", player.getName());
            if (dead >= 1 && !state.deathHandled) {
                state.deathHandled = true;
                handleDeath(player);
            } else if (dead == 0 && state.deathHandled) {
                state.deathHandled = false;
            }
        }
    }

    private void handleDeath(Player victim) {
        if (McmxPlugin.roleOf(victim) != RoleType.DETECTIVE) {
            return;
        }
        KillerInfo info = lastDamager.get(victim.getUniqueId());
        String killerName = "未知";
        if (info != null && System.currentTimeMillis() - info.time() <= KILLER_MEMORY_MS) {
            killerName = info.name();
        }
        bridge.broadcast(Component.text("侦探“" + victim.getName() + "”被“" + killerName + "”杀死了！", NamedTextColor.RED));
    }
}
