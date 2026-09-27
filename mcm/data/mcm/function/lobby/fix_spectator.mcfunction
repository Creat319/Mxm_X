#> McmX: 修复卡在旁观的玩家（切回冒险模式 + 回到大厅）
#> 插件只会在 $gamestate != 1（游戏未进行）时调用这个函数。
#> 复用数据包原本的 player_leave：清 spectator/queued 等标记、清效果、发大厅物品。
function mcm:player_leave
