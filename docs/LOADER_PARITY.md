# BigBangSkills — loader parity

Atualizado em 2026-08-10. `PASS` abaixo significa validação executada; build/boot não substituem gameplay.

| Feature | Fabric | NeoForge |
| --- | --- | --- |
| Build | PASS | PASS |
| Boot dedicado | PASS | PASS |
| Player join / profile load | PASS — smoke manual | PENDENTE — repetir smoke |
| Player quit / unload | PENDENTE — restart/logout dedicado | PENDENTE — repetir smoke |
| Block break adapter | PASS — smoke manual | PENDENTE — repetir smoke |
| Block place provenance | PASS — smoke manual | PENDENTE — repetir smoke |
| Mining XP | PASS — smoke manual | PENDENTE — repetir smoke |
| Woodcutting XP | PASS — smoke manual | PENDENTE — repetir smoke |
| `/skills` | PASS — resposta do jogador | PENDENTE — resposta do jogador |
| `/skills mining` | PASS — resposta do jogador | PENDENTE — resposta do jogador |
| `/skillsadmin status` | PASS no console | PENDENTE — console anterior não consumiu input |
| SQLite startup/migration | PASS | PASS |
| Periodic/logout/shutdown persistence | AUTOMATED PASS; gameplay PENDENTE | AUTOMATED PASS; gameplay PENDENTE |
| Persisted provenance restart | AUTOMATED PASS; gameplay PENDENTE | AUTOMATED PASS; gameplay PENDENTE |

Os adapters usam o mesmo `PlayerProgressService`, `GameplayService`, formatter e repository JDBC. A provenance persistente também é common: bitsets por section são gravados em `world/data/bigbangskills-provenance.dat` com escrita assíncrona e fail-closed.

## Regra de cliente

O Core é server-side. O smoke executado usou o cliente de desenvolvimento para automatizar a sessão; ainda é obrigatório repetir a conexão com um cliente vanilla sem `bigbangskills` antes de marcar essa linha como `PASS`.
