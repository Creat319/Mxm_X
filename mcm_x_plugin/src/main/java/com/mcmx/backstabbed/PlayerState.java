package com.mcmx.backstabbed;

/** 每局游戏中，单个玩家的插件状态。 */
public class PlayerState {

    /** 侦探是否已经解锁“查询身份”。 */
    public boolean detectiveSkillUnlocked;

    /** 剩余查询次数（侦探：解锁后为 2；眼线：从 0 递增到 2）。 */
    public int queryUses;

    /** 侦探是否已经用 10 碎片兑换过手枪。 */
    public boolean gotGunFromFragments;

    /** 本局死亡是否已经处理过（用于“最后的情报”播报）。 */
    public boolean deathHandled;

    /** 上次推送碎片提示的时间，避免刷屏。 */
    public long lastFragmentPrompt;

    /** 上次使用奶龙号角的时间，避免重复触发。 */
    public long lastHornUse;

    /** 上一 tick 号角是否处于冷却中，用于轮询检测使用。 */
    public boolean hornCooldownActive;

    /** 上次成功查询的时间，用于防止连点瞬间查两次。 */
    public long lastQueryTime;

    /** 黑庄当前的伪身份（null = 还没伪装）。 */
    public RoleType fakeRole;

    /** 赌徒当前选中的下注目标。 */
    public java.util.UUID gambleTarget;
    public String gambleTargetName;

    /** 上次推送赌博相关提示的时间。 */
    public long lastGamblePrompt;

    public void resetForNewGame() {
        detectiveSkillUnlocked = false;
        queryUses = 0;
        gotGunFromFragments = false;
        deathHandled = false;
        lastFragmentPrompt = 0L;
        lastHornUse = 0L;
        hornCooldownActive = false;
        lastQueryTime = 0L;
        fakeRole = null;
        gambleTarget = null;
        gambleTargetName = null;
        lastGamblePrompt = 0L;
    }
}
