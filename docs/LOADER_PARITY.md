# BigBangSkills — loader parity

Atualizado em 2026-08-10. `PASS` abaixo significa validação executada; build/boot não substituem gameplay.

| Feature | Fabric | NeoForge |
| --- | --- | --- |
| Build | PASS — clean build + remapJar | PASS — clean build + jar |
| Boot dedicado | PASS — mixins server loaded; SQLite/Hikari nesta rodada | PASS — mixins/events server loaded; SQLite/Hikari nesta rodada |
| Player join / profile load | PASS — smoke manual + Loom client quickplay (`Player978` joined) | PENDENTE — repetir smoke |
| Player quit / unload | PENDENTE — logout dedicado | PENDENTE — repetir smoke |
| Block break adapter | PASS — smoke manual | PENDENTE — repetir smoke |
| Block place provenance | PASS — smoke manual | PENDENTE — repetir smoke |
| Mining XP | AUTOMATED PASS — tabela baseline; gameplay smoke anterior | AUTOMATED PASS — tabela common; gameplay PENDENTE |
| Woodcutting XP | AUTOMATED PASS — tabela baseline; gameplay smoke anterior | AUTOMATED PASS — tabela common; gameplay PENDENTE |
| Mining/Woodcutting bonus drops | COMPILE PASS — BEFORE/AFTER loot path | COMPILE PASS — Break/BlockDrops path |
| Active ability state/cooldown | COMPILE PASS — server command/state | COMPILE PASS — server command/state |
| Generic action XP dispatcher | AUTOMATED PASS — common queue | AUTOMATED PASS — common queue |
| Combat dispatcher/effects | COMPILE + boot PASS; Fabric hurt-variable resolver plus post-damage effects; gameplay PENDENTE | COMPILE + boot PASS; mutable damage resolver; gameplay PENDENTE |
| Fishing/taming/repair/smelting boundaries | Mixins compile + boot PASS; fishing guard/tier/Luck of the Sea treasure, Shake/Fisherman's Diet hooks, Master Angler/Ice Fishing, pet hurt mixin, Second Smelt serverTick and restart-safe station owner | Events/mixins compile + boot PASS; fishing guard/tier/Luck of the Sea treasure, Shake/Fisherman's Diet hooks, Master Angler/Ice Fishing, wolf defence, pet combat, Second Smelt serverTick and restart-safe station owner |
| Salvage / Alchemy boundaries | Salvage interaction + brewing timer/doBrew mixins compile + boot PASS | Salvage interaction + brewing timer/doBrew mixins compile + boot PASS |
| Skill registry (19 baseline skills) | COMPILE PASS | COMPILE PASS |
| `/skills <skill>` dynamic details | COMPILE PASS | COMPILE PASS |
| `/skills` | PASS — resposta recebida pelo cliente conectado; manual player flow remains limited | PENDENTE — resposta do jogador |
| `/skills mining` | PASS — resposta do jogador | PENDENTE — resposta do jogador |
| `/skillsadmin status` | PASS no console | BUILD PASS; gameplay/admin console PENDENTE |
| SQLite startup/migration | PASS | PASS |
| Periodic/logout/shutdown persistence | AUTOMATED PASS; gameplay PENDENTE | AUTOMATED PASS; gameplay PENDENTE |
| Persisted provenance restart | AUTOMATED PASS; gameplay PENDENTE | AUTOMATED PASS; gameplay PENDENTE |
| `/skills top [skill]` / admin mutations | BUILD PASS; manual PENDENTE | BUILD PASS; manual PENDENTE |
| Vanilla client without mod | PENDENTE | PENDENTE |

Os adapters usam o mesmo `PlayerProgressService`, `GameplayService`, formatter e repository JDBC. A provenance persistente também é common: bitsets por section são gravados em `world/data/bigbangskills-provenance.dat` com escrita assíncrona e fail-closed. Fishing/Taming compartilham as fórmulas comuns; só os pontos de mutação do dano variam por API.

## Regra de cliente

O Core é server-side. O smoke Fabric executado usou o cliente de desenvolvimento para automatizar a sessão; isso prova o adapter, mas não a compatibilidade sem mod no cliente. A linha vanilla permanece pendente até uma conexão separada com cliente vanilla.

Permissão administrativa atualmente usa o nível vanilla de operador (`source.hasPermission(2)`) nos dois loaders. Isso é um fallback seguro e não depende de LuckPerms; permission nodes nomeados ficam para uma integração de permissões posterior.
