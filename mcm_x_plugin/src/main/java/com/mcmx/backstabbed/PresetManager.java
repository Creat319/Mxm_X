package com.mcmx.backstabbed;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 下一局身份预设。
 *
 * <p>管理员用 {@code /mcmx preset <玩家> <身份>} 指定某个玩家下一局的身份。
 * 预设写入 {@code presets.yml}，在开局宽限期由 {@link #prepareRound()} 转换成标签，
 * 交给数据包在 {@code mcm:game/pick_roles} 里分配阵营；插件再负责细分特殊身份。
 * 每个预设只生效一次。</p>
 */
public class PresetManager {

    private static final List<String> PRESET_TAGS = List.of(
            "mcmx_preset_evil", "mcmx_preset_good", "mcmx_preset_gunner",
            "mcmx_preset_murderer", "mcmx_preset_milk", "mcmx_preset_spy",
            "mcmx_preset_black_dealer", "mcmx_preset_detective",
            "mcmx_preset_purifier", "mcmx_preset_gambler", "mcmx_preset_innocent");

    private final McmxPlugin plugin;
    private final File file;
    private final Map<UUID, RoleType> presets = new HashMap<>();

    public PresetManager(McmxPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "presets.yml");
        load();
    }

    public void load() {
        presets.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                RoleType role = RoleType.fromName(cfg.getString(key));
                if (role != null) {
                    presets.put(uuid, role);
                }
            } catch (IllegalArgumentException ignored) {
                // 忽略坏数据
            }
        }
    }

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, RoleType> entry : presets.entrySet()) {
            cfg.set(entry.getKey().toString(), entry.getValue().name());
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("无法创建插件目录，预设可能无法保存。");
            }
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("保存 presets.yml 失败: " + ex.getMessage());
        }
    }

    public void set(Player player, RoleType role) {
        presets.put(player.getUniqueId(), role);
        save();
    }

    public void clear(Player player) {
        presets.remove(player.getUniqueId());
        clearPresetTags(player);
        save();
    }

    public RoleType getPreset(Player player) {
        return presets.get(player.getUniqueId());
    }

    public boolean hasPreset(Player player) {
        return presets.containsKey(player.getUniqueId());
    }

    public Map<UUID, RoleType> all() {
        return presets;
    }

    /** 开局选身份前调用：把预设转换成 scoreboard tag，供数据包 pick_roles 使用。 */
    public void prepareRound() {
        for (Map.Entry<UUID, RoleType> entry : presets.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }
            RoleType role = entry.getValue();
            clearPresetTags(player);
            player.addScoreboardTag(role.isEvil() ? "mcmx_preset_evil" : "mcmx_preset_good");
            player.addScoreboardTag(presetTag(role));
            if (role == RoleType.GUNNER) {
                player.addScoreboardTag("mcmx_preset_gunner");
            }
        }
    }

    /** 选完身份后调用：清掉本局生效玩家的标签和预设（一次性）。 */
    public void consumeApplied(GameBridge bridge) {
        List<UUID> done = new ArrayList<>();
        for (UUID id : presets.keySet()) {
            Player player = plugin.getServer().getPlayer(id);
            if (player == null) {
                continue;
            }
            clearPresetTags(player);
            if (bridge.hasTag(player, "queued")) {
                done.add(id);
            }
        }
        for (UUID id : done) {
            presets.remove(id);
        }
        save();
    }

    public void clearPresetTags(Player player) {
        for (String tag : PRESET_TAGS) {
            player.removeScoreboardTag(tag);
        }
    }

    public static String presetTag(RoleType role) {
        return switch (role) {
            case NORMAL_MURDERER -> "mcmx_preset_murderer";
            case MILK_DRAGON -> "mcmx_preset_milk";
            case SPY -> "mcmx_preset_spy";
            case BLACK_DEALER -> "mcmx_preset_black_dealer";
            case DETECTIVE -> "mcmx_preset_detective";
            case PURIFIER -> "mcmx_preset_purifier";
            case GAMBLER -> "mcmx_preset_gambler";
            case GUNNER -> "mcmx_preset_gunner";
            case INNOCENT -> "mcmx_preset_innocent";
            default -> "mcmx_preset_unknown";
        };
    }
}
