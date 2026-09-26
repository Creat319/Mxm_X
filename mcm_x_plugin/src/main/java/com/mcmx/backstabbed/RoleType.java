package com.mcmx.backstabbed;

/**
 * 插件识别到的身份。
 * <p>注意：奶龙、眼线在数据包里仍然带有 {@code murderer} 标签（属于坏人阵营），
 * 这样 mcm 的胜负判定、掉落、提示等逻辑才能正常运作；插件用额外标签区分它们。</p>
 */
public enum RoleType {

    NORMAL_MURDERER("普通杀手", true),
    MILK_DRAGON("奶龙", true),
    SPY("眼线", true),
    BLACK_DEALER("黑庄", true),
    DETECTIVE("侦探", false),
    PURIFIER("净化者", false),
    GAMBLER("赌徒", false),
    GUNNER("枪手", false),
    INNOCENT("平民", false),
    UNKNOWN("未知", false);

    /** 赌徒可以押注的全部坏人身份（不管当局有没有出现）。 */
    public static final java.util.List<RoleType> BAD_ROLES = java.util.List.of(
            NORMAL_MURDERER, MILK_DRAGON, SPY, BLACK_DEALER);

    /** 黑庄伪身份的随机池。 */
    public static final java.util.List<RoleType> DISGUISE_ROLES = java.util.List.of(
            NORMAL_MURDERER, MILK_DRAGON, SPY);

    private final String display;
    private final boolean evil;

    RoleType(String display, boolean evil) {
        this.display = display;
        this.evil = evil;
    }

    public String display() {
        return display;
    }

    public boolean isEvil() {
        return evil;
    }

    /** 解析命令/配置里的身份名。 */
    public static RoleType fromName(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "murderer", "killer", "wolf", "normal", "杀手", "普通杀手" -> NORMAL_MURDERER;
            case "milk", "milk_dragon", "milkdragon", "dragon", "奶龙" -> MILK_DRAGON;
            case "spy", "mole", "眼线" -> SPY;
            case "blackdealer", "black_dealer", "black", "黑庄" -> BLACK_DEALER;
            case "detective", "det", "侦探" -> DETECTIVE;
            case "purifier", "purify", "净化者" -> PURIFIER;
            case "gambler", "bet", "赌徒" -> GAMBLER;
            case "gunner", "枪手" -> GUNNER;
            case "innocent", "平民", "无辜者" -> INNOCENT;
            default -> null;
        };
    }
}
