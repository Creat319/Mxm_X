#> Assign roles when grace period is over

#> Force murder assign for dubug reasons
tag @a[tag=force_murderer] add murderer

execute if entity @a[tag=force_murderer,tag=queued] run tellraw @a [{"text":"Murderer override is currently active!","color":"red","bold":true}]
execute if entity @a[tag=force_murderer,tag=queued] run tellraw @a [{"text":"Murderers: ","color":"red"},{"selector":"@a[tag=queued,tag=force_murderer]"}]


#> McmX 预设：先把预设为坏人的玩家标记成 murderer（阵营由数据包决定，插件只负责细分）
tag @a[tag=queued,tag=mcmx_preset_evil] add murderer
scoreboard players operation $num_murderers CmdData = $murderers GameRules
#> 减去已经预设成坏人的数量，避免多出身份
execute store result score $mcmx_preset_evils CmdData if entity @a[tag=queued,tag=mcmx_preset_evil,tag=murderer]
scoreboard players operation $num_murderers CmdData -= $mcmx_preset_evils CmdData
execute if score $num_murderers CmdData matches 1.. unless entity @a[tag=queued,tag=force_murderer] run function mcm:game/assign_murderer

schedule function mcm:game/murderertip 6s



# Murderer items
execute as @a[tag=murderer] run function mcm:items/loadouts/murderer
execute as @a[tag=murderer] run item replace entity @s hotbar.8 with netherite_scrap[custom_data={no_drop_on_death:1b,Tags:["KeyItem"]},custom_name='{"translate":"mcm.item.scrap","italic":false}',lore=['[{"translate":"mcm.item.scrap.lore","italic":false}]']]
# Give murderers their 1 free recall
tag @a[tag=murderer] add free_knife


# Gunner（优先预设枪手，且不让预设好人被随机成枪手）
execute as @a[tag=queued,tag=mcmx_preset_gunner,tag=!murderer,limit=1] at @s run tag @s add gunner
execute unless entity @a[tag=gunner] as @a[tag=queued,tag=!murderer,tag=!mcmx_preset_good,tag=!gunner,limit=1,sort=random] at @s run tag @s add gunner
# Gun gets NoDrop because it's already in an inventory
execute as @a[tag=gunner] run function mcm:items/give {item: gun}
schedule function mcm:game/gunnertip 6s
#execute as @a[tag=gunner] at @s run item replace entity @s hotbar.8 with netherite_scrap[custom_model_data=1,custom_data={no_drop_on_death:1b,Tags:["KeyItem"]},custom_name='{"translate":"mcm.item.scrap","italic":false}',lore=['[{"translate":"mcm.item.scrap.lore","italic":false}]']]

# Innocent
execute as @a[tag=queued,tag=!murderer,tag=!gunner] at @s run tag @s add innocent
execute if score $startscrap GameRules matches 1.. run item replace entity @a[tag=innocent] hotbar.8 with netherite_scrap[custom_data={no_drop_on_death:1b,Tags:["KeyItem"]},custom_name='{"translate":"mcm.item.scrap","italic":false}',lore=['[{"translate":"mcm.item.scrap.lore","italic":false}]']]
schedule function mcm:game/innocenttip 6s
# Make it easier to track the gunner as an innocent
execute as @a[tag=queued,tag=gunner] at @s run tag @s add innocent

# Give everyone a spyglass in their 8th slot
item replace entity @a[tag=queued] hotbar.7 with spyglass[custom_data={NoDrop:1b,no_drop_on_death:1b}]

#> 如果安装了 McmX 插件，则由插件统一播报最终身份（含扩展身份）
execute unless score $mcmx CmdData matches 1 run function mcm:game/role_messages

scoreboard players set $pickedroles CmdData 1
playsound minecraft:block.beehive.enter ambient @a ~ ~ ~ 1 0 1

execute store result score $InnocentCount CmdData if entity @a[tag=innocent,tag=!spectating]
execute store result score $MurdererCount CmdData if entity @a[tag=murderer,tag=!spectating]