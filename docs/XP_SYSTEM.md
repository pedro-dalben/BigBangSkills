# Sistema de XP

## Regra central

Nenhuma skill escreve diretamente no estado do jogador. Todo ganho vira um `XpRequest` e passa pelo pipeline único:

```text
Minecraft event
  -> adapter normaliza DomainAction
  -> validation
  -> eligibility / protection / anti-exploit
  -> base XP
  -> modifiers
  -> persistence commit
  -> state transition
  -> level/unlock/power events
  -> notification/reward
```

O ganho só é confirmado ao jogador após o commit assíncrono. Isso evita anunciar XP que desaparece se o banco falhar.

## `XpRequest`

```text
requestId: UUID
playerId: UUID
skillId: SkillId
baseAmount: Decimal >= 0
source: XpSource
reason: XpReason
scope: ProgressionScope
context: immutable action snapshot
```

Fontes iniciais:

```text
BLOCK_BREAK, BLOCK_PLACE, ENTITY_KILL, ENTITY_DAMAGE,
FISHING, HARVEST, CRAFT, SMELT, REPAIR, EXPLORATION,
CUSTOM, ADMIN, INTEGRATION
```

Reasons explicam o evento (`natural_ore_break`, `player_kill`, `admin_grant` etc.) e não substituem a fonte.

## Ordem e composição dos modifiers

O resultado é determinístico e auditável:

```text
amount = baseAmount
amount *= skillMultiplier
amount *= globalMultiplier
amount *= serverMultiplier
amount *= worldMultiplier
amount *= permissionMultiplier
amount *= temporaryMultiplier
amount *= eventMultiplier
amount *= partyMultiplier
amount = roundDownToConfiguredPrecision(amount)
amount = clamp(0, perActionCap)
```

Multipliers da mesma camada compõem por produto apenas se a configuração disser isso. No padrão, um multiplier de evento e um multiplier VIP multiplicam; um override administrativo explícito substitui o resultado. Cada modificador possui ID, prioridade, origem e explicação no `XpDecision`.

Exemplo sem valores de balanceamento finais:

```text
100 × 1.00 × 1.10 × 2.00 × 1.20 = 264
```

Não fixar números de XP nesta fase; eles pertencem às definições de gameplay e precisam de testes de curva/balanceamento.

## Curvas

```java
interface XpCurve {
    Decimal xpToNextLevel(int level);
    Decimal totalXpForLevel(int level);
    int levelAt(Decimal totalXp, int maxLevel);
}
```

Implementações previstas:

* `LINEAR`: incremento configurável;
* `EXPONENTIAL`: base e expoente configuráveis;
* `POLYNOMIAL`: parâmetros validados;
* `CUSTOM_TABLE`: tabela finita, com política para além do último item.

As funções devem tratar nível 0, cap, valores exatos no limite, XP fracionário e overflow sem loops por cada ponto de XP. `totalXpForLevel(level)` é a fonte para conversão de comando/admin e migrações.

Perguntas que toda implementação deve responder:

```text
xpToNextLevel(0)
xpToNextLevel(50)
xpToNextLevel(100)
totalXpForLevel(100)
levelAt(totalXpForLevel(100) - epsilon)
levelAt(totalXpForLevel(100))
```

Não haverá uma fórmula “especial” por skill. A definição escolhe a curva e seus parâmetros.

## Transição de nível

1. O estado carregado contém `total_xp`.
2. `levelAt` calcula o novo nível limitado ao cap.
3. O serviço emite um evento por nível atravessado ou um evento agregado com todos os marcos, conforme configuração.
4. Unlocks são resolvidos pela definição e registrados de forma idempotente.
5. Power level é recalculado somente quando uma skill muda.
6. Notifications/rewards recebem eventos internos, nunca o callback do loader.

XP acima do cap não é descartado silenciosamente: o estado mantém total XP somente até a política definida (`cap_total` ou `overflow_buffer`). A primeira versão usará `cap_total` e mostrará “nível máximo”; a decisão pode mudar antes de persistência de produção.

## Power level

```java
interface PowerLevelCalculator {
    int calculate(Collection<SkillState> skills, PowerLevelPolicy policy);
}
```

Políticas previstas:

* soma dos níveis habilitados;
* soma ponderada por categoria;
* média normalizada;
* fórmula customizada registrada.

A primeira política será soma dos níveis habilitados, com testes que excluem skills sem permissão/fora do escopo. Não congelar a fórmula em schema.

## Ações Mining/Woodcutting

`fabric` e `neoforge` traduzem a quebra normal de bloco para uma ação com:

```text
player UUID
block registry ID
world/dimension ID
position snapshot
tool/action metadata
event cancellation/protection result
automation/fake-player flags
```

Mining aceita blocos mineable/pickaxe quando o jogador usa uma picareta, além de reconhecer minérios pela tag `#bigbangskills:mining_ores`; o valor base é 1 XP e minério recebe 2 XP nesta fase. Woodcutting consulta `#bigbangskills:woodcutting_logs`; os bonus drops consultam também `config/bigbangskills/skills/woodcutting-drops.properties`. Nenhum dos dois adapters conhece o `SkillState` internamente.

## Abilities e rewards

Abilities passivas são avaliadas dentro do resultado da ação. Ativas seguem:

```text
trigger -> requirements -> permission -> cooldown -> protection check
        -> activate -> duration/timestamp -> expire
```

`CooldownService` mantém `availableAt` por jogador/ability e consulta timestamps; não cria task por jogador. Estado que precisa sobreviver restart entra no schema apenas quando o requisito for explícito; cooldown transitório desaparece no restart.

Rewards escutam `SkillLevelUpEvent`/`AbilityUnlockEvent`, são idempotentes por `(player, unlock, definitionVersion)` e não ficam dentro do parser de skill.

## Eventos internos

```text
SkillXpGainEvent
SkillLevelUpEvent
SkillAbilityUnlockEvent
SkillAbilityActivateEvent
SkillAbilityExpireEvent
PlayerPowerLevelChangeEvent
```

Eventos carregam estado anterior/novo, scope, requestId e origem. Eventos de domínio são internos; uma API pública pode expor cópias estáveis e não canceláveis depois da Fase 1.

## Testes obrigatórios do domínio

* curvas nos níveis 0, 1, 50 e 100;
* total XP e inversão `levelAt`;
* limite e valores exatos;
* composição e arredondamento de modifiers;
* XP rejeitado por mundo, permissão e anti-exploit;
* múltiplos níveis em um único ganho;
* unlock idempotente;
* cooldown antes, no limite e depois do timestamp;
* power level em políticas diferentes;
* serialização dos objetos e requestId repetido.

Não adicionar framework de testes customizado: JUnit/Gradle existente será suficiente quando o projeto for bootstrapado.
