# BigBangSkills — modelo de dados

## Princípios

* IDs de skill são `namespace:path`, nunca enum fechado.
* O cliente não é fonte de verdade.
* `total_xp` é a fonte de verdade; nível é derivado pela curva e pelo cap da definição.
* Definições de gameplay versionadas não alteram retroativamente uma linha sem política explícita.
* Escopo faz parte da chave da progressão.
* Timestamps são UTC; números de XP usam decimal fixo, não `float`.

## Objetos de domínio

```text
SkillId
  namespace + path

SkillDefinition
  id, nameKey, descriptionKey, enabled, category, maxLevel,
  curveId, curveParameters, xpMultiplier, actions, abilities,
  permissions, worldRules, definitionVersion

PlayerId
  UUID

ProgressionKey
  playerId, skillId, scopeType, scopeId

SkillState
  totalXp, derivedLevel, currentLevelXp, xpToNextLevel,
  definitionVersion, revision

PlayerProgress
  playerId, map<SkillId, SkillState>, derivedPowerLevel

XpRequest
  requestId, playerId, skillId, amount, source, reason,
  scope, context, createdAt
```

`level` pode ser calculado sempre com `XpCurve.levelAt(totalXp)`. Exibir `level` no cache é permitido como valor derivado, mas não é persistido como coluna autoritativa. Isso evita estados impossíveis como level 50 com XP de level 49.

## Escopos

```text
NETWORK  -> scope_id lógico da rede
SERVER   -> scope_id estável do servidor
WORLD    -> scope_id UUID/lógico do mundo, somente quando necessário
```

O modo atual é uma configuração, não uma diferença de schema. O mesmo jogador pode ter uma linha por skill e escopo.

## Schema inicial lógico

```sql
CREATE TABLE player_progress (
    player_uuid        CHAR(36)      NOT NULL,
    skill_id           VARCHAR(200)  NOT NULL,
    scope_type         VARCHAR(16)   NOT NULL,
    scope_id           VARCHAR(128)  NOT NULL,
    total_xp           DECIMAL(20,4) NOT NULL,
    revision           BIGINT        NOT NULL,
    definition_version INT           NOT NULL,
    updated_at         TIMESTAMP     NOT NULL,
    PRIMARY KEY (player_uuid, skill_id, scope_type, scope_id),
    CHECK (total_xp >= 0),
    CHECK (revision >= 0)
);

CREATE TABLE xp_ledger (
    event_id           CHAR(36)      NOT NULL,
    player_uuid        CHAR(36)      NOT NULL,
    skill_id           VARCHAR(200)  NOT NULL,
    scope_type         VARCHAR(16)   NOT NULL,
    scope_id           VARCHAR(128)  NOT NULL,
    delta_xp           DECIMAL(20,4) NOT NULL,
    source             VARCHAR(32)   NOT NULL,
    reason             VARCHAR(64)   NOT NULL,
    server_id          VARCHAR(128)  NOT NULL,
    created_at         TIMESTAMP     NOT NULL,
    PRIMARY KEY (event_id)
);

CREATE INDEX idx_progress_skill_scope
    ON player_progress (skill_id, scope_type, scope_id, total_xp DESC);

CREATE TABLE player_session (
    player_uuid        CHAR(36)     NOT NULL PRIMARY KEY,
    server_id          VARCHAR(128) NOT NULL,
    lease_token        CHAR(36)     NOT NULL,
    lease_until        TIMESTAMP    NOT NULL,
    acquired_at        TIMESTAMP    NOT NULL
);

CREATE TABLE schema_version (
    version            INT          NOT NULL PRIMARY KEY,
    checksum           VARCHAR(128) NOT NULL,
    applied_at         TIMESTAMP    NOT NULL
);

CREATE TABLE admin_audit (
    audit_id           CHAR(36)      NOT NULL PRIMARY KEY,
    actor_uuid         CHAR(36)      NULL,
    action              VARCHAR(64)  NOT NULL,
    target_uuid         CHAR(36)     NULL,
    skill_id            VARCHAR(200) NULL,
    scope_type          VARCHAR(16)  NULL,
    scope_id            VARCHAR(128) NULL,
    before_xp           DECIMAL(20,4) NULL,
    after_xp            DECIMAL(20,4)  NULL,
    reason              VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL
);
```

O schema real terá migrations versionadas e variantes SQL testadas para SQLite e MySQL/MariaDB. Nunca editar uma migration já aplicada.

## Ledger e idempotência

Cada lote de XP recebe `event_id` único. A transação insere no ledger e atualiza `player_progress`; uma repetição do mesmo ID não aplica o delta novamente. O ledger também fornece auditoria mínima para comandos administrativos e investigação de perda/duplicação.

Para uma operação de `set`, o serviço lê a linha dentro da mesma transação, calcula o delta e grava um evento administrativo. Não há UPDATE cego fora de transação.

## Cache

```text
LOGIN  -> repository.load(scope) async
       -> cache somente após sucesso
EVENT  -> valida e enfileira delta imutável
COMMIT -> atualiza cache/derived state na main thread
TICK   -> agrupa dirty deltas, sem SQL no listener
LOGOUT -> drena fila do jogador e libera lease
STOP   -> fecha novas entradas, drena com timeout e reporta falhas
```

`/skills` lê cache. Cache miss não faz query síncrona: informa “dados ainda carregando” ou usa uma leitura assíncrona com resposta posterior.

## Configuração e definição

Separar:

```text
config/bigbangskills/general.*       -> lifecycle e limites
config/bigbangskills/database.*      -> conexão, batch, retry, scope
config/bigbangskills/notifications.* -> canais e formato
config/bigbangskills/anti_exploit.*  -> políticas e thresholds
data/bigbangskills/skills/*.json     -> gameplay declarativo
```

Reload valida tudo em uma cópia nova e troca o snapshot somente se a validação completa passar. Schema, JDBC, executor e tipos de registry exigem restart.

## Validação

Falham no startup/reload:

* skill sem namespace, ID duplicado ou translation key ausente;
* `maxLevel < 1`, XP negativo, multiplier não finito ou curve desconhecida;
* unlock fora do cap, cooldown/duration negativos;
* escopo, world rule ou tag com formato inválido;
* reward/ability sem implementação registrada quando a definição exige uma.

## Compatibilidade

`definition_version` identifica a regra que originou o estado. Alterar somente texto, tags adicionais ou multiplier pode ser reloadable conforme política. Alterar curva, cap ou interpretação histórica exige migration/balance decision documentada; não recalcular silenciosamente o progresso do jogador.

## API de leitura

```text
getPlayer(UUID)
getSkill(SkillId)
getState(UUID, SkillId, Scope)
getLevel(...)       // derivado
getXp(...)          // total e progresso para próxima faixa
getPowerLevel(...)
```

Repositorios e `xp_ledger` não fazem parte da API pública.
