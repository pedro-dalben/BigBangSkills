# BigBangSkills — loader parity

Atualizado em 2026-08-10. `PASS` abaixo significa validação executada; build/boot não substituem gameplay.

| Feature | Fabric | NeoForge |
| --- | --- | --- |
| Build | PASS | PASS |
| Boot dedicado | PASS | PASS |
| Player join / profile load | PASS — smoke manual | PENDENTE — repetir smoke |
| Player quit / unload | PENDENTE — logout dedicado | PENDENTE — repetir smoke |
| Block break adapter | PASS — smoke manual | PENDENTE — repetir smoke |
| Block place provenance | PASS — smoke manual | PENDENTE — repetir smoke |
| Mining XP | PASS — smoke manual | PENDENTE — repetir smoke |
| Woodcutting XP | PASS — smoke manual | PENDENTE — repetir smoke |
| `/skills` | PASS — resposta do jogador | PENDENTE — resposta do jogador |
| `/skills mining` | PASS — resposta do jogador | PENDENTE — resposta do jogador |
| `/skillsadmin status` | PASS no console | BUILD PASS; gameplay/admin console PENDENTE |
| SQLite startup/migration | PASS | PASS |
| Periodic/logout/shutdown persistence | AUTOMATED PASS; gameplay PENDENTE | AUTOMATED PASS; gameplay PENDENTE |
| Persisted provenance restart | AUTOMATED PASS; gameplay PENDENTE | AUTOMATED PASS; gameplay PENDENTE |
| `/skills top [skill]` / admin mutations | BUILD PASS; manual PENDENTE | BUILD PASS; manual PENDENTE |
| Vanilla client without mod | PENDENTE | PENDENTE |

Os adapters usam o mesmo `PlayerProgressService`, `GameplayService`, formatter e repository JDBC. A provenance persistente também é common: bitsets por section são gravados em `world/data/bigbangskills-provenance.dat` com escrita assíncrona e fail-closed.

## Regra de cliente

O Core é server-side. O smoke Fabric executado usou o cliente de desenvolvimento para automatizar a sessão; isso prova o adapter, mas não a compatibilidade sem mod no cliente. A linha vanilla permanece pendente até uma conexão separada com cliente vanilla.

Permissão administrativa atualmente usa o nível vanilla de operador (`source.hasPermission(2)`) nos dois loaders. Isso é um fallback seguro e não depende de LuckPerms; permission nodes nomeados ficam para uma integração de permissões posterior.
