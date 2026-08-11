# Full mcMMO Skill Parity Audit

Baseline: `mcMMO-Dev/mcMMO` commit `ad8444c2f394ee97c510acdfc7b23623885b071c`.
This audit covers 19 primary skills and 81 subskills from `SubSkillType` and
`skillranks.yml`. The reference checkout is documentation input only.

| Skill | XP/action data | Common mechanics | Fabric | NeoForge |
|---|---|---|---|---|
| Acrobatics | fall/dodge/roll | roll, graceful roll, dodge and fall XP | hurt/fall callbacks | damage/fall events |
| Alchemy | potion stages | action dispatcher + Catalysis + stage classifier; namespaced ingredient/effect table | brewing-stand timer/doBrew mixins | brewing-stand timer/doBrew mixins |
| Archery | combat | skill shot/daze/arrow retrieval/distance XP | damage XP/effects + death-time arrow recovery + bounded arrow-origin distance XP | pre-mitigation damage + death-time arrow recovery + bounded arrow-origin distance XP |
| Axes | combat | mastery/critical/impact/skull splitter | damage XP/effects | pre-mitigation damage |
| Crossbows | combat | powered shot/trick shot | damage XP/effects + bounded block ricochet | pre-mitigation damage + bounded block ricochet |
| Excavation | blocks/archaeology | Giga Drill chain break | shovel blocks | shovel blocks |
| Fishing | fish/shake/food | action/rank tables, tiered treasure resolver with Luck of the Sea, configurable Shake entity loot with four-hit cap, Master Angler timer reduction, ice conversion, Fisherman's Diet and stationary/rapid-catch guard | `FishingHook.retrieve` + hook target tick + player food completion mixins | real fish event + hook target tick + food completion event + guard/timer/ice/treasure hooks |
| Herbalism | crops/flowers | mature-crop gate, Double Drops/Verdant Bounty, Hylian Luck, Green Terra/Shroom Thumb conversions | block/drop path | block/drop path |
| Maces | combat | crush/cripple | damage XP/effects | pre-mitigation damage |
| Mining | blocks | drops/Super Breaker/Blast Mining/Bigger Bombs | pickaxe/drop path; configurable Silk Touch, rank caps and remote blast radius gate | pickaxe/drop path; configurable Silk Touch, rank caps and remote blast radius gate |
| Repair | repair material | material-weighted repair XP + pure mastery math | `AnvilMenu.onTake` mixin | anvil repair event |
| Salvage | salvageable item | durability yield, level gate, child-derived view | configured Gold Block interaction | configured Gold Block interaction |
| Smelting | furnace outputs | output XP, Fuel Efficiency, mutable Second Smelt, child-derived view | furnace output/serverTick mixins | furnace output/serverTick mixins |
| Spears | combat | mastery/momentum | damage XP/effects | pre-mitigation damage |
| Swords | combat | stab/rupture/serrated/counter attack | damage XP/effects | pre-mitigation damage |
| Taming | tame/pet combat | tame XP, gore, claws, Fast Food, Pummel, Holy Hound, configurable tameable summon recipes with owner limits/expiry, Beast Lore, defence and teleport | entity callback/pet combat + hurt mixin | entity event/pet combat + incoming damage |
| Tridents | combat | impale | damage XP/effects | pre-mitigation damage |
| Unarmed | combat | steel arm/berserk/disarm/arrow deflect/iron grip | damage XP/effects | pre-mitigation damage |
| Woodcutting | logs | drops/Tree Feller/Leaf Blower | log/drop path; configured Tree Feller limit, reduced XP and placed-block guard | log/drop path; configured Tree Feller limit, reduced XP and placed-block guard |

Shared guarantees: fixed standard XP curve; validated enablement, level cap,
XP multiplier and PvP/PvE gates; configurable block/action/formula tables with
modded IDs; all 81 ability definitions and rank thresholds; bounded async
persistence; placement provenance; localized English/pt-BR names; one combat
dispatcher; right-click active-ability activation with cooldown feedback; and
dynamic server-side commands.

Verification on 2026-08-10: `./gradlew clean build` passed (34 actionable tasks),
the focused suite has 75 common tests, loader compilation passes, and `git diff --check` passes. Post-build Fabric and NeoForge dedicated boots both reached `Done` with SQLite/Hikari initialization and no mixin errors. A Fabric Loom client also connected through `--quickPlayMultiplayer`; the server logged `Player978 joined the game` and the client received the 19-line `/skills` response. This is session/network evidence only, not vanilla-client or block-interaction proof.

Remaining gates are runtime player proof,
furnace XP player proof after the native `AbstractCookingRecipe#getExperience` hook,
exact TNT owner/tracked-entity behavior, and several combat secondary-target
effects. Salvage now has a two-click, expiring native
confirmation path on both loaders; it still needs player smoke.
The common formulas and loader boot are tested; build success is not counted as
gameplay parity.

## Cross-cutting audit checks

| Check | Result | Evidence / remaining action |
|---|---|---|
| Missing abilities | PARTIAL | Catalog has all 81 baseline entries; runtime effects now include ranked PvP Limit Break damage with armor-quality scaling, Arrow Retrieval, Concoctions, Dodge, Arrow Deflect, Iron Grip, Block Cracker, Counter Attack, Rupture ticks, bounded area combat damage, Leaf Blower, Knock on Wood, Archaeology, Hylian Luck, Green Terra/Shroom Thumb, Farmers Diet, full baseline fishing treasure entries with Luck of the Sea/Magic Hunter, configurable Shake resolution with potion metadata, Master Angler, Ice Fishing conversion, Fisherman's Diet, configurable tameable Call of the Wild, Beast Lore, bounded Arcane Forging/Salvage, remote Blast Mining with ore/debris processing, illegal-drop blacklist and Demolitions Expertise; exact TNT owner/tracked-entity behavior and several secondary effects remain. |
| Missing configs / hardcoded values | PARTIAL | XP/action/item/formula tables, fishing/archaeology treasure tables, modded block/entity/ingredient/weapon recipes and skill gates are validated/externalized; remaining loader policies are listed in `MCMO_CONFIG_MAPPING.md`. |
| Wrong XP | PARTIAL | Fixed reference action tables plus standard linear and configurable exponential curves are tested; Tree Feller reduced XP and native Understanding the Art cooking XP mutation are wired on both loaders, with player smoke still pending. |
| Wrong formulas / cooldowns | PARTIAL | Shared formulas, fishing rank/timer formulas, attack-strength-scaled Cripple/Momentum/Rupture, repair mastery and capped standard ability duration are tested; ability-specific runtime cooldown smoke remains pending. |
| Restrictions | PASS common / PARTIAL loader | PvP/PvE, enabled-skill, provenance, child-skill and fishing anti-exploit gates are centralized; salvage confirmation is loader-native, while some environment restrictions remain. |
| Fabric / NeoForge differences | PASS boot / PARTIAL gameplay | Both dedicated servers reach `Done`; damage mutation uses Fabric `LivingEntity.hurt` and NeoForge `LivingIncomingDamageEvent`. |
| Performance | PASS bounded core / PENDING soak | Persistence is async/bounded and block chains are bounded; no production soak or large AOE combat test was run. |
| Anti-exploit | PARTIAL | Placement provenance, actor checks, bounded chain effects, expiring summon ownership, fishing rapid/stationary guard and optional diminished returns are active; all modded provenance sources remain. |
| Disabled tests | PASS | `:common:test` runs the focused suite; no test is disabled or excluded by build configuration. |
| Duplicated logic | PARTIAL | XP/formulas/registry are common; loader callbacks still duplicate small Minecraft item/block mappings by necessity. |
