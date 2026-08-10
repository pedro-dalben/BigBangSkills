# BigBangSkills — loader parity

Atualizado em 2026-08-10. `PASS` abaixo significa validação executada nesta etapa; `PENDENTE` não é inferido de compilação.

| Feature | Fabric | NeoForge |
| --- | --- | --- |
| Boot dedicado | PASS | PASS |
| Player join / profile load | PENDENTE: sem cliente conectado | PENDENTE: sem cliente conectado |
| Player quit / unload | PENDENTE: sem cliente conectado | PENDENTE: sem cliente conectado |
| Block break adapter | PENDENTE: sem cliente conectado | PENDENTE: sem cliente conectado |
| Block place provenance | PENDENTE: sem cliente conectado | PENDENTE: sem cliente conectado |
| Mining XP | PENDENTE: sem cliente conectado | PENDENTE: sem cliente conectado |
| Woodcutting XP | PENDENTE: sem cliente conectado | PENDENTE: sem cliente conectado |
| `/skills` registration | PASS no boot; player response pendente | PASS no boot; player response pendente |
| `/skills mining` | PASS no boot; player response pendente | PASS no boot; player response pendente |
| `/skillsadmin status` | PASS no console | PENDENTE: console do run não consumiu input |
| SQLite startup/migration | PASS no boot | PASS no boot |
| Periodic/logout/shutdown persistence with player state | PENDENTE: no player smoke | PENDENTE: no player smoke |

Os adapters usam o mesmo `PlayerProgressService`, `GameplayService`, formatter de mensagens e repository JDBC. As diferenças restantes são somente conversão de eventos/lifecycle do loader.

## Smoke pendente

Executar com um cliente Minecraft 1.21.1 conectado a cada servidor: join, `/skills`, minério natural, log, minério colocado, quit, restart e nova entrada. O servidor local já está preparado com `eula=true` apenas nos diretórios ignorados `fabric/run` e `neoforge/run`; isso não é distribuído pelo mod.
