#> 基础阵营身份播报（仅在 McmX 插件不存在时使用）。
#> 插件存在时（$mcmx CmdData == 1），由插件统一发送最终身份消息，
#> 包含额外的 奶龙/眼线/黑庄/侦探/净化者/赌徒。

execute if score $murderers GameRules matches ..1 run title @a[tag=murderer] subtitle {"translate":"mcm.game.murderer.subtitle","color":"gold"}
execute if score $murderers GameRules matches ..1 run tellraw @a[tag=murderer] {"translate":"mcm.game.role","color":"gold","with":[{"translate":"mcm.game.murderer","color":"red"}]}
title @a[tag=murderer] title {"translate":"mcm.game.role","color":"gold","with":[{"translate":"mcm.game.murderer","color":"red"}]}
execute if score $murderers GameRules matches 2 as @a[tag=murderer] at @s run title @s subtitle {"translate":"mcm.game.murderer2.subtitle", "color":"gold", "with" :[{"selector" : "@p[tag=murderer,distance=0.01..]", "color":"green"}]}
execute if score $murderers GameRules matches 2 as @a[tag=murderer] at @s run tellraw @s {"translate":"mcm.game.murderer2.msg","color":"gold","with":[{"selector":"@p[tag=murderer,distance=0.01..]", "color":"green"}, {"selector":"@s","color":"green"}, {"translate":"mcm.game.murderers","color":"gold"}]}
execute if score $murderers GameRules matches 3 as @a[tag=murderer] at @s run title @s subtitle {"translate":"mcm.game.murderer3.subtitle", "color":"gold", "with" :[{"selector" :"@p[tag=murderer,distance=0.01..]", "color":"green"},{"selector" :"@p[tag=murderer,distance=0.01..,sort=furthest]", "color":"green"}]}
execute if score $murderers GameRules matches 3 as @a[tag=murderer] at @s run tellraw @s {"translate":"mcm.game.murderer3.msg","color":"gold","with":[{"selector":"@p[tag=murderer,distance=0.01..]", "color":"green"}, {"selector":"@p[tag=murderer,distance=0.01..,sort=furthest]", "color":"green"}, {"selector":"@s","color":"green"}, {"translate":"mcm.game.murderers","color":"gold"}]}
execute as @a[tag=gunner] at @s run tellraw @s {"translate":"mcm.game.role","color":"gold","with":[{"translate":"mcm.game.gunner","color":"dark_aqua"}]}
execute as @a[tag=gunner] at @s run title @s title {"translate":"mcm.game.role","color":"gold","with":[{"translate":"mcm.game.gunner","color":"dark_aqua"}]}
execute as @a[tag=gunner] at @s run title @s subtitle {"translate":"mcm.game.gunner.subtitle","color":"dark_gray"}
tellraw @a[tag=innocent] {"translate":"mcm.game.role","color":"gold","with":[{"translate":"mcm.game.innocent","color":"light_purple"}]}
title @a[tag=innocent] title {"translate":"mcm.game.role","color":"gold","with":[{"translate":"mcm.game.innocent","color":"light_purple"}]}
title @a[tag=innocent] subtitle {"translate":"mcm.game.innocent.subtitle","color":"gold"}
