# Roadmap incremental

Cada PR deve partir da branch principal atualizada, conter testes proporcionais, `git diff --check`, build Fabric/NeoForge quando o módulo existir e uma verificação separada de dedicated server. Commits devem ser pequenos e temáticos; refactors não relacionados ficam fora. Não publicar release automaticamente na primeira fase.

## Fase 0 — pesquisa e arquitetura (este trabalho)

Entregues:

* análise funcional do mcMMO;
* decisão multi-loader;
* arquitetura de domínio e adapters;
* modelo de dados, XP, anti-exploit e rede;
* ADRs e este roadmap.

Não contém implementação do mod.

## PR 1 — Research & Architecture

Somente documentação, schema lógico e riscos. Critério: decisões explícitas, alternativas e fontes.

## PR 2 — Multiloader Bootstrap

Criar `api`, `common`, `fabric` e `neoforge`. Startup server-side imprime `BigBangSkills loaded`. CI compila os três lados; dedicated server inicia nos dois loaders.

## PR 3 — Core Domain

Implementar `SkillId`, definitions/registry, `SkillState`, `XpCurve`, `XpService`, `PlayerProgress`, `PowerLevelCalculator`, modifiers e eventos internos. Testes JVM; sem eventos Minecraft complexos.

## PR 4 — Persistence

Implementar repository, migrations, SQLite dev, JDBC MySQL/MariaDB, cache, dirty tracking e worker assíncrono. Testar retry, ledger idempotente, rollback e concorrência.

## PR 5 — Mining vertical slice

Mining em Fabric e NeoForge: tags, XP, curva, mundo/permissão, notification, command de leitura, provenance básico, persistence e dedicated-server smoke.

## PR 6 — Woodcutting

Adicionar segunda skill somente por definição/adapter mínimo. Se o diff exigir mudanças espalhadas no core, pausar e revisar ADR-003.

## PR 7 — Abilities

Framework de passivas/ativas, unlock, requirement e cooldown por timestamp. Começar com uma ability simples e protegida; sem tree-break complexo ainda.

## PR 8 — Skills adicionais

Adicionar poucas skills por PR, agrupadas por fonte de evento. Cada uma precisa de política anti-exploit e teste de fluxo; não aceitar pacote “todas as skills”.

## PR 9 — Leaderboards e comandos

`/skills`, `/skills <skill>`, `/skills stats`, `/skills top` com cache/snapshot, limites e permissions. Admin commands com auditoria e confirmação.

## PR 10 — Network mode

Ativar `NETWORK`, session lease, troca BigMonCraft ↔ AllTheMons, MySQL/MariaDB real e smoke de reconexão/concorrência. Provar que A/B não perdem deltas.

## PR 11 — Cobblemon integration

Somente após estudar eventos/API oficiais do Cobblemon 1.21.1. Módulo opcional, ausência não impede startup, sem mixin quando houver API suficiente.

## Gates da Definition of Done

Uma fase só fecha quando:

* compila no Fabric e NeoForge quando aplicável;
* tem testes adequados e `diff --check` limpo;
* não bloqueia game thread;
* não bypassa proteção;
* não tem exploit óbvio coberto pelo escopo;
* possui configuração/documentação/erro observável;
* dedicated server foi testado;
* o fluxo real pedido foi smoke-testado quando a fase envolve gameplay.

Gradle boot não substitui player smoke. JUnit não substitui dedicated server. Dedicated server sem o fluxo pedido não prova a feature completa.
