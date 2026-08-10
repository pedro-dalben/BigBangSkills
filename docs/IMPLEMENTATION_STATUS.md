# BigBangSkills — status da implementação

Atualizado em 2026-08-10.

## Entregue nesta branch

* Gradle multi-project com `api`, `common`, `persistence-sql`, `fabric` e `neoforge`.
* Domínio sem imports de loader/Minecraft: IDs namespaced, registry, XP total, curva linear, níveis derivados, power level e modifiers.
* Pipeline de Mining/Woodcutting com tags, provenance conservadora, rejeição de actor não-humano e cooldown/ability timestamps.
* Repository JDBC assíncrono com ledger idempotente e teste SQLite; drivers MySQL/MariaDB declarados.
* `/skills` inicial no Fabric, hooks de quebra em Fabric e NeoForge, CI e empacotamento dos módulos no JAR final.
* Testes JVM, build Fabric/NeoForge e boot de dedicated server nos dois loaders; o run de desenvolvimento foi encerrado pelo gate de EULA no Fabric e chegou a `Done` no NeoForge.

## Ainda não é Definition of Done

* Cache persistente de login/logout/shutdown ainda não está ligado ao hook de gameplay.
* Persistência MySQL/MariaDB tem implementação JDBC comum, mas ainda não tem teste contra servidor real/Testcontainers nem configuração runtime.
* Admin commands, notifications configuráveis, localization, reload, status operacional e auditoria admin ainda faltam.
* Leaderboard atual é snapshot em memória; falta integrar consulta SQL/cache no servidor.
* Provenance ainda é memória limitada por servidor; falta armazenamento por chunk e propagação de piston/explosão/transformações.
* NeoForge ainda precisa do equivalente completo de comandos, place tracking e lifecycle de flush.
* Não houve player smoke com cliente conectado; build/boot não prova quebra real de bloco, XP persistido ou `/skills` em jogo.
