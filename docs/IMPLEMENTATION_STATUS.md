# BigBangSkills — status da implementação

Atualizado em 2026-08-11.

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
* O registry agora contém as 19 skills primárias do baseline, com comandos `/skills <skill>` dinâmicos e localização `en_us`/`pt_br`.
* Mining e Woodcutting usam tabelas próprias por registry ID derivadas do `experience.yml` fixado; a resolução não fica mais hardcoded nos adapters. Provenance, actor validation e blocos colocados continuam no pipeline comum.
* O catálogo comum carrega 81 subskills, todos os thresholds de rank do baseline e cooldowns de abilities ativas; `/skills <skill>` expõe unlocks, ranks, tipo passive/active, ativação, cooldowns, restrições e a curva de XP configurada.
* O dispatcher comum aceita XP de qualquer skill/ação pela fila transacional; Mining/Woodcutting/Excavation/Herbalism aplicam efeitos de drops ou cadeia nos dois loaders, com maturidade de culturas, tabelas externas de Archaeology/Hylian Luck, e conversões Green Terra/Shroom Thumb; Fishing tem resolver de tesouros por tier, políticas mcMMO de drops/substituição/peixe extra/Luck e integração de drops nos dois loaders; combate usa o dispatcher comum e abilities ativas podem ser acionadas por `/skills ability <skill> <ability>` ou pelo clique direito nativo de ferramenta nos dois loaders.
* Fabric e NeoForge usam mixins server-side nos limites vanilla de pesca, consumo de alimento, taming, anvil, output de furnace, timer de brewing, entrada de hopper e ricochete de flecha; eventos nativos são usados onde disponíveis. Fishing tem guard de catches rápidos/estacionários, Luck of the Sea, Master Angler, Shake configurável, Fisherman's Diet e conversão de gelo; pet combat inclui Gore/Claws/Fast Food/Pummel, Call of the Wild configurável para entidades tameable/horse com expiração e defesa extensível. Alchemy aceita ingredientes/efeitos namespaced e controla transferência/processamento por hopper; Mining/Woodcutting aceitam blocos modded por tabela XP. Combat também aplica Arrow Retrieval, Trick Shot limitado, Dodge, Arrow Deflect, Iron Grip, Block Cracker, Counter Attack, Rupture periódico e dano em área limitado por tier com filtro de pets próprios, espectadores e PvP; Tree Feller respeita Leaf Blower, componentes crimson/warped stripped, limite configurável, XP reduzido e provenance. Brewing/furnace associam owners com persistência de restart, e Diminished Returns é opcional e fail-closed.
* A auditoria completa, skill a skill e subskill a subskill, está em [FULL_SKILL_PARITY_AUDIT.md](audits/FULL_SKILL_PARITY_AUDIT.md); presença no registry/catalog não é contada como paridade funcional.
* Testes de fila pré-load, dirty durante save, admin cache, underflow administrativo, ledger idempotente, writers concorrentes, leaderboard SQL e contrato MySQL/MariaDB opt-in.

## Histórico preservado da base

* A base anterior já entregava o Gradle multi-project, domínio sem imports de loader, repository JDBC assíncrono com ledger idempotente, drivers MySQL/MariaDB declarados, `/skills` inicial no Fabric e hooks de quebra nos dois loaders.
* O boot dedicado anterior chegou a `Done` no NeoForge e era bloqueado pelo gate de EULA local no Fabric; o gate agora foi resolvido somente nos diretórios ignorados de desenvolvimento.
* Antes desta etapa ainda faltavam cache de login/logout/shutdown, configuração runtime de banco, admin status, flush lifecycle, place tracking NeoForge e player smoke; os fluxos comuns foram implementados e os gates manuais ainda não executados permanecem abaixo.

## Validado

* `./gradlew clean build` passou em 2026-08-11, incluindo Fabric remapJar e NeoForge jar; os JARs finais contêm `common`, API, fórmulas e recursos compartilhados.
* `fabric:runServer` passou pelo boot dedicado nesta rodada, abriu SQLite/Hikari em 25565 e não registrou erro de mixin; a execução foi encerrada após a prova de boot.
* NeoForge compilou e alcançou `Done` nesta rodada, com SQLite/Hikari inicializados; Fabric também alcançou `Done` sem erro de mixin.
* Um cliente Loom Fabric 1.21.1 conectou via `--quickPlayMultiplayer`; o servidor registrou `Player978 joined the game` e o cliente recebeu a resposta das 19 skills. A tela gráfica bloqueada impediu validar quebra de bloco por entrada manual.
* Gameplay de jogador, `/skillsadmin` e smoke dos hooks nativos continuam separados da prova de boot e estão listados como pendentes na auditoria canônica.
* A tabela detalhada por loader está em [LOADER_PARITY.md](LOADER_PARITY.md).

## Ainda pendente

* Smoke com cliente vanilla sem o mod, e repetição de gameplay em NeoForge, incluindo logout/restart e XP restaurado.
* Execução do contrato MySQL/MariaDB contra servidor real nesta máquina; o teste reproduzível está preparado, mas `BIGBANGSKILLS_MYSQL_JDBC_URL` não está configurada.
* Identificação de origem em explosões externas, árvores e transformações de mods; pistões transferem a marca em extensão/retração, fluidos de balde propagam/limpam a marca e explosões limpam markers atingidos nos dois loaders.
* Lease de sessão NETWORK e outbox entre servidores; o cache de leaderboard SQL já possui TTL de 30 segundos.
* Hot reload de regras de XP/notificações/anti-exploit; o comando atual valida arquivos e informa que os valores entram após restart.
* As 19 skills ainda não têm hooks nativos completos em ambos os loaders; o registry não é evidência de paridade funcional. As lacunas exatas estão na auditoria.
* Salvage já tem o fluxo básico por bloco configurado, confirmação nativa em dois cliques e tabela de material de reparo; Smelting/Alchemy têm fórmulas comuns; Second Smelt já muta o output no tick do furnace e brewing calcula estágio no limite `doBrew`, com Concoctions vanilla por rank. Magic Hunter aplica a tabela de encantamentos baseline; smoke de Concoctions/Shake/Fisherman's Diet continua pendente. Diminished Returns segue o baseline: `0` significa cap de nível ilimitado, janela de 10 minutos e mínimo garantido de 5%, desativado por padrão.

## Known Issues

* Provenance persistida cobre place/break de minério/log observado pelos loaders, transferência por pistão, fluxo de líquidos de balde e limpeza após explosões; identificação de origem de explosões e transformações externas continua fora do escopo.
* Permissões nomeadas ainda usam o fallback vanilla de nível de operador; não há integração obrigatória com LuckPerms.
* Admin offline pode concorrer com um login simultâneo; a operação usa transação/ledger, mas precisa de lease/session ownership antes de multi-servidor.
* O cliente vanilla sem BigBangSkills e o gameplay NeoForge ainda não foram executados nesta rodada; build/boot não são equivalentes a gameplay PASS.
* A validação manual após a política de XP para picareta ainda é necessária nos dois loaders.
* `persistence-sql` usa um executor single-writer por instância; a atualização agregada é atômica para múltiplos writers, mas throughput maior exigirá pool de workers/particionamento medido.
* O warning do Loom sobre a versão SQLite `3.46.1.0` não impede o build; o artifact foi incluído nos JARs finais.
* Nesta rodada não foi iniciado Minecraft automaticamente. O `clean build` e a inspeção dos JARs provam empacotamento/compilação, não gameplay.
