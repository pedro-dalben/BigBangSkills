# ADR-002 — XP total derivado, ledger idempotente e cache assíncrono

* Status: aceito para a Fase 0
* Data: 2026-08-10

## Contexto

XP chega por eventos frequentes, a rede pode ter vários servidores e a mesma conta não pode perder uma atualização por “última gravação vence”. SQL não pode rodar na game thread.

## Decisão

Persistir `total_xp`, `revision`, a versão da definição e o escopo. Derivar nível da curva. Cada lote recebe evento idempotente no `xp_ledger`; uma transação aplica o ledger e o delta no agregado. Usar cache de sessão e workers assíncronos.

Para progressão `NETWORK`, usar lease de sessão por jogador. A atualização atômica/ledger continua necessária para tolerar concorrência e retry; o lease reduz split-brain durante troca de servidor.

## Consequências

* não existem level/XP incompatíveis persistidos;
* retry não duplica XP;
* leaderboard pode usar índice no agregado sem somar eventos a cada consulta;
* mudanças de curva exigem política de migração explícita;
* lease expira após crash e pode causar breve indisponibilidade durante a recuperação;
* o modo fail-closed pode adiar XP quando o banco está indisponível.

## Alternativas rejeitadas

`UPDATE total_xp = valor_do_cache` perde atualizações concorrentes. Persistir level + XP atual multiplica estados inconsistentes. Uma query por bloco é proibida por performance.
