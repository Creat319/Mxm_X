#> 客栈惨案（Inn）地图逻辑

#> 虚空死亡：Y < -10 直接死（建筑/旁观/免出图玩家除外）
execute as @a[tag=queued,tag=!spectating,tag=!mcmx_pg,gamemode=!creative,gamemode=!spectator] at @s if entity @s[y=..-10] run function mcm:game/playerdeath

#> TODO: 出图传送 / 旁观者边界 —— 等你提供地图实际边界后再加
