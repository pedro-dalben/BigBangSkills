# Gameplay validation

Data: 2026-08-10  
Branch: `feat/bigbangskills-base`

| Test | Loader | Environment | Expected | Observed | Result | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| First login/profile | Fabric | Dedicated server local + Minecraft 1.21.1 client | Join without errors; `/skills` responds | Player joined and profile response rendered | PASS | Confirmado manualmente pelo usuário |
| `/skills` overview | Fabric | Same session | Mining/Woodcutting and power shown | Response showed BigBangSkills, power level and both skills | PASS | Core response is server-generated |
| `/skills mining` | Fabric | Same session | Level/current/next/total XP shown | Mining detail rendered | PASS | Confirmado manualmente pelo usuário |
| Woodcutting block break | Fabric | Same session | XP changes once | Woodcutting XP feedback appeared; no duplicate domain event observed | PASS | Natural log flow |
| Placed block anti-exploit | Fabric | Same session | Placed ore/log gives 0 XP | User confirmed expected result | PASS | Persistent restart variant still pending |
| Vanilla client without mod | Fabric | Dedicated server | Client connects and uses gameplay | Not yet separately executed | PENDENTE | Required by server-side-only architecture |
| Logout/restart persistence | Fabric | Dedicated server | XP survives logout and restart | Not yet separately executed | PENDENTE | Automated persistence tests pass |
| First login/profile | NeoForge | Dedicated server local | Join without errors; profile ready | Not yet executed with player | PENDENTE | Build/boot validated only |
| Mining/Woodcutting/anti-exploit | NeoForge | Dedicated server local | Same domain behavior as Fabric | Not yet executed with player | PENDENTE | Do not infer from boot |

## Evidence boundary

`PASS` de gameplay acima é baseado na sessão manual confirmada pelo usuário e nos logs locais do dedicated server. O cliente utilizado para essa sessão foi o cliente de desenvolvimento do Loom; isso não altera a regra de que BigBangSkills Core não pode exigir instalação no cliente. A validação vanilla-client, logout/restart e a repetição NeoForge continuam gates reais antes de produção.
