package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;

/**
 * 与 mcm 数据包交互的桥接层：读取计分板、标签，以及调用数据包函数。
 */
public class GameBridge {

    private final McmxPlugin plugin;

    public GameBridge(McmxPlugin plugin) {
        this.plugin = plugin;
    }

    /** 数据包的 CmdData 计分板是否存在。 */
    public boolean isMcmLoaded() {
        return Bukkit.getScoreboardManager().getMainScoreboard().getObjective("CmdData") != null;
    }

    /** 读取主计分板某个目标上的分数，未设置时返回 0。 */
    public int getScore(String objectiveName, String entry) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            return 0;
        }
        Score score = objective.getScore(entry);
        return score.isScoreSet() ? score.getScore() : 0;
    }

    public void setScore(String objectiveName, String entry, int value) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective != null) {
            objective.getScore(entry).setScore(value);
        }
    }

    public boolean hasTag(Player player, String tag) {
        return player.getScoreboardTags().contains(tag);
    }

    public List<Player> playersWithTag(String tag) {
        List<Player> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasTag(player, tag)) {
                result.add(player);
            }
        }
        return result;
    }

    /** 当局还在游戏中的玩家（排队中且不是旁观）。 */
    public List<Player> matchPlayers() {
        List<Player> result = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasTag(player, "queued") && !hasTag(player, "spectating")) {
                result.add(player);
            }
        }
        return result;
    }

    /** 坏人阵营（普通杀手 + 奶龙 + 眼线，数据包里都带 murderer 标签）。 */
    public List<Player> evilPlayers() {
        return playersWithTag("murderer");
    }

    /**
     * 调用 mcm 自己的发物品函数，保证物品组件与数据包完全兼容。
     * 例如 item = "gun"。
     */
    public void giveMcmItem(Player player, String item) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "execute as " + player.getName() + " run function mcm:items/give {item:\"" + item + "\"}");
    }

    /** 给额外生成的黑庄补一套杀手装备（数据包函数）。 */
    public void giveMurdererLoadout(Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "execute as " + player.getName() + " run function mcm:items/loadouts/murderer");
    }

    public void broadcast(Component component) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
    }

    public void broadcastToEvil(Component component) {
        for (Player player : evilPlayers()) {
            player.sendMessage(component);
        }
    }
}
