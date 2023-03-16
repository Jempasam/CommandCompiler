setblock ~ ~ ~ fire
summon minecraft:creeper ~ ~1 ~
execute if entity @e[type=minecraft:creeper] run function <*:
	say He spawned
	say It is cool
>