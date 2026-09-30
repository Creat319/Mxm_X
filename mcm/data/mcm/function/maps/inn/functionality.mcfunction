#> 客栈惨案（Inn）地图逻辑

#> 虚空死亡：Y < -10 直接死（建筑/旁观/免出图玩家除外）
execute as @a[tag=queued,tag=!spectating,tag=!mcmx_pg,gamemode=!creative,gamemode=!spectator] at @s if entity @s[y=..-10] run function mcm:game/playerdeath

#> 出图传送回出生点（免出图/创造/旁观除外）
execute as @a[tag=queued,tag=!spectating,tag=!mcmx_pg,gamemode=!creative,gamemode=!spectator,predicate=!mcm:bounding_boxes/inn] at @s run tp @s @e[tag=PlayerSpawn,limit=1,sort=random]

#> 旁观者边界
execute as @a[tag=spectating,tag=!mcmx_pg,gamemode=!creative] at @s unless predicate mcm:bounding_boxes/inn run tp @s @e[type=marker,tag=SpectatorSpawn,limit=1,sort=nearest]
execute as @a[tag=spectating,tag=!mcmx_pg,gamemode=!creative] at @s unless predicate mcm:bounding_boxes/inn run playsound minecraft:entity.shulker.shoot hostile @s ~ ~ ~ 1 1 0
