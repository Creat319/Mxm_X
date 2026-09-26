package com.mcmx.backstabbed;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 在 mcm 数据包完成基础选角（杀手 / 枪手 / 平民）之后，
 * 把坏人细分为 普通杀手 / 奶龙 / 眼线 / 黑庄，并选出 侦探 / 净化者 / 赌徒。
 *
 * <p>预设身份（{@link PresetManager}）优先：被预设的玩家直接获得对应身份，
 * 不会再参与其它身份的随机分配，因此不会出现一个人两个身份。</p>
 */
public class RoleManager {

    private final McmxPlugin plugin;
    private final GameBridge bridge;
    private final Random random = new Random();

    public boolean murdererEnabled = true;
    public boolean milkEnabled = true;
    public boolean spyEnabled = true;
    public boolean detectiveEnabled = true;
    public boolean purifierEnabled = true;
    public boolean purifierRequireMilkDragon = false;
    /** 赌徒 + 黑庄共用一个开关。 */
    public boolean gambleEnabled = true;

    public RoleManager(McmxPlugin plugin, GameBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
        loadConfig();
    }

    public void loadConfig() {
        murdererEnabled = plugin.getConfig().getBoolean("roles.murderer", true);
        milkEnabled = plugin.getConfig().getBoolean("roles.milk_dragon", true);
        spyEnabled = plugin.getConfig().getBoolean("roles.spy", true);
        detectiveEnabled = plugin.getConfig().getBoolean("roles.detective", true);
        purifierEnabled = plugin.getConfig().getBoolean("roles.purifier", true);
        purifierRequireMilkDragon = plugin.getConfig().getBoolean("purifier.require-milk-dragon", false);
        gambleEnabled = plugin.getConfig().getBoolean("roles.gambler_black_dealer", true);
    }

    /** 在 mcm 的 $pickedroles 变为 1 之后调用一次。 */
    public void assignRoles() {
        PresetManager presets = plugin.getPresetManager();
        List<Player> evils = new ArrayList<>(bridge.evilPlayers());

        // 清掉上一局/重复注入的插件标签
        for (Player player : evils) {
            player.removeScoreboardTag("mcmx_milk_dragon");
            player.removeScoreboardTag("mcmx_spy");
            player.removeScoreboardTag("mcmx_black_dealer");
        }
        for (Player player : bridge.playersWithTag("innocent")) {
            player.removeScoreboardTag("mcmx_detective");
            player.removeScoreboardTag("mcmx_purifier");
            player.removeScoreboardTag("mcmx_gambler");
        }

        if (evils.isEmpty()) {
            presets.consumeApplied(bridge);
            return;
        }
        Collections.shuffle(evils, random);

        List<Player> candidates = new ArrayList<>();
        for (Player player : bridge.playersWithTag("innocent")) {
            if (player.getScoreboardTags().contains("gunner")
                    || player.getScoreboardTags().contains("spectating")) {
                continue;
            }
            candidates.add(player);
        }
        Collections.shuffle(candidates, random);

        // 本局实际开关 = 投票结果（若有）覆盖 config 默认值；普通杀手永远开启
        RoleVoteManager vote = plugin.getRoleVoteManager();
        boolean milkOn = vote.isEnabled("milk_dragon", milkEnabled);
        boolean spyOn = vote.isEnabled("spy", spyEnabled);
        boolean detectiveOn = vote.isEnabled("detective", detectiveEnabled);
        boolean purifierOn = vote.isEnabled("purifier", purifierEnabled);
        boolean gambleOn = vote.isEnabled("gambler_black_dealer", gambleEnabled);

        // ---------------- 坏人阵营 ----------------
        List<Player> randomEvils = new ArrayList<>();
        boolean presetMilk = false;
        boolean presetSpy = false;
        boolean presetBlack = false;
        for (Player player : evils) {
            RoleType preset = presets.getPreset(player);
            if (preset == RoleType.MILK_DRAGON) {
                giveMilkDragon(player);
                presetMilk = true;
            } else if (preset == RoleType.SPY) {
                giveSpy(player);
                presetSpy = true;
            } else if (preset == RoleType.BLACK_DEALER) {
                giveBlackDealer(player);
                presetBlack = true;
            } else if (preset == RoleType.NORMAL_MURDERER) {
                // 普通杀手：不需要特殊标签
            } else {
                randomEvils.add(player);
            }
        }

        List<RoleType> enabled = new ArrayList<>();
        List<RoleType> required = new ArrayList<>();
        enabled.add(RoleType.NORMAL_MURDERER);
        if (milkOn && !presetMilk) {
            enabled.add(RoleType.MILK_DRAGON);
            required.add(RoleType.MILK_DRAGON);
        }
        if (spyOn && !presetSpy) {
            enabled.add(RoleType.SPY);
            required.add(RoleType.SPY);
        }
        boolean blackDealerAvailable = gambleOn && !presetBlack && !candidates.isEmpty();
        if (blackDealerAvailable) {
            enabled.add(RoleType.BLACK_DEALER);
        }
        boolean hasBlackDealer = presetBlack;

        List<RoleType> assignment = buildAssignment(randomEvils.size(), enabled, required);
        for (int i = 0; i < randomEvils.size(); i++) {
            Player player = randomEvils.get(i);
            RoleType type = assignment.get(i);
            if (type == RoleType.MILK_DRAGON) {
                giveMilkDragon(player);
            } else if (type == RoleType.SPY) {
                giveSpy(player);
            } else if (type == RoleType.BLACK_DEALER) {
                giveBlackDealer(player);
                hasBlackDealer = true;
            }
        }

        boolean hasMilkDragon = false;
        for (Player player : evils) {
            if (player.getScoreboardTags().contains("mcmx_milk_dragon")) {
                hasMilkDragon = true;
                break;
            }
        }

        // ---------------- 好人阵营 ----------------
        boolean presetDetective = false;
        boolean presetPurifier = false;
        Player gambler = null;
        List<Player> randomGood = new ArrayList<>();
        for (Player player : candidates) {
            RoleType preset = presets.getPreset(player);
            if (preset == RoleType.DETECTIVE) {
                giveDetective(player);
                presetDetective = true;
            } else if (preset == RoleType.PURIFIER) {
                givePurifier(player);
                presetPurifier = true;
            } else if (preset == RoleType.GAMBLER) {
                gambler = player;
                giveGambler(player);
            } else if (preset == RoleType.INNOCENT || preset == RoleType.GUNNER) {
                // 普通好人 / 枪手：数据包已处理
            } else {
                randomGood.add(player);
            }
        }

        // 有黑庄但还没赌徒 -> 补一个赌徒
        if (hasBlackDealer && gambler == null && !randomGood.isEmpty()) {
            gambler = randomGood.remove(0);
            giveGambler(gambler);
        }

        boolean wantDetective = detectiveOn && !presetDetective && evils.size() >= 2;
        boolean wantPurifier = purifierOn && !presetPurifier && (!purifierRequireMilkDragon || hasMilkDragon);

        if (wantDetective && !randomGood.isEmpty()) {
            giveDetective(randomGood.remove(0));
        }
        if (wantPurifier && !randomGood.isEmpty()) {
            givePurifier(randomGood.remove(0));
        }

        // 统一播报最终身份（数据包已把播报权交给插件）
        announceFinalRoles();

        // 预设已生效，清掉标签和本次预设
        presets.consumeApplied(bridge);

        // 枪手介绍：红色大字直接显示在屏幕上
        String gunnerTitle = plugin.getConfig().getString("messages.gunner-title", "铲除一切害人虫");
        for (Player gunner : bridge.playersWithTag("gunner")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!gunner.isOnline()) {
                    return;
                }
                gunner.showTitle(Title.title(
                        Component.text(gunnerTitle, NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))));
            }, 10L);
        }
    }

    // ------------------------------------------------------------------
    // 身份授予
    // ------------------------------------------------------------------

    private void giveMilkDragon(Player player) {
        player.addScoreboardTag("mcmx_milk_dragon");
        plugin.getHornManager().giveHorn(player);
        player.sendMessage(Component.text("你是奶龙（坏人阵营）！右键使用山羊角发动【奶龙爆笑】。", NamedTextColor.LIGHT_PURPLE));
    }

    private void giveSpy(Player player) {
        player.addScoreboardTag("mcmx_spy");
        player.sendMessage(Component.text("你是眼线（坏人阵营，叛变的侦探）！输入 /mcmx query 可消耗 "
                + plugin.getFragmentManager().spyCost + " 个碎片查探一人，共 "
                + plugin.getFragmentManager().spyUses + " 次。", NamedTextColor.DARK_PURPLE));
    }

    private void giveBlackDealer(Player player) {
        player.addScoreboardTag("mcmx_black_dealer");
        plugin.state(player).resetForNewGame();
        player.sendMessage(Component.text("你是黑庄（坏人阵营）！收集 5 个碎片可点击赋予自己一个随机伪身份。", NamedTextColor.DARK_RED));
    }

    private void giveDetective(Player player) {
        player.addScoreboardTag("mcmx_detective");
        plugin.state(player).resetForNewGame();
        player.sendMessage(Component.text("你是侦探（好人阵营）！收集 3 个碎片可解锁查询技能；死亡时会自动播报凶手。", NamedTextColor.AQUA));
    }

    private void givePurifier(Player player) {
        player.addScoreboardTag("mcmx_purifier");
        plugin.state(player).resetForNewGame();
        plugin.getPurifierManager().givePurifier(player);
        player.sendMessage(Component.text("你是净化者（好人阵营）！右键使用净化器，清除周围玩家的 反胃/失明/缓慢，"
                + "但用后你会暴露 5 秒。", NamedTextColor.AQUA));
    }

    private void giveGambler(Player player) {
        player.addScoreboardTag("mcmx_gambler");
        plugin.state(player).resetForNewGame();
        player.sendMessage(Component.text("你是赌徒（好人阵营）！收集 " + plugin.getGambleManager().betCost
                + " 个碎片后下注，猜中对方身份就赢，猜错你会当场死亡。", NamedTextColor.GOLD));
        plugin.getGambleManager().openBet(player);
    }

    // ------------------------------------------------------------------
    // 随机分配
    // ------------------------------------------------------------------

    /**
     * 生成 size 个坏人身份。
     * <ul>
     *     <li>1 个坏人：在启用的特殊身份里随机（奶龙 / 眼线 / 黑庄），都没有则普通杀手。</li>
     *     <li>2 个坏人：在可用身份里随机，特殊身份最多各 1 个；
     *         若池子只有「普通杀手 + 一个奶龙/眼线」则保证各 1 个。</li>
     *     <li>3 个及以上：奶龙/眼线（required）必须出现，黑庄只是随机池的一部分，不强制。</li>
     * </ul>
     */
    List<RoleType> buildAssignment(int size, List<RoleType> enabled, List<RoleType> required) {
        if (size <= 0) {
            return Collections.emptyList();
        }

        if (size == 1) {
            List<RoleType> specials = new ArrayList<>();
            for (RoleType type : enabled) {
                if (type != RoleType.NORMAL_MURDERER) {
                    specials.add(type);
                }
            }
            if (!specials.isEmpty()) {
                return List.of(specials.get(random.nextInt(specials.size())));
            }
            return List.of(RoleType.NORMAL_MURDERER);
        }

        if (size == 2 && murdererEnabled && enabled.size() == 2) {
            RoleType special = null;
            for (RoleType type : enabled) {
                if (type != RoleType.NORMAL_MURDERER) {
                    special = type;
                }
            }
            if (special == RoleType.MILK_DRAGON || special == RoleType.SPY) {
                List<RoleType> pair = new ArrayList<>();
                pair.add(special);
                pair.add(RoleType.NORMAL_MURDERER);
                Collections.shuffle(pair, random);
                return pair;
            }
        }

        boolean requireAll = size >= 3;
        for (int attempt = 0; attempt < 800; attempt++) {
            List<RoleType> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                result.add(enabled.get(random.nextInt(enabled.size())));
            }
            if (count(result, RoleType.MILK_DRAGON) > 1
                    || count(result, RoleType.SPY) > 1
                    || count(result, RoleType.BLACK_DEALER) > 1) {
                continue;
            }
            if (requireAll) {
                boolean allPresent = true;
                for (RoleType type : required) {
                    if (!result.contains(type)) {
                        allPresent = false;
                        break;
                    }
                }
                if (!allPresent) {
                    continue;
                }
            }
            return result;
        }

        List<RoleType> fallback = new ArrayList<>();
        for (RoleType type : required) {
            if (fallback.size() < size) {
                fallback.add(type);
            }
        }
        while (fallback.size() < size) {
            fallback.add(enabled.get(random.nextInt(enabled.size())));
        }
        return fallback;
    }

    /**
     * 插件存在时，由插件统一发送最终身份。
     * 基础身份（杀手/枪手/平民）在这里补聊天栏+标题；
     * 特殊身份的详细技能说明已经在分配时发送，这里只补标题，避免刷屏。
     */
    private void announceFinalRoles() {
        for (Player player : bridge.matchPlayers()) {
            RoleType role = McmxPlugin.roleOf(player);
            switch (role) {
                case NORMAL_MURDERER -> {
                    player.sendMessage(Component.text("你是杀手（坏人阵营）。", NamedTextColor.RED));
                    showRoleTitle(player, "杀手", NamedTextColor.RED);
                }
                case GUNNER -> player.sendMessage(Component.text("你是枪手（好人阵营）。", NamedTextColor.DARK_AQUA));
                case INNOCENT -> {
                    player.sendMessage(Component.text("你是平民（好人阵营）。", NamedTextColor.LIGHT_PURPLE));
                    showRoleTitle(player, "平民", NamedTextColor.LIGHT_PURPLE);
                }
                case MILK_DRAGON -> showRoleTitle(player, "奶龙", NamedTextColor.LIGHT_PURPLE);
                case SPY -> showRoleTitle(player, "眼线", NamedTextColor.DARK_PURPLE);
                case BLACK_DEALER -> showRoleTitle(player, "黑庄", NamedTextColor.DARK_RED);
                case DETECTIVE -> showRoleTitle(player, "侦探", NamedTextColor.AQUA);
                case PURIFIER -> showRoleTitle(player, "净化者", NamedTextColor.AQUA);
                case GAMBLER -> showRoleTitle(player, "赌徒", NamedTextColor.GOLD);
                default -> {
                }
            }
        }

        List<Player> evils = new ArrayList<>(bridge.evilPlayers());
        if (evils.size() >= 2) {
            for (Player evil : evils) {
                StringBuilder names = new StringBuilder();
                for (Player other : evils) {
                    if (other.getUniqueId().equals(evil.getUniqueId())) {
                        continue;
                    }
                    if (names.length() > 0) {
                        names.append("、");
                    }
                    names.append(other.getName());
                }
                evil.sendMessage(Component.text("你的坏人同伙：" + names, NamedTextColor.DARK_RED));
            }
        }
    }

    private void showRoleTitle(Player player, String roleName, NamedTextColor color) {
        player.showTitle(Title.title(
                Component.text(roleName, color).decorate(TextDecoration.BOLD),
                Component.empty(),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))));
    }

    private int count(List<RoleType> list, RoleType type) {
        int count = 0;
        for (RoleType value : list) {
            if (value == type) {
                count++;
            }
        }
        return count;
    }
}
