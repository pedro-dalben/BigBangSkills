# mcMMO default config parity

Baseline: `ad8444c2f394ee97c510acdfc7b23623885b071c`.

| mcMMO key | mcMMO default | BigBangSkills default | Parity |
| --- | ---: | ---: | --- |
| `Experience_Formula.Curve` | `LINEAR` | standard linear curve | PASS for progression formula |
| `Experience_Formula.Linear_Values.base` | `1020` | `1020` | PASS |
| `Experience_Formula.Linear_Values.multiplier` | `20` | `20` | PASS |
| `Experience_Formula.Exponential_Values.base/multiplier/exponent` | `2000 / 0.1 / 1.80` | `experience.exponential_base/multiplier/exponent` | PASS when `experience.curve=EXPONENTIAL`; baseline remains LINEAR |
| `Experience_Formula.Multiplier.Global` | `1.0` | `experience.global_xp_multiplier=1` | PASS validated central modifier |
| `Experience_Formula.Multiplier.PVP` | `1.0` | `experience.pvp_xp_multiplier=1` | PASS validated central modifier |
| `Experience_Values.PVP.Rewards` | `true` | `experience.pvp_rewards=true` | PASS central PvP award gate |
| `Experience_Values.PVP.BaseXP` | `20` | `combat.pvp_base_xp=20` | PASS shared combat XP base |
| `Experience_Formula.Player_Tamed.Multiplier` | `0.0` | `combat.tamed_mob_xp_multiplier=0` | PASS common fixture and both loader paths |
| `Experience_Formula.Mobspawners/Eggs/Nether_Portal/Breeding.Multiplier` | `0.0 / 0.0 / 0.0 / 1.0` | `combat.spawner_mob_xp_multiplier=0` / `combat.egg_mob_xp_multiplier=0` / `combat.nether_portal_mob_xp_multiplier=0` / `combat.bred_mob_xp_multiplier=1` | PASS common fixture and loader origin tags |
| `ExploitFix.Combat.XPCeiling.Enabled/Damage_Limit` | `true / 100` | `combat.xp_ceiling_enabled=1` / `combat.xp_damage_ceiling=100` | PASS common combat XP calculation |
| `Diminished_Returns.Enabled` | `false` | `diminished-returns.properties` | PASS; disabled by default |
| `Diminished_Returns.Guaranteed_Minimum_Percentage` | `0.05` | `guaranteed_minimum_fraction` | PASS config validation |
| `Diminished_Returns.Time_Interval` | `10` minutes | `interval_minutes` | PASS config validation |
| `Diminished_Returns.Threshold.<skill>` | `20000` for primary skills | `threshold.<skill>=20000` | PASS; child Salvage/Smelting are excluded like baseline |
| `Skills.Acrobatics.Prevent_Dodge_Lightning` | `false` | `acrobatics.prevent_dodge_lightning=0` | PASS loader gate |
| `Experience_Values.Acrobatics.FeatherFall_Multiplier` | `2.0` | `actions-xp.properties`: `acrobatics|featherfall_multiplier=2.0` | PASS loader XP multiplier |
| `Skills.Alchemy.Enabled_for_Hoppers` | `true` | `alchemy.enabled_for_hoppers=true` | PASS loader hopper-cycle gate; station owner still controls which player receives XP |
| `Skills.Alchemy.Prevent_Hopper_Transfer_Ingredients` | `false` | `alchemy.prevent_hopper_transfer_ingredients=false` | PASS loader mixin |
| `Skills.Alchemy.Prevent_Hopper_Transfer_Bottles` | `false` | `alchemy.prevent_hopper_transfer_bottles=false` | PASS loader mixin |
| `Abilities.Enabled` | `true` | `skill.<skill>.abilities_enabled=true` | PASS; disabling it blocks active triggers while passive effects remain enabled |
| `Abilities.Activation.Only_Activate_When_Sneaking` | `false` | `abilities.only_activate_when_sneaking=false` | PASS common/config path on both loaders |
| `Abilities.Cooldowns.Super_Breaker` | `240` seconds | `DefaultAbilityCatalog` + `CooldownService` | PASS common/command path |
| `Abilities.Cooldowns.Tree_Feller` | `240` seconds | `DefaultAbilityCatalog` + `CooldownService` | PASS common/command path |
| `Abilities.Cooldowns.Blast_Mining` | `60` seconds | `DefaultAbilityCatalog` + `CooldownService` | PASS common/command path |
| `Skills.General.Ability.Length.Standard.CapLevel/IncreaseLevel` | `50 / 5` | `abilities.duration_cap_level=50` / `abilities.duration_increase_level=5` | PASS common formula/loader path |
| `Abilities.Tools.Durability_Loss` | `1` | active block ability consumes one main-hand durability | PASS loader policy |
| `Abilities.Tools.Durability_Loss=0` | disables extra ability damage | `abilities.durability_loss=0` | PASS common effect gate |
| `Skills.Mining.Level_Cap` | `0` (no limit) | `0` (no limit), configurable | PASS |
| `Skills.Woodcutting.Level_Cap` | `0` (no limit) | `0` (no limit), configurable | PASS |
| `Skills.Fishing.ShakeChance.Rank_1..8` | `15,20,25,35,45,55,65,75` | `FishingEngine` rank table | PASS common formula; loader reward interaction pending smoke |
| `Fishing_treasures.Shake` | entity loot table | `fishing-shake.properties` with vanilla paths or namespaced modded entity IDs, item IDs and potion metadata | PASS table/resolver; runtime player smoke pending |
| `Fishing_treasures.Treasure` | baseline rarity/item entries | `fishing-treasures.properties` generated from the fixed baseline; namespaced item IDs and reward values validated | PASS table/resolver; runtime player smoke pending |
| `Skills.Fishing.VanillaXPMultiplier.Rank_1..8` | `1,2,3,3,4,4,5,5` | `FishingHook.retrieve` XP-orb hook | PASS loader hook; player smoke pending |
| `Fishing_ExploitFix_Options.MoveRange` | `3` | `FishingEngine` `3` | PASS common guard |
| `Fishing_ExploitFix_Options.OverFishLimit` | `10` | `FishingEngine` `10` | PASS common guard |
| `Skills.Fishing.MasterAngler` | `10/30` ticks per rank, `10/30` boat bonus, caps `40/100` | `fishing.master_angler_*` | PASS common formula and loader cap path; player smoke pending |
| `Skills.Fishing.Drops_Enabled` | `true` | `fishing.drops_enabled=true` | PASS config and both loader reward gates |
| `Skills.Fishing.Override_Vanilla_Treasures` | `true` | `fishing.override_vanilla_treasures=true` | PASS pre-spawn Fabric hook and NeoForge fishing event replacement |
| `Skills.Fishing.Extra_Fish` | `false` | `fishing.extra_fish=false` | PASS custom treasure replaces vanilla catch by default; loader smoke pending |
| `Skills.Fishing.Lure_Modifier` | `4.0` | `fishing.lure_modifier=4.0` | PASS common roll and both loader callers |
| `Skills.Fishing.Allow_Conflicting_Enchants` | `false` | `fishing.allow_conflicting_enchants=false` | PASS shared enchantment compatibility gate; player smoke pending |
| `Skills.Archery.ArrowRetrieval.ChanceMax/MaxBonusLevel` | `100.0/100` | `combat.archery.arrow_retrieval_*` | PASS formula; death-event inventory smoke pending |
| `Experience_Values.Archery.Distance_Multiplier` | `0.025` | `combat.archery.distance_xp_multiplier=0.025` | PASS common formula and bounded loader origin tracking; player smoke pending |
| `Skills.Archery.ForceMultiplier` | `2.0` | `combat.archery.force_multiplier=2.0` | PASS common formula and loader origin tracking; player smoke pending |
| `Skills.Crossbows.TrickShot` | rank 1..3 max bounces | `DefaultAbilityCatalog` rank + bounded arrow mixin | PARTIAL: baseline block ricochet wired; entity bounce is not part of the fixed reference, protection-context confirmation remains |
| `Skills.General.LimitBreak.AllowPVE` | `false` | `combat.limit_break_allow_pve=0` | PASS; ranked Limit Break damage is centralized in combat resolution |
| Secondary AOE targets | own pets/party/PvP/environment checks | own tamed pets/horses, spectators and disabled-PvP players filtered | PARTIAL: party, region, NPC and vanish integrations remain pending |
| `Skills.Taming.Gore.Modifier` | `2.0` | `combat.taming.gore_multiplier=2.0` | PASS formula |
| Combat chance `MaxBonusLevel` values | `100` for Daze/Critical Strikes/Gore/Arrow Deflect/Iron Grip/Counter Attack | corresponding `combat.*_max_level=100` keys | PASS common formula path |
| `Skills.Taming.FastFoodService.Chance` | `50.0` | `taming.fast_food_chance=50.0` | PASS formula |
| `Skills.Taming.CallOfTheWild.MinHorseJumpStrength/MaxHorseJumpStrength` | `0.7/2.0` | `taming.call_of_wild_min_horse_jump_strength=0.7` / `taming.call_of_wild_max_horse_jump_strength=2.0` | PASS common/loader path |
| `ExploitFix.COTWBreeding` | `true` | `taming.cotw_breeding_prevented=1` | PASS loader mixin; summoned pets and horses cannot create offspring |
| `Skills.Unarmed.Disarm.ChanceMax/MaxBonusLevel` | `33 / 100` | `combat.unarmed.disarm_max_percent=33` / `combat.unarmed.disarm_max_level=100` | PASS common formula test |
| `Skills.Taming.ThickFur.Modifier` | `2.0` | `taming.thick_fur_divisor=2.0` | PASS formula |
| `Skills.Taming.ShockProof.Modifier` | `6.0` | `taming.shock_proof_divisor=6.0` | PASS formula |
| `Skills.Taming.SharpenedClaws.Bonus` | `2.0` | `taming.sharpened_claws_bonus=2.0` | PASS formula |
| `Skills.Maces.Cripple.Chance_To_Apply_On_Hit.Rank_1..4` | `10/15/20/33` | `combat.maces.cripple_chance_rank_1..4` | PASS common formula path |
| `Skills.Smelting.VanillaXPMultiplier.Rank_1..8` | `1,2,3,3,4,4,5,5` | `smelting.vanilla_xp_multiplier_rank_1..8` | PASS common formula and loader path |
| `Skills.Axes.SkullSplitter.DamageModifier` | `2.0` | `combat.axes.skull_splitter_damage_divisor=2.0` | PASS common formula path |
| `Skills.Swords.Rupture.Duration_In_Seconds` | `5` players / `5` mobs | `combat.swords.rupture_duration_ticks_pvp/pve=100` | PASS common formula path |
| `Skills.Swords.Rupture.Chance_To_Apply_On_Hit.Rank_1..4` | `15/33/40/66` | `combat.swords.rupture_chance_rank_1..4`, cap `66` | PASS common formula path |
| `Skills.Spears.Momentum.Chance_To_Apply_On_Hit.Rank_1..10` | `5/10/15/20/25/30/35/40/45/50` | `combat.spears.momentum_chance_rank_1..10`, cap `50` | PASS common formula path |
| `Skills.Swords.SerratedStrikes.DamageModifier` | `4.0` | `combat.swords.serrated_strikes_damage_divisor=4.0` | PASS common formula path |
| `Skills.Acrobatics.XP_After_Teleport_Cooldown` | `5` seconds | `acrobatics.xp_after_teleport_cooldown_seconds=5` | PASS loader teleport tracking; fall effect remains active |
| `Skills.Mining.DoubleDrops.SilkTouch` | `true` | `mining.double_drops_silk_touch=1` | PASS configurable loader gate |
| `Bonus_Drops.Mining` | baseline material allowlist | `skills/mining-drops.properties` | PASS common allowlist with validated vanilla/modded overrides |
| `Skills.Mining.BlastMining` | radius, ore bonus and demolitions ranks | `BlastMiningEngine` + `mining.blast_*_rank_*` formula keys + explosion drop hook | PASS bounded/configurable loader path; player smoke pending |
| `Skills.Mining.BlastMining.Bonus_Drops.Enabled` | `true` | `mining.blast_bonus_drops_enabled=1` | PASS common/loader drop path |
| `Skills.Mining.BlastMining.Bonus_Drops.Chance` | `50%` | `mining.blast_bonus_drop_chance=50` | PASS common/loader drop path |
| `Skills.Mining.Detonator_Name` | `FLINT_AND_STEEL` | `mining.detonator_item=minecraft:flint_and_steel` | PASS configurable remote detonation item |
| `Skills.Woodcutting.TreeFeller.TreeFellerReducedXP` | `true` | `woodcutting.tree_feller_reduced_xp=1` | PASS formula/config path; reduction includes the initial log; player drop smoke pending |
| `Skills.Woodcutting.CleanCuts/HarvestLumber.MaxBonusLevel` | `1000 / 100` | `woodcutting.clean_cuts_max_level=1000` / `woodcutting.harvest_lumber_max_level=100`; `woodcutting-drops.properties` controls eligible blocks | PASS common and Tree Feller formula paths |
| `Skills.Herbalism.DoubleDrops.MaxBonusLevel` | `100` | `herbalism.double_drops_max_level=100` | PASS common formula path |
| `Skills.Herbalism.GreenThumb/HylianLuck/ShroomThumb.MaxBonusLevel` | `100 / 100 / 100` | corresponding `herbalism.*_max_level=100` keys | PASS common/loader formula path |
| `Skills.Herbalism.Prevent_AFK_Leveling` | `true` | `herbalism.prevent_afk_leveling=1` | PASS common/loader vehicle gate |
| `Skills.Repair.SuperRepair.MaxBonusLevel` | `100` | `repair.super_repair_max_level=100` | PASS common formula path |
| `Skills.Woodcutting.KnockOnWood.XP_Orb` | enabled by default | `woodcutting.knock_on_wood_xp_orb_enabled=1` | PASS formula/runtime path; player smoke pending |
| `Salvage.ArcaneSalvage` | loss/downgrade enabled; max enchant 5 | `salvage.arcane_salvage_*` | PASS bounded extraction policy |
| `Skills.Unarmed.Disarm.AntiTheft` | `false` | `combat.unarmed.disarm_anti_theft=0` | PASS loader mixin; owner can recover, other players cannot |
| `Skills.Unarmed.SteelArmStyle.Damage_Override` | `false` | `combat.unarmed.steel_arm_damage_override=0` | PASS default formula; rank overrides available |
| `Skills.Unarmed.Block_Cracker.Allow_Block_Cracker` | `true` | `combat.unarmed.block_cracker_enabled=1` | PASS loader interaction gate |
| `Skills.Unarmed.Items_As_Unarmed` | `false` | `combat.unarmed.items_as_unarmed=0` | PASS loader classification gate |

Call of the Wild and Concoctions also generate validated namespaced override
files under `config/bigbangskills/skills`; their generated defaults preserve
the fixed baseline recipes.

Block and action XP tables are derived from the fixed baseline and can be
overridden under `config/bigbangskills/skills`; full per-skill event parity is
tracked in `FULL_SKILL_PARITY_AUDIT.md`.
