# Full mcMMO Skill Parity Audit

> Canonical audit: [`docs/audits/FULL_SKILL_PARITY_AUDIT.md`](../audits/FULL_SKILL_PARITY_AUDIT.md). This reference copy is retained for compatibility with earlier links.

Baseline: `mcMMO-Dev/mcMMO` commit `ad8444c2f394ee97c510acdfc7b23623885b071c` (2026-07-30).
Target: BigBangSkills common engine with Fabric and NeoForge adapters for Minecraft 1.21.1.

The audit covers all 19 primary skills and all 81 `SubSkillType` entries. The
reference source is behavioral/configuration input only: it is not a runtime
dependency and no source was copied into this project.

| Primary skill | Registry | XP data | Event path | Abilities/effects | Status |
|---|---:|---:|---:|---:|---|
| Acrobatics | yes | action table | fall callback/event | roll resolver | partial |
| Alchemy | yes | action table | brewing timer/doBrew mixins | Catalysis/stage classifier | partial |
| Archery | yes | action table | combat damage hooks | Skill Shot/Daze resolver | partial |
| Axes | yes | action table | combat damage hooks | mastery/critical/impact/Skull Splitter resolver | partial |
| Crossbows | yes | action table | combat damage hooks | Powered Shot resolver | partial |
| Excavation | yes | action + block table | Fabric/NeoForge shovel blocks | Giga Drill Breaker state | partial |
| Fishing | yes | action table | retrieve/fishing event + anti-exploit guard | rank tables, rapid/stationary catch guard | partial |
| Herbalism | yes | action + block table | Fabric/NeoForge crop/flower blocks | Double Drops, Green Terra state | partial |
| Maces | yes | action table | combat damage hooks | Crush/Cripple resolver | partial |
| Mining | yes | block + action table | Fabric/NeoForge pickaxe/tag blocks and owner-bearing TNT | double/triple drops, Super Breaker and fail-closed Blast Mining provenance | partial |
| Repair | yes | action table | anvil mixin/event | repair math | partial |
| Salvage | yes | action table | configured Gold Block interaction | durability yield | partial |
| Smelting | yes | action table | furnace output/serverTick mixins | Fuel Efficiency/Second Smelt | partial |
| Spears | yes | action table | combat damage hooks | mastery/Momentum resolver | partial |
| Swords | yes | action table | combat damage hooks | Stab/Rupture/Serrated resolver | partial |
| Taming | yes | action table | tame/pet boundaries + wolf hurt mutation | Gore, Sharpened Claws, Fast Food, Pummel, wolf defence | partial |
| Tridents | yes | action table | combat damage hooks | Impale resolver | partial |
| Unarmed | yes | action table | combat damage hooks | Berserk/Disarm resolver | partial |
| Woodcutting | yes | block + action table | Fabric/NeoForge log/tag blocks | Harvest Lumber, Clean Cuts, Tree Feller state | partial |

Shared guarantees currently implemented:

- Standard 10-level grouped XP curve with max-level cap.
- Validated per-skill enablement, cap, XP multiplier, PvP/PvE gates, ability
  enablement, cooldown, and duration settings.
- External `config/bigbangskills/skills/{mining-xp,woodcutting-xp,actions-xp}.properties`
  files with defaults merged on restart, including modded registry IDs.
- Fixed-baseline catalog with all 81 ability entries and rank unlock metadata.
- Generic server-side skill-award dispatcher and bounded persistence queue.
- Fabric and NeoForge block/drop integration, placement provenance, fail-closed
  anti-exploit handling, and server-side boundaries for fishing, taming, anvil,
  furnace, and brewing.
- Dynamic `/skills <skill>`, `/skills top <skill>`, and
  `/skills ability <skill> <ability>` command paths.

Remaining parity gates are explicit: real player smoke tests, WorldGuard/region
context, party/NPC/vanish combat context, and external provenance integrations.
Owner-bearing TNT, ore/debris handling, illegal-drop filtering, fail-closed
placed-block handling, native confirmation and the furnace/brewing boundaries
are implemented in both loaders but still require manual gameplay evidence.
`level_cap=0` is now migrated as unlimited, and Diminished Returns is
implemented disabled-by-default with the baseline window and minimum fraction.
