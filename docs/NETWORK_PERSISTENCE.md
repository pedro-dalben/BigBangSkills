# Persistência e concorrência em rede

## Objetivos

* SQLite para desenvolvimento/single-server;
* MySQL/MariaDB remoto como primeira classe em produção;
* nenhuma operação SQL na game thread;
* batch/write-behind sem UPDATE por bloco;
* retry idempotente;
* troca de servidor sem “última gravação vence”;
* falha visível, sem perda silenciosa.

## Runtime online

`PlayerProgressService` é a fonte do estado de jogadores online. Seu mapa UUID → entrada mantém explicitamente `LOADING`, `READY`, `DIRTY`, `SAVING`, `FAILED` e `UNLOADING`.

* Join cria a entrada e dispara `loadAll` no executor JDBC; callbacks retornam ao executor principal do loader.
* Enquanto `LOADING`/`FAILED`, no máximo `max_preload_xp_per_player` ações imutáveis ficam enfileiradas. Ao carregar, são reaplicadas pelo mesmo pipeline; excedente é rejeitado com motivo explícito.
* XP confirmado no cache cria um evento idempotente bounded em `max_pending_save_events_per_player`; o listener não executa SQL.
* Flush periódico padrão é de 30 segundos. O lote é um snapshot da fila. Eventos criados durante o save ficam fora desse snapshot e continuam `DIRTY`.
* Logout entra em `UNLOADING` e remove o cache somente após o lote final confirmar. Shutdown para novas mutações, drena com timeout padrão de 15 segundos e registra quantidade pendente em timeout.
* Falhas mantêm os eventos, usam backoff `1s, 2s, 5s, 10s, 30s` e não anunciam sucesso durável.

As configurações ficam em `config/bigbangskills/runtime.properties` e `config/bigbangskills/database.properties`. O segundo usa `java.util.Properties` para não adicionar uma dependência YAML só para configuração operacional.

## Topologia

```text
Fabric/NeoForge server
  -> common XpService
  -> bounded queue / player single-writer
  -> persistence worker
  -> JDBC transaction
       xp_ledger (event_id unique)
       player_progress (atomic aggregate)
  -> callback na main thread
```

O pool é pequeno e limitado. Backpressure impede que uma falha de banco consuma memória sem limite. O servidor expõe `DEGRADED` quando a fila cresce, há retry ou existem dirty players.

## Write-behind

O listener apenas valida, cria um delta imutável e enfileira. O worker agrupa por `(player, skill, scope)` dentro de uma janela curta. O batch é uma transação:

1. inserir cada `event_id` no ledger;
2. ignorar IDs já confirmados;
3. aplicar apenas deltas novos ao agregado;
4. incrementar `revision`;
5. commit;
6. atualizar o cache derivado na main thread.

Não confirmar/notificar um ganho durável antes do commit. Se o requisito futuro exigir feedback imediato, o estado pendente será explicitamente separado de XP confirmado e não poderá ser perdido silenciosamente.

## Falhas

* retry com backoff e limite;
* conexão reaberta fora da thread do jogo;
* evento idempotente evita duplicação após timeout de resposta;
* fila permanece dirty até commit;
* logout aguarda/draina o jogador com timeout configurado;
* shutdown para entrada nova, drena filas e grava health report;
* após falha definitiva, grava outbox local ou mantém estado não confirmado conforme a política de durabilidade; nunca descarta e segue como se tivesse salvo;
* comandos admin falhos não respondem sucesso.

Por padrão, XP é fail-closed quando a persistência não pode confirmar. Recompensas dependentes de level também aguardam a transição confirmada.

## Escopos de progressão

`SERVER` é o padrão inicial: sem concorrência entre servidores, simples de operar e adequado ao vertical slice.

`NETWORK` exige:

```text
network_scope_id estável
player_session lease
event_id idempotente
transação atômica
refresh/invalidation do cache
```

`WORLD` só deve existir quando um produto realmente precisar separar mundos; aumenta as linhas, comandos, leaderboards e migrações.

## Lease de sessão

No login de uma progressão `NETWORK`, o servidor tenta adquirir:

```text
player_uuid -> server_id, lease_token, lease_until
```

Aquisição é transacional: permite se expirado ou pertence ao mesmo token; caso contrário, aguarda/recusa carregamento em vez de abrir dois writers. Logout libera o token. Crash deixa lease até expirar; o TTL deve ser curto o bastante para recuperação e longo o bastante para evitar duplicidade em troca normal.

O lease não substitui o ledger: conexões podem expirar, mensagens podem repetir e admin/API pode escrever fora da sessão.

As três proteções têm papéis diferentes: o ledger garante idempotência, `total_xp = total_xp + delta` faz incremento atômico para ganhos concorrentes e `revision` funciona como optimistic check para operações administrativas/set. Nenhuma delas usa “última gravação do cache vence”.

## Perda de atualização

Este fluxo é proibido:

```text
Server A lê 5000
Server B lê 4200
A salva 5000
B salva 4200
```

O fluxo permitido grava delta/evento:

```text
A registra +800 event-a
B registra +300 event-b
agregado = 4200 + 800 + 300 = 5300
```

Para remoção/set administrativo, a transação bloqueia a linha, valida a revisão e registra operação auditável. Um `set` concorrente sem revisão não é aceito.

## Cache e consistência

* comandos de jogador leem cache confirmado;
* uma escrita de outro servidor pode tornar um cache remoto stale;
* o lease reduz o caso normal;
* no futuro, pub/sub/invalidation pode reduzir a janela restante;
* leaderboard é snapshot cacheado e informa idade, nunca consulta cada comando sem limite.

## Banco

O repositório usa SQL parametrizado, transações curtas, índices em `(skill_id, scope_type, scope_id, total_xp)` e pool limitado. MySQL/MariaDB e SQLite têm testes de contrato para:

* migration idempotente;
* UPSERT/insert-ignore equivalente;
* decimal e timestamp;
* concorrência e retry;
* rollback parcial;
* charset/UUID;
* leaderboard `LIMIT`.

Produção não depende de `CREATE TABLE IF NOT EXISTS` espalhado pelo startup.

## Leaderboards futuros

`LeaderboardService` consulta somente snapshots/queries limitadas. Refresh periódico assíncrono, cache com idade e índice dedicado evitam peso em `/skills top`. Tie-breaker determinístico: `total_xp`, `updated_at` e UUID, nesta ordem definida pela política.

## Métricas

```text
cached_players
dirty_players
pending_save_events
oldest_pending_age
db_latency_ms
db_failures/retries
ledger_duplicates
lease_conflicts
leaderboard_cache_age
```

Não registrar UUID/XP completo em log de alto volume sem necessidade operacional.
