package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** 单挑模式：/mx dt */
public class DuelManager {

    public static final String TEAM = "test4";
    private static final Location SPAWN_A = new Location(Bukkit.getWorlds().get(0), -59.5, 10, 1409.5, 0, 0);
    private static final Location SPAWN_B = new Location(Bukkit.getWorlds().get(0), -27.5, 10, 1413.5, 180, 0);

    private final McmxPlugin plugin;
    private final GameBridge bridge;
    private final Random random = new Random();
    private final Map<UUID, String> pendingType = new HashMap<>();
    private final Map<UUID, UUID> inviteFrom = new HashMap<>();

    private UUID duelA;
    private UUID duelB;
    private String duelMode;
    private BukkitTask task;

    public DuelManager(McmxPlugin plugin, GameBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    private String name(String mode) {
        return switch (mode) {
            case "knife" -> "刀刀对决";
            case "gun" -> "枪枪对决";
            default -> "刀枪对决";
        };
    }

    public boolean isDueling(Player p) {
        return p.getUniqueId().equals(duelA) || p.getUniqueId().equals(duelB);
    }

    private boolean busy(Player p) {
        return isDueling(p);
    }

    /** /mx dt */
    public void openMenu(Player p) {
        if (busy(p) || inviteFrom.containsKey(p.getUniqueId())) {
            p.sendMessage(Component.text("你已经在决斗或邀请中。", NamedTextColor.RED));
            return;
        }
        p.sendMessage(Component.text("=== 选择单挑模式 ===", NamedTextColor.GOLD));
        for (String mode : List.of("knife", "gun", "mixed")) {
            p.sendMessage(Component.text("[" + name(mode) + "]", NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand("/mx dt " + mode))
                    .hoverEvent(HoverEvent.showText(Component.text("点击选择 " + name(mode)))));
        }
    }

    /** /mx dt <模式> */
    public void chooseType(Player p, String mode) {
        if (busy(p)) {
            return;
        }
        pendingType.put(p.getUniqueId(), mode);
        p.sendMessage(Component.text("=== 选择要邀请的玩家（" + name(mode) + "）===", NamedTextColor.GOLD));
        for (Player t : Bukkit.getOnlinePlayers()) {
            if (t.equals(p) || busy(t)) {
                continue;
            }
            p.sendMessage(Component.text(" - ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(t.getName(), NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/mx dt " + mode + " " + t.getName()))));
        }
    }

    /** /mx dt <模式> <玩家> */
    public void invite(Player p, String mode, Player target) {
        if (busy(p) || target == null || target.equals(p) || busy(target)) {
            p.sendMessage(Component.text("无法邀请该玩家。", NamedTextColor.RED));
            return;
        }
        inviteFrom.put(target.getUniqueId(), p.getUniqueId());
        pendingType.put(p.getUniqueId(), mode);
        target.sendMessage(Component.text(p.getName() + " 邀请你单挑（" + name(mode) + "）：", NamedTextColor.GOLD)
                .append(Component.text(" [接受]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/mx dt accept " + p.getName())))
                .append(Component.text(" [拒绝]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/mx dt deny " + p.getName()))));
        p.sendMessage(Component.text("已邀请 " + target.getName() + "，等待对方接受。", NamedTextColor.GREEN));
    }

    /** /mx dt accept <邀请者> */
    public void accept(Player p, Player inviter) {
        UUID from = inviteFrom.remove(p.getUniqueId());
        if (from == null || inviter == null || !from.equals(inviter.getUniqueId())) {
            p.sendMessage(Component.text("没有待处理的邀请。", NamedTextColor.RED));
            return;
        }
        if (busy(p) || busy(inviter)) {
            return;
        }
        String mode = pendingType.getOrDefault(inviter.getUniqueId(), "mixed");
        start(inviter, p, mode);
    }

    public void deny(Player p, Player inviter) {
        inviteFrom.remove(p.getUniqueId());
        p.sendMessage(Component.text("已拒绝邀请。", NamedTextColor.YELLOW));
        if (inviter != null) {
            inviter.sendMessage(Component.text(p.getName() + " 拒绝了你的单挑邀请。", NamedTextColor.RED));
        }
    }

    private void start(Player a, Player b, String mode) {
        duelA = a.getUniqueId();
        duelB = b.getUniqueId();
        duelMode = mode;
        boolean aKnife = "knife".equals(mode) || ("mixed".equals(mode) && random.nextBoolean());
        boolean bKnife = "knife".equals(mode) || ("mixed".equals(mode) && !aKnife);

        prepare(a, SPAWN_A, aKnife);
        prepare(b, SPAWN_B, bKnife);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join " + TEAM + " " + a.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join " + TEAM + " " + b.getName());
        a.sendMessage(Component.text("单挑开始：" + name(mode), NamedTextColor.GOLD));
        b.sendMessage(Component.text("单挑开始：" + name(mode), NamedTextColor.GOLD));

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 5L);
    }

    private void prepare(Player p, Location spawn, boolean knife) {
        p.teleport(spawn);
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        bridge.giveMcmItem(p, knife ? "knife" : "gun");
        p.addScoreboardTag("mcmx_duel");
        if (knife) {
            p.addScoreboardTag("HoldKnife");
        }
    }

    private void tick() {
        Player a = duelA == null ? null : Bukkit.getPlayer(duelA);
        Player b = duelB == null ? null : Bukkit.getPlayer(duelB);
        if (a == null || b == null) {
            end("玩家离线");
            return;
        }
        // 主游戏开始 / 被游戏传走 -> 结束
        if (bridge.getScore("CmdData", "$gamestate") == 1) {
            end("主游戏开始");
            return;
        }
        if (a.getLocation().distanceSquared(SPAWN_A) > 3600 || b.getLocation().distanceSquared(SPAWN_B) > 3600) {
            end("玩家被传送离开");
            return;
        }
        if (a.getLocation().getY() < 6) {
            end(b.getName() + " 获胜（" + a.getName() + " 掉入虚空）");
            return;
        }
        if (b.getLocation().getY() < 6) {
            end(a.getName() + " 获胜（" + b.getName() + " 掉入虚空）");
            return;
        }
        if (a.getGameMode() == GameMode.SPECTATOR || bridge.getScore("dead", a.getName()) >= 1) {
            end(b.getName() + " 获胜");
            return;
        }
        if (b.getGameMode() == GameMode.SPECTATOR || bridge.getScore("dead", b.getName()) >= 1) {
            end(a.getName() + " 获胜");
        }
    }

    public void end(String reason) {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID id : List.of(duelA == null ? new UUID(0, 0) : duelA, duelB == null ? new UUID(0, 0) : duelB)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                continue;
            }
            p.removeScoreboardTag("mcmx_duel");
            p.removeScoreboardTag("HoldKnife");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team leave " + TEAM + " " + p.getName());
            p.setGameMode(GameMode.ADVENTURE);
            p.teleport(new Location(p.getWorld(), -1, 1, 69, 0, 0));
            p.getInventory().clear();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(Component.text("[单挑] 结束：" + reason, NamedTextColor.GOLD));
        }
        duelA = null;
        duelB = null;
        duelMode = null;
    }
}
