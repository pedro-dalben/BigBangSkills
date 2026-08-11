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
| `Diminished_Returns.Enabled` | `false` | `diminished-returns.properties` | PASS; disabled by default |
| `Diminished_Returns.Guaranteed_Minimum_Percentage` | `0.05` | `guaranteed_minimum_fraction` | PASS config validation |
| `Diminished_Returns.Time_Interval` | `10` minutes | `interval_minutes` | PASS config validation |
| `Diminished_Returns.Threshold.<skill>` | `20000` for primary skills | `threshold.<skill>=20000` | PASS; child Salvage/Smelting are excluded like baseline |
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
| `Skills.Fishing.FishermansDiet.RankChange` | `20` | `FishingEngine` `20` | PASS formula and loader consumption hook |
| `Skills.Fishing.MasterAngler` | `10/30` ticks per rank, `10/30` boat bonus, caps `40/100` | `fishing.master_angler_*` | PASS common formula and loader cap path; player smoke pending |
| `Skills.Fishing.FishermansDiet.RankChange` | `20` | `fishing.fishermans_diet_rank_change=20` | PASS common formula path |
| `Skills.Archery.ArrowRetrieval.ChanceMax/MaxBonusLevel` | `100.0/100` | `combat.archery.arrow_retrieval_*` | PASS formula; death-event inventory smoke pending |
| `Experience_Values.Archery.Distance_Multiplier` | `0.025` | `combat.archery.distance_xp_multiplier=0.025` | PASS common formula and bounded loader origin tracking; player smoke pending |
| `Skills.Crossbows.TrickShot` | rank 1..3 max bounces | `DefaultAbilityCatalog` rank + bounded arrow mixin | PARTIAL: baseline block ricochet wired; entity bounce is not part of the fixed reference, protection-context confirmation remains |
| `Skills.General.LimitBreak.AllowPVE` | `false` | `combat.limit_break_allow_pve=0` | PASS; ranked Limit Break damage is centralized in combat resolution |
| Secondary AOE targets | own pets/party/PvP/environment checks | own tamed pets/horses, spectators and disabled-PvP players filtered | PARTIAL: party, region, NPC and vanish integrations remain pending |
| `Skills.Taming.Gore.Modifier` | `2.0` | `combat.taming.gore_multiplier=2.0` | PASS formula |
| Combat chance `MaxBonusLevel` values | `100` for Daze/Critical Strikes/Gore/Arrow Deflect/Iron Grip/Counter Attack | corresponding `combat.*_max_level=100` keys | PASS common formula path |
| `Skills.Taming.FastFoodService.Chance` | `50.0` | `taming.fast_food_chance=50.0` | PASS formula |
| `Skills.Taming.ThickFur.Modifier` | `2.0` | `taming.thick_fur_divisor=2.0` | PASS formula |
| `Skills.Taming.ShockProof.Modifier` | `6.0` | `taming.shock_proof_divisor=6.0` | PASS formula |
| `Skills.Taming.SharpenedClaws.Bonus` | `2.0` | `taming.sharpened_claws_bonus=2.0` | PASS formula |
| `Skills.Maces.Cripple.Chance_To_Apply_On_Hit.Rank_1..4` | `10/15/20/33` | `combat.maces.cripple_chance_rank_1..4` | PASS common formula path |
| `Skills.Mining.DoubleDrops.SilkTouch` | `true` | `mining.double_drops_silk_touch=1` | PASS configurable loader gate |
| `Skills.Mining.BlastMining` | radius, ore bonus and demolitions ranks | `BlastMiningEngine` + `mining.blast_*_rank_*` formula keys + explosion drop hook | PASS bounded/configurable loader path; player smoke pending |
| `Skills.Mining.BlastMining.Bonus_Drops.Enabled` | `true` | `mining.blast_bonus_drops_enabled=1` | PASS common/loader drop path |
| `Skills.Mining.BlastMining.Bonus_Drops.Chance` | `50%` | `mining.blast_bonus_drop_chance=50` | PASS common/loader drop path |
| `Skills.Woodcutting.TreeFeller.TreeFellerReducedXP` | `true` | `woodcutting.tree_feller_reduced_xp=1` | PASS formula/config path; player drop smoke pending |
| `Skills.Woodcutting.CleanCuts/HarvestLumber.MaxBonusLevel` | `1000 / 100` | `woodcutting.clean_cuts_max_level=1000` / `woodcutting.harvest_lumber_max_level=100` | PASS common formula path |
| `Skills.Herbalism.DoubleDrops.MaxBonusLevel` | `100` | `herbalism.double_drops_max_level=100` | PASS common formula path |
| `Skills.Herbalism.GreenThumb/HylianLuck/ShroomThumb.MaxBonusLevel` | `100 / 100 / 100` | corresponding `herbalism.*_max_level=100` keys | PASS common/loader formula path |
| `Skills.Repair.SuperRepair.MaxBonusLevel` | `100` | `repair.super_repair_max_level=100` | PASS common formula path |
| `Skills.Woodcutting.KnockOnWood.XP_Orb` | enabled by default | `woodcutting.knock_on_wood_xp_orb_enabled=1` | PASS formula/runtime path; player smoke pending |
| `Salvage.ArcaneSalvage` | loss/downgrade enabled; max enchant 5 | `salvage.arcane_salvage_*` | PASS bounded extraction policy |
| `Skills.Unarmed.SteelArmStyle.Damage_Override` | `false` | `combat.unarmed.steel_arm_damage_override=0` | PASS default formula; rank overrides available |

Call of the Wild and Concoctions also generate validated namespaced override
files under `config/bigbangskills/skills`; their generated defaults preserve
the fixed baseline recipes.

Block and action XP tables are derived from the fixed baseline and can be
overridden under `config/bigbangskills/skills`; full per-skill event parity is
tracked in `FULL_SKILL_PARITY_AUDIT.md`.
