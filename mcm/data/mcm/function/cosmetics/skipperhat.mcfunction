execute if entity @s[advancements={mcm:secrets/riverboat/riverboat=true}] unless items entity @s armor.head diamond_hoe[item_model="minecraft:hats/skipperhat"] at @s run playsound minecraft:item.armor.equip_leather block @s ~ ~ ~ 3 0
execute if entity @s[advancements={mcm:secrets/riverboat/riverboat=true}] unless items entity @s armor.head diamond_hoe[item_model="minecraft:hats/skipperhat"] run item replace entity @s armor.head with diamond_hoe[custom_data={no_drop_on_death:1b},item_model="minecraft:hats/skipperhat",enchantments={levels:{binding_curse:1},show_in_tooltip:false},unbreakable={show_in_tooltip:false},custom_name='{"translate":"mcm.item.skipperhat","color":"gray","italic":false}']

execute if entity @s[advancements={mcm:secrets/riverboat/riverboat=false}] run tellraw @s {"translate":"mcm.cosmetic.cannot.equip","color":"red"}
execute if entity @s[advancements={mcm:secrets/riverboat/riverboat=false}] at @s run playsound minecraft:entity.villager.no neutral @s ~ ~ ~ 1 1 0

execute if entity @s[advancements={mcm:secrets/riverboat/riverboat=true}] run scoreboard players set @s selected_hat 7
execute unless entity @s[advancements={mcm:secrets/riverboat/riverboat=true}] run scoreboard players set @s selected_hat 0