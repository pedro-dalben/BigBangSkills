# BigBangSkills — loader parity

Atualizado em 2026-08-11. `PASS` abaixo significa validação executada; build/boot não substituem gameplay.

| Feature | Fabric | NeoForge |
| --- | --- | --- |
| Build | PASS — clean build + remapJar | PASS — clean build + jar |
| Boot dedicado | PASS — mixins server loaded; SQLite/Hikari nesta rodada | PASS — mixins/events server loaded; SQLite/Hikari nesta rodada |
| Player join / profile load | HISTORICAL PASS — evidence from an earlier manual/session cycle | PENDENTE — user-provided manual evidence |
| Player quit / unload | PENDENTE — logout dedicado | PENDENTE — repetir smoke |
| Block break adapter | PASS — smoke manual | PENDENTE — repetir smoke |
| Block place provenance | PASS — smoke manual | PENDENTE — repetir smoke |
| Mining XP | AUTOMATED PASS — tabela baseline; gameplay smoke anterior | AUTOMATED PASS — tabela common; gameplay PENDENTE |
| Woodcutting XP | AUTOMATED PASS — tabela baseline; gameplay smoke anterior | AUTOMATED PASS — tabela common; gameplay PENDENTE |
| Mining/Woodcutting bonus drops | COMPILE PASS — BEFORE/AFTER loot path | COMPILE PASS — Break/BlockDrops path |
| Active ability state/cooldown | COMPILE PASS — server command/state | COMPILE PASS — server command/state |
| Generic action XP dispatcher | AUTOMATED PASS — common queue | AUTOMATED PASS — common queue |
| Combat dispatcher/effects | COMPILE + boot PASS; Fabric hurt-variable resolver plus post-damage effects/XP; gameplay PENDENTE | COMPILE + boot PASS; mutable pre-damage resolver plus final-damage XP event; gameplay PENDENTE |
| Fishing/taming/repair/smelting boundaries | Mixins compile + boot PASS; fishing guard/tier/Luck of the Sea treasure, Shake/Fisherman's Diet hooks, Master Angler/Ice Fishing, pet hurt mixin, Second Smelt serverTick and restart-safe station owner | Events/mixins compile + boot PASS; fishing guard/tier/Luck of the Sea treasure, Shake/Fisherman's Diet hooks, Master Angler/Ice Fishing, wolf defence, pet combat, Second Smelt serverTick and restart-safe station owner |
| Salvage / Alchemy boundaries | Salvage interaction + brewing timer/doBrew/hopper policy mixins compile + boot PASS | Salvage interaction + brewing timer/doBrew/hopper policy mixins compile + boot PASS |
| Skill registry (19 baseline skills) | COMPILE PASS | COMPILE PASS |
| `/skills <skill>` dynamic details | COMPILE PASS; formatter common exposes XP curve, passive/active modes, activation, cooldowns and restrictions | COMPILE PASS; same common formatter |
| `/skills` | PASS — resposta recebida pelo cliente conectado; manual player flow remains limited | PENDENTE — resposta do jogador |
| `/skills mining` | PASS — resposta do jogador | PENDENTE — resposta do jogador |
| `/skillsadmin status` | PASS no console | BUILD PASS; gameplay/admin console PENDENTE |
| SQLite startup/migration | PASS | PASS |
| Periodic/logout/shutdown persistence | AUTOMATED PASS; gameplay PENDENTE | AUTOMATED PASS; gameplay PENDENTE |
| Persisted provenance restart | AUTOMATED PASS; gameplay PENDENTE | AUTOMATED PASS; gameplay PENDENTE |
| `/skills top [skill]` / admin mutations | BUILD PASS; manual PENDENTE | BUILD PASS; manual PENDENTE |
| Vanilla client without mod | PENDENTE | PENDENTE |

Os adapters usam o mesmo `PlayerProgressService`, `GameplayService`, formatter e repository JDBC. A provenance persistente também é common: bitsets por section são gravados em `world/data/bigbangskills-provenance.dat` com escrita assíncrona e fail-closed. Fishing/Taming compartilham as fórmulas comuns; só os pontos de mutação do dano variam por API.

## Matriz completa por skill

`COMPILE` prova que o adapter está presente e compila. `AUTOMATED` prova apenas
as fórmulas, tabelas ou serviços common cobertos pelos testes. `GAMEPLAY` só
vira `PASS` com evidência manual do usuário; nenhum cliente/servidor Minecraft
é iniciado automaticamente por este projeto.

| Skill | Fabric | NeoForge | Common automated coverage |
| --- | --- | --- | --- |
| Acrobatics | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — roll, graceful roll, dodge |
| Alchemy | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — brew speed, tiers, concoctions |
| Archery | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — retrieval, distance, combat dispatcher |
| Axes | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — mastery, impact, limit break |
| Crossbows | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — shared combat and bounded trick-shot math |
| Excavation | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — archaeology and bounded chain |
| Fishing | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — tiers, shake, anti-exploit, food |
| Herbalism | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — maturity, conversions, Hylian Luck |
| Maces | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — crush/cripple boundaries |
| Mining | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — XP, drops, provenance, blast bounds |
| Repair | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — mastery and Arcane Forging |
| Salvage | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — yield, gates, Arcane Salvage |
| Smelting | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — fuel, Second Smelt boundary, XP |
| Spears | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — shared combat dispatcher |
| Swords | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — rupture, serrated, counter |
| Taming | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — tame bonuses, summon, lethal guard |
| Tridents | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — impale and shared combat dispatcher |
| Unarmed | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — steel arm, deflect, iron grip |
| Woodcutting | COMPILE PASS; GAMEPLAY PENDENTE | COMPILE PASS; GAMEPLAY PENDENTE | PASS — XP, drops, Tree Feller bounds |

## Regra de cliente

O Core é server-side. Há evidência histórica de uma sessão Fabric anterior,
mas ela não é repetida automaticamente e não substitui a bateria manual atual.
A linha vanilla permanece pendente até uma conexão separada feita manualmente
pelo usuário.

Permissão administrativa atualmente usa o nível vanilla de operador (`source.hasPermission(2)`) nos dois loaders. Isso é um fallback seguro e não depende de LuckPerms; permission nodes nomeados ficam para uma integração de permissões posterior.
