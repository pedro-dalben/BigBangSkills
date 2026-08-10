# ADR-003 — skill como definição + implementação opcional

* Status: aceito para a Fase 0
* Data: 2026-08-10

## Contexto

Um enum ou `switch` central torna cada nova skill uma alteração no core. JSON sozinho não descreve com segurança uma ability que interage com mundo, drops ou entidades.

## Decisão

Separar `SkillDefinition` declarativa de `SkillImplementation` opcional. O registry resolve por `SkillId`. Ações comuns usam regras data-driven; mecânicas especiais registram handlers pequenos e isolados.

## Consequências

* Mining e Woodcutting podem provar extensibilidade sem duplicar o pipeline;
* admins adicionam blocos via tags/data pack sem recompilar;
* mecânica complexa continua type-safe e respeita proteção;
* parser não vira uma DSL Turing-complete;
* uma skill nova pode exigir apenas definição e adapter/implementation quando o hook for novo.

## Proibições

Não copiar classes/algoritmos do mcMMO, não colocar `switch(skillId)` nos serviços centrais e não permitir que uma definição bypassa a pipeline de XP.
