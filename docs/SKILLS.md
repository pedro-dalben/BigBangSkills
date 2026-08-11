# Mapa de skills

## Critério

O nome da skill descreve a fantasia/gameplay, não copia classes, nomes de abilities ou implementação do mcMMO. Cada skill é uma definição namespaced e pode combinar fontes de XP, passivas, abilities e rewards.

## Primeiro vertical slice (histórico)

| Skill | ID | Ação inicial | Dados | Código especial inicial |
| --- | --- | --- | --- | --- |
| Mining | `bigbangskills:mining` | quebra de blocos `mineable/pickaxe` com picareta; minérios também entram em `#bigbangskills:mining_ores` | XP base por bloco, bônus simples para minério, cap, curva, mundos, permissões | nenhum bônus de drop ainda; apenas pipeline, persistência e feedback |
| Woodcutting | `bigbangskills:woodcutting` | quebra de blocos na tag `#bigbangskills:woodcutting_logs` | XP por registry/tag, cap, curva, mundos, permissões | Harvest Lumber, Clean Cuts, Tree Feller, Leaf Blower e Knock on Wood |

A segunda skill é obrigatória para provar que adicionar conteúdo não exige alterar serviços centrais ou um grande `switch`.

## Modelo mínimo de definição

```json
{
  "id": "bigbangskills:mining",
  "enabled": true,
  "name_key": "bigbangskills.skill.mining.name",
  "description_key": "bigbangskills.skill.mining.description",
  "category": "gathering",
  "max_level": 100,
  "curve": { "id": "linear", "base": 100, "step": 25 },
  "actions": {
    "block_break": {
      "tag": "bigbangskills:mining_ores",
      "xp": 10
    }
  },
  "world_rules": { "default_enabled": true, "multiplier": 1.0 },
  "permissions": ["bigbangskills.skill.mining"]
}
```

Os valores são exemplo de schema, não balanceamento aprovado. O parser valida IDs, curva, cap, tags, permissions e limites.

## Skills planejadas

### Gathering e mundo

`Mining`, `Woodcutting`, `Excavation`, `Herbalism`, `Fishing`, `Exploration`, `Building`, `Adventuring`.

### Combate

`Swords`, `Axes`, `Archery`, `Unarmed`, `Crossbows`, `Tridents`, `Hunting`.

### Produção e utilidade

`Repair`, `Salvage`, `Smelting`, `Alchemy`, `Cooking`, `Engineering`.

### Companheiros e Cobblemon

`Taming` é planejada no core. `Trainer`, `Capture`, `Battling`, `Breeding`, `Research`, `Pokédex` e `Raids` ficam em integração opcional depois de estudar a API/eventos Cobblemon 1.21.1.

## Ability roadmap

1. framework de passivas sem efeitos de mundo perigosos;
2. unlock por level e permission;
3. cooldown por timestamp;
4. active ability com duração e expiração;
5. efeitos de Mining/Woodcutting somente após hook de proteção testado;
6. integração de rewards idempotentes.

O roadmap acima é histórico da primeira versão. A implementação atual expandiu o escopo para a paridade mcMMO documentada em [FULL_SKILL_PARITY_AUDIT.md](audits/FULL_SKILL_PARITY_AUDIT.md); novas habilidades devem seguir essa auditoria e manter as lacunas explicitamente registradas.

## Configuração de blocos/mods

Vanilla e modded blocks entram por registry ID e tags. Administradores podem adicionar `create:*`, `cobblemon:*` ou outro namespace via data pack/config validada. Nenhum bloco de mod será assumido como vanilla no código.

## Integrações futuras

* Cobblemon opcional, iniciado apenas após documentação da API oficial;
* BigBangEssentials via API/eventos, sem banco compartilhado direto;
* BigBangRegions via proteção normal e hooks opcionais;
* permissions via `PermissionService`, sem dependência obrigatória de LuckPerms;
* quests, ranks, crates, Discord e sites via eventos públicos.
