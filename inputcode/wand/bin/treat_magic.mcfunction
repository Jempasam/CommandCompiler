execute if entity @s[tag=gravity] run effect give @e[distance=..3] minecraft:levitation 10 0
execute if entity @s[tag=poison] run function wand:treat_magic/poison
execute if entity @s[tag=vision] run effect give @e[distance=..40] minecraft:glowing 10 0
execute if entity @s[tag=battle] run particle minecraft:wax_off ~ ~ ~ 1.5 1.5 1.5 0 100
execute if entity @s[tag=slow] run function wand:treat_magic/slow
execute if entity @s[tag=move] run function wand:treat_magic/move
execute if entity @s[tag=life] run function wand:treat_magic/life
execute if entity @s[tag=battle] run function wand:treat_magic/battle
execute if entity @s[tag=dark] run function wand:treat_magic/dark
execute if entity @s[tag=nature] run function wand:treat_magic/grow
execute if entity @s[tag=chaos] run spreadplayers ~ ~ 0 10 false @e[distance=..3]
execute if entity @s[tag=ground] as @e[distance=..3] at @s run tp ~ ~-1 ~
execute if entity @s[tag=tp] run tp @e[distance=..5] ~ ~ ~
execute if entity @s[tag=sand] run function wand:treat_magic/sand
execute if entity @s[tag=fire] run function wand:treat_magic/fire
execute if entity @s[tag=destruction] run function wand:treat_magic/destruction
function wand:treat_magic_skyland_compat
kill @s