# Análise funcional do mcMMO

## Escopo e método

O repositório foi clonado em 2026-08-10 para consulta local, commit analisado:

```text
ad8444c2f394ee97c510acdfc7b23623885b071c
```

O mcMMO é um plugin Bukkit/Spigot sob GPLv3. Ele é referência de problemas e experiência de jogador, não fonte de código. Nenhuma classe, método, algoritmo, configuração ou implementação será copiado.

Fontes principais:

* [repositório mcMMO](https://github.com/mcMMO-Dev/mcMMO);
* [PlayerProfile](https://github.com/mcMMO-Dev/mcMMO/blob/master/src/main/java/com/gmail/nossr50/datatypes/player/PlayerProfile.java);
* [SkillManager](https://github.com/mcMMO-Dev/mcMMO/blob/master/src/main/java/com/gmail/nossr50/skills/SkillManager.java);
* [PrimarySkillType](https://github.com/mcMMO-Dev/mcMMO/blob/master/src/main/java/com/gmail/nossr50/datatypes/skills/PrimarySkillType.java);
* [FormulaManager](https://github.com/mcMMO-Dev/mcMMO/blob/master/src/main/java/com/gmail/nossr50/util/experience/FormulaManager.java);
* [eventos de XP](https://github.com/mcMMO-Dev/mcMMO/tree/master/src/main/java/com/gmail/nossr50/events/experience);
* [DatabaseManager](https://github.com/mcMMO-Dev/mcMMO/blob/master/src/main/java/com/gmail/nossr50/database/DatabaseManager.java);
* [block metadata](https://github.com/mcMMO-Dev/mcMMO/tree/master/src/main/java/com/gmail/nossr50/util/blockmeta);
* [Changelog](https://github.com/mcMMO-Dev/mcMMO/blob/master/Changelog.txt).

## Mapa de funcionalidades

| Funcionalidade | Como funciona no mcMMO | Por que existe | Queremos? | Não queremos? | Queremos modificada? | Como implementaríamos | Prioridade |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Skills | `PrimarySkillType` enum lista skills e há pacotes/Managers por skill; configuração desloca parte dos dados | dar identidade e progressão a ações Minecraft | sim | enum fechado como único registry | sim, IDs namespaced e registry dinâmico | `SkillDefinition` + `SkillRegistry` + implementação opcional | P0 |
| XP | Managers iniciam ganho com source/reason; eventos pre/post permitem alteração/cancelamento | converter ação em progressão mensurável | sim | cada skill mutar XP diretamente | pipeline único com request e decisão | `XpRequest` -> eligibility -> modifiers -> `XpService` | P0 |
| Curva/level | `FormulaManager` suporta linear/exponential, cacheia XP por nível e converte total XP; profile mantém nível e XP corrente | balancear ritmo e permitir mudança de fórmula | sim | balanceamento final agora | persistir total XP e derivar nível | `XpCurve` com funções forward/inverse e testes de limites | P0 |
| PlayerProfile | agrega UUID/nome, mapas de level/XP, cooldowns, dirty flag, load/save e diminished returns | manter estado disponível sem buscar tudo a cada evento | sim | um objeto acoplado ao Bukkit e à persistência | separar `PlayerProgress`, cache e repository | cache por UUID; DTOs puros; repository assíncrono | P0 |
| SkillManager | classe base fornece player, skill level, aplicação de XP e razão PVP/PVE; cada skill estende | compartilhar operações sem eliminar a especialização | parcialmente | hierarquia que dependa de Bukkit | service/handler pequeno por implementação | `SkillActionRouter` e `SkillImplementation` | P0 |
| Habilidades passivas | subskills e managers alteram drops, dano, eficiência, chance e efeitos durante ações | transformar level em sensação de poder | sim | nomes/comportamento copiados | efeitos precisam de protection hook e root action | `AbilityDefinition` + effects server-side | P2 |
| Habilidades ativas | abilities têm ativação, duração, desativação, permissões e cooldown | criar decisão/ritmo além de bônus permanente | sim | task espalhada por listener | timestamps e serviço central | `AbilityService` + `CooldownService` | P2 |
| Cooldowns | profile guarda timestamps de abilities e runnable/task faz manutenção/notificação | impedir spam de habilidades fortes | sim | centenas de tasks por jogador | usar `availableAt`; persistir só se necessário | mapa indexado por player/ability e tick de expiração | P2 |
| XP sources | enum inclui SELF, PASSIVE, PARTY_MEMBERS, COMMAND e CUSTOM | separar origem operacional | sim | enum que bloqueie integrações novas | IDs extensíveis e source + reason separados | `XpSource` namespaced, `XpReason` textual validado | P0 |
| XP reasons | enum inclui PVP, PVE, shared e command | explicar contexto e permitir regras diferentes | sim | usar reason como permission | reason é metadado/policy input | `source`, `reason` e contexto imutável | P0 |
| Modifiers/perks | configuração, permissões e eventos alteram XP; changelog mostra rates globais/per-skill e boosts | eventos, VIPs e administração precisam de multiplicação controlada | sim | monetização dentro do core | composição determinística e auditável | lista de `XpModifier` com ordem/prioridade | P1 |
| Skills/abilities config | vários YAMLs (`config.yml`, `experience.yml`, `advanced.yml`, skills, locale, sounds) permitem tuning | servidor precisa ajustar gameplay sem recompilar | sim | espalhar config operacional e gameplay sem schema | JSON/data pack para gameplay; config para operação | snapshot validado, reload por classe | P0 |
| Permissions | métodos e nós controlam skills, abilities, comandos, boosts e visibilidade | servidor precisa restringir e customizar acesso | sim | depender de LuckPerms no domínio | porta agnóstica | `PermissionService` adapter | P1 |
| Party XP | `Party`, eventos e comandos compartilham XP por membros com razões próprias | cooperação e reduzir competição | depois | acoplar core ao party/chat/teleport | módulo futuro com policy anti-multi-account | `XpDistributor` opcional e source `PARTY` | P3 |
| Leaderboards | `DatabaseManager` lê leaderboard por skill/power; há rank, snapshot bulk e cache de placeholders | competição e consulta global | depois | query por comando sem limite | snapshot/cache/index/limite | `LeaderboardService` async e cacheado | P3 |
| Storage | `DatabaseManager` abstrai flat file e SQL; profile é salvo e há conversão/purge | sobreviver restart e suportar instalações diferentes | sim | SQL dentro de skill | JDBC separado, migrations e ledger | `PlayerProgressRepository` + SQLite/MySQL/MariaDB | P0 |
| Cache | user/profile managers carregam perfil e mantêm estado em memória; leaderboard possui cache | reduzir IO e latency durante gameplay | sim | cache sem dirty/error state | cache confirmado + dirty/pending/health | `PlayerProgressCache` por sessão | P0 |
| Concorrência | referência tradicional é profile/cache + saves; não é modelo de rede multi-server authoritative | identificar que “save completo” pode perder atualização | sim | last-write-wins | delta ledger, idempotency e lease | atomic transaction por evento | P1 |
| API | pacotes `api`, `ExperienceAPI`, `DatabaseAPI`, events e helpers expõem operações | outros plugins precisam integrar XP/skills | sim | expor `PlayerProfile`/DB interno | API pequena, estável e sem infra | `BigBangSkillsApi` com DTOs | P1 |
| Eventos | muitos eventos Bukkit para XP, level, ability, skill, party, notification e scoreboard | extensibilidade e veto/observação | sim | domínio conhecendo evento Bukkit | eventos internos puros, bridge no adapter | records imutáveis e listeners públicos depois | P0 |
| Comandos | comandos de skills, stats, ability, XP, level, top, rank e administração | UX, suporte e operações | sim | aliases e dezenas de comandos na primeira fase | `/skills` e `/skillsadmin` pequenos, permissionados | Brigadier/loader adapter -> application service | P1 |
| Anti-exploit: blocks | tracker de blocos por chunk marca localizações inelegíveis; armazenamento compacto e persistente | evitar place/break farming | sim, crítico | confiar só na heap ou em nome do bloco | provenance por tag/chunk e política UNKNOWN | `BlockProvenanceService` + bitset/metadata | P0 |
| Anti-exploit: diminishing | estado recente por skill aplica retorno reduzido em janela/threshold | evitar loops, AFK e farms sem bloquear tudo | sim | punição permanente por heurística | janelas configuráveis, métrica e motivo | `AntiExploitService` central | P0 |
| Anti-exploit: ações especiais | managers checam fishing, combat, plant growth, fake events e limites | cada fonte tem exploit diferente | sim | regras isoladas e inconsistentes | política por source com root action | eligibility adapters + caps | P1 |
| Notificações | locale, action feedback, level-up, cooldown, sounds e toggles informam o jogador | progressão precisa ser percebida | sim | mensagens hardcoded e spam | canais configuráveis + rate limit | `NotificationService` com CHAT/ACTION_BAR/TITLE/BOSS_BAR/SOUND/NONE | P1 |
| Localization | `LocaleLoader` e arquivos de locale substituem textos; skill names têm variantes de exibição | rede multilíngue e customização | sim | strings em classes | translation keys e parâmetros | pt_br/en_us no adapter, keys no common | P1 |
| Scoreboards/visual | manager cria scoreboards, barras/XP visual e placeholders; há opt-out | feedback contínuo e status | depois | exigir client mod | vanilla scoreboard/action bar/boss bar primeiro | `NotificationService`; scoreboard em módulo posterior | P2 |
| Database migrations | referência possui conversões e formatos históricos, além de config/database types | evolução sem apagar dados | sim | `CREATE TABLE` improvisado | migration versionada e checksum | `schema_version`/runner SQL | P0 |
| Error handling | profile save tenta novamente e loga falha; bugs/exploit fixes aparecem continuamente no changelog | evitar perda silenciosa e degradar com clareza | sim | ignorar falha ou anunciar sucesso falso | health state, retry, fail-closed | fila, backoff, métricas, shutdown flush | P0 |
| Integração proteção | há integração WorldGuard/listeners e eventos fake para ações simuladas | habilidades não podem quebrar proteção | sim, obrigatório | `setBlock` direto | adapter chama operação normal/protection hook | `ProtectionService`/normal event result | P0 |
| Client visual | mcMMO usa APIs Bukkit/scoreboard; BigBang não depende de cliente | compatibilidade de servidor/modpack | sim server-side | companion obrigatório | companion futuro somente leitura | vanilla channels agora, packets depois | P0 |

## Lições arquiteturais

1. A referência cresceu em torno de um profile central, managers por skill, muitos eventos e compatibilidade histórica. BigBangSkills mantém os limites funcionais, mas separa domínio, adapters, cache e persistence desde o primeiro PR.
2. `PlayerProfile` demonstra que estado de XP, level, cooldown, dirty tracking e diminished returns tendem a se misturar. O novo modelo torna `total_xp`, cooldown e anti-exploit subsistemas explícitos.
3. O tracker de blocos mostra que provenance precisa sobreviver a unload/restart e que posição local pode ser compactada. A primeira política será conservadora quando a origem for desconhecida.
4. A API de eventos permite integrações, mas eventos canceláveis e mutáveis aumentam a superfície. No core novo, transições são determinísticas; bridges de loader e API pública têm contratos menores.
5. Scoreboard, locale, placeholders, commands e database são produto, não detalhes de uma skill. Devem consumir eventos/queries, nunca serem chamados diretamente por Mining/Woodcutting.

## O que explicitamente não será reproduzido agora

Party chat/teleport, hardcore stat loss, dezenas de skills, child skills, scoreboard completo, placeholders externos, integração WorldGuard/Cobblemon e todas as abilities ficam no roadmap. A referência serve para evitar esquecer problemas, não para aumentar o primeiro PR.
