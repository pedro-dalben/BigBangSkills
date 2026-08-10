# BigBangSkills — status da implementação

Atualizado em 2026-08-10.

## Entregue

* Gradle multi-project com `api`, `common`, `persistence-sql`, `fabric` e `neoforge`.
* Registry, Mining/Woodcutting, XP, levels, power level, modifiers, provenance conservadora, abilities/cooldowns e pipeline server-side.
* `PlayerProgressService` com cache por UUID, estados explícitos, load assíncrono, fila bounded durante load, dirty tracking, flush periódico, flush de logout e shutdown com timeout.
* Snapshot de save com eventos idempotentes; mutações que chegam durante um save permanecem na fila seguinte.
* JDBC versionado com `schema_version`, SQLite, MySQL e MariaDB; HikariCP lazy e empacotado nos dois loaders; configuração runtime sem logar senha/JDBC URL secreto.
* `/skills`, `/skills mining`, `/skills woodcutting`, `/skills top [skill]` e a suíte `/skillsadmin` nos dois adapters, usando cache/formatter comum.
* Player join/quit, server started/stopping/stopped e place tracking nos dois loaders.
* NotificationService com agregação de XP, feedback de level-up e mensagens `en_us`/`pt_br`.
* Provenance persistente bounded por bitset de section, com flush atômico e fail-closed após falha de leitura/escrita.
* Testes de fila pré-load, dirty durante save, admin cache, underflow administrativo, ledger idempotente, writers concorrentes, leaderboard SQL e contrato MySQL/MariaDB opt-in.

## Histórico preservado da base

* A base anterior já entregava o Gradle multi-project, domínio sem imports de loader, repository JDBC assíncrono com ledger idempotente, drivers MySQL/MariaDB declarados, `/skills` inicial no Fabric e hooks de quebra nos dois loaders.
* O boot dedicado anterior chegou a `Done` no NeoForge e era bloqueado pelo gate de EULA local no Fabric; o gate agora foi resolvido somente nos diretórios ignorados de desenvolvimento.
* Antes desta etapa ainda faltavam cache de login/logout/shutdown, configuração runtime de banco, admin status, flush lifecycle, place tracking NeoForge e player smoke; os fluxos comuns foram implementados e os gates manuais ainda não executados permanecem abaixo.

## Validado

* `./gradlew clean build` passou em 2026-08-10 antes da última rodada de hardening; o build limpo desta rodada ainda é o gate final.
* `fabric:runServer` passou pelo boot dedicado, abriu SQLite/Hikari, executou migration e respondeu `/skillsadmin status` no console.
* `neoforge:runServer` passou pelo boot dedicado, abriu SQLite/Hikari e executou migration.
* Fabric teve sessão manual com jogador: login, `/skills`, `/skills mining`, Woodcutting e place/break anti-exploit.
* A tabela detalhada por loader está em [LOADER_PARITY.md](LOADER_PARITY.md).

## Ainda pendente

* Smoke com cliente vanilla sem o mod, e repetição de gameplay em NeoForge, incluindo logout/restart e XP restaurado.
* Execução do contrato MySQL/MariaDB contra servidor real nesta máquina; o teste reproduzível está preparado, mas `BIGBANGSKILLS_MYSQL_JDBC_URL` não está configurada.
* Propagação de provenance em piston, explosão, fluidos, árvores e transformações de mods.
* Lease de sessão NETWORK e outbox entre servidores; o cache de leaderboard SQL já possui TTL de 30 segundos.
* Hot reload de regras de XP/notificações/anti-exploit; o comando atual valida arquivos e informa que os valores entram após restart.

## Known Issues

* Provenance persistida cobre place/break de minério/log observado pelos loaders; piston, explosão, fluidos e transformações externas continuam fora do escopo.
* Permissões nomeadas ainda usam o fallback vanilla de nível de operador; não há integração obrigatória com LuckPerms.
* Admin offline pode concorrer com um login simultâneo; a operação usa transação/ledger, mas precisa de lease/session ownership antes de multi-servidor.
* O cliente vanilla sem BigBangSkills e o gameplay NeoForge ainda não foram executados nesta rodada; build/boot não são equivalentes a gameplay PASS.
* `persistence-sql` usa um executor single-writer por instância; a atualização agregada é atômica para múltiplos writers, mas throughput maior exigirá pool de workers/particionamento medido.
* O warning do Loom sobre a versão SQLite `3.46.1.0` não impede o build; o artifact foi incluído nos JARs finais.
