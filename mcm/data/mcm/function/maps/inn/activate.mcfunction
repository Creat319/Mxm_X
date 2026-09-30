#> 客栈惨案（Inn）地图激活
#> 清理上一张图残留的地图实体
kill @e[tag=MapEntity]

#> 玩家出生点（同时也是碎片刷新点）
execute positioned -16 -7 854 run function mcm:game/markers/playerspawn
execute positioned -43 3 874 run function mcm:game/markers/playerspawn
execute positioned 15 -5 864 run function mcm:game/markers/playerspawn
execute positioned -24 -5 885 run function mcm:game/markers/playerspawn
execute positioned 5 15 836 run function mcm:game/markers/playerspawn
execute positioned -34 15 837 run function mcm:game/markers/playerspawn

#> 额外碎片刷新点
execute positioned 4 -5 856 run function mcm:game/markers/scrapspawn
execute positioned -34 -5 859 run function mcm:game/markers/scrapspawn
execute positioned 14 3 864 run function mcm:game/markers/scrapspawn
execute positioned -33 3 864 run function mcm:game/markers/scrapspawn
execute positioned -14 -5 835 run function mcm:game/markers/scrapspawn

#> 旁观者出生点（可自行调整）
execute positioned -14 20 860 rotated 0 0 run function mcm:game/markers/spectatorspawn

tellraw @a ["\n",{"text":"| ","bold":true,"color":"dark_gray"},{"translate":"mcm.map.ready","underlined":true,"color":"green","bold":false}]
tellraw @a [{"text":"| ","bold":true,"color":"dark_gray"},{"translate":"mcm.map.selected","color":"gray","bold":false,"with":[{"translate":"mcm.inn.name","color":"dark_green"}]}]
