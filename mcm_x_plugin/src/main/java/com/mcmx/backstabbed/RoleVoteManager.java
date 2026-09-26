package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 开局宽限期内的“特殊身份投票”。
 *
 * <p>杀手（普通杀手）默认开启，不参与投票；玩家对 奶龙 / 眼线 / 侦探 / 净化者 / 赌徒和黑庄
 * 分别点击 [启用] / [禁用]。投票截止时统计票数，并在聊天栏公布每个身份是谁投的。</p>
 *
 * <p>投票结果只影响本局（覆盖 config 默认值）；`/mcmx role` 的开关作为兜底默认值。</p>
 */
public class RoleVoteManager {

    /** 可投票的身份（顺序即聊天栏显示顺序）。 */
    public static final List<String> ROLE_KEYS = List.of(
            "milk_dragon", "spy", "detective", "purifier", "gambler_black_dealer");

    private final McmxPlugin plugin;
    private final GameBridge bridge;

    public boolean enabled = true;
    public int finalizeGraceperiod = 20;

    private boolean active;
    private boolean finalized;
    private final Map<UUID, Map<String, Boolean>> votes = new HashMap<>();
    private final Map<String, Boolean> result = new HashMap<>();
    private final Set<UUID> prompted = new HashSet<>();

    public RoleVoteManager(McmxPlugin plugin, GameBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
        loadConfig();
    }

    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("role-vote.enabled", true);
        finalizeGraceperiod = plugin.getConfig().getInt("role-vote.finalize-at-graceperiod", 20);
        if (!enabled) {
            reset();
        }
    }

    public void reset() {
        active = false;
        finalized = false;
        votes.clear();
        result.clear();
        prompted.clear();
    }

    /** 每 10 tick 调用一次（仅 gamestate == 1 时）。 */
    public void tick() {
        if (!enabled) {
            return;
        }
        int grace = bridge.getScore("CmdData", "$graceperiod");
        if (!active && !finalized && grace >= 200) {
            start();
        }
        if (active && !finalized) {
            for (Player player : bridge.matchPlayers()) {
                if (prompted.add(player.getUniqueId())) {
                    sendMenu(player);
                }
            }
            if (grace <= finalizeGraceperiod || everyoneVoted()) {
                finalizeVote();
            }
        }
    }

    private void start() {
        active = true;
        finalized = false;
        votes.clear();
        result.clear();
        prompted.clear();
        bridge.broadcast(Component.text("=== 身份投票开始！点击下面按钮投票（杀手默认开启，不受影响） ===", NamedTextColor.YELLOW));
        for (Player player : bridge.matchPlayers()) {
            prompted.add(player.getUniqueId());
            sendMenu(player);
        }
    }

    public void sendMenu(Player player) {
        player.sendMessage(Component.text("请投票决定本局启用的特殊身份：", NamedTextColor.GOLD));
        for (String key : ROLE_KEYS) {
            String name = display(key);
            player.sendMessage(Component.text("  " + name + "： ", NamedTextColor.GRAY)
                    .append(Component.text("[启用]", NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/mcmx vote " + commandKey(key) + " on"))
                            .hoverEvent(HoverEvent.showText(Component.text("投票启用 " + name))))
                    .append(Component.text("  ", NamedTextColor.GRAY))
                    .append(Component.text("[禁用]", NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/mcmx vote " + commandKey(key) + " off"))
                            .hoverEvent(HoverEvent.showText(Component.text("投票禁用 " + name)))));
        }
        player.sendMessage(Component.text("可重复点击修改自己的投票。", NamedTextColor.DARK_GRAY));
    }

    public void vote(Player player, String rawKey, boolean value) {
        if (!enabled) {
            player.sendMessage(Component.text("身份投票未启用。", NamedTextColor.RED));
            return;
        }
        if (!active || finalized) {
            player.sendMessage(Component.text("当前没有进行中的身份投票。", NamedTextColor.RED));
            return;
        }
        String key = canonicalKey(rawKey);
        if (key == null) {
            player.sendMessage(Component.text("未知身份：" + rawKey, NamedTextColor.RED));
            return;
        }
        votes.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>()).put(key, value);
        player.sendMessage(Component.text("你投票：" + (value ? "启用 " : "禁用 ") + display(key), NamedTextColor.GREEN));
    }

    private boolean everyoneVoted() {
        List<Player> players = bridge.matchPlayers();
        if (players.isEmpty()) {
            return false;
        }
        for (Player player : players) {
            Map<String, Boolean> playerVotes = votes.get(player.getUniqueId());
            if (playerVotes == null) {
                return false;
            }
            for (String key : ROLE_KEYS) {
                if (!playerVotes.containsKey(key)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void finalizeVote() {
        finalized = true;
        active = false;
        result.clear();

        bridge.broadcast(Component.text("=== 身份投票结果 ===", NamedTextColor.YELLOW));
        for (String key : ROLE_KEYS) {
            List<String> onVoters = new ArrayList<>();
            List<String> offVoters = new ArrayList<>();
            for (Map.Entry<UUID, Map<String, Boolean>> entry : votes.entrySet()) {
                Boolean value = entry.getValue().get(key);
                if (value == null) {
                    continue;
                }
                Player player = Bukkit.getPlayer(entry.getKey());
                String name = player != null ? player.getName() : "离线玩家";
                (value ? onVoters : offVoters).add(name);
            }
            boolean finalValue = decide(key, onVoters.size(), offVoters.size());
            result.put(key, finalValue);
            String suffix;
            if (onVoters.isEmpty() && offVoters.isEmpty()) {
                suffix = "（无票 → 禁用）";
            } else if (onVoters.size() == offVoters.size()) {
                suffix = "（平票 → 禁用）";
            } else {
                suffix = "";
            }
            bridge.broadcast(Component.text(display(key) + "：" + (finalValue ? "启用" : "禁用") + suffix,
                    finalValue ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (!onVoters.isEmpty()) {
                bridge.broadcast(Component.text("  投启用（" + onVoters.size() + "）：" + String.join("、", onVoters), NamedTextColor.GREEN));
            }
            if (!offVoters.isEmpty()) {
                bridge.broadcast(Component.text("  投禁用（" + offVoters.size() + "）：" + String.join("、", offVoters), NamedTextColor.RED));
            }
        }
    }

    /** 投票结果是否启用某身份；没有结果时用 fallback（config 默认值）。 */
    public boolean isEnabled(String key, boolean fallback) {
        if (!enabled) {
            return fallback;
        }
        Boolean value = result.get(key);
        return value != null ? value : fallback;
    }

    /**
     * 只有「启用票严格多于禁用票」才启用；
     * 无票、平票、禁用票多 一律禁用。
     */
    private boolean decide(String key, int on, int off) {
        return on > off;
    }

    public static String display(String key) {
        return switch (key) {
            case "milk_dragon" -> "奶龙";
            case "spy" -> "眼线";
            case "detective" -> "侦探";
            case "purifier" -> "净化者";
            case "gambler_black_dealer" -> "赌徒和黑庄";
            default -> key;
        };
    }

    public static String commandKey(String key) {
        return switch (key) {
            case "milk_dragon" -> "milk";
            case "gambler_black_dealer" -> "gambler";
            default -> key;
        };
    }

    public static String canonicalKey(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "milk", "milk_dragon", "dragon", "奶龙" -> "milk_dragon";
            case "spy", "mole", "眼线" -> "spy";
            case "detective", "det", "侦探" -> "detective";
            case "purifier", "purify", "净化者" -> "purifier";
            case "gambler", "blackdealer", "black_dealer", "gamble", "赌徒", "黑庄", "赌徒和黑庄" -> "gambler_black_dealer";
            default -> null;
        };
    }
}
