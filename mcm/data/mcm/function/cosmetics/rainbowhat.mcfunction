execute if entity @s[advancements={mcm:secrets/lobby/ring_bell=true}] unless items entity @s armor.head diamond_hoe[item_model="minecraft:hats/rainbowhat"] at @s run playsound minecraft:block.glass.place block @s ~ ~ ~ 2 1
execute if entity @s[advancements={mcm:secrets/lobby/ring_bell=true}] unless items entity @s armor.head diamond_hoe[item_model="minecraft:hats/rainbowhat"] run item replace entity @s armor.head with diamond_hoe[custom_data={no_drop_on_death:1b},item_model="minecraft:hats/rainbowhat",custom_name="{\"translate\":\"mcm.item.rainbow_hat\",\"color\":\"light_purple\"}",enchantments={levels:{binding_curse:1},show_in_tooltip:false}]

execute if entity @s[advancements={mcm:secrets/lobby/ring_bell=false}] run tellraw @s {"translate":"mcm.cosmetic.cannot.equip","color":"red"}
execute if entity @s[advancements={mcm:secrets/lobby/ring_bell=false}] at @s run playsound minecraft:entity.villager.no neutral @s ~ ~ ~ 1 1 0

execute if entity @s[advancements={mcm:secrets/lobby/ring_bell=true}] run scoreboard players set @s selected_hat 1
execute unless entity @s[advancements={mcm:secrets/lobby/ring_bell=true}] run scoreboard players set @s selected_hat 0