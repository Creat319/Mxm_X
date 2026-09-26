#> executed as a player holding an amethyst shard
scoreboard players operation $tempuuid playerUUID = @s playerUUID
execute anchored eyes run tp @e[type=armor_stand,tag=tp_beacon_display_mount,predicate=mcm:matches_uuid] ^ ^ ^
scoreboard players add @s airship 1
execute if score @s airship matches 100.. run function mcm:maps/airship/crystals/kill_displays
execute if score @s airship matches 100.. run function mcm:maps/airship/crystals/summon_displays
execute if score @s airship matches 100.. run scoreboard players reset @s airship

execute as @s[scores={crystalClick=1..}] at @s run function mcm:maps/airship/crystals/teleport
function mcm:util/reset_carrot_on_stick
