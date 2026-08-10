# BigBangSkills — arquitetura

Status: proposta da Fase 0
Data: 2026-08-10
Alvo inicial: Minecraft 1.21.1, Java 21, Fabric e NeoForge

## Resumo executivo

BigBangSkills será uma plataforma server-side de progressão. O domínio será Java puro e não conhecerá classes, eventos, mappings ou registries de Fabric, NeoForge ou Minecraft. Cada loader converterá eventos do jogo em comandos de domínio e implementará as portas de plataforma.

Escolha inicial:

```text
api/             contratos públicos, tipos de domínio estáveis
common/          progressão, skills, XP, abilities, regras e portas
persistence-sql/ JDBC, migrações e repositórios SQL
fabric/          lifecycle, eventos, comandos e runtime Fabric
neoforge/        lifecycle, eventos, comandos e runtime NeoForge
```

`common` não importará `net.minecraft`, `net.fabricmc`, `net.neoforged` ou JDBC. `persistence-sql` também não será carregado no caminho de evento; seus workers executam fora da thread do servidor.

## Fluxo principal

```text
Evento do loader
  -> adapter cria DomainAction
  -> SkillActionRouter encontra definições/implementações
  -> XpEligibilityService valida contexto, mundo, permissão e exploit
  -> XpService aplica modificadores
  -> ProgressionService calcula XP, níveis, unlocks e power level
  -> eventos internos imutáveis
       -> NotificationService
       -> RewardService
       -> PlayerProgressCache / persistência assíncrona
```

Nenhum objeto de Minecraft atravessa esse limite. O adapter passa somente IDs, UUIDs, posições/handles opacos quando indispensáveis e snapshots imutáveis do contexto.

## Módulos e responsabilidades

### `api`

Expõe somente contratos que outros mods podem consumir:

* `BigBangSkillsApi`;
* `SkillId`, `ProgressionScope`, `XpSource` e resultados imutáveis;
* eventos públicos e consultas de leitura;
* registro de skills e modificadores via interfaces versionadas.

Não expõe repository, SQL, cache interno, objetos do loader ou detalhes de serialização.

### `common`

Contém:

* `SkillDefinition`, `SkillRegistry`, `SkillImplementation`;
* `SkillState`, `PlayerProgress` e `PowerLevelCalculator`;
* `XpRequest`, `XpModifier`, `XpPipeline` e `XpCurve`;
* `AbilityDefinition`, `AbilityService` e `CooldownService`;
* validação de configuração e regras de mundo/permissão por portas;
* eventos internos e serviços de notificação/recompensa;
* contratos de persistência e observabilidade.

O core não quebra blocos, causa dano, lê chat, toca som ou acessa banco. Essas ações são portas executadas pelo adapter.

### `persistence-sql`

Implementa as portas com JDBC, migrações versionadas, SQLite para desenvolvimento e MySQL/MariaDB para produção. O SQL deve suportar transações, idempotência e atualização atômica. A implementação fornece `CompletableFuture`/fila assíncrona; nenhuma API síncrona será chamada pelo listener do jogo.

### `fabric` e `neoforge`

Cada módulo contém somente:

* entrada do mod e lifecycle;
* registro de eventos, comandos, tags e data loaders;
* conversão de callbacks para `DomainAction`;
* execução de efeitos no servidor;
* integração com networking/permissions/proteções disponíveis no loader;
* tradução de recursos e empacotamento.

As diferenças entre os dois módulos são esperadas nos adapters, não na regra de progressão.

## Contratos centrais

```text
SkillDefinition
  id, translation keys, enabled, maxLevel, curve, category,
  xp rules, permissions, world rules, abilities, rewards

XpRequest
  playerId, skillId, baseAmount, source, reason, context, requestId

XpDecision
  accepted/rejected, reason, finalAmount, modifiers, audit metadata

SkillImplementation
  optional handlers for mechanics that JSON cannot express safely
```

Uma skill data-driven pode somente descrever regras. Mecânicas que mudam mundo, drops, dano ou entidades exigem uma implementação registrada por ID. Assim, JSON não vira uma linguagem de código nem um bypass de proteção.

## Registry e data-driven

IDs sempre serão namespaced, por exemplo `bigbangskills:mining`. Tags de blocos e entidades também serão namespaced. O core consulta definições carregadas e não usa `switch` por skill.

Data packs são adequados para listas de blocos, entidades, valores de XP e regras declarativas. Configuração operacional fica em `config/bigbangskills/`. Uma definição inválida falha no startup com o arquivo e campo apontados; reload aceita apenas classes classificadas como seguras.

## Threads

| Componente | Regra |
| --- | --- |
| listeners, registries e mundo | `MAIN THREAD ONLY` |
| cálculo puro de progressão | thread-safe, sem objetos de jogo |
| cache de jogador | acesso serializado por jogador ou executor single-writer |
| JDBC, migração, leaderboard | `ASYNC ONLY` |
| retorno ao mundo/notificação | agenda callback na thread principal |

Objetos de entidades, níveis, inventários ou mundos nunca serão guardados para uso assíncrono. O adapter captura dados simples e o worker trabalha com cópias.

## API e integrações

Integrações futuras com BigBangEssentials, BigBangRegions, quests, Discord, Cobblemon e Professor Carvalho dependem de `api` e eventos públicos. Não haverá dependência obrigatória entre esses mods.

Proteção de regiões é uma pré-condição do adapter: uma ability que pretende alterar o mundo solicita uma operação normal de plataforma e recebe uma decisão de proteção/evento. `setBlock(AIR)` direto não é uma implementação válida.

## Comandos iniciais

O adapter registra e localiza mensagens, mas a aplicação valida tudo no common:

```text
/skills
/skills <skill>
/skills stats
/skills top

/skillsadmin xp add|remove|set <player> <skill> <amount>
/skillsadmin level set <player> <skill> <level>
/skillsadmin reset <player> [skill]
/skillsadmin reload
```

Todos têm permission check, sugestões, IDs válidos, mensagens traduzidas e resultado assíncrono quando dependem de cache/banco. Mutação admin exige actor, motivo e registro de auditoria; nunca aceita valor vindo do cliente como estado authoritative.

## Server-side first

A regra de produto é explícita: `BigBangSkills Core` é totalmente server-side. Um cliente Minecraft vanilla, sem o mod instalado, deve conectar e usar toda a gameplay/progressão. O servidor é authoritative para XP, níveis, cooldowns, abilities, provenance e persistência. Qualquer módulo de cliente futuro será opcional, presentation-only e nunca pré-requisito para gameplay.

A primeira versão usa chat, action bar, title, boss bar, scoreboard, som e partículas. O cliente não envia XP, nível, cooldown, ability ou recompensa. Networking futuro será somente para UI opcional e terá validação server-side mesmo assim.

## Observabilidade e falhas

O serviço expõe contadores e gauges para jogadores em cache, dirty players, filas/pending saves, latência e falhas do banco, XP por segundo, rejeições anti-exploit e idade do cache do leaderboard. Não haverá log de cada ganho em produção.

Progressão persistente usa fail-closed: se o commit assíncrono não ocorrer, o ganho não é confirmado nem notificado como ganho durável. O sistema mantém fila/retry com backoff, sinaliza estado `DEGRADED` e alerta admins; não descarta silenciosamente XP.

## Estrutura futura mínima

```text
bigbangskills/
├── api/
├── common/
├── persistence-sql/
├── fabric/
├── neoforge/
├── docs/
└── .github/workflows/
```

Não criar `integration-cobblemon`, `leaderboard` ou `client` antes de existir uma necessidade concreta. Eles podem nascer como módulos posteriores sem mover o domínio.

## Fontes da decisão

* [Fabric — developer guides](https://docs.fabricmc.net/develop/index) e [events](https://docs.fabricmc.net/develop/events): hooks substituem mixins quando existem; custom hooks ficam restritos ao adapter.
* [NeoForge 1.21.1 — getting started](https://docs.neoforged.net/docs/1.21.1/gettingstarted/): Java 21 e dedicated-server como gate de teste.
* [NeoForge — structuring](https://docs.neoforged.net/docs/1.21.1/gettingstarted/structuring/): packages únicos e estrutura clara.
* [mcMMO — repositório](https://github.com/mcMMO-Dev/mcMMO): referência funcional, não fonte de código.
* Decisão de loaders detalhada em [ADR-001](architecture/ADR-001-multiloader.md).
