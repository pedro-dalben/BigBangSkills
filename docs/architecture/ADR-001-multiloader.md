# ADR-001 — multi-loader sem dependência obrigatória de Architectury

* Status: aceito para a Fase 0
* Data: 2026-08-10
* Escopo: Minecraft 1.21.1 / Java 21 / Fabric + NeoForge

## Contexto

O domínio precisa rodar em dois loaders, mas não deve carregar tipos de Fabric ou NeoForge. Architectury oferece abstrações úteis, porém cria dependência em uma API/runtime e em uma toolchain adicional. A documentação atual verificada está focada em versões Minecraft mais novas; não há motivo para assumir compatibilidade 1.21.1 sem um build reproduzível.

## Decisão

Usar Gradle multi-project próprio. O domínio será Java puro e os módulos `fabric` e `neoforge` serão adapters diretos. `persistence-sql` será infraestrutura compartilhada sem acesso a Minecraft.

## Consequências

Positivas:

* testes do domínio não precisam iniciar Minecraft;
* dependências e mappings ficam visíveis no módulo que os usa;
* APIs do loader não vazam para a progressão;
* uma migração futura de loader não exige reescrever o domínio.

Negativas:

* listeners, comandos e efeitos têm alguma duplicação;
* networking/registries precisam de dois adapters;
* mudanças no Minecraft podem exigir dois ajustes.

Mitigações:

* portas pequenas e value objects próprios;
* smoke test por loader;
* abstrair somente duplicação observada;
* CI compila os dois artefatos separadamente.

## Alternativas rejeitadas

Architectury continua tecnicamente viável e será reavaliado após um vertical slice. Um “common” que importe APIs de um loader foi rejeitado porque apenas esconde o acoplamento.

## Fontes

* [Fabric events](https://docs.fabricmc.net/develop/events)
* [NeoForge 1.21.1](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
* [Architectury API](https://github.com/architectury/architectury-api)
