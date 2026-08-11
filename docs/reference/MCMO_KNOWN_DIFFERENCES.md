# Known mcMMO differences

| mcMMO behavior | Platform limitation | BigBangSkills decision |
| --- | --- | --- |
| Bukkit/Paper block, combat, projectile, brewing and inventory events | Fabric/NeoForge expose different event APIs on 1.21.1 | Keep behavior in common services and feed normalized adapter actions; no Bukkit dependency |
| Reference master includes Pale Oak and other post-1.21.1 content | Target runtime is Minecraft 1.21.1 | Treat later blocks as config/tag content only; do not pretend they are vanilla |
| Current mcMMO Hylian Luck table includes `Copper_Nugget` | Minecraft 1.21.1 has no vanilla copper nugget item | The generated 1.21.1 default uses `minecraft:copper_ingot`; replace it with a namespaced mod item when the server provides the reference item |
| mcMMO baseline exposes the Spears skill | Minecraft 1.21.1 has no vanilla spear item | Spears remains registered and configurable, but runtime classification requires a modded item ID containing `spear` or an explicit `combat-weapons.properties` mapping |
| mcMMO uses Bukkit material/config names | Modded runtime uses registry IDs and tags | BigBangSkills uses namespaced IDs/tags; administrators can add modded entries |
| mcMMO standard levels are calculated from grouped retro levels | Existing domain levels start at 1 | Level 1 represents mcMMO's starting level; grouped standard formula is implemented and tested |
| Exact Paper loot/enchantment APIs do not exist on both loaders | No portable one-to-one API | Recreate output rules through normalized server-side actions; any non-equivalent case remains documented until adapter support exists |
| Blast Mining tracks delayed TNT owner metadata and WorldGuard context | Fabric/NeoForge explosion APIs expose different source/event state | Player-triggered TNT now uses an owner-bearing `PrimedTnt` on both loaders, requires reliable provenance, rejects placed blocks for custom XP/drops, and keeps ore/debris processing, illegal-drop filtering, Demolitions damage and PvP cap; WorldGuard/region parity remains pending and gameplay proof is manual |
| mcMMO validates secondary combat targets and environment flags per plugin integration | No shared WorldGuard/party/NPC contract in this server-side core | Combat effects are bounded to configured weapon-tier target counts and server entities; party/region/NPC-specific parity remains pending |
| Vanilla client does not expose an ability UI | Feature must remain server-side | Activation must use vanilla interactions; client smoke is a separate gate |
| `Experience_Formula.Cumulative_Curve=true` uses power level for every skill threshold | BigBangSkills progression curves receive one skill's XP/level and do not have a power-level-dependent threshold contract | The fixed baseline keeps this option false; no approximation is enabled, and the setting is documented as unsupported |

No difference is silently treated as parity. Each row is a temporary compatibility boundary and can be removed only after both loader behavior is verified.
