# Manual gameplay validation plan

This document is the handoff for a human Minecraft test. BigBangSkills must not automate Minecraft input or launch a Minecraft client as part of repository validation.

## Required setup

1. Start a clean Fabric server and, separately, a clean NeoForge server with the built artifact.
2. Connect once with an unmodified vanilla 1.21.1 client and once with the supported modpack client, if applicable.
3. Use a fresh test UUID or record the exact player name, loader, mod list, server build and timestamp.
4. Preserve the server log, client log and screenshots for every failed or surprising result.

## Test matrix

| Area | Manual action | Required evidence |
| --- | --- | --- |
| Login/profile | Join, run `/skills`, reconnect | Server join/quit lines; screenshot of the 19-skill overview |
| Details/leaderboard | Run `/skills mining`, `/skills woodcutting`, `/skills fishing`, `/skills top mining` | Chat screenshots showing level, current XP, next XP, total XP, passive/active ranks, activation syntax, cooldowns, restrictions, XP formula and leaderboard |
| Mining | Break configured stone/ores with correct tool; compare Silk Touch/Fortune and ability states | Before/after inventory screenshot, XP text, server log and coordinates |
| Active abilities | Right-click with empty hand, pickaxe, axe, shovel, hoe and sword; repeat while on cooldown and use `/skills ability` as fallback | Chat/action-bar activation and cooldown/unavailable screenshots, held-item screenshot and matching server log |
| Mining anti-exploit | Place an ore, reconnect, push and retract it with a piston, then break it; flow water/lava from a bucket across a configured ore/log and break the destination; explode a placed ore/log with vanilla TNT and repeat the configured Blast Mining path | Placement/piston/fluid/explosion/break screenshots and server log proving zero or expected XP after movement; include bucket source, explosion and destination coordinates, TNT owner and whether the explosion was vanilla or Blast Mining |
| Woodcutting | Break vanilla and configured modded logs; test Tree Feller, Leaf Blower, durability and placed logs | Screenshots of drops/tool durability/XP and server log |
| Gathering | Test Excavation, Herbalism, mature crops, Green Thumb replant loops and treasure; break the automatically replanted crop again and verify no XP is paid as a fresh natural crop | Inventory/XP screenshots, exact block/item coordinates, seed consumption and server log for both harvest cycles |
| Fishing | With a fresh rod, fish normal fish and junk/treasure; repeat with `fishing.drops_enabled=false`, `fishing.override_vanilla_treasures=false`, `fishing.extra_fish=true`; use Luck of the Sea levels 0/1/3; test Magic Hunter on an enchanted reward, Shake against one target four times, Ice Fishing, Master Angler in water/boat, Fisherman's Diet, and stationary rapid catches | For each config: exact `skills.properties`, rod enchantments, catch inventory before/after, screenshots of item count/XP/chat, target health and Shake drops, hook timing or video/coordinates for water/boat/ice, and the complete server log window covering each catch. Send both loader bundles; do not infer from compile/boot |
| Combat | Test Swords, Axes, Unarmed, Archery, Crossbows, Tridents, Maces and Spears in PvE and PvP where allowed; include tameable/horse, spawner, spawn-egg, Nether Portal and bred targets to verify combat-origin multipliers; separately move a normal mob through a portal and confirm it is not treated as portal-spawned; test Unarmed Disarm with `combat.unarmed.disarm_anti_theft=0` and `=1`, including owner and another player attempting pickup; for Serrated Strikes/Skull Splitter place a normal mob, another player's pet, a spectator and a player nearby | Before/after health/armor/inventory/XP screenshots, target ownership/spawn origin, portal-spawn versus portal-travel setup, owner UUID, pickup player identity, chat feedback and server log proving only eligible secondary targets were affected |
| Acrobatics | Test fall XP, Roll, Graceful Roll and Dodge at representative levels | Fall-distance coordinates, health/XP screenshots and log |
| Taming | Tame, use pet and horse combat, Call of the Wild, owner limits, summoned-pet XP/breeding guard, Beast Lore on tameable and horse, fall damage immunity, and lethal/non-lethal Environmental Awareness damage | Pet/horse screenshots, owner UUID, breeding attempt/result, fall health before/after, hazard/teleport coordinates, horse speed/jump output and server log |
| Repair/Salvage | Repair vanilla/modded items on configured `repair.anvil_block` (default iron block), vanilla anvil, enchant preservation, salvage confirmation and enchanted salvage | Input/output inventory screenshots, station blocks, item durability before/after, levels and server log |
| Smelting/Alchemy | Smelt, Second Smelt, Fuel Efficiency, brew stages, Catalysis and Concoctions; test hopper ingredient/bottle transfer with both filters enabled and disabled; fill furnace result to `maxStackSize - 1` and verify Second Smelt does not overflow | Furnace/brewing screenshots, hopper inventory state, result counts, timing, XP and server log |
| Persistence | Gain XP, quit, restart the server, rejoin and query the same skills | Old/new screenshots and the complete shutdown/startup/join log window |
| Loader parity | Repeat the representative Mining, Woodcutting, Fishing, Combat and persistence cases on both loaders | One evidence bundle per loader; do not infer gameplay from boot |

## What to send back

For each case send:

- loader and exact server/mod version;
- player name/UUID and test timestamp;
- commands and manual actions in order;
- screenshots before and after the action, including chat/inventory/UI when relevant;
- the matching server-log excerpt with timestamps;
- client-log excerpt for connection, chat, crash or disconnect;
- expected result versus observed result;
- coordinates, world/dimension, item/block/entity IDs and tool/enchantments;
- whether the result was reproducible.

For a failure, send the full exception/stack trace and the smallest reproducible sequence. Do not send credentials, database passwords, or public server secrets.

## Minimum evidence bundle

Use one folder per loader and test case, for example
`fabric/mining-blast-placed-2026-08-11/`, containing:

- `actions.txt`: player UUID/name, loader, server/mod JAR build, dimension, coordinates, commands and actions in exact order;
- `expected-vs-observed.txt`: expected mcMMO-baseline value and observed value, including XP before/after and item counts;
- `server.log.txt`: the timestamped server excerpt covering join, action, XP feedback, warning or error;
- `client.log.txt`: only the matching connection/chat/crash/disconnect excerpt;
- `before.png` and `after.png`: inventory, hotbar, health, tool durability, chat or GUI relevant to the case;
- `config-snapshot/`: the effective `config/bigbangskills` files when a formula, multiplier, unlock, cooldown or table is under test.

For Blast Mining specifically, include the TNT and ore coordinates, whether the ore was natural or placed, the pickaxe/enchantments, the owner player UUID, the resulting drops, XP feedback, and the log window from TNT placement through detonation. A placed block is expected to be destroyed without custom Blast Mining XP or bonus drops; an unreliable or still-loading provenance state is expected to fail closed and leave the normal server explosion path in control.

## Automated repository gates

Allowed without Minecraft runtime:

```text
./gradlew :common:test
./gradlew :common:test :fabric:compileJava :neoforge:compileJava
./gradlew clean build
git diff --check
```

These gates prove code/build health only. They do not turn a row in the gameplay matrix into `GAMEPLAY_PASS`.
