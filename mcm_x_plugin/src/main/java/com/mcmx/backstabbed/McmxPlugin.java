package com.mcmx.backstabbed;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * McmX —— Backstabbed!（谁是杀手）数据包的扩展身份插件。
 *
 * <p>插件不直接改动数据包，而是：</p>
 * <ol>
 *     <li>读取数据包的 {@code CmdData} 计分板（$gamestate / $pickedroles）判断游戏阶段；</li>
 *     <li>在数据包完成基础选角后，用额外标签把坏人的细分为 奶龙 / 眼线；</li>
 *     <li>用额外的 {@code mcmx_detective} 标签标记侦探。</li>
 * </ol>
 */
public class McmxPlugin extends JavaPlugin {

    private final Map<UUID, PlayerState> states = new HashMap<>();

    private GameBridge bridge;
    private RoleManager roleManager;
    private FragmentManager fragmentManager;
    private QueryManager queryManager;
    private RoleVoteManager roleVoteManager;
    private PresetManager presetManager;
    private HornManager hornManager;
    private PurifierManager purifierManager;
    private GambleManager gambleManager;
    private DeathTracker deathTracker;

    private boolean injected;
    private boolean presetsPrepared;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        bridge = new GameBridge(this);
        fragmentManager = new FragmentManager(this, bridge);
        roleManager = new RoleManager(this, bridge);
        queryManager = new QueryManager(this, bridge, fragmentManager);
        roleVoteManager = new RoleVoteManager(this, bridge);
        presetManager = new PresetManager(this);
        hornManager = new HornManager(this);
        purifierManager = new PurifierManager(this);
        gambleManager = new GambleManager(this, bridge, fragmentManager);
        deathTracker = new DeathTracker(this, bridge);

        getServer().getPluginManager().registerEvents(hornManager, this);
        getServer().getPluginManager().registerEvents(purifierManager, this);
        getServer().getPluginManager().registerEvents(gambleManager, this);
        getServer().getPluginManager().registerEvents(deathTracker, this);

        McmxCommand command = new McmxCommand(this);
        if (getCommand("mcmx") != null) {
            getCommand("mcmx").setExecutor(command);
            getCommand("mcmx").setTabCompleter(command);
        }

        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 10L);
        getLogger().info("McmX 已启用（配合 Backstabbed! 数据包）。");
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }

    /** 每 10 tick 跑一次，负责阶段检测、身份注入、碎片/死亡轮询。 */
    private void tick() {
        if (bridge == null || !bridge.isMcmLoaded()) {
            return;
        }

        // 告诉数据包：McmX 插件已安装，身份播报交给插件（mcm:game/role_messages 会跳过）
        bridge.setScore("CmdData", "$mcmx", 1);

        int gamestate = bridge.getScore("CmdData", "$gamestate");
        if (gamestate != 1) {
            roleVoteManager.reset();
            presetsPrepared = false;
            if (injected) {
                resetRound();
                injected = false;
            }
            return;
        }

        int picked = bridge.getScore("CmdData", "$pickedroles");

        // 选身份前把预设转成标签，供数据包 pick_roles 使用（只做一次）
        if (!presetsPrepared && picked == 0) {
            presetManager.prepareRound();
            presetsPrepared = true;
        }

        // 身份投票在选身份之前（宽限期内）进行
        roleVoteManager.tick();

        if (!injected && picked == 1) {
            // 插件中途重载时，如果本局已经注入过，就不要再注入一遍
            boolean alreadyInjected = false;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getScoreboardTags().contains("mcmx_milk_dragon")
                        || player.getScoreboardTags().contains("mcmx_spy")
                        || player.getScoreboardTags().contains("mcmx_detective")) {
                    alreadyInjected = true;
                    break;
                }
            }
            if (alreadyInjected) {
                injected = true;
            } else {
                resetRound();
                roleManager.assignRoles();
                injected = true;
                getLogger().info("McmX 已在第 " + bridge.getScore("CmdData", "$gameID") + " 局注入身份。");
            }
        }

        if (injected) {
            fragmentManager.tick();
            gambleManager.tick();
            deathTracker.tick();
            for (Player player : Bukkit.getOnlinePlayers()) {
                hornManager.tickPoll(player);
            }
        }
    }

    /** 清理本局的插件标签与状态（游戏结束或新一局开始时调用）。 */
    public void resetRound() {
        for (PlayerState state : states.values()) {
            state.resetForNewGame();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removeScoreboardTag("mcmx_milk_dragon");
            player.removeScoreboardTag("mcmx_spy");
            player.removeScoreboardTag("mcmx_detective");
            player.removeScoreboardTag("mcmx_purifier");
            player.removeScoreboardTag("mcmx_gambler");
            player.removeScoreboardTag("mcmx_black_dealer");
            if (hornManager != null) {
                hornManager.removeHorns(player);
            }
            if (purifierManager != null) {
                purifierManager.removePurifiers(player);
            }
        }
    }

    public PlayerState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), key -> new PlayerState());
    }

    /** 根据标签判断玩家身份；奶龙 / 眼线优先于基础 murderer 标签。 */
    public static RoleType roleOf(Player player) {
        if (player.getScoreboardTags().contains("mcmx_detective")) {
            return RoleType.DETECTIVE;
        }
        if (player.getScoreboardTags().contains("mcmx_milk_dragon")) {
            return RoleType.MILK_DRAGON;
        }
        if (player.getScoreboardTags().contains("mcmx_spy")) {
            return RoleType.SPY;
        }
        if (player.getScoreboardTags().contains("mcmx_purifier")) {
            return RoleType.PURIFIER;
        }
        if (player.getScoreboardTags().contains("mcmx_black_dealer")) {
            return RoleType.BLACK_DEALER;
        }
        if (player.getScoreboardTags().contains("mcmx_gambler")) {
            return RoleType.GAMBLER;
        }
        if (player.getScoreboardTags().contains("murderer")) {
            return RoleType.NORMAL_MURDERER;
        }
        if (player.getScoreboardTags().contains("gunner")) {
            return RoleType.GUNNER;
        }
        if (player.getScoreboardTags().contains("innocent")) {
            return RoleType.INNOCENT;
        }
        return RoleType.UNKNOWN;
    }

    public void reloadAll() {
        reloadConfig();
        roleManager.loadConfig();
        fragmentManager.loadConfig();
        hornManager.loadConfig();
        purifierManager.loadConfig();
        gambleManager.loadConfig();
        roleVoteManager.loadConfig();
    }

    public GameBridge getBridge() {
        return bridge;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public QueryManager getQueryManager() {
        return queryManager;
    }

    public HornManager getHornManager() {
        return hornManager;
    }

    public RoleVoteManager getRoleVoteManager() {
        return roleVoteManager;
    }

    public PresetManager getPresetManager() {
        return presetManager;
    }

    public PurifierManager getPurifierManager() {
        return purifierManager;
    }

    public GambleManager getGambleManager() {
        return gambleManager;
    }

    public DeathTracker getDeathTracker() {
        return deathTracker;
    }
}
