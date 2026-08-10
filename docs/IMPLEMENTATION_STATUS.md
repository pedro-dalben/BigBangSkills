# BigBangSkills — status da implementação

Atualizado em 2026-08-10.

## Entregue

* Gradle multi-project com `api`, `common`, `persistence-sql`, `fabric` e `neoforge`.
* Registry, Mining/Woodcutting, XP, levels, power level, modifiers, provenance conservadora, abilities/cooldowns e pipeline server-side.
* `PlayerProgressService` com cache por UUID, estados explícitos, load assíncrono, fila bounded durante load, dirty tracking, flush periódico, flush de logout e shutdown com timeout.
* Snapshot de save com eventos idempotentes; mutações que chegam durante um save permanecem na fila seguinte.
* JDBC versionado com `schema_version`, SQLite, MySQL e MariaDB; HikariCP lazy e empacotado nos dois loaders; configuração runtime sem logar senha/JDBC URL secreto.
* `/skills`, `/skills mining`, `/skills woodcutting` e `/skillsadmin status` nos dois adapters, usando cache/formatter comum.
* Player join/quit, server started/stopping/stopped e place tracking nos dois loaders.
* Testes de fila pré-load, dirty durante save, ledger idempotente, writers concorrentes, leaderboard SQL e contrato MySQL/MariaDB opt-in.

## Histórico preservado da base

* A base anterior já entregava o Gradle multi-project, domínio sem imports de loader, repository JDBC assíncrono com ledger idempotente, drivers MySQL/MariaDB declarados, `/skills` inicial no Fabric e hooks de quebra nos dois loaders.
* O boot dedicado anterior chegou a `Done` no NeoForge e era bloqueado pelo gate de EULA local no Fabric; o gate agora foi resolvido somente nos diretórios ignorados de desenvolvimento.
* Antes desta etapa ainda faltavam cache de login/logout/shutdown, configuração runtime de banco, admin status, flush lifecycle, place tracking NeoForge e player smoke; esses gaps continuam separados abaixo quando a validação manual ainda não existe.

## Validado

* `./gradlew clean build` passou em 2026-08-10.
* `fabric:runServer` passou pelo boot dedicado, abriu SQLite/Hikari, executou migration e respondeu `/skillsadmin status` no console.
* `neoforge:runServer` passou pelo boot dedicado, abriu SQLite/Hikari e executou migration.
* A tabela detalhada por loader está em [LOADER_PARITY.md](LOADER_PARITY.md).

## Ainda pendente

* Smoke com cliente real conectado nos dois loaders: join, block break, place/break anti-exploit, `/skills`, quit, restart e XP restaurado.
* Execução do contrato MySQL/MariaDB contra servidor real nesta máquina; o teste reproduzível roda quando `BIGBANGSKILLS_MYSQL_JDBC_URL` e credenciais são fornecidos.
* Provenance persistente por chunk e propagação de piston, explosão, fluidos, árvores e transformações de mods.
* Lease de sessão NETWORK, outbox local e cache SQL de leaderboard no comando; o repository já oferece query limitada.
* Admin XP/level/reset, auditoria de mutações administrativas e persistência de estado de abilities.

## Known Issues

* O tracker de provenance atual é bounded em memória por processo; após restart, a política segura depende de integração persistente ainda não implementada.
* `persistence-sql` usa um executor single-writer por instância; a atualização agregada é atômica para múltiplos writers, mas throughput maior exigirá pool de workers/particionamento medido.
* O warning do Loom sobre a versão SQLite `3.46.1.0` não impede o build; o artifact foi incluído nos JARs finais.
