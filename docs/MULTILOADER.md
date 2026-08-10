# Estratégia Fabric + NeoForge

## Decisão

Adotar um Gradle multi-project próprio com `api`, `common`, `persistence-sql`, `fabric` e `neoforge`. `api` e `common` serão Java puro; os dois loaders dependem deles e mantêm seus próprios mappings, APIs e metadados.

Não usar Architectury na primeira implementação.

## Comparação

| Opção | Vantagens | Custos/riscos | Decisão |
| --- | --- | --- | --- |
| Multi-project próprio | dependências explícitas; domínio realmente independente; stack trace e mappings locais ao adapter; menos API intermediária | mais código de integração; eventos/networking/registries precisam de duas implementações | escolhida |
| Architectury | reduz boilerplate; abstrai eventos, networking, registries e loader calls; estrutura `common` + loaders já conhecida | adiciona API/runtime/build toolchain; abstração define o menor denominador comum; debugging atravessa camada; versão 1.21.1 precisa ser fixada e testada | descartada por enquanto |
| common puro + adapters | máxima separação; facilita testes JVM e possível reutilização fora do Minecraft | exige desenhar portas próprias; algumas APIs do jogo não têm equivalente comum | escolhida como princípio dentro do multi-project |

## Regras de dependência

```text
api             -> Java 21
common          -> api
persistence-sql -> api, common (ports only), JDBC
fabric          -> api, common, persistence-sql, Fabric Loader/API, Minecraft 1.21.1
neoforge        -> api, common, persistence-sql, NeoForge 1.21.1, Minecraft 1.21.1
```

`common` não pode importar uma classe de Minecraft para “facilitar” um adapter. Se um conceito é necessário, criar um value object próprio (`SkillId`, `WorldId`, `BlockId`, `PlayerId`) ou uma porta.

## O que é compartilhado

* regras e cálculos de XP;
* transições de nível e unlocks;
* composição de modifiers;
* políticas anti-exploit;
* contratos de evento e persistência;
* parser/validador de definições;
* testes JVM do domínio.

## O que fica específico

* lifecycle e entrada do mod;
* callbacks de bloco, entidade, pesca, colheita e comandos;
* registries e tags do Minecraft;
* formato de metadata do loader;
* envio de mensagens, sons, particles, boss bars e networking;
* integração com APIs de proteção/permissão;
* GameTests e smoke tests dedicados.

## Eventos, registries e data packs

O adapter deve preferir hook oficial do loader. Mixins só entram quando não houver hook suficiente, com teste de compatibilidade e justificativa no código. Identificadores declarados pelo servidor são convertidos para o tipo próprio do domínio; não ficam presos a Yarn ou Mojmap.

Tags de blocos e definições declarativas são recursos do jogo e serão carregadas no adapter, mas o resultado normalizado é entregue ao `common`.

## Networking

O core não depende de pacote cliente. Qualquer payload futuro é clientbound, pequeno e derivado do estado authoritative. Payload serverbound nunca contém um valor aceito diretamente para XP/level/reward.

## Debugging e mappings

Cada loader terá tarefas de compilação e run separadas. Falha no adapter deve permanecer local ao módulo. Não compartilhar bytecode remapeado entre loaders; cada artefato é produzido e inspecionado pelo seu toolchain.

## CI mínimo

```text
./gradlew :common:test
./gradlew :fabric:compileJava :fabric:build
./gradlew :neoforge:compileJava :neoforge:build
./gradlew check
```

O pipeline publica/arquiva JAR Fabric e JAR NeoForge como artefatos separados, com Minecraft/loader/mappings no nome ou metadata. A Fase 0 não cria release nem deploy automático.

Gates posteriores incluem dedicated server Fabric e NeoForge, GameTests quando úteis e smoke de Mining/Woodcutting. O primeiro build verde não será tratado como prova de fluxo de jogador.

## Reavaliação de Architectury

Reconsiderar Architectury somente se os adapters acumularem duplicação mensurável em pelo menos dois subsistemas estáveis. Antes disso, adicionar uma dependência para remover boilerplate especulativo aumenta o acoplamento sem resolver um problema observado.

## Fontes consultadas em 2026-08-10

* [Fabric developer guides](https://docs.fabricmc.net/develop/index), [Loom](https://docs.fabricmc.net/develop/loom/), [events](https://docs.fabricmc.net/develop/events) e [networking](https://docs.fabricmc.net/develop/networking).
* [Fabric example mod 1.21](https://github.com/FabricMC/fabric-example-mod/tree/1.21).
* [NeoForge 1.21.1 getting started](https://docs.neoforged.net/docs/1.21.1/gettingstarted/) e [structuring](https://docs.neoforged.net/docs/1.21.1/gettingstarted/structuring/).
* [NeoForge ModDevGradle](https://github.com/neoforged/ModDevGradle), incluindo o modo Vanilla para subprojetos com Minecraft sem API do loader.
* [Architectury API](https://github.com/architectury/architectury-api), [setup](https://docs.architectury.dev/api/26.1.x/getting-started/setup/) e [project structure](https://docs.architectury.dev/api/getting-started/project-structure/). A documentação atual consultada é de versões posteriores; por isso não serve como prova suficiente para fixar 1.21.1.
