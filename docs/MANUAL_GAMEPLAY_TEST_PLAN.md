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
| Details/leaderboard | Run `/skills mining`, `/skills woodcutting`, `/skills fishing`, `/skills top mining` | Chat screenshots showing level, current XP, next XP, total XP and leaderboard |
| Mining | Break configured stone/ores with correct tool; compare Silk Touch/Fortune and ability states | Before/after inventory screenshot, XP text, server log and coordinates |
| Mining anti-exploit | Place an ore, reconnect, then break it; test explosion/fake-player path where enabled | Placement/break screenshots and server log proving zero or expected XP |
| Woodcutting | Break vanilla and configured modded logs; test Tree Feller, Leaf Blower, durability and placed logs | Screenshots of drops/tool durability/XP and server log |
| Gathering | Test Excavation, Herbalism, mature crops, replant loops and treasure | Inventory/XP screenshots and exact block/item coordinates |
| Fishing | Fish vanilla loot, treasure tiers, Shake, Ice Fishing, Master Angler and Fisherman's Diet | Rod/inventory/chat screenshots plus server log |
| Combat | Test Swords, Axes, Unarmed, Archery, Crossbows, Tridents, Maces and Spears in PvE and PvP where allowed | Before/after health/armor screenshots, chat feedback and server log |
| Acrobatics | Test fall XP, Roll, Graceful Roll and Dodge at representative levels | Fall-distance coordinates, health/XP screenshots and log |
| Taming | Tame, use pet combat, Call of the Wild, Beast Lore and owner limits | Pet/entity screenshots, owner UUID and server log |
| Repair/Salvage | Repair vanilla/modded items, enchant preservation, salvage confirmation and enchanted salvage | Input/output inventory screenshots, levels and server log |
| Smelting/Alchemy | Smelt, Second Smelt, Fuel Efficiency, brew stages, Catalysis and Concoctions | Furnace/brewing screenshots, timing, XP and server log |
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

## Automated repository gates

Allowed without Minecraft runtime:

```text
./gradlew :common:test
./gradlew :common:test :fabric:compileJava :neoforge:compileJava
./gradlew clean build
git diff --check
```

These gates prove code/build health only. They do not turn a row in the gameplay matrix into `GAMEPLAY_PASS`.
