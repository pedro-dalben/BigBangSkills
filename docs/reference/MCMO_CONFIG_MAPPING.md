# mcMMO configuration mapping

| mcMMO capability | BigBangSkills equivalent | Current status |
| --- | --- | --- |
| `Experience_Formula.Curve` | `experience.curve` in `skills.properties` | `LINEAR` validated at startup; unsupported curves fail closed |
| `Experience_Formula.Linear_Values.base/multiplier` | `experience.linear_base` / `experience.linear_multiplier` in `skills.properties` | 1020 / 20 defaults mapped, validated and consumed by the registry |
| `Experience_Formula.Exponential_Values.base/multiplier/exponent` | `experience.exponential_base` / `experience.exponential_multiplier` / `experience.exponential_exponent` | 2000 / 0.1 / 1.80 defaults mapped, validated and consumed when `experience.curve=EXPONENTIAL` |
| `Experience_Formula.Cumulative_Curve` | no equivalent | Not enabled by the fixed baseline; rejected rather than silently applying a per-skill approximation |
| Existing `skills.properties` files | schema-preserving fallback/migration | Missing progression/activation keys are rewritten as schema 4 while existing skill settings are retained |
| Existing `formulas.properties` files | default-preserving formula migration | Missing validated formula keys are written on load while existing numeric overrides and salvage block are retained |
| `Experience_Formula.Multiplier.Global` | `experience.global_xp_multiplier` | validated central modifier, default `1` |
| `Experience_Formula.Multiplier.PVP` | `experience.pvp_xp_multiplier` | validated central PvP modifier, default `1` |
| `Experience_Values.PVP.Rewards` | `experience.pvp_rewards` | validated central PvP award gate, default `true` |
| `Experience_Values.PVP.BaseXP` | `combat.pvp_base_xp` | validated shared combat XP base, default `20` |
| `Experience_Formula.Skill_Multiplier.*` | per-skill XP modifier | `skills.properties` `skill.<path>.xp_multiplier` |
| `Experience_Values.<Skill>.<Material>` | namespaced XP table keyed by registry ID/tag | block/action tables externalized; Excavation/Herbalism block path active |
| `Treasures.yml` / `Hylian_Luck` | per-source bonus item, XP, chance and level | Excavation and Herbalism tables are external at `skills/excavation-treasures.properties` and `skills/herbalism-treasures.properties`, with fixed baseline defaults |
| `Skills.<Skill>.Level_Cap` | `SkillDefinition.maxLevel` | validated `skills.properties` `level_cap`; `0` means no limit |
| `Skills.<Skill>.Enabled_For_PVP/PVE` | event-context policy | validated gates in common award dispatcher |
| `Abilities.Enabled` | `skill.<skill>.abilities_enabled` | gates active ability activation; passive effects remain enabled, matching mcMMO's activation-only setting |
| `Abilities.Activation.Only_Activate_When_Sneaking` | `abilities.only_activate_when_sneaking` | default `false`; both loaders reject right-click active-ability activation while standing when enabled |
| `Abilities.Tools.Durability_Loss` | `abilities.durability_loss` in `formulas.properties` | default `1`; `0` disables the extra active-ability tool damage on both loaders |
| `Abilities.Cooldowns.*` | `CooldownService` + `skill.<skill>.ability_cooldown_seconds` | server activation and `/skills <skill>` display use catalog defaults (Blast Mining 60s; other active abilities 240s); a non-default skill value overrides |
| `Abilities.Max_Seconds.*` | `AbilityDefinition.duration` | zero default uses configurable formula `2 + min(abilities.duration_cap_level, level) / abilities.duration_increase_level`; explicit duration overrides |
| `Skills.General.Ability.Length.Standard.CapLevel/IncreaseLevel` | `abilities.duration_cap_level` / `abilities.duration_increase_level` | defaults `50` / `5`, validated and consumed by both loaders |
| `Rank.*` / `SubSkillType` unlocks | `DefaultAbilityCatalog` loaded from baseline-derived resource | rank-1 metadata mapped for all 81 subskills |
| `Diminished_Returns.*` | central anti-exploit rate policy | `diminished-returns.properties`; disabled by default and applied before XP persistence |
| `Skills.General.LimitBreak.AllowPVE` | global Limit Break target restriction | `formulas.properties` `combat.limit_break_allow_pve`; default disabled |
| mcMMO secondary combat target checks | `CombatSkillEngine.secondaryTargetAllowed` plus loader entity ownership/PvP checks | AOE skips own tamed pets/horses, spectators and players outside enabled PvP; party, region, NPC and vanish integration remains pending |
| `Bonus_Drops.*` | configured drop modifiers | Mining/Woodcutting/Herbalism Double Drops and Verdant Bounty are level/formula driven; Blast Mining bonus drops are configurable with rank multiplier/chance |
| `ExploitFix.TreeFellerReducedXP` | `woodcutting.tree_feller_reduced_xp` | validated boolean-like formula; default `1` preserves the mcMMO default |
| `Fishing` treasure files | `fishing-treasures.properties` plus `fishing-shake.properties` | Baseline reward entries are generated externally; namespaced item IDs, amount, XP, rarity and enchantability are validated |
| `Fishing_ExploitFix_Options` | `SkillFormulaConfig` + `FishingEngine` stationary/rapid catch guard | defaults 3 blocks / 10 catches; external values validated and active; `food.<namespace>:<item>` action entries opt modded food into Fisherman's Diet |
| `Skills.Fishing.ShakeChance` / `VanillaXPMultiplier` | `FishingEngine` rank tables | common table and vanilla reward mutation active; configurable Shake loot mutation active |
| `Skills.Fishing.MasterAngler` | `fishing.master_angler_*` formula keys | per-rank/boat/Lure reductions and minimum wait caps are validated and applied by both loader mixins |
| `Skills.Taming.Gore/FastFood/ThickFur/ShockProof/SharpenedClaws` | `SkillFormulaConfig` | defaults and external validation active |
| Combat chance `MaxBonusLevel` values (Daze, Critical Strikes, Gore, Arrow Deflect, Iron Grip, Counter Attack) | corresponding `combat.*_max_level` keys | baseline `100` limits are externalized and validated |
| `Skills.Taming.CallOfTheWild` | `taming-summons.properties` | namespaced entity/item recipes, owner limit and lifespan; vanilla wolf/cat/horse defaults generated |
| `Skills.Alchemy.Concoctions` | `alchemy-concoctions.properties` | namespaced ingredient rank and optional registry-backed effect; vanilla effect fallback retained |
| `Skills.Mining.BlastMining.Bonus_Drops` | `mining.blast_bonus_drops_*` | enabled by default; rank multiplier and 50% bonus chance match baseline |
| `Skills.Mining.BlastMining.BlastRadiusModifier` / `OreBonus` / `BlastDamageDecrease` | `mining.blast_radius_bonus_rank_*` / `mining.blast_ore_bonus_rank_*` / `mining.blast_damage_reduction_rank_*` | all eight baseline ranks and base radius are externalized and validated |
| `Skills.Mining.BlastMining.RemoteDetonationDistance` | `mining.blast_remote_detonation_distance` | validated integer distance in blocks; default `100` |
| `Skills.Woodcutting.CleanCuts/HarvestLumber.MaxBonusLevel` | `woodcutting.clean_cuts_max_level` / `woodcutting.harvest_lumber_max_level` | defaults `1000` / `100`, validated and consumed by the common drop path |
| `Skills.Herbalism.DoubleDrops.MaxBonusLevel` | `herbalism.double_drops_max_level` | default `100`, validated and consumed by the common drop path |
| `Experience_Values.Combat.Multiplier.<entity>` | namespaced combat entity XP action | `actions-xp.properties` accepts `multiplier.<namespace>:<entity>` with vanilla path fallback |
| Modded combat weapon classification | `combat-weapons.properties` | optional namespaced item-to-skill map for custom weapons; vanilla classes and spear IDs remain automatic |
| `Experience_Values.Archery.Distance_Multiplier` | projectile distance XP bonus | `formulas.properties` `combat.archery.distance_xp_multiplier=0.025`, capped at 50 blocks from tracked arrow origin |
| Repair/Salvage material maps | namespaced item/material rules | `salvage.properties` and generated `repair.properties`; modded item IDs can override repair XP category |
| `Salvage.ArcaneSalvage` loss/downgrade/max level | `SkillFormulaConfig` | `salvage.arcane_salvage_*` validated and used by extraction |
| `Skills.Unarmed.SteelArmStyle.Damage_Override/Override.Rank_*` | `combat.unarmed.steel_arm_damage_override` / `combat.unarmed.steel_arm_override_rank_*` | default override disabled; all 20 baseline values are externalized and validated |

All future mappings must use registry IDs, tags or validated config keys. No skill listener may embed material XP values.
